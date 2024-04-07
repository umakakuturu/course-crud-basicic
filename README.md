@FeignClient(name = "personService", url = "${person.service.url}") // Name should match the service name in FeignClient
public interface PersonClient {

    @RequestLine("GET /persons/{personId}") // Define the GET request to retrieve a person by ID
    Person getPersonById(URI url); // The URI will be constructed based on the personId
}

===
package com.bcbsm.mbp.other.service;

import com.bcbsm.mbp.cms.client.PersonClient;
import com.bcbsm.mbp.cms.model.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.URISyntaxException;

@Service
public class OtherService {

    private final PersonClient personClient;

    @Autowired
    public OtherService(PersonClient personClient) {
        this.personClient = personClient;
    }

    public Person getPersonById(Long personId) {
        try {
            // Construct the URI for the getPersonById endpoint with the personId
            URI personUrl = new URI("/persons/" + personId);

            // Invoke the Feign client to retrieve the Person object
            return personClient.getPersonById(personUrl);
        } catch (URISyntaxException e) {
            // Handle URI syntax exception (e.g., invalid URI)
            throw new IllegalArgumentException("Invalid person URL", e);
        }
    }
}
