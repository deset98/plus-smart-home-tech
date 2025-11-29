package ru.yandex.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.mapper.HubEventMapperProto;
import ru.yandex.practicum.mapper.SensorEventMapperProto;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;
import ru.yandex.practicum.service.CollectorService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class CollectorController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final CollectorService collectorService;
    private final HubEventMapperProto hubEventMapperProto;
    private final SensorEventMapperProto sensorEventMapperProto;

    @Override
    public void collectHubEvent(HubEventProto hubEventProto, StreamObserver<Empty> responseObserver) {
        try {
            HubEvent hubEvent = hubEventMapperProto.toJava(hubEventProto);

            if (hubEvent.getHubId() == null || hubEvent.getHubId().isEmpty()) {
                log.warn("Received HubEvent with empty hubId: {}", hubEvent);
                responseObserver.onError(
                        new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("hubId is missing"))
                );
                return;
            }

            collectorService.sendHubEvent(hubEvent);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error collecting HubEvent", e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.INTERNAL.withDescription(e.getMessage()).withCause(e))
            );
        }
    }

    @Override
    public void collectSensorEvent(SensorEventProto sensorEventProto, StreamObserver<Empty> responseObserver) {
        try {
            SensorEvent sensorEvent = sensorEventMapperProto.toJava(sensorEventProto);

            if (sensorEvent.getHubId() == null || sensorEvent.getHubId().isEmpty()) {
                log.warn("Received SensorEvent with empty hubId: {}", sensorEvent);
                responseObserver.onError(
                        new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("hubId is missing"))
                );
                return;
            }

            collectorService.sendSensorEvent(sensorEvent);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error collecting SensorEvent", e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.INTERNAL.withDescription(e.getMessage()).withCause(e))
            );
        }
    }
}
