package ru.yandex.practicum.model.hub;

import lombok.*;
import ru.yandex.practicum.model.hub.enums.ConditionOperation;
import ru.yandex.practicum.model.hub.enums.ConditionType;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
public class ScenarioCondition {

    private String sensorId;

    private ConditionType type;

    private ConditionOperation operation;

    private Integer value;
}