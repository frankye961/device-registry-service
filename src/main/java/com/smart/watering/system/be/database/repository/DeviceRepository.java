package com.smart.watering.system.be.database.repository;

import com.smart.watering.system.be.database.model.Device;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.util.Optional;

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
}
