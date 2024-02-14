package com.example;

import org.mule.sdk.api.annotation.param.Config;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.process.CompletionCallback;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContactFilter {

    public void filterContacts(@Config ContactConfig config, Map<String, String> filters, CompletionCallback<Result<List<Map<String, String>>, Void>> callback) {
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
}
