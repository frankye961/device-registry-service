package com.smart.watering.system.be.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.watering.model.DeviceRegistrySnapshotEvent;
import com.smart.watering.model.IoTPlantEvent;
import com.smart.watering.system.be.service.DeviceRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
@Service
public class TelemetryReadingEvent {

    private static final String DLQ_BINDING = "telemetryDlq-out-0";

    private final ObjectMapper mapper;
    private final DeviceRegistryService service;
    private final StreamBridge streamBridge;

    @Autowired
    public TelemetryReadingEvent(ObjectMapper mapper, DeviceRegistryService service, StreamBridge streamBridge) {
        this.mapper = mapper;
        this.service = service;
        this.streamBridge = streamBridge;
    }

    @Bean
    public Function<Flux<Message<String>>, Flux<Message<?>>> ingestTelemetryData() {
        return inbound -> inbound.flatMap(this::processMessage);
    }

    private Mono<Message<DeviceRegistrySnapshotEvent>> processMessage(Message<String> inbound) {
        return mapDeviceMessage(inbound.getPayload())
                .flatMap(service::elaborateDeviceInfo)
                .map(this::buildMessage)
                .onErrorResume(ex -> {
                    log.error("Error in reading telemetry - sending to DLQ: {}", ex.getMessage(), ex);
                    sendToDlq(inbound, ex);
                    return Mono.empty();
                });
    }

    private Message<DeviceRegistrySnapshotEvent> buildMessage(DeviceRegistrySnapshotEvent event) {
        return MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.KEY, event.getDevice().getDeviceId())
                .build();
    }

    private void sendToDlq(Message<String> inbound, Throwable ex) {
        streamBridge.send(
                DLQ_BINDING,
                MessageBuilder.withPayload(inbound)
                        .copyHeaders(inbound.getHeaders())
                        .setHeader("dlq.errorType", ex.getClass().getName())
                        .setHeader("dlq.errorMessage", ex.getMessage())
                        .build()
        );
    }

    private Mono<IoTPlantEvent> mapDeviceMessage(String inbound) {
        return Mono.fromCallable(() -> mapper.readValue(inbound, IoTPlantEvent.class));
    }
}
