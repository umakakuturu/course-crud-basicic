<ee:transform doc:name="Transform Message" doc:id="f2fc0db3-7980-47c1-b0e8-0a77a9454a79">
    <ee:message >
        <ee:set-payload ><![CDATA[%dw 2.0
output application/json
---
{
    "contacts": (payload.contacts filter ((contact) -> {
        not isEmpty(vars.queryParam) and
        (
            (vars.queryParam contains contact.firstName) or
            (vars.queryParam contains contact.lastName) or
            (vars.queryParam contains contact.role) or
            (vars.queryParam contains contact.id)
        )
    }))[0..1]
}]]></ee:set-payload>
    </ee:message>
</ee:transform>
