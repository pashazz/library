package io.github.pashazz.library.controller;

import io.github.pashazz.library.entity.Book;
import io.github.pashazz.library.form.BookForm;
import io.github.pashazz.library.repository.BookRepository;
import org.keycloak.KeycloakSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@Controller
public class LibraryController {
	private final HttpServletRequest request;
	private final BookRepository bookRepository;

	@Autowired
	public LibraryController(HttpServletRequest request, BookRepository bookRepository) {
		this.request = request;
		this.bookRepository = bookRepository;
	}

	@GetMapping(value = "/")
	public String getHome() {
		return "index";
	}

	@GetMapping(value = "/books")
	public String getBooks(Model model) {
		configCommonAttributes(model);
		model.addAttribute("books", bookRepository.readAll());
		return "books";
	}

	@GetMapping(value = "/book/{id}")
	public @ResponseBody
	Book getBook(@PathVariable("id") String id) {
		// Check if we can read the book

		return bookRepository.getBook(id);
	}

	@DeleteMapping(value = "/book/{id}")
	public @ResponseBody
	ResponseEntity deleteBook(@PathVariable("id") String id) {
		bookRepository.deleteBook(id);
		return new ResponseEntity(HttpStatus.NO_CONTENT); //we don't include the book there
	}

	@PostMapping(value = "/book")
	public @ResponseBody
	Book createBook(@RequestBody BookForm form) {
		Book book = bookRepository.createBook(form.getTitle(), form.getAuthor());
		return book;
	}

	@GetMapping(value = "/manager")
	public String getManager(Model model) {
		configCommonAttributes(model);
		model.addAttribute("books", bookRepository.readAll());
		return "manager";
	}

	@GetMapping(value = "/logout")
	public String logout() throws ServletException {
		request.logout();
		return "redirect:/";
	}

	private void configCommonAttributes(Model model) {
		// Don't use getIdToken, as it's only supplied by GUI interface, not by grant_type=password authorization

		model.addAttribute("name", getKeycloakSecurityContext().getToken().getGivenName());
	}

	private KeycloakSecurityContext getKeycloakSecurityContext() {
		return (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
	}
}