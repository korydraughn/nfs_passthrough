package org.irods.nfs;

import java.io.File;
import java.util.concurrent.Callable;

import org.dcache.nfs.ExportFile;
import org.dcache.nfs.v4.MDSOperationExecutor;
import org.dcache.nfs.v4.NFSServerV41;
import org.dcache.nfs.v4.NFSv4StateHandler;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class ServerMain implements Callable<Integer> {
	
	@Parameters(index = "0",
				paramLabel = "EXPORTS_FILE",
				description = "The path to the NFS exports file.")
	private String exportsFile;
	
	@Option(names = {"-p", "--port"},
			defaultValue = "2049",
			paramLabel = "NUMBER",
			description = "The port number to listen on. Defaults to ${DEFAULT-VALUE}")
	private int portNumber;
	
	@Option(names = {"--user-mapping-file"},
			defaultValue = "config/ip_to_user_mapping.json",
			paramLabel = "FILE",
			description = "The path to a JSON file containing mappings from IPs to usernames. Defaults to ${DEFAULT-VALUE}")
	private String ipToUserMappingFilePath;

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
		
		final var nfsStateHandler = new NFSv4StateHandler();
		final var vfs = new VfsPassthrough(nfsStateHandler, ipToUserMappingFilePath);
		
		final var nfs4 = new NFSServerV41.Builder()
			.withExportTable(new ExportFile(new File(exportsFile)))
			.withStateHandler(nfsStateHandler)
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
