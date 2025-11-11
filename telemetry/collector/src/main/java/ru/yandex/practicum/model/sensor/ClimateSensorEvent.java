package ru.yandex.practicum.model.sensor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import ru.yandex.practicum.model.sensor.enums.SensorEventType;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ClimateSensorEvent extends SensorEvent {
    private Integer temperatureC;

    private Integer humidity;

    private Integer co2Level;

    @Override
    public SensorEventType getType() {
        return SensorEventType.CLIMATE_SENSOR_EVENT;
    }
}