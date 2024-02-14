<flow name="demomoduleFlow">
    <http:listener doc:name="Listener" config-ref="HTTP_Listener_config" path="/mulesoft" allowedMethods="GET" />
    <set-variable value="#[attributes.queryParams.filter]" doc:name="Set Variable" variableName="filter" />
    <ee:transform doc:name="Transform Message">
        <ee:message>
            <ee:set-payload><![CDATA[%dw 2.0
output application/json
---
java!com.example.demomodule.ContactUtils.filterContacts(vars.filter)]]></ee:set-payload>
        </ee:message>
    </ee:transform>
</flow>
