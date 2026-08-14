package com.weatherspoon.library.entity;

public class TestDataFactory {

    public static Book createTestBook(Long id, String title, String author, String isbn){
        return new Book(id, title, author, isbn, null);
    }

    public static User createTestUser(Long id, String firstName, String lasName, String email) {
        return new User(id, firstName, lasName, email);
    }

}
