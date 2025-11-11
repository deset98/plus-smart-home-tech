package ru.yandex.practicum.model.hub;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import ru.yandex.practicum.model.hub.enums.ActionType;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder
public class DeviceAction {

    private String sensorId;

    private ActionType type;

    private Integer value;
}