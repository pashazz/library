package io.github.pashazz.library.repository;

import io.github.pashazz.library.entity.Book;
import io.github.pashazz.library.exception.NotFoundException;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthorizationContext;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap
;
@Repository
@Transactional
public class BookRepository {

    @PersistenceContext
    private EntityManager em;

    @Resource
    private HttpServletRequest request;

    public static final String  SCOPE_BOOK_READ = "book:read";

    public Collection<Book> readAll() {
        Map<String, Book> books = new HashMap<>();
        List<PermissionTicketRepresentation> permissions = getAuthzClient().protection().permission().find(null, null, null, getKeycloakSecurityContext().getToken().getSubject(),
                true, true, null, null);

        for (PermissionTicketRepresentation permission : permissions) {
            Book book = books.get(permission.getResource());
            if (book == null) {
                book = em.createQuery("from Book where protectionId = :protectionId", Book.class).setParameter("protectionId", permission.getResource()).getSingleResult();
                books.put(permission.getResource(), book);
            }

        }
        return books.values();
    }

    public Book createBook(String title, String author) {
        Book book = new Book(UUID.randomUUID().toString(), title, author);
        try {
            this.em.persist(book);
            // Create a protected resource
            Set<ScopeRepresentation> scopes = Set.of(new ScopeRepresentation(SCOPE_BOOK_READ));


            ResourceRepresentation bookResource = new ResourceRepresentation(
                    title,
                    scopes,
                    String.format("/book/%s", book.getId()),
                    "book"
            );
            book.setProtectionId(bookResource.getId());
            bookResource.setOwner(request.getUserPrincipal().getName());
            bookResource.setOwnerManagedAccess(true); //Для мандатного доступа - False.

        }
        catch (Exception e ) {
            getAuthzClient().protection().resource().delete(book.getProtectionId());;
            throw e;
        }
        return book;
    }

    public void deleteBook(String id) {
        Book book = this.em.find(Book.class, id);
        if (book == null) {
            // wrong id
            System.out.printf("Incorrect book ID: %s\n", id);
            throw new NotFoundException();
        }
        //Delete a protected resource
        String uri = String.format("/book/%s", book.getId());
        try {
            ProtectionResource protection = getAuthzClient().protection();
            List<ResourceRepresentation> search = protection.resource().findByUri(uri);

            if (search.isEmpty()) {
                throw new RuntimeException("Could not find protected resource with URI [" + uri + "]");
            }

            protection.resource().delete(search.get(0).getId());
        } catch (Exception e) {
            throw new RuntimeException("Could not find a protected resource by book ID " + id, e);
        } finally {
            this.em.remove(book);
        }

    }
    private ClientAuthorizationContext getAuthorizationContext() {
        return ClientAuthorizationContext.class.cast(getKeycloakSecurityContext().getAuthorizationContext());
    }

    private KeycloakSecurityContext getKeycloakSecurityContext() {
        return KeycloakSecurityContext.class.cast(request.getAttribute(KeycloakSecurityContext.class.getName()));
    }

    private AuthzClient getAuthzClient() {
        return getAuthorizationContext().getClient();
    }
}