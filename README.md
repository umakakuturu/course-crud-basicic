<?xml version="1.0" encoding="UTF-8"?>
<mule xmlns:http="http://www.mulesoft.org/schema/mule/http"
    xmlns="http://www.mulesoft.org/schema/mule/core"
    xmlns:doc="http://www.mulesoft.org/schema/mule/documentation"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd
        http://www.mulesoft.org/schema/mule/http http://www.mulesoft.org/schema/mule/http/current/mule-http.xsd">

    <http:listener-config name="HTTP_Listener_config" doc:name="HTTP Listener config">
        <http:listener-connection host="0.0.0.0" port="8081" />
    </http:listener-config>

    <flow name="demomoduleFlow">
        <http:listener doc:name="Listener" config-ref="HTTP_Listener_config" path="/mulesoft" allowedMethods="GET" />
        <set-variable value="#[attributes.queryParams.filter]" doc:name="Set Variable" variableName="filter" />
        <ee:transform doc:name="Transform Message">
            <ee:message>
                <ee:set-payload><![CDATA[%dw 2.0
output application/json
---
payload.contacts filter ($.firstName contains vars.filter or $.lastName contains vars.filter or $.role contains vars.filter or $.id contains vars.filter)]]></ee:set-payload>
            </ee:message>
        </ee:transform>
    </flow>

</mule>
