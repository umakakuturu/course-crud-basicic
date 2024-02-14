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
            (string(contact.id) contains vars.queryParam) or
            (contact.emailAddress contains vars.queryParam) or
            (contact.phoneNumber contains vars.queryParam)
        )
    }))[0..1]
}]]></ee:set-payload>
    </ee:message>
</ee:transform>
