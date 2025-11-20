package ru.yandex.practicum;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SnapshotService {
    private final Map<String, SensorsSnapshotAvro> snapshotAvroMap = new HashMap<>();

    Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        SensorsSnapshotAvro snapshot =
                snapshotAvroMap.getOrDefault(event.getHubId(),
                        SensorsSnapshotAvro.newBuilder()
                                .setHubId(event.getHubId())
                                .setTimestamp(event.getTimestamp())
                                .setSensorsState(new HashMap<>())
                                .build());

        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        SensorStateAvro oldState = sensorsState.get(event.getId());
        if (oldState != null &&
                (oldState.getData().equals(event.getPayload()) ||
                        oldState.getTimestamp().isAfter(event.getTimestamp()))) {
            return Optional.empty();
        }

        SensorStateAvro newState =
                SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();
        sensorsState.put(event.getId(), newState);
        snapshotAvroMap.put(event.getHubId(), snapshot);
        snapshot.setTimestamp(newState.getTimestamp());
        return Optional.of(snapshot);
    }
}