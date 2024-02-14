public void filterContacts(ContactConfig config, CompletionCallback<Result<List<Map<String, String>>, Void>> callback, String inputString) {
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

    Result<List<Map<String, String>>, Void> result = Result.<List<Map<String, String>>, Void>builder().output(contacts).build();
    callback.success(result);
}
