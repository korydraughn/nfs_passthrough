package org.irods.nfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dcache.nfs.ExportFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ExportFileUpdateHandler extends ChannelInboundHandlerAdapter {
	
	private String exportFilePath;
	private ExportFile exportFile;
	private Map<Integer, Function<ApiRequest, ApiResponse>> operations;
	
	public ExportFileUpdateHandler(String exportFilePath, ExportFile exportFile) {
		this.exportFilePath = Preconditions.checkNotNull(exportFilePath);
		this.exportFile = Preconditions.checkNotNull(exportFile);
		initApiOperations();
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws JsonProcessingException {
        final var request = (ApiRequest) msg;
        
        System.out.println("Received API request: " + Common.objectMapper.writeValueAsString(request));
        
        final var op = operations.get(request.opCode);

        if (op == null) {
        	ctx.writeAndFlush(API_RESPONSE_ERROR);
        	return;
        }
        
        ctx.writeAndFlush(op.apply(request));
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
	
	private void initApiOperations() {
		operations = new HashMap<>();
		
		operations.put(ApiOpCode.SET_EXPORT_FILE_DATA, req -> {
			try {
                System.out.println("Executing SET_EXPORT_FILE_DATA operation ...");

                final var tempExportFilePath = Paths.get("/tmp/new_export_file");

                Files.writeString(tempExportFilePath, req.payload,
                				  StandardOpenOption.CREATE,
                				  StandardOpenOption.WRITE,
                				  StandardOpenOption.TRUNCATE_EXISTING);

                Files.move(tempExportFilePath,
                		   Paths.get(this.exportFilePath),
                		   StandardCopyOption.REPLACE_EXISTING);

                exportFile.rescan();
                printExportFileContents();

                return API_RESPONSE_SUCCESS;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			finally {
                System.out.println("Operation completed.");
			}

            return API_RESPONSE_ERROR;
		});
		
		operations.put(ApiOpCode.READ_EXPORT_FILE, req -> {
			try {
                System.out.println("Executing READ_EXPORT_FILE operation ...");
                final var contents = exportFile.exports().collect(Collectors.toList());
                return new ApiResponse(0, contents.toString());
			}
			finally {
                System.out.println("Operation completed.");
			}
		});
	}
	
	private void printExportFileContents() {
        final var contents = exportFile.exports().collect(Collectors.toList());
        System.out.println("Current export file:");
        System.out.println(contents);
	}
	
	private static final ApiResponse API_RESPONSE_SUCCESS = new ApiResponse();
	private static final ApiResponse API_RESPONSE_ERROR   = new ApiResponse(-1, "Invalid Operation");

}
