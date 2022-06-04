package org.irods.nfs;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.dcache.nfs.ExportFile;
import org.dcache.nfs.v4.MDSOperationExecutor;
import org.dcache.nfs.v4.NFSServerV41;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class ServerMain implements Callable<Integer>
{
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);

    OncRpcSvc nfsSvc;
    ExportFileUpdateServer exportFileServer;

    @Parameters(index = "0", paramLabel = "EXPORT_FILE", description = "The path to the NFS exports file.")
    private String exportFilePath;

    @Option(names = {"-p", "--port"},
            defaultValue = "2049",
            paramLabel = "INTEGER",
            description = "The port number to listen on for NFS operations. Defaults to ${DEFAULT-VALUE}.")
    private int nfsPortNumber;

    @Option(names = {"--export-server-port"},
            defaultValue = "9402",
            paramLabel = "INTEGER",
            description = "The port number to listen on for export file operations. Defaults to ${DEFAULT-VALUE}.")
    private int exportServerPortNumber;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Displays help message.")
    private boolean helpRequested;

    @Override public Integer call() throws Exception
    {
        addShutdownHookForLogger();

        // clang-format off
        nfsSvc = new OncRpcSvcBuilder()
            .withPort(nfsPortNumber)
            .withTCP()
            .withAutoPublish()
            .withWorkerThreadIoStrategy()
            .build();
        // clang-format on

        final var exportFile = new ExportFile(new File(exportFilePath));
        exportFileServer = new ExportFileUpdateServer(exportServerPortNumber, exportFilePath, exportFile);

        try {
            exportFileServer.run();

            // clang-format off
            final var nfs4 = new NFSServerV41.Builder()
                .withExportTable(exportFile)
                .withVfs(new VfsPassthrough())
                .withOperationExecutor(new MDSOperationExecutor())
                .build();
            // clang-format on

            nfsSvc.register(new OncRpcProgram(100003, 4), nfs4);

            nfsSvc.start();

            System.in.read();
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }

        return 0;
    }

    private void addShutdownHookForLogger()
    {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run()
            {
                log.info("Shutting down server.");

                try {
                    if (nfsSvc != null) {
                        nfsSvc.stop();
                    }
                }
                catch (Exception e) {
                }

                if (exportFileServer != null) {
                    exportFileServer.shutdown();
                }

                LogManager.shutdown();
            }
        });
    }

    public static void main(String[] args)
    {
        System.exit(new CommandLine(new ServerMain()).execute(args));
    }
}
