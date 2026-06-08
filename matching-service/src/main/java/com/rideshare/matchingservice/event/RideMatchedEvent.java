package com.rideshare.matchingservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * event published to kafka topic: ride.matched
 * consumed by Ride Service to update ride with assigned driver
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideMatchedEvent {

    private String riderId;
    private String rideId;
    private String driverId;
    private double driverLatitude;
    private double driverLongitude;
    private double distanceToPickupKm;
}
