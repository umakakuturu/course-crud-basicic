<flow name="demomoduleFlow">
    <http:listener doc:name="Listener" config-ref="HTTP_Listener_config" path="/mulesoft" allowedMethods="GET" />
    <set-variable value="#[attributes.queryParams.filter]" doc:name="Set Variable" variableName="filter" />
    <static:invoke doc:name="Static Invoke" className="com.example.demomodule.ContactUtils" method="filterContacts">
        <static:args><![CDATA[#[vars.filter]]]></static:args>
    </static:invoke>
</flow>
