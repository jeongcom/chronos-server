package com.chronos.gateway.protocol;

public enum DeviceMessageType {
    TEMPERATURE_CHANGED(1, "DEVICE.TEMPERATURE.CHANGED"),
    DOOR_OPENED(2, "DOOR.OPENED"),
    DOOR_CLOSED(3, "DOOR.CLOSED"),
    LIGHT_ON(4, "LIGHT.TURNED_ON"),
    LIGHT_OFF(5, "LIGHT.TURNED_OFF"),
    HEARTBEAT(6, "DEVICE.HEARTBEAT"),
    GENERIC_JSON_EVENT(100, null),
    ACK(127, null);

    private final int code;
    private final String eventType;

    DeviceMessageType(int code, String eventType) {
        this.code = code;
        this.eventType = eventType;
    }

    public int code() { return code; }
    public String eventType() { return eventType; }

    public static DeviceMessageType fromCode(int code) {
        for (var value : values()) if (value.code == code) return value;
        throw new IllegalArgumentException("Unsupported messageType=" + code);
    }
}
