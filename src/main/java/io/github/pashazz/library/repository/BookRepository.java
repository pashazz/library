package io.github.pashazz.library.repository;

import io.github.pashazz.library.entity.Book;
import io.github.pashazz.library.exception.ForbiddenException;
import io.github.pashazz.library.exception.NotFoundException;
import org.apache.commons.logging.LogFactory;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthorizationContext;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.idm.authorization.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;
import org.apache.commons.logging.Log;



@Repository
@Transactional
public class BookRepository {

    private static final Log LOG = LogFactory.getLog(BookRepository.class);

    @PersistenceContext
    private EntityManager em;

    @Resource
    private HttpServletRequest request;

    public static final String  SCOPE_BOOK_READ = "book:read";
    public static final String ROLE_LIBRARIAN = "ROLE_Librarian";

    public boolean isLibrarian() {
        var principal = (KeycloakAuthenticationToken) request.getUserPrincipal();
        for (var authority : principal.getAuthorities()) {
            if (authority.getAuthority().equals(ROLE_LIBRARIAN)) {
                return true;
            }
        }
        return false;
    }

    public Collection<Book> readAllShared() {
                Map<String, Book> books = new HashMap<>();

        String userId = getUserId();
        LOG.debug("obtaining all books for " + userId);
        String [] res = getAuthzClient().protection().resource().findAll();
        var ar = new AuthorizationRequest();

        var response = getAuthzClient().authorization().authorize(ar);
        var rpt = getAuthzClient().protection().introspectRequestingPartyToken(response.getToken());
        for (Permission granted : rpt.getPermissions()) {
            System.out.println(granted.toString());
        }



        List<PermissionTicketRepresentation> permissions = getAuthzClient().protection().permission().find(null, null, null, userId,
                true, true, null, null);
        for (PermissionTicketRepresentation permission : permissions) {
            Book book = books.get(permission.getResource());
            if (book == null) {
                try {
                    book = em.createQuery("from Book where protectionId = :protectionId", Book.class).setParameter("protectionId", permission.getResource()).getSingleResult();
                    books.put(permission.getResource(), book);
                } catch (Exception e) {
                    LOG.debug("Book with protected resource id " + permission.getResource() + " not found. Deleting Protected resource from keycloak");
                    ProtectionResource protection = getAuthzClient().protection();
                    protection.resource().delete(permission.getResource());
                }
            }

        }
        return books.values();
    }

    public Collection<Book> readAllLibrarian() {
         return  em.createQuery("from Book ", Book.class).getResultList();
    }

    public Collection<Book> readAll() {
       // if (isLibrarian()) {
//            return readAllLibrarian();
//        } else {
            return readAllShared();
//        }
    }


    public Book createBook(String title, String author) {
         if (!isLibrarian()) {
             LOG.debug("User " + getUserId() + " can't create books since he's no Librarian");
             throw new ForbiddenException();
         }
        Book book = new Book(UUID.randomUUID().toString(), title, author);
        book = this.em.merge(book);
        try {
            // Create a protected resource
            Set<ScopeRepresentation> scopes = Set.of(new ScopeRepresentation(SCOPE_BOOK_READ));


            ResourceRepresentation bookResource = new ResourceRepresentation(
                    title,
                    scopes,
                    String.format("/book/%s", book.getId()),
                    "book"
            );
            String userId = getUserId();
            bookResource.setOwner(userId);
            bookResource.setOwnerManagedAccess(true); //Для мандатного доступа - False.

            ResourceRepresentation response = getAuthzClient().protection().resource().create(bookResource);

            // Create a permission for this protected resource as t
            book.setProtectionId(response.getId());
            this.em.merge(book);

            /*
            // This is how we might create a permission using Java API
            PermissionTicketRepresentation perm = new PermissionTicketRepresentation();
            perm.setResource(response.getId());
            perm.setScope(SCOPE_BOOK_READ);
            perm.setOwner(userId);
            perm.setRequester(userId);
            perm.setGranted(true);
            getAuthzClient().protection().permission().create(perm);
*/

        }
        catch (Exception e ) {
            getAuthzClient().protection().resource().delete(book.getProtectionId());
            throw e;
        }
        finally {
            this.em.flush();
        }
        return book;
    }

    private String getUserId() {
        return getKeycloakSecurityContext().getToken().getSubject();
    }

    public void deleteBook(String id) {
         if (!isLibrarian()) {
             LOG.debug("User " + getUserId() + " can't delete books since he's no Librarian");
             throw new ForbiddenException();
         }
        Book book = getBook(id);
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

    public Book getBook(String id) {
         Book book = this.em.find(Book.class, id);
         if (isLibrarian()) {
             return book;
         }
        // Check if we can read the book
        ProtectionResource protection = getAuthzClient().protection();

        if (book == null) {
            // wrong id
            LOG.debug("Incorrect book ID: " + id);
            throw new NotFoundException(id);
        }
        // Check protection for the book
        var perms = protection.permission().find(book.getProtectionId(), null, null, getUserId(), true, true, null, null);
        if (perms.size() == 0) {
            LOG.debug("Permissions not found for user " + getUserId() + " for book " + book.getId() + " with Keycloak resource id " + book.getProtectionId());
            throw new NotFoundException(id);
        }
        return book;
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