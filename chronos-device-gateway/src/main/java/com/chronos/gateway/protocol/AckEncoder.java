package com.chronos.gateway.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class AckEncoder extends MessageToByteEncoder<DeviceAck> {
    @Override
    protected void encode(ChannelHandlerContext ctx, DeviceAck ack, ByteBuf out) {
        out.writeShort(ChronosDeviceProtocol.MAGIC);
        out.writeByte(ChronosDeviceProtocol.VERSION_1);
        out.writeByte(DeviceMessageType.ACK.code());
        out.writeInt(ChronosDeviceProtocol.ACK_FRAME_BYTES);
        out.writeLong(ack.sequence());
        out.writeInt(ack.statusCode());
        out.writeLong(ack.eventSeq());
    }
}
