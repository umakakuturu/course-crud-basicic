public void filterContacts(@Config ContactConfig config, @Parameter @DisplayName("Input String") String inputString, CompletionCallback<List<Map<String, String>>, Void> callback) {
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
