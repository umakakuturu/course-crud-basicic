<flow name="demomoduleFlow">
    <http:listener doc:name="Listener" config-ref="HTTP_Listener_config" path="/mulesoft" allowedMethods="GET" />
    <set-variable value="#[attributes.queryParams.filter]" doc:name="Set Variable" variableName="filter" />
    <invoke-static doc:name="Invoke Static" class="com.example.demomodule.ContactUtils" method="filterContacts">
        <arguments>
            <argument value="#[vars.filter]" />
        </arguments>
    </invoke-static>
</flow>
