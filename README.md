public class Contact {
    private String firstName;
    private String lastName;
    private String emailAddress;
    private String phoneNumber;
    private String role;
    private String id;

    // Getters and setters
}
=========
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class ContactFilter {

    @Parameter
    private String filter;

    // Getter and setter
}
===
<?xml version="1.0" encoding="UTF-8"?>
<mule xmlns:http="http://www.mulesoft.org/schema/mule/http"
    xmlns="http://www.mulesoft.org/schema/mule/core"
    xmlns:doc="http://www.mulesoft.org/schema/mule/documentation"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd
        http://www.mulesoft.org/schema/mule/http http://www.mulesoft.org/schema/mule/http/current/mule-http.xsd">

    <http:listener-config name="HTTP_Listener_config" doc:name="HTTP Listener config" doc:id="f4291697-9176-40c1">
        <http:listener-connection host="0.0.0.0" port="8081" />
    </http:listener-config>

    <flow name="demomoduleFlow" doc:id="2ad4aafe-28bb-4d17">
        <http:listener doc:name="Listener" doc:id="3301a193-318b-4a48" config-ref="HTTP_Listener_config" path="/mulesoft" allowedMethods="GET">
            <http:query-params-to-map />
        </http:listener>
        <set-variable value="#[attributes.queryParams.filter]" doc:name="Set Variable" doc:id="f1d97348-b681-420e-a859" variableName="filter" />
        <ee:transform doc:name="Transform Message" doc:id="d3e38b02-4d68-4be4-b13a-f6ee6516ec21">
            <ee:message>
                <ee:set-payload><![CDATA[%dw 2.0
output application/json
---
payload.contacts filter ($.firstName contains vars.filter or $.lastName contains vars.filter or $.role contains vars.filter or $.id contains vars.filter)]]></ee:set-payload>
            </ee:message>
        </ee:transform>
    </flow>

</mule>
