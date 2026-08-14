package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.Person;
import ar.com.anepanet.planificator.repository.PersonRepository;
import ar.com.anepanet.planificator.web.dto.CreatePersonRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PersonService {

    private final PersonRepository people;
    private final AuditService audit;

    public PersonService(PersonRepository people, AuditService audit) {
        this.people = people;
        this.audit = audit;
    }

    public List<Person> list() {
        return people.findAllActive();
    }

    public Person create(CreatePersonRequest req) {
        Person created = people.insert(req.name().trim(), req.color());
        audit.record(
                AuditService.ACTION_CREATE,
                AuditService.TYPE_PERSON,
                created.id().toString(),
                created.name()
        );
        return created;
    }

    public void delete(UUID id) {
        Person existing = people.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada"));
        if (!people.softDelete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada");
        }
        audit.record(
                AuditService.ACTION_DELETE,
                AuditService.TYPE_PERSON,
                id.toString(),
                existing.name()
        );
    }
}
