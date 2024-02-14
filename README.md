package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContactFilter {
    public List<Map<String, String>> filterContacts(ContactConfig config, String inputString) {
        List<Map<String, String>> contacts = config.getContacts();
        List<Map<String, String>> filteredContacts = new ArrayList<>();

        for (Map<String, String> contact : contacts) {
            boolean match = false;
            for (String value : contact.values()) {
                if (value.contains(inputString)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                filteredContacts.add(contact);
            }
        }

        return filteredContacts;
    }
}
