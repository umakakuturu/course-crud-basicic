public void filterContacts(ContactConfig config, CompletionCallback<List<Map<String, String>>, Void> callback, String inputString) {
    List<Map<String, String>> contacts = config.getContacts().stream()
            .filter(contact -> {
                for (String value : contact.values()) {
                    if (value.contains(inputString)) {
                        return true;
                    }
                }
                return false;
            })
            .collect(Collectors.toList());

    Result<List<Map<String, String>>, Void> result = Result.<List<Map<String, String>>, Void>builder().output(contacts).build();
    callback.success(result);
}
