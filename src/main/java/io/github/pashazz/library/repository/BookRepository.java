package io.github.pashazz.library.repository;

import io.github.pashazz.library.entity.Book;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap
;
@Repository
@Transactional
public class BookRepository {

    @PersistenceContext
    private EntityManager em;


    public List<Book> readAll() {
        //List<Book> allBooks = new ArrayList<>(books.values());
        allBooks.sort(Comparator.comparing(Book::getId));
        return allBooks;
    }
}