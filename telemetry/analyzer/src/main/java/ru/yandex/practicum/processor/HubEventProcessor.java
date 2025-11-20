package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dal.model.Action;
import ru.yandex.practicum.dal.model.Condition;
import ru.yandex.practicum.dal.model.Scenario;
import ru.yandex.practicum.dal.model.Sensor;
import ru.yandex.practicum.dal.model.enums.ActionType;
import ru.yandex.practicum.dal.model.enums.ConditionOperation;
import ru.yandex.practicum.dal.model.enums.ConditionType;
import ru.yandex.practicum.dal.repository.ActionRepository;
import ru.yandex.practicum.dal.repository.ConditionRepository;
import ru.yandex.practicum.dal.repository.ScenarioRepository;
import ru.yandex.practicum.dal.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    @Value("${app.topics.hubs}")
    private String hubsTopic;

    private final KafkaConsumer<String, HubEventAvro> hubEventConsumer;

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Override
    public void run() {
        try {
            log.info("Подписка HubEventProcessor на топик {}", hubsTopic);
            hubEventConsumer.subscribe(List.of(hubsTopic));

            while (true) {
                var records = hubEventConsumer.poll(Duration.ofSeconds(1));
                if (!records.isEmpty()) {
                    records.forEach(rec -> processEvent(rec.value()));
                }
            }
        } catch (WakeupException e) {
            log.info("hubEventConsumer получил WakeupException");
        } catch (Exception e) {
            log.error("Ошибка в hubEventConsumer", e);
        } finally {
            try {
                hubEventConsumer.close();
                log.info("hubEventConsumer закрыт");
            } catch (Exception e) {
                log.error("Ошибка при закрытии hubEventConsumer", e);
            }
        }
    }

    private void processEvent(HubEventAvro hubEvent) {
        if (hubEvent == null || hubEvent.getPayload() == null || hubEvent.getHubId() == null) {
            return;
        }

        String hubId = hubEvent.getHubId();
        Object payload = hubEvent.getPayload();

        switch (payload) {
            case DeviceAddedEventAvro deviceAddedEvent -> handleEvent(hubId, deviceAddedEvent);
            case DeviceRemovedEventAvro deviceRemovedEvent -> handleEvent(hubId, deviceRemovedEvent);
            case ScenarioAddedEventAvro scenarioAddedEvent -> handleEvent(hubId, scenarioAddedEvent);
            case ScenarioRemovedEventAvro scenarioRemovedEvent -> handleEvent(hubId, scenarioRemovedEvent);
            default -> log.warn("Неизвестный тип payload: {}", payload.getClass().getName());
        }
    }

    private void handleEvent(String hubId, DeviceAddedEventAvro event) {
        sensorRepository.findByIdAndHubId(event.getId(), hubId)
                .orElseGet(() -> sensorRepository.save(
                        Sensor.builder()
                                .id(event.getId())
                                .hubId(hubId)
                                .build()));
        log.info("Device added: hub={}, sensor={}", hubId, event.getId());
    }

    private void handleEvent(String hubId, DeviceRemovedEventAvro deviceRemovedEvent) {
        String sensorId = deviceRemovedEvent.getId();
        sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensorRepository::delete);
        log.info("Device removed: hub={}, sensor={}", hubId, sensorId);
    }

    private void handleEvent(String hubId, ScenarioAddedEventAvro scenarioAddedEvent) {
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, scenarioAddedEvent.getName())
                .orElseGet(() -> Scenario.builder()
                        .hubId(hubId)
                        .name(scenarioAddedEvent.getName())
                        .build());

        List<Action> oldActions = new ArrayList<>(scenario.getActions().values());
        actionRepository.deleteAll(oldActions);
        scenario.getActions().clear();

        List<Condition> oldConditions = new ArrayList<>(scenario.getConditions().values());
        conditionRepository.deleteAll(oldConditions);
        scenario.getConditions().clear();


        for (ScenarioConditionAvro scenarioCondition : scenarioAddedEvent.getConditions()) {
            Condition condition = Condition.builder()
                    .type(ConditionType.valueOf(scenarioCondition.getType().name()))
                    .operation(ConditionOperation.valueOf(scenarioCondition.getOperation().name()))
                    .value(convertValue(scenarioCondition.getValue()))
                    .build();
            scenario.addCondition(scenarioCondition.getSensorId(), condition);
        }

        for (DeviceActionAvro deviceAction : scenarioAddedEvent.getActions()) {
            Action action = Action.builder()
                    .type(ActionType.valueOf(deviceAction.getType().name()))
                    .build();
            if (deviceAction.getType().equals(ActionTypeAvro.SET_VALUE)) {
                action.setValue(convertValue(deviceAction.getValue()));
            }
            scenario.addAction(deviceAction.getSensorId(), action);
        }

        conditionRepository.saveAll(scenario.getConditions().values());
        actionRepository.saveAll(scenario.getActions().values());
        scenarioRepository.save(scenario);
    }

    private void handleEvent(String hubId, ScenarioRemovedEventAvro sr) {
        scenarioRepository.findByHubIdAndName(hubId, sr.getName())
                .ifPresent(scenario -> {
                    conditionRepository.deleteAll(scenario.getConditions().values());
                    actionRepository.deleteAll(scenario.getActions().values());
                    scenarioRepository.delete(scenario);
                });
        log.info("Scenario removed: hub={}, name={}", hubId, sr.getName());
    }

    private Integer convertValue(Object value) {
        return switch (value) {
            case Integer i -> i;
            case Boolean b -> b ? 1 : 0;
            default -> null;
        };
    }
}