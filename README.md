public static Map<String, Object> filterContacts(String filter) throws IOException {
    List<Contact> contacts = Arrays.asList(mapper.readValue(inputStream, Contact[].class));

    List<Contact> filteredContacts = new ArrayList<>();
    for (Contact contact : contacts) {
        if (contact.getFirstName().contains(filter) ||
            contact.getLastName().contains(filter) ||
            contact.getRole().contains(filter) ||
            contact.getId().contains(filter)) {
            filteredContacts.add(contact);
        }
    }

    Map<String, Object> resultMap = new HashMap<>();
    resultMap.put("filteredContacts", filteredContacts);
    return resultMap;
}
