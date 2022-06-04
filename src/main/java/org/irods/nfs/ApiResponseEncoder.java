package org.irods.nfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ApiResponseEncoder extends MessageToByteEncoder<ApiResponse>
{
    private static final Logger log = LoggerFactory.getLogger(ApiResponseEncoder.class);

    @Override protected void encode(ChannelHandlerContext ctx, ApiResponse msg, ByteBuf out) throws Exception
    {
        log.info("Encoding response ...");

        final var jsonString = Common.objectMapper.writeValueAsString(msg);
        log.info("response = {}", jsonString);

        out.writeInt(jsonString.length());
        out.writeBytes(jsonString.getBytes());
    }
}
