package io.github.pashazz.library.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

/**
 * This represents a Book entity
 */
@Entity
public class Book {

    public Book() {}

    @Id
    @GeneratedValue
    private String id;

    @Column(nullable = false)
    @NotBlank(message = "Please provide a title")
    private String title;

    @Column(nullable = false)
    @NotBlank(message = "Please provide an author")
    private String author;

    public Book(String id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    @Column(nullable = false)
    private String protectionId;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getProtectionId() {
        return protectionId;
    }

    public void setProtectionId(String protectionId) {
        this.protectionId = protectionId;
    }
}
