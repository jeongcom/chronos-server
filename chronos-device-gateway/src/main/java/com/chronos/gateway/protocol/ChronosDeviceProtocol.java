package com.chronos.gateway.protocol;

public final class ChronosDeviceProtocol {
    private ChronosDeviceProtocol() {}

    public static final short MAGIC = (short) 0x4348; // 'C''H'
    public static final int HEADER_BYTES = 28;
    public static final int VERSION_1 = 1;
    public static final int ACK_FRAME_BYTES = 28;
}
