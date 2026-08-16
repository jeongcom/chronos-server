package com.chronos.api.device;

import com.chronos.application.device.DeviceRegistryUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceRegistryUseCase devices;
    public DeviceController(DeviceRegistryUseCase devices) { this.devices = devices; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceRegistryUseCase.RegisteredDevice register(@Valid @RequestBody RegisterRequest r) {
        return devices.register(new DeviceRegistryUseCase.RegisterDevice(r.deviceId(), r.spaceId(), r.deviceType(),
                r.manufacturer(), r.model(), r.protocol(), r.protocolVersion()));
    }

    @GetMapping("/{deviceId}")
    public DeviceRegistryUseCase.DeviceView get(@PathVariable String deviceId) {
        return devices.find(deviceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public List<DeviceRegistryUseCase.DeviceView> list() { return devices.list(); }

    public record RegisterRequest(@NotBlank String deviceId, @NotBlank String spaceId, @NotBlank String deviceType,
                                  String manufacturer, String model, String protocol, String protocolVersion) {}
}
