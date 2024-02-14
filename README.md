package com.example;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContactFilter {

    public void filterContacts(@Config ContactConfig config, @Parameter @DisplayName("Input String") String inputString, CompletionCallback<List<Map<String, String>>> callback) {
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
}
