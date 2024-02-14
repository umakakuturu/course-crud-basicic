<ee:transform doc:name="Transform Message" doc:id="f2fc0db3-7980-47c1-b0e8-0a77a9454a79">
    <ee:message >
        <ee:set-payload ><![CDATA[%dw 2.0
output application/json
---
{
    "contacts": payload.contacts filter ((contact) -> 
        not isEmpty(vars.queryParam) and
        (
            (contact.firstName containsIgnoreCase vars.queryParam) or
            (contact.lastName containsIgnoreCase vars.queryParam) or
            (contact.role containsIgnoreCase vars.queryParam) or
            (string(contact.id) contains vars.queryParam) or
            (contact.emailAddress containsIgnoreCase vars.queryParam) or
            (contact.phoneNumber contains vars.queryParam)
        )
    )
}]]></ee:set-payload>
    </ee:message>
</ee:transform>
