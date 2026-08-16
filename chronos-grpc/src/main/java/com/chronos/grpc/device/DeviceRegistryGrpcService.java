package com.chronos.grpc.device;

import com.chronos.application.device.DeviceRegistryUseCase;
import com.chronos.contract.v1.*;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class DeviceRegistryGrpcService extends DeviceRegistryServiceGrpc.DeviceRegistryServiceImplBase {
    private final DeviceRegistryUseCase devices;
    public DeviceRegistryGrpcService(DeviceRegistryUseCase devices) { this.devices = devices; }

    @Override public void authenticate(AuthenticateDeviceRequest r, StreamObserver<AuthenticateDeviceResponse> o) {
        var a = devices.authenticate(r.getDeviceId(), r.getSecret(), r.getGatewayId(), r.getConnectionId());
        o.onNext(AuthenticateDeviceResponse.newBuilder().setAuthenticated(a.authenticated()).setMessage(a.message())
            .setSpaceId(a.spaceId() == null ? "" : a.spaceId()).setExpectedSequence(a.expectedSequence()).build());
        o.onCompleted();
    }
    @Override public void heartbeat(DeviceHeartbeatRequest r, StreamObserver<DeviceHeartbeatResponse> o) {
        boolean ok = devices.heartbeat(r.getDeviceId(), r.getGatewayId(), r.getConnectionId(), r.getLastSequence());
        o.onNext(DeviceHeartbeatResponse.newBuilder().setAccepted(ok).build()); o.onCompleted();
    }
    @Override public void disconnect(DeviceDisconnectRequest r, StreamObserver<DeviceDisconnectResponse> o) {
        devices.offline(r.getDeviceId(), r.getGatewayId(), r.getConnectionId(), r.getReason());
        o.onNext(DeviceDisconnectResponse.newBuilder().setAccepted(true).build()); o.onCompleted();
    }
}
