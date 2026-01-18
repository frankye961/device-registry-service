package com.smart.watering.system.be.database.repository;

import com.smart.watering.system.be.database.model.Device;
import com.smart.watering.system.be.enums.StatusEnum;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface DeviceRepository extends ReactiveMongoRepository<Device, Integer> {

    Mono<Device> findByDeviceId(String deviceId);

    @Query("{ 'deviceId': ?0 }")
    @Update("{ '$set': { " +
            "  'batteryMv': ?#{#device.batteryMv}, " +
            "  'rssi': ?#{#device.rssi}, " +
            "  'lastSeen': ?#{#device.lastSeen} " +
            "} }")
    Mono<Void> updateDevice(String deviceId, Device device);

    @Query("{ 'deviceId': ?0 }")
    @Update("{ '$set': { " +
            "  'status': ?1 " +
            "} }")
    Mono<Void> updateStatus(String deviceId, StatusEnum status);
}
