package org.irods.nfs;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.CharsetUtil;

public class ApiRequestDecoder extends ReplayingDecoder<ApiRequest>
{
    private static final Logger log = LoggerFactory.getLogger(ApiRequestDecoder.class);

    @Override protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        final var msgLength = in.readInt();
        log.debug("msgLength = {}", msgLength);
        checkpoint();

        final var msg = in.readBytes(msgLength);
        checkpoint();

        final var jsonString = msg.toString(CharsetUtil.UTF_8);
        log.debug("jsonString = {}", jsonString);

        out.add(Common.objectMapper.readValue(jsonString, ApiRequest.class));
    }
}
