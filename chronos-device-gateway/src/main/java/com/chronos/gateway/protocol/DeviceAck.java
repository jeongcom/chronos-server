package com.chronos.gateway.protocol;

public record DeviceAck(long sequence, int statusCode, long eventSeq) {
    public static final int ACCEPTED = 0;
    public static final int DUPLICATE = 1;
    public static final int INVALID = 2;
    public static final int REJECTED = 3;
}
