package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.hub.*;
import ru.yandex.practicum.model.hub.enums.ActionType;
import ru.yandex.practicum.model.hub.enums.ConditionOperation;
import ru.yandex.practicum.model.hub.enums.ConditionType;
import ru.yandex.practicum.model.hub.enums.DeviceType;

import java.time.Instant;
import java.util.Optional;

@Component
public class HubEventMapperProto {

    public HubEvent toJava(HubEventProto hubEventProto) {
        final Instant timestamp = Optional.of(hubEventProto.getTimestamp())
                .map(ts -> Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()))
                .orElse(Instant.now());
        final String hubId = hubEventProto.getHubId();


        return switch (hubEventProto.getPayloadCase()) {
            case DEVICE_ADDED -> {
                var hubEvent = hubEventProto.getDeviceAdded();
                yield DeviceAddedEvent.builder()
                        .id(hubEvent.getId())
                        .hubId(hubId)
                        .timestamp(timestamp)
                        .deviceType(mapDeviceType(hubEvent.getType()))
                        .build();
            }
            case DEVICE_REMOVED -> {
                var hubEvent = hubEventProto.getDeviceRemoved();
                yield DeviceRemovedEvent.builder()
                        .id(hubEvent.getId())
                        .hubId(hubId)
                        .timestamp(timestamp)
                        .build();
            }
            case SCENARIO_ADDED -> {
                var hubEvent = hubEventProto.getScenarioAdded();
                yield ScenarioAddedEvent.builder()
                        .hubId(hubId)
                        .timestamp(timestamp)
                        .name(hubEvent.getName())
                        .conditions(hubEvent.getConditionList().stream()
                                .map(this::mapCondition)
                                .toList())
                        .actions(hubEvent.getActionList().stream()
                                .map(this::mapAction)
                                .toList())
                        .build();
            }
            case SCENARIO_REMOVED -> {
                var hubEvent = hubEventProto.getScenarioRemoved();
                yield ScenarioRemovedEvent.builder()
                        .hubId(hubId)
                        .timestamp(timestamp)
                        .name(hubEvent.getName())
                        .build();
            }
            default -> throw new IllegalArgumentException("Ошибка в методе toJava: " + hubEventProto.getPayloadCase());
        };
    }

    private ScenarioCondition mapCondition(ScenarioConditionProto proto) {
        Integer value =
                switch (proto.getValueCase()) {
                    case BOOL_VALUE -> proto.getBoolValue() ? 1 : 0;
                    case INT_VALUE -> proto.getIntValue();
                    default -> null;
                };

        return ScenarioCondition.builder()
                .sensorId(proto.getSensorId())
                .type(mapConditionType(proto.getType()))
                .operation(mapOperation(proto.getOperation()))
                .value(value)
                .build();
    }

    private DeviceAction mapAction(DeviceActionProto protoEvent) {
        return DeviceAction.builder()
                .sensorId(protoEvent.getSensorId())
                .type(mapActionType(protoEvent.getType()))
                .value(protoEvent.hasValue() ? protoEvent.getValue() : null)
                .build();
    }

    private DeviceType mapDeviceType(DeviceTypeProto typeProto) {
        return switch (typeProto) {
            case MOTION_SENSOR -> DeviceType.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceType.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceType.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceType.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceType.SWITCH_SENSOR;
            default -> throw new IllegalArgumentException("Неподдерживаемый DeviceTypeProto: " + typeProto);
        };
    }

    private ConditionType mapConditionType(ConditionTypeProto typeProto) {
        return switch (typeProto) {
            case MOTION -> ConditionType.MOTION;
            case LUMINOSITY -> ConditionType.LUMINOSITY;
            case SWITCH -> ConditionType.SWITCH;
            case TEMPERATURE -> ConditionType.TEMPERATURE;
            case CO2LEVEL -> ConditionType.CO2LEVEL;
            case HUMIDITY -> ConditionType.HUMIDITY;
            default -> throw new IllegalArgumentException("Неподдерживаемый тип ConditionTypeProto: " + typeProto);
        };
    }

    private ActionType mapActionType(ActionTypeProto typeProto) {
        return switch (typeProto) {
            case ACTIVATE -> ActionType.ACTIVATE;
            case DEACTIVATE -> ActionType.DEACTIVATE;
            case INVERSE -> ActionType.INVERSE;
            case SET_VALUE -> ActionType.SET_VALUE;
            default -> throw new IllegalArgumentException("Неподдерживаемый ActionTypeProto: " + typeProto);
        };
    }

    private ConditionOperation mapOperation(ConditionOperationProto operationProto) {
        return switch (operationProto) {
            case EQUALS -> ConditionOperation.EQUALS;
            case GREATER_THAN -> ConditionOperation.GREATER_THAN;
            case LOWER_THAN -> ConditionOperation.LOWER_THAN;
            default -> throw new IllegalArgumentException("Неподдерживаемая ConditionOperationProto:" + operationProto);
        };
    }
}