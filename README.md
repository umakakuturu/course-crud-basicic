package com.example.demomodule;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContactUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Contact> filterContacts(String filter) throws IOException {
        InputStream inputStream = ContactUtils.class.getResourceAsStream("/contacts.json");
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
        return filteredContacts;
    }
}
