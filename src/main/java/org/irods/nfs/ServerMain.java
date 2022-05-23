package org.irods.nfs;

import java.io.File;
import java.util.concurrent.Callable;

import org.dcache.nfs.ExportFile;
import org.dcache.nfs.v4.MDSOperationExecutor;
import org.dcache.nfs.v4.NFSServerV41;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class ServerMain implements Callable<Integer> {
	
	@Parameters(index = "0",
			    paramLabel = "EXPORT_FILE",
			    description = "The path to the NFS exports file.")
	private String exportFilePath;

	@Option(names = {"-p", "--port"},
			defaultValue = "2049",
			paramLabel = "INTEGER",
			description = "The port number to listen on. Defaults to ${DEFAULT-VALUE}.")
	private int portNumber;

	@Option(names = {"-h", "--help"},
			usageHelp = true,
			description = "Displays help message.")
	private boolean helpRequested;

	@Override
	public Integer call() throws Exception {
		// @formatter:off
		final var nfsSvc = new OncRpcSvcBuilder()
			.withPort(portNumber)
			.withTCP()
			.withAutoPublish()
			.withWorkerThreadIoStrategy()
			.build();
		
		final var exportFile = new ExportFile(new File(exportFilePath));
		final var vfs = new VfsPassthrough();
		
		final var nfs4 = new NFSServerV41.Builder()
			.withExportTable(exportFile)
			.withVfs(vfs)
			.withOperationExecutor(new MDSOperationExecutor())
			.build();
		// @formatter:on
		
		nfsSvc.register(new OncRpcProgram(100003, 4), nfs4);

		nfsSvc.start();
			
		System.in.read();
		
		return 0;
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new ServerMain()).execute(args));
	}

}
