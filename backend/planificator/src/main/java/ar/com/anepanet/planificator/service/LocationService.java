package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.repository.LocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locations;

    public LocationService(LocationRepository locations) {
        this.locations = locations;
    }

    public List<Location> list() {
        return locations.findAllActive();
    }
}
