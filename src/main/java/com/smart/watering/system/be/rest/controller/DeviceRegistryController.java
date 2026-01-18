package com.smart.watering.system.be.rest.controller;

import com.smart.watering.system.be.mapper.DeviceEventMapper;
import com.smart.watering.system.be.rest.dto.DeviceDTO;
import com.smart.watering.system.be.rest.dto.DeviceTelemetrySnapshotDTO;
import com.smart.watering.system.be.service.DeviceRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceRegistryController {

    private final DeviceRegistryService service;
    private final DeviceEventMapper mapper;

    /**
     * Get device by deviceId
     */
    @GetMapping("/{deviceId}")
    public Mono<ResponseEntity<DeviceDTO>> getDevice(@PathVariable String deviceId) {
        return service.getDevice(deviceId)
                .map(mapper::mapDeviceToDeviceDTO)
                .map(device -> ResponseEntity.ok()
                        .eTag("\"" + device.getVersion() + "\"")
                        .body(device)
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    /**
     * Set device status (BLOCKED / ACTIVE / INACTIVE)
     */
    @PutMapping("/{deviceId}/status")
    public Mono<ResponseEntity<Object>> updateStatus(
            @PathVariable String deviceId,
            @RequestBody String request
    ) {
        return service.updateStatus(deviceId, request)
                .thenReturn(ResponseEntity.ok().build())
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity.badRequest().build())
                );
    }

    /**
     * Get last-known telemetry snapshot
     */
    @GetMapping("/{deviceId}/telemetry")
    public Mono<ResponseEntity<DeviceTelemetrySnapshotDTO>> getTelemetry(
            @PathVariable String deviceId
    ) {
        return service.getDevice(deviceId)
                .map(mapper::mapTelemetry)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
