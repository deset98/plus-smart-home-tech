package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mapper.HubEventMapperAvro;
import ru.yandex.practicum.mapper.SensorEventMapperAvro;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;
import ru.yandex.practicum.serializer.AvroSerializer;

@Service
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${app.topics.hubs}")
    private String hubTopic;
    @Value("${app.topics.sensors}")
    private String sensorTopic;

    private final AvroSerializer avroSerializer;

    @Override
    public void sendHubEvent(HubEvent hubEvent) {
        SpecificRecordBase record = HubEventMapperAvro.toAvro(hubEvent);
        byte[] data = avroSerializer.serialize(hubTopic, record);
        kafkaTemplate.send(hubTopic, hubEvent.getHubId(), data);
    }

    @Override
    public void sendSensorEvent(SensorEvent sensorEvent) {
        SpecificRecordBase record = SensorEventMapperAvro.toAvro(sensorEvent);
        byte[] data = avroSerializer.serialize(sensorTopic, record);
        kafkaTemplate.send(sensorTopic, sensorEvent.getId(), data);
    }
}