package org.irods.nfs;

import org.dcache.nfs.ExportFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ExportFileUpdateServer
{
    private static final Logger log = LoggerFactory.getLogger(ExportFileUpdateServer.class);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    //    private ChannelFuture shutdownFuture;
    private int port;
    private String exportFilePath;
    private ExportFile exportFile;

    public ExportFileUpdateServer(int port, String exportFilePath, ExportFile exportFile)
    {
        Preconditions.checkArgument(port > 1023);
        this.port = port;
        this.exportFilePath = Preconditions.checkNotNull(exportFilePath);
        this.exportFile = Preconditions.checkNotNull(exportFile);
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
    }

    public void run() throws Exception
    {
        log.info("Starting export file update server ...");

        // clang-format off
        final var bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    final var pipeline = ch.pipeline();
                    pipeline.addLast(new ApiRequestDecoder());
                    pipeline.addLast(new ApiResponseEncoder());
                    pipeline.addLast(new ExportFileUpdateHandler(exportFilePath, exportFile));
                }
            })
            .option(ChannelOption.SO_BACKLOG, 1)
            .childOption(ChannelOption.SO_KEEPALIVE, true);
        // clang-format on

        // Bind and start to accept incoming connections.
        //        shutdownFuture = bootstrap.bind(port); //.sync();
        bootstrap.bind(port); //.sync();

        log.info("Export file update server is ready.");
    }

    public void shutdown()
    {
        log.info("Shutting down export file update server ...");

        //        try {
        //            // Wait until the server socket is closed.
        //        	// TODO Is this actually needed?
        //			shutdownFuture.channel().closeFuture().sync();
        //		}
        //        catch (InterruptedException e) {
        //			e.printStackTrace();
        //		}

        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();

        log.info("Export file update server has been shut down.");
    }

    public static void main(String[] args) throws Exception
    {
        new ExportFileUpdateServer(9010, null, null).run();
    }
}
