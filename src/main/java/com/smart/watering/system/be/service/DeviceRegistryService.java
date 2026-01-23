package com.smart.watering.system.be.service;

import com.smart.watering.model.DeviceMeta;
import com.smart.watering.model.DeviceRegistrySnapshotEvent;
import com.smart.watering.model.DeviceStatus;
import com.smart.watering.model.IoTPlantEvent;
import com.smart.watering.system.be.database.model.Device;
import com.smart.watering.system.be.database.repository.DeviceRepository;
import com.smart.watering.system.be.enums.StatusEnum;
import com.smart.watering.system.be.mapper.DeviceEventMapper;
import com.smart.watering.system.be.mapper.DeviceRegistrySnapshotMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import static java.sql.Timestamp.from;

@Slf4j
@Service
public class DeviceRegistryService {

    private final DeviceRepository repository;
    private final DeviceEventMapper mapper;
    private final DeviceRegistrySnapshotMapper deviceRegistrySnapshotMapper;

    @Autowired
    public DeviceRegistryService(DeviceRepository repository,
                                 DeviceEventMapper mapper,
                                 DeviceRegistrySnapshotMapper deviceRegistrySnapshotMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.deviceRegistrySnapshotMapper = deviceRegistrySnapshotMapper;
    }

    public Mono<DeviceRegistrySnapshotEvent> elaborateDeviceInfo(IoTPlantEvent event) {
        DeviceMeta meta = event.getDevice();
        String deviceId = meta.getDeviceId();
        Instant ts = event.getTs().toInstant();

        return repository.findByDeviceId(deviceId)
                .switchIfEmpty(createDevice(meta, ts))
                .filter(d -> StatusEnum.ACTIVE.equals(d.getStatus()))
                .flatMap(d -> upsertTelemetry(d, meta, ts))
                .map(updated -> mapOutboundEvent(event));
    }

    public Mono<Device> getDevice(String deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    public Mono<Void> updateStatus(String deviceId, String status) {
        StatusEnum statusEnum = StatusEnum.fromValue(status);
        return repository.updateStatus(deviceId, statusEnum).then();
    }

    private Mono<Device> createDevice(DeviceMeta meta, Instant ts) {
        Device device = mapper.mapDeviceMetaToDevice(meta);
        device.setStatus(StatusEnum.ACTIVE);
        device.setLastSeen(java.sql.Timestamp.from(ts));
        log.info("Device to save {}", device);
        return repository.save(device);
    }

    private Mono<Device> upsertTelemetry(Device existing, DeviceMeta incoming, Instant ts) {
        boolean changed = false;

        Integer newBattery = incoming.getBatteryMv();
        Integer newRssi = incoming.getRssi();

        if (newBattery != null && !newBattery.equals(existing.getBatteryMv())) {
            existing.setBatteryMv(newBattery);
            changed = true;
        }

        if (newRssi != null && !newRssi.equals(existing.getRssi())) {
            existing.setRssi(newRssi);
            changed = true;
        }

        existing.setLastSeen(from(ts));
        changed = true;

        return changed ? repository.save(existing) : Mono.just(existing);
    }

    private DeviceRegistrySnapshotEvent mapOutboundEvent(IoTPlantEvent event) {
        return deviceRegistrySnapshotMapper.toSnapshot(
                event,
                DeviceStatus.ACTIVE,
                event.getMessageId(),
                Date.from(Instant.now())
        );
    }
}

