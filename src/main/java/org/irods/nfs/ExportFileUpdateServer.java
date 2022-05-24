package org.irods.nfs;

import org.dcache.nfs.ExportFile;

import com.google.common.base.Preconditions;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ExportFileUpdateServer
{
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture shutdownFuture;
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
        shutdownFuture = bootstrap.bind(port); //.sync();
    }

    public void shutdown()
    {
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
    }

    public static void main(String[] args) throws Exception
    {
        new ExportFileUpdateServer(9010, null, null).run();
    }
}
