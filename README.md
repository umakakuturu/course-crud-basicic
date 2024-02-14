hi my code is i have {
    "contacts": [
        {
            "firstName": "John",
            "lastName": "Doe",
            "emailAddress": "johndoe@email.com",
            "phoneNumber": "123-456-7890",
            "role": "Head",
            "id": "1"
        },
        {
            "firstName": "John",
            "lastName": "Doe",
            "emailAddress": "johndoe@email.com",
            "phoneNumber": "123-456-1230",
            "role": "Sample",
            "id": "2"
        }
    ]
}
above one inside src/main/sources contacts.json and 
<?xml version="1.0" encoding="UTF-8"?>
<mule xmlns:ee="http://www.mulesoft.org/schema/mule/ee/core"
    xmlns:http="http://www.mulesoft.org/schema/mule/http"
    xmlns="http://www.mulesoft.org/schema/mule/core"
    xmlns:file="http://www.mulesoft.org/schema/mule/file"
    xsi:schemaLocation="http://www.mulesoft.org/schema/mule/http http://www.mulesoft.org/schema/mule/http/current/mule-http.xsd
        http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd
        http://www.mulesoft.org/schema/mule/ee/core http://www.mulesoft.org/schema/mule/ee/core/current/mule-ee.xsd
        http://www.mulesoft.org/schema/mule/file http://www.mulesoft.org/schema/mule/file/current/mule-file.xsd"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <file:config name="File_Config" doc:name="File Configuration" doc:id="0edf9f16-0d75-4e70-9d60-794f609ed6b3" />

    <http:listener-config name="HTTP_Listener_config" doc:name="HTTP Listener config" doc:id="e8f6a16d-84d1-4e16-8156-2dca0bfc2c1a">
        <http:listener-connection host="0.0.0.0" port="8080" />
    </http:listener-config>

    <flow name="getContactsFlow">
        <http:listener doc:name="Listener" doc:id="a16d9d92-ba57-4a6a-ae6c-d5b384c8f1bf" config-ref="HTTP_Listener_config" path="/getContacts" />
        
        <file:read doc:name="Read contacts JSON" path="classpath:contacts.json" config-ref="File_Config" />

     <ee:transform doc:name="Transform Message" doc:id="f2fc0db3-7980-47c1-b0e8-0a77a9454a79">
    <ee:message >
        <ee:set-payload ><![CDATA[%dw 2.0
output application/json
---
{
    "contacts": (payload.contacts filter ((contact) -> {
        not isEmpty(vars.queryParam) and
        (
            (contact.firstName contains vars.queryParam) or
            (contact.lastName contains vars.queryParam) or
            (contact.role contains vars.queryParam) or
            (contact.id contains vars.queryParam) or
            (contact.emailAddress contains vars.queryParam) or
            (contact.phoneNumber contains vars.queryParam)
        )
    }))[0..1]
}]]></ee:set-payload>
    </ee:message>
</ee:transform>

    </flow>
</mule>

above inside mule which is mule-config.xml

and 2 java files are

package com.example;

public class ContactConfig {
    private List<Map<String, String>> contacts;

    public List<Map<String, String>> getContacts() {
        return contacts;
    }

    public void setContacts(List<Map<String, String>> contacts) {
        this.contacts = contacts;
    }
}
above is contactsconfig,java and 
package com.example;

import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContactFilter {
public void filterContacts(ContactConfig config, CompletionCallback<List<Map<String, String>>, Void> callback, String inputString) {
    List<Map<String, String>> contacts = config.getContacts().stream()
            .filter(contact -> {
                boolean match = false;
                for (String value : contact.values()) {
                    if (value.contains(inputString)) {
                        match = true;
                        break;
                    }
                }
                return match;
            })
            .collect(Collectors.toList());

    Result<List<Map<String, String>>, Void> result = Result.<List<Map<String, String>>, Void>builder().output(contacts).build();
    callback.success(result);
}

}

contact filter.java in pom added sdk annotations  but ge
