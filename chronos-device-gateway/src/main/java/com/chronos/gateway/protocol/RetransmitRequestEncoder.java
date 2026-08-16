package com.chronos.gateway.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class RetransmitRequestEncoder extends MessageToByteEncoder<RetransmitRequest> {
    @Override protected void encode(ChannelHandlerContext ctx, RetransmitRequest r, ByteBuf out) {
        out.writeShort(ChronosDeviceProtocol.MAGIC);
        out.writeByte(ChronosDeviceProtocol.VERSION_1);
        out.writeByte(DeviceMessageType.RETRANSMIT_REQUEST.code());
        out.writeInt(24);
        out.writeLong(r.fromSequence());
        out.writeLong(r.toSequence());
    }
}
