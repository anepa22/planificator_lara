package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.Person;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.service.LocationService;
import ar.com.anepanet.planificator.service.PersonService;
import ar.com.anepanet.planificator.web.dto.CreatePersonRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final LocationService locations;
    private final PersonService people;

    public CatalogController(LocationService locations, PersonService people) {
        this.locations = locations;
        this.people = people;
    }

    @GetMapping("/locations")
    public List<Location> locations() {
        return locations.list();
    }

    @GetMapping("/people")
    public List<Person> people() {
        return people.list();
    }

    @PostMapping("/people")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.PEOPLE_WRITE + "')")
    public Person createPerson(@Valid @RequestBody CreatePersonRequest request) {
        return people.create(request);
    }

    @DeleteMapping("/people/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.PEOPLE_WRITE + "')")
    public void deletePerson(@PathVariable UUID id) {
        people.delete(id);
    }
}
