package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;
import ru.yandex.practicum.service.CollectorServiceImpl;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class CollectorControllerRest {

    private final CollectorServiceImpl collectorService;

    @PostMapping("/hubs")
    @ResponseStatus(HttpStatus.OK)
    public void sendHubEvent(@Valid @RequestBody HubEvent event) {
        collectorService.sendHubEvent(event);
    }

    @PostMapping("/sensors")
    @ResponseStatus(HttpStatus.OK)
    public void sendSensorEvent(@Valid @RequestBody SensorEvent event) {
        collectorService.sendSensorEvent(event);
    }
}