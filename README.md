package com.example.demomodule;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ContactUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Contact> filterContacts(String filter) throws IOException {
        InputStream inputStream = ContactUtils.class.getResourceAsStream("/contacts.json");
        List<Contact> contacts = Arrays.asList(mapper.readValue(inputStream, Contact[].class));

        return contacts.stream()
                .filter(contact -> 
                    contact.getFirstName().contains(filter) ||
                    contact.getLastName().contains(filter) ||
                    contact.getRole().contains(filter) ||
                    contact.getId().contains(filter))
                .collect(Collectors.toList());
    }
}
===================================
<flow name="demomoduleFlow">
    <http:listener doc:name="Listener" config-ref="HTTP_Listener_config" path="/mulesoft" allowedMethods="GET" />
    <set-variable value="#[attributes.queryParams.filter]" doc:name="Set Variable" variableName="filter" />
    <ee:transform doc:name="Transform Message">
        <ee:message>
            <ee:set-payload><![CDATA[%dw 2.0
output application/json
---
com.example.demomodule.ContactUtils.filterContacts(vars.filter)]]></ee:set-payload>
        </ee:message>
    </ee:transform>
</flow>

