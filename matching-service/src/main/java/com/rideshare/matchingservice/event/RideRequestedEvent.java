package com.rideshare.matchingservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * event consumed from kafka topic: ride.requested
 * published by Ride Service when the user requests a ride
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestedEvent {

    private String riderId;
    private String rideId;
    private double pickupLatitude;
    private double pickupLongitude;
    private String pickupAddress;
    private double dropLatitude;
    private double dropLongitude;
    private String dropAddress;
}
