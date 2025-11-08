package ru.yandex.practicum.mapper;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.hub.*;

@Component
public class HubEventMapper {

    public static SpecificRecordBase toAvro(HubEvent hubEvent) {

        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(hubEvent.getHubId())
                .setTimestamp(hubEvent.getTimestamp());

        switch (hubEvent.getType()) {
            case DEVICE_ADDED -> {
                DeviceAddedEvent added = (DeviceAddedEvent) hubEvent;
                builder.setPayload(DeviceAddedEventAvro.newBuilder()
                        .setId(added.getId())
                        .setType(DeviceTypeAvro.valueOf(added.getDeviceType().name()))
                        .build());
            }
            case DEVICE_REMOVED -> {
                DeviceRemovedEvent removed = (DeviceRemovedEvent) hubEvent;
                builder.setPayload(DeviceRemovedEventAvro.newBuilder()
                        .setId(removed.getId())
                        .build());
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEvent scenarioAdded = (ScenarioAddedEvent) hubEvent;
                builder.setPayload(ScenarioAddedEventAvro.newBuilder()
                        .setName(scenarioAdded.getName())
                        .setConditions(
                                scenarioAdded.getConditions().stream()
                                        .map(cond -> ScenarioConditionAvro.newBuilder()
                                                .setSensorId(cond.getSensorId())
                                                .setType(ConditionTypeAvro.valueOf(cond.getType().name()))
                                                .setOperation(ConditionOperationAvro.valueOf(cond.getOperation().name()))
                                                .setValue(cond.getValue())
                                                .build())
                                        .toList()
                        )
                        .setActions(
                                scenarioAdded.getActions().stream()
                                        .map(act -> DeviceActionAvro.newBuilder()
                                                .setSensorId(act.getSensorId())
                                                .setType(ActionTypeAvro.valueOf(act.getType().name()))
                                                .setValue(act.getValue())
                                                .build())
                                        .toList()
                        )
                        .build());
            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent scenarioRemoved = (ScenarioRemovedEvent) hubEvent;
                builder.setPayload(ScenarioRemovedEventAvro.newBuilder()
                        .setName(scenarioRemoved.getName())
                        .build());
            }
            default -> throw new IllegalArgumentException("Unsupported HubEvent type: " + hubEvent.getType());
        }

        return builder.build();
    }
}