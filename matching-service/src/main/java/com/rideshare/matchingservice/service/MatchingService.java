package com.rideshare.matchingservice.service;

import com.rideshare.matchingservice.client.LocationServiceClient;
import com.rideshare.matchingservice.dto.NearbyDriverResponse;
import com.rideshare.matchingservice.event.RideMatchedEvent;
import com.rideshare.matchingservice.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final LocationServiceClient locationServiceClient;
    private final KafkaTemplate<String, RideMatchedEvent> kafkaTemplate;

    private static final String RIDE_MATCHED_TOPIC = "ride.matched";
    private static final double DEFAULT_SEARCH_RADIUS = 5.0;

    /**
     * main matching algorithm
     * called when RideRequestedEvent is consumed from kafka
     *
     * STEPS:
     * 1. ask the Location Service for nearby drivers
     * 2. score each driver and pick the best one
     * @param event
     */
    public void matchDriver(RideRequestedEvent event){

        List<NearbyDriverResponse> nearbyDrivers = locationServiceClient.getNearbyDrivers(event.getPickupLatitude(),
                event.getPickupLongitude(), DEFAULT_SEARCH_RADIUS);

        if(nearbyDrivers.isEmpty()){
            log.warn("No drivers found near ride: {}");
            return;
        }
        //STEP 2. score each driver and pick the best one
        Optional<NearbyDriverResponse> bestDriver=findBestDriver(nearbyDrivers);

        if(bestDriver.isEmpty()){
            log.warn("could not find best driver for ride");
            return;
        }
        NearbyDriverResponse assignedDriver = bestDriver.get();

        //STEP 3. publish the RideMatchedEvent to kafka
        RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.getRideId(),
                event.getRiderId(),
                assignedDriver.getDriverId(),
                assignedDriver.getLatitude(),
                assignedDriver.getLongitude(),
                assignedDriver.getDistanceInKm()
        );

        kafkaTemplate.send(RIDE_MATCHED_TOPIC, event.getRideId(), matchedEvent);
        log.info("RideMatchedEvent Published");
    }

    /**
     * Driver scoring algorithm
     *
     * Distance: 70%
     * Rating: 30%
     *
     * Score : 1/(distance) * distanceWeight + ratings * ratingWeight
     *
     * @param drivers
     * @return
     */

    private Optional<NearbyDriverResponse> findBestDriver(List<NearbyDriverResponse> drivers) {

        double distanceWeight=0.7;
        double ratingWeight=0.3;

        return drivers.stream()
                .max((Comparator.comparingDouble(driver -> {

                    //distance score: closer = higher score
                    //add 0.1 to avoid division by 0

                    double distanceScore= (1/driver.getDistanceInKm() + 0.1);

                    //Simulated Rating between 4.0 and 5.0
                    //in production: fetch from driver service

                    double simulatedRating = 4.0 + Math.random();
                    return (distanceScore*distanceWeight)+(simulatedRating*ratingWeight);
                })));


    }
}
