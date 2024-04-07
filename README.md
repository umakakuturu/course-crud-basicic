import com.bcbsm.mbp.cms.model.Person;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "personClient", url = "${person.service.url}")
public interface PersonClient {

    @GetMapping("/api/persons/{personId}")
    Person getPersonById(@PathVariable("personId") Long personId);
}
=============
import com.bcbsm.mbp.cms.client.PersonClient;
import com.bcbsm.mbp.cms.model.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class YourService {

    private final PersonClient personClient;

    @Autowired
    public YourService(PersonClient personClient) {
        this.personClient = personClient;
    }

    public Person getPersonById(Long personId) {
        try {
            // Invoke the Feign client to retrieve the Person by personId
            return personClient.getPersonById(personId);
        } catch (Exception e) {
            // Handle exception (e.g., FeignClientException, RuntimeException)
            throw new RuntimeException("Error retrieving person from remote service", e);
        }
    }
}
