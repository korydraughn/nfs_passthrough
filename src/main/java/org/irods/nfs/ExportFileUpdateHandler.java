package org.irods.nfs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ExportFileUpdateHandler extends ChannelInboundHandlerAdapter
{
    private static final Logger log = LoggerFactory.getLogger(ExportFileUpdateServer.class);

    private String exportFilePath;
    private DynamicExportFile exportFile;
    private Map<Integer, Function<String, ApiResponse>> operations = new HashMap<>();
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public ExportFileUpdateHandler(String exportFilePath, DynamicExportFile exportFile)
    {
        this.exportFilePath = Preconditions.checkNotNull(exportFilePath);
        this.exportFile = Preconditions.checkNotNull(exportFile);
        initApiOperationsTable();
    }

    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws JsonProcessingException
    {
        final var request = (ApiRequest) msg;

        log.info("Received API request: {}", Common.objectMapper.writeValueAsString(request));

        final var op = operations.get(request.opCode);

        if (op == null) {
            ctx.writeAndFlush(API_RESPONSE_INVALID_API_NUMBER);
            return;
        }

        ctx.writeAndFlush(op.apply(request.payload));
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        cause.printStackTrace();
        ctx.close();
    }

    private void initApiOperationsTable()
    {
        // clang-format off
        operations.put(ApiOpCode.SET_EXPORT_FILE_DATA, this::setExportFileData);
        operations.put(ApiOpCode.READ_EXPORT_FILE,     this::readExportFile);
        // clang-format on
    }

    private ApiResponse setExportFileData(String data)
    {
        if (data == null || data.isEmpty()) {
            return API_RESPONSE_NULL_OR_EMPTY_STRING;
        }

        // This guards against simultaneous update requests.
        // If an update is already in progress, return immediately.
        if (!rwLock.writeLock().tryLock()) {
            return API_RESPONSE_UPDATE_IN_PROGRESS;
        }

        try {
            log.info("Executing SET_EXPORT_FILE_DATA operation ...");

            final var tempExportFilePath = Paths.get("/tmp/new_export_file.nfs_passthrough");

            Files.writeString(tempExportFilePath,
                              data,
                              StandardOpenOption.CREATE,
                              StandardOpenOption.WRITE,
                              StandardOpenOption.TRUNCATE_EXISTING);

            // Do not allow empty export files!
            //
            // Load a temporary export file object with the new information.
            // If the temp object does not contain any entries, we know there
            // was a problem with the new export information.

            final var tempExportFile = new DynamicExportFile(new File(tempExportFilePath.toString()));

            if (tempExportFile.exports().count() == 0) {
                return API_RESPONSE_NO_VALID_EXPORT_ENTRIES;
            }

            // The export file data is good.
            // Make a backup of the original file and then move the temp
            // file over it (i.e. overwrite the original).

            final var fromPath = Paths.get(exportFilePath);
            final var toPath = Paths.get(exportFilePath + ".backup_" + Instant.now().getEpochSecond());

            Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);

            // TODO Does moving/overwriting the file in this way affect the
            // ExportFile object?
            //
            // For instance, perhaps the ExportFile object contains an open
            // file descriptor that is invalidated by the following operation.
            Files.move(tempExportFilePath, fromPath, StandardCopyOption.REPLACE_EXISTING);

            exportFile.rescan();
            printExportFileContents();

            if (!exportFile.getMalformedExportEntries().isEmpty()) {
                return new ApiResponse(EC_PARTIAL_UPDATE_SUCCESS, exportFile.getMalformedExportEntries().toString());
            }

            return API_RESPONSE_SUCCESS;
        }
        catch (IOException e) {
            log.error(e.getMessage());
        }
        finally {
            rwLock.writeLock().unlock();
            log.info("Operation completed.");
        }

        return API_RESPONSE_ERROR;
    }

    private ApiResponse readExportFile(String data)
    {
        rwLock.readLock().lock();

        try {
            log.info("Executing READ_EXPORT_FILE operation ...");
            final var contents = exportFile.exports().collect(Collectors.toList());
            return new ApiResponse(0, contents.toString());
        }
        finally {
            rwLock.readLock().unlock();
            log.info("Operation completed.");
        }
    }

    private void printExportFileContents()
    {
        final var contents = exportFile.exports().collect(Collectors.toList());
        log.info("Current export file: {}", contents);
    }

    // clang-format off
    private static final int EC_GENERAL_ERROR           = -1000;
    private static final int EC_INVALID_API_NUMBER      = -1001;
    private static final int EC_UPDATE_IN_PROGRESS      = -1002;
    private static final int EC_NULL_OR_EMPTY_STRING	= -1003;
    private static final int EC_NO_VALID_EXPORT_ENTRIES = -1004;
    private static final int EC_PARTIAL_UPDATE_SUCCESS  = -1005;

    private static final ApiResponse API_RESPONSE_SUCCESS                   = new ApiResponse();
    private static final ApiResponse API_RESPONSE_ERROR                     = new ApiResponse(EC_GENERAL_ERROR,           "Error while processing request.");
    private static final ApiResponse API_RESPONSE_INVALID_API_NUMBER        = new ApiResponse(EC_INVALID_API_NUMBER,      "Invalid API number.");
    private static final ApiResponse API_RESPONSE_UPDATE_IN_PROGRESS        = new ApiResponse(EC_UPDATE_IN_PROGRESS,      "Update already in progress.");
    private static final ApiResponse API_RESPONSE_NO_VALID_EXPORT_ENTRIES   = new ApiResponse(EC_NO_VALID_EXPORT_ENTRIES, "No valid export entries.");
    private static final ApiResponse API_RESPONSE_NULL_OR_EMPTY_STRING      = new ApiResponse(EC_NULL_OR_EMPTY_STRING,    "Invalid input: Null or empty string");
    // clang-format on
}
