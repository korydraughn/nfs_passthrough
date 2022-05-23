package org.irods.nfs;

import org.dcache.nfs.ExportFile;

import com.google.common.base.Preconditions;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class ExportFileUpdateHandler extends ChannelInboundHandlerAdapter {
	
	private ExportFile exportFile;
	
	public ExportFileUpdateHandler(ExportFile exportFile) {
		this.exportFile = Preconditions.checkNotNull(exportFile);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		try {
			// TODO Handle incoming API request.
			// 
		}
		finally {
			ReferenceCountUtil.release(msg);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}
