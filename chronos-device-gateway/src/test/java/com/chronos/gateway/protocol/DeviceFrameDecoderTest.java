package com.chronos.gateway.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DeviceFrameDecoderTest {
    @Test
    void reassemblesFragmentedTcpFrame() {
        byte[] bytes = sampleFrame();
        EmbeddedChannel channel = new EmbeddedChannel(new DeviceFrameDecoder(1024 * 1024));

        assertFalse(channel.writeInbound(Unpooled.wrappedBuffer(bytes, 0, 5)));
        assertTrue(channel.writeInbound(Unpooled.wrappedBuffer(bytes, 5, bytes.length - 5)));

        DeviceFrame frame = channel.readInbound();
        assertEquals("TEMP-001", frame.deviceId());
        assertEquals("LAB-001", frame.spaceId());
        assertEquals(42L, frame.sequence());
        assertEquals(DeviceMessageType.TEMPERATURE_CHANGED, frame.messageType());
        assertTrue(frame.payloadJson().contains("24.3"));
        channel.finishAndReleaseAll();
    }

    @Test
    void resynchronizesAfterGarbageBytes() {
        byte[] frame = sampleFrame();
        byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03};
        EmbeddedChannel channel = new EmbeddedChannel(new DeviceFrameDecoder(1024 * 1024));
        ByteBuf combined = Unpooled.buffer(garbage.length + frame.length).writeBytes(garbage).writeBytes(frame);
        channel.writeInbound(combined);
        DeviceFrame decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals(42L, decoded.sequence());
        channel.finishAndReleaseAll();
    }

    private static byte[] sampleFrame() {
        byte[] device = "TEMP-001".getBytes(StandardCharsets.UTF_8);
        byte[] space = "LAB-001".getBytes(StandardCharsets.UTF_8);
        byte[] payload = "{\"temperature\":24.3,\"unit\":\"C\"}".getBytes(StandardCharsets.UTF_8);
        int total = ChronosDeviceProtocol.HEADER_BYTES + device.length + space.length + payload.length;
        ByteBuf out = Unpooled.buffer(total);
        out.writeShort(ChronosDeviceProtocol.MAGIC);
        out.writeByte(1);
        out.writeByte(DeviceMessageType.TEMPERATURE_CHANGED.code());
        out.writeInt(total);
        out.writeLong(42L);
        out.writeLong(1786930335123L);
        out.writeShort(device.length);
        out.writeShort(space.length);
        out.writeBytes(device);
        out.writeBytes(space);
        out.writeBytes(payload);
        byte[] bytes = new byte[out.readableBytes()];
        out.readBytes(bytes);
        out.release();
        return bytes;
    }
}
