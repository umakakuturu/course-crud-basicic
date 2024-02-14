<?xml version="1.0" encoding="UTF-8"?>
<mule xmlns:ee="http://www.mulesoft.org/schema/mule/ee/core"
    xmlns:http="http://www.mulesoft.org/schema/mule/http"
    xmlns="http://www.mulesoft.org/schema/mule/core"
    xmlns:json="http://www.mulesoft.org/schema/mule/json"
    xmlns:file="http://www.mulesoft.org/schema/mule/file"
    xmlns:dw="http://www.mulesoft.org/schema/mule/dataweave"
    xsi:schemaLocation="http://www.mulesoft.org/schema/mule/ee/core http://www.mulesoft.org/schema/mule/ee/core/current/mule-ee.xsd
        http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd
        http://www.mulesoft.org/schema/mule/http http://www.mulesoft.org/schema/mule/http/current/mule-http.xsd
        http://www.mulesoft.org/schema/mule/json http://www.mulesoft.org/schema/mule/json/current/mule-json.xsd
        http://www.mulesoft.org/schema/mule/file http://www.mulesoft.org/schema/mule/file/current/mule-file.xsd
        http://www.mulesoft.org/schema/mule/dataweave http://www.mulesoft.org/schema/mule/dataweave/current/mule-dataweave.xsd"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <http:listener-config name="GetContactsListenerConfig">
        <http:listener-connection host="0.0.0.0" port="8080" />
    </http:listener-config>

    <flow name="filterContactsFlow">
        <http:listener name="ContactFilterListener"
             config-ref="GetContactsListenerConfig"
             path="/getContacts"
             allowedMethods="GET" />

        <file:read name="LoadContactsJson"
                 path="classpath:contacts.json"
                 result="inputJson" />

        <ee:transform name="FilterContactsTransform" doc:name="Filter Contacts">
            <ee:message>
                <dw:set-payload>
                    <![CDATA[
                    %dw 2.0
                    input payload => payload.contacts filter ((contact) -> {
                        var searchTerm = attributes.queryParams.searchTerm;
                        var lowerCaseSearchTerm = searchTerm.toLowerCase();
                        for (var key in contact) {
                            if (key != "id" && contact[key].toString().toLowerCase().contains(lowerCaseSearchTerm)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    ]]]
                </dw:set-payload>
            </ee:message>
        </ee:transform>

        <json:write name="WriteFilteredContactsJson"
                   result="filteredContactsJson"
                   path="classpath:filtered-contacts.json"
                   dataFormat="application/json" />

        <http:outbound-endpoint exchange-pattern="responder">
            <message:transformer ref="filteredContactsJsonTransformer" />
        </http:outbound-endpoint>

        <logger message="#{payload}" level="INFO" />
    </flow>

    <message-processor:transformer name="filteredContactsJsonTransformer">
        <message:transform expression="#[payload]" mimeType="application/json" />
    </message-processor:transformer>

</mule>
