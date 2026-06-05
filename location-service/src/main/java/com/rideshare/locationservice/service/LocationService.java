package com.rideshare.locationservice.service;

import com.rideshare.locationservice.dto.DriverLocationRequest;
import com.rideshare.locationservice.dto.NearbyDriverResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final RedisTemplate<String, String> redisTemplate;
    //Redis key for all the drivers locations
    private static final String DRIVERS_GEO_KEY = "drivers:locations";

    /*
     * Update driver locations in Redis
     * called every 3 seconds by driver's phone
     * maps to Redis GEOADD command
     */

    public void updateDriverLocation(DriverLocationRequest driverLocationRequest) {
        log.info("Updating driver location: {}", driverLocationRequest.getDriverId());

        //IMPORTANT: longitude first, latitude second - Geospatial standard
        Point driverPoint=new Point(
                driverLocationRequest.getLongitude(),
                driverLocationRequest.getLatitude()
        );

        redisTemplate.opsForGeo().add(
                DRIVERS_GEO_KEY, driverPoint, driverLocationRequest.getDriverId()
        );

        log.info("Updated driver location: {}", driverLocationRequest.getDriverId());
    }


    /*
    * Find nearby drivers within given radius
    * called by matching service on ride request
    * maps to redis GEORADIUS command
    * */
    public List<NearbyDriverResponse> findNearbyDrivers(double latitude, double longitude, double radiusInKm) {

        log.info("Finding drivers near lat {} long {} within {} km", latitude, longitude, radiusInKm);

        Circle searchArea = new Circle(new Point(longitude, latitude),
                new Distance(radiusInKm, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(
                DRIVERS_GEO_KEY, searchArea, RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeCoordinates()
                                .includeDistance()
                                .sortAscending()
                                .limit(10)
                        );

        List<NearbyDriverResponse> nearbyDrivers = new ArrayList<>();

        if(results!=null){
            results.getContent().forEach(result -> {
                RedisGeoCommands.GeoLocation<String> location=result.getContent();

                nearbyDrivers.add(new NearbyDriverResponse(
                        location.getName(),
                        location.getPoint().getY(),
                        location.getPoint().getX(),
                        result.getDistance().getValue()
                ));
            });
        }

        log.info("Found {} drivers nearby", nearbyDrivers.size());
        return nearbyDrivers;
    }


    /*
    * remove driver when he is offline
    * maps to Redis ZREM command
    * */

    public void removeDriver(String driverID) {
        log.info("Removing driver: {}", driverID);

        redisTemplate.opsForGeo().remove(DRIVERS_GEO_KEY, driverID);
    }

}
