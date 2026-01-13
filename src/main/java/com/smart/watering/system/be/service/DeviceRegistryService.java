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
import java.util.Optional;

@Slf4j
@Service
public class DeviceRegistryService {

    private final DeviceRepository repository;
    private final DeviceEventMapper mapper;
    private final DeviceRegistrySnapshotMapper deviceRegistrySnapshotMapper;

    @Autowired
    public DeviceRegistryService(DeviceRepository repository, DeviceEventMapper mapper,
                                 DeviceRegistrySnapshotMapper deviceRegistrySnapshotMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.deviceRegistrySnapshotMapper = deviceRegistrySnapshotMapper;
    }


    public Mono<DeviceRegistrySnapshotEvent> elaborateDeviceInfo(IoTPlantEvent event) {
        DeviceMeta meta = event.getDevice();
        String deviceId = meta.getDeviceId();

        return checkAndExtractDevice(deviceId)
                .switchIfEmpty(createDevice(meta))
                .flatMap(device -> {
                    if (!device.getStatus().equals(StatusEnum.ACTIVE)) {
                        return Mono.empty();
                    }
                    return updateDevice(device, meta, event.getTs().toInstant())
                            .then(Mono.just(mapOutboundEvent(event)));
                });
    }

    private Mono<Device> createDevice(DeviceMeta deviceMeta){
        Device device = mapDeviceMeta(deviceMeta);
        device.setStatus(StatusEnum.ACTIVE);
        repository.save(device);
        return Mono.just(device);
    }

    private Mono<Device> updateDevice(Device device, DeviceMeta incomingDevice, Instant ts){
        if((!device.getBatteryMv().equals(incomingDevice.getBatteryMv()) &&  incomingDevice.getBatteryMv() < 30)
                || (!device.getRssi().equals(incomingDevice.getRssi()) &&  incomingDevice.getRssi() < 3)) {
            repository.updateDevice(device.getDeviceId(), device);
            return Mono.just(device);
        }
        return Mono.empty();
    }

    private Mono<Device> checkAndExtractDevice(String deviceId){
        return repository.findByDeviceId(deviceId);
    }

    private Device mapDeviceMeta(DeviceMeta deviceMeta){
        return mapper.mapDeviceMetaToDevice(deviceMeta);
    }

    private DeviceRegistrySnapshotEvent mapOutboundEvent(IoTPlantEvent event){
        return deviceRegistrySnapshotMapper.toSnapshot(event, DeviceStatus.ACTIVE, event.getMessageId(), Date.from(Instant.now()));
    }
}
