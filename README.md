<ee:transform doc:name="Transform Message" doc:id="f2fc0db3-7980-47c1-b0e8-0a77a9454a79">
    <ee:message >
        <ee:set-payload ><![CDATA[%dw 2.0
output application/json
---
{
    "contacts": (payload.contacts filter ((contact) -> {
        allOf(vars.queryParam mapObject ((value, key, index) -> contact[key] contains value))
    }))[0..1]
}]]></ee:set-payload>
    </ee:message>
</ee:transform>
