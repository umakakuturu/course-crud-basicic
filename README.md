# course-crud-basicic

package com.example;

public class ContactConfig {
    private List<Map<String, String>> contacts;

    public List<Map<String, String>> getContacts() {
        return contacts;
    }

    public void setContacts(List<Map<String, String>> contacts) {
        this.contacts = contacts;
    }
}
