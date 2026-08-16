package com.chronos.gateway.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * CHRONOS Device Protocol v1 (big endian):
 * magic(2) version(1) type(1) totalLength(4) sequence(8) occurredAtEpochMs(8)
 * deviceIdLength(2) spaceIdLength(2) deviceId(N) spaceId(N) payloadJson(remaining).
 */
public final class DeviceFrameDecoder extends ByteToMessageDecoder {
    private final int maxFrameBytes;

    public DeviceFrameDecoder(int maxFrameBytes) {
        this.maxFrameBytes = maxFrameBytes;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 8) return;

        in.markReaderIndex();
        short magic = in.readShort();
        if (magic != ChronosDeviceProtocol.MAGIC) {
            in.resetReaderIndex();
            resync(in);
            return;
        }

        int version = in.readUnsignedByte();
        int typeCode = in.readUnsignedByte();
        int totalLength = in.readInt();

        if (totalLength < ChronosDeviceProtocol.HEADER_BYTES || totalLength > maxFrameBytes) {
            throw new CorruptedFrameException("Invalid totalLength=" + totalLength);
        }
        in.resetReaderIndex();
        if (in.readableBytes() < totalLength) return;

        magic = in.readShort();
        version = in.readUnsignedByte();
        typeCode = in.readUnsignedByte();
        in.readInt();
        long sequence = in.readLong();
        long occurredAtEpochMs = in.readLong();
        int deviceIdLength = in.readUnsignedShort();
        int spaceIdLength = in.readUnsignedShort();

        int variableBytes = totalLength - ChronosDeviceProtocol.HEADER_BYTES;
        if (deviceIdLength + spaceIdLength > variableBytes) {
            throw new CorruptedFrameException("Invalid string lengths");
        }

        String deviceId = in.readCharSequence(deviceIdLength, StandardCharsets.UTF_8).toString();
        String spaceId = in.readCharSequence(spaceIdLength, StandardCharsets.UTF_8).toString();
        int payloadLength = variableBytes - deviceIdLength - spaceIdLength;
        String payload = in.readCharSequence(payloadLength, StandardCharsets.UTF_8).toString();

        if (version != ChronosDeviceProtocol.VERSION_1) {
            throw new CorruptedFrameException("Unsupported protocol version=" + version);
        }
        if (deviceId.isBlank() || spaceId.isBlank()) {
            throw new CorruptedFrameException("deviceId and spaceId are required");
        }

        out.add(new DeviceFrame(
                version,
                DeviceMessageType.fromCode(typeCode),
                sequence,
                Instant.ofEpochMilli(occurredAtEpochMs),
                deviceId,
                spaceId,
                payload));
    }

    private static void resync(ByteBuf in) {
        while (in.readableBytes() >= 2) {
            if (in.getUnsignedByte(in.readerIndex()) == 0x43 &&
                in.getUnsignedByte(in.readerIndex() + 1) == 0x48) {
                return;
            }
            in.skipBytes(1);
        }
    }
}
