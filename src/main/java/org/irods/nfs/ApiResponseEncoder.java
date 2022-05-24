package org.irods.nfs;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ApiResponseEncoder extends MessageToByteEncoder<ApiResponse>
{
    @Override protected void encode(ChannelHandlerContext ctx, ApiResponse msg, ByteBuf out) throws Exception
    {
        System.out.println("Encoding response ...");

        final var jsonString = Common.objectMapper.writeValueAsString(msg);
        System.out.println("response = " + jsonString);

        out.writeInt(jsonString.length());
        out.writeBytes(jsonString.getBytes());
    }
}
