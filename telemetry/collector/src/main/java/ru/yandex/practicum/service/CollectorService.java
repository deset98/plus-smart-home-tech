package ru.yandex.practicum.service;

import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;

public interface CollectorService {

    void sendHubEvent(HubEvent event);

    void sendSensorEvent(SensorEvent event);
}