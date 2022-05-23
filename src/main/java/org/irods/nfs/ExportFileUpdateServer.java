package org.irods.nfs;

import org.dcache.nfs.ExportFile;

import com.google.common.base.Preconditions;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ExportFileUpdateServer {
	
	private int port;
	private ExportFile exportFile;
	
	public ExportFileUpdateServer(int port, ExportFile exportFile) {
		Preconditions.checkArgument(port > 1023);
		this.port = port;
		this.exportFile = Preconditions.checkNotNull(exportFile);
	}
	
	public void run() throws Exception {
		var bossGroup = new NioEventLoopGroup();
		var workerGroup = new NioEventLoopGroup();
		
		try {
			var bootstrap = new ServerBootstrap()
				.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new ExportFileUpdateHandler(exportFile));
					}
				})
				.option(ChannelOption.SO_BACKLOG, 1)
				.childOption(ChannelOption.SO_KEEPALIVE, true);
			
			// Bind and start to accept incoming connections.
			var future = bootstrap.bind(port).sync();
			
			// Wait until the server socket is closed.
			// Here, this does not happen, but we can do that to gracefully
			// shut down the server.
			future.channel().closeFuture().sync();
		}
		finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

}
