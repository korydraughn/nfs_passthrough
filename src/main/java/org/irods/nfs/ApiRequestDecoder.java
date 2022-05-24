package org.irods.nfs;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.CharsetUtil;

public class ApiRequestDecoder extends ReplayingDecoder<ApiRequest>
{
    @Override protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        final var msgLength = in.readInt();
        System.out.println("msgLength = " + msgLength);
        checkpoint();

        final var msg = in.readBytes(msgLength);
        checkpoint();

        final var jsonString = msg.toString(CharsetUtil.UTF_8);
        System.out.println("jsonString = " + jsonString);

        out.add(Common.objectMapper.readValue(jsonString, ApiRequest.class));
    }
}
