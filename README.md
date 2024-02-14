# course-crud-basicic

package com.example;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContactFilter {

    public void filterContacts(@Config ContactConfig config, @Parameter @DisplayName("Filters") Map<String, String> filters, CompletionCallback<List<Map<String, String>>> callback) {
        List<Map<String, String>> contacts = config.getContacts().stream()
                .filter(contact -> {
                    boolean match = true;
                    for (Map.Entry<String, String> entry : filters.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (!contact.containsKey(key) || !contact.get(key).equals(value)) {
                            match = false;
                            break;
                        }
                    }
                    return match;
                })
                .collect(Collectors.toList());

        callback.success(contacts, Result.<List<Map<String, String>>>builder().output(contacts).build());
    }
}


===================mule=================

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
        allOf(attributes.queryParams mapObject ((value, key, index) -> contact[key] == value))
    }))[0..1]
}]]></ee:set-payload>
            </ee:message>
        </ee:transform>
    </flow>
</mule>
