public void filterContacts(ContactConfig config, CompletionCallback<List<Map<String, String>>> callback, String inputString) {
    List<Map<String, String>> contacts = config.getContacts().stream()
            .filter(contact -> {
                boolean match = false;
                for (String value : contact.values()) {
                    if (value.contains(inputString)) {
                        match = true;
                        break;
                    }
                }
                return match;
            })
            .collect(Collectors.toList());

    callback.success(contacts);
}
