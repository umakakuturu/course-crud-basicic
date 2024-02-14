public void filterContacts(@Config ContactConfig config, Map<String, String> filters, CompletionCallback callback) {
    List<Map<String, String>> contacts = config.getContacts().stream()
            .filter(contact -> {
                boolean match = true;
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (!contact.containsKey(key) || !contact.get(key).equals(value)) {
                        match = false;
                        break;
                    }
                }
                return match;
            })
            .collect(Collectors.toList());

    callback.success(Result.<List<Map<String, String>>, Void>builder().output(contacts).build());
}
