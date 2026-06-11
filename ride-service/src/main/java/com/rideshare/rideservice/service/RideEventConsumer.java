package com.rideshare.rideservice.service;

import com.rideshare.rideservice.event.RideMatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * consumes the event published by Matching Service
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class RideEventConsumer {

    private final RideService rideService;

    @KafkaListener(
            topics = "ride.matched",
            groupId = "ride-service-group"
    )
    public void consumeRideMatchedEvent(RideMatchedEvent event){
        log.info("====== Kafka Consumer Caught RideMatchedEvent! ======");
        log.info("Processing Ride ID: {} assigned to Driver ID: {}", event.getRideId(), event.getDriverId());
        rideService.updateRideWithDriver(event.getRideId(), event.getDriverId());
    }
}
