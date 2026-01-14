package com.example.agents_mcp_ohmy.controller;

import com.example.agents_mcp_ohmy.domain.Book;
import com.example.agents_mcp_ohmy.repository.BookRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACT 1: The Traditional Approach - Spring Data Neo4j
 *
 * Demonstrates the pain points of the traditional approach:
 * - Hardcoded repository methods for specific questions
 * - No flexibility for query variations
 * - Manual coding for every new question type
 */
@RestController
@RequestMapping("/act1")
public class Act1TraditionalController {
    private final BookRepository bookRepository;

    public Act1TraditionalController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Hardcoded method: Get books with review ratings of 5 stars
     * Problem: Can't handle variations like "4 or 5 stars"
     */
    @GetMapping("/five-star-books")
    public List<Book> getFiveStarBooks() {
        return bookRepository.findFiveStarBooks();
    }

    /**
     * Another hardcoded method: Get books the user might like
     * Problem: Demonstrates we need ANOTHER method for each question type
     */
    @GetMapping("/books-not-read")
    public List<Book> getRecommendations(@RequestParam(defaultValue = "8842281e1d1347389f2ab93d60773d4d") String userId) {
        return bookRepository.findBooksNotRead(userId);
    }

    /**
     * And another hardcoded method for user stats
     * Shows custom methods to answer each question
     */
    @GetMapping("/count-read-books")
    public Long countBooksReadByUser(@RequestParam(defaultValue = "8842281e1d1347389f2ab93d60773d4d") String userId) {
        return bookRepository.countBooksReadByUser(userId);
    }
}
