package com.example.agents_mcp_ohmy.service;

import com.example.agents_mcp_ohmy.domain.Book;
import com.example.agents_mcp_ohmy.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that exposes repository methods as AI Tools for function calling.
 * Registered with Spring AI for the agent to use in Acts 2 and 3.
 *
 * Includes basic queries, multi-hop graph traversals, RAG, and GraphRAG tools.
 */
@Component
public class BookToolService {

    private static final Logger log = LoggerFactory.getLogger(BookToolService.class);

    private final BookRepository bookRepository;
    private final VectorStore vectorStore;

    public BookToolService(BookRepository bookRepository, VectorStore vectorStore) {
        this.bookRepository = bookRepository;
        this.vectorStore = vectorStore;
    }

    /**
     * Tool 1: Get 5-star books
     */
    @Tool(description = "Get books rated highly (5 stars). Returns list of Book objects.")
    public List<Book> getHighlyRatedBooks() {
        log.info(">>> TOOL CALLED: getHighlyRatedBooks");
        List<Book> results = bookRepository.findFiveStarBooks();
        log.info("<<< TOOL RESULT: Found {} five-star books", results.size());
        return results;
    }

    /**
     * Tool 2: Get books rated highly (4-5 stars) that user hasn't read
     */
    @Tool(description = "Get books rated highly (4 or 5 stars) that user hasn't read. Returns list of Book objects.")
    public List<Book> getQualityRecommendations(String userId) {
        log.info(">>> TOOL CALLED: getQualityRecommendations(userId={})", userId);
        List<Book> results = bookRepository.findBooksNotRead(userId);
        log.info("<<< TOOL RESULT: Found {} unread quality books", results.size());
        return results;
    }

    /**
     * Tool 3: Count how many books user has read
     */
    @Tool(description = "Count the total number of books a user has read. Returns a Long count.")
    public Long countBooksRead(String userId) {
        log.info(">>> TOOL CALLED: countBooksRead(userId={})", userId);
        Long count = bookRepository.countBooksReadByUser(userId);
        log.info("<<< TOOL RESULT: User has read {} books", count);
        return count;
    }

    /**
     * Tool 4: Multi-hop graph traversal — find other books by the same author.
     * Graph path: Book <- AUTHORED <- Author -> AUTHORED -> OtherBook
     */
    @Tool(description = "Find other books written by the same author as a given book. " +
            "Use when the user wants more books by an author they liked. " +
            "Takes a keyword to match against book titles.")
    public List<Book> findOtherBooksBySameAuthor(String keyword) {
        log.info(">>> TOOL CALLED: findOtherBooksBySameAuthor(keyword={})", keyword);
        List<Book> results = bookRepository.findOtherBooksBySameAuthor(keyword);
        log.info("<<< TOOL RESULT: Found {} books by the same author(s)", results.size());
        return results;
    }

    /**
     * Tool 5: Multi-hop graph traversal — collaborative filtering.
     * Graph path: User -> Review -> Book <- Review <- OtherUser -> Review -> RecommendedBook
     * Finds books enjoyed by readers with similar taste (6 hops!).
     */
    @Tool(description = "Get personalized book recommendations based on collaborative filtering. " +
            "Finds books that readers with similar taste also enjoyed. " +
            "This uses multi-hop graph traversal for graph-native reasoning.")
    public List<Book> getReaderRecommendations(String userId) {
        log.info(">>> TOOL CALLED: getReaderRecommendations(userId={})", userId);
        List<Book> results = bookRepository.findCollaborativeRecommendations(userId);
        log.info("<<< TOOL RESULT: Found {} collaborative recommendations", results.size());
        return results;
    }

    /**
     * Tool 6: RAG — Find books similar to a description using vector search.
     * Uses semantic similarity to find reviews matching a description.
     */
    @Tool(description = "Find reviews similar to a given description or theme using semantic search. " +
            "Use this when the user is searching for book reviews with general themes or topics. " +
            "Returns list of reviews as documents.")
    public List<Document> findBookReviewsByDescription(String description) {
        log.info(">>> TOOL CALLED: findBookReviewsByDescription(description={})", description);
        try {
            List<Document> similarReviews = vectorStore.similaritySearch(description);
            log.info("<<< TOOL RESULT: Found {} similar reviews via vector search", similarReviews.size());
            return similarReviews;
        } catch (Exception e) {
            log.error("<<< TOOL ERROR: Vector RAG failed - {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Tool 7: GraphRAG — Combine vector search with graph traversal.
     * First finds similar reviews via semantic search, then uses graph relationships
     * to find additional relevant books.
     */
    @Tool(description = "Find book information semantically similar to user's topics or themes. " +
            "Use this when the user is looking for book information similar to their theme or topic. " +
            "Returns a list of books.")
    public List<Book> findBooksWithGraphRAG(String description, String userId) {
        log.info(">>> TOOL CALLED: findBooksWithGraphRAG(description={}, userId={})", description, userId);
        try {
            // Step 1: Vector search to find semantically similar reviews
            List<Document> similarDocs = vectorStore.similaritySearch(description);

            if (similarDocs.isEmpty()) {
                log.info("<<< TOOL RESULT: No similar documents found");
                return List.of();
            }

            // Extract ids from vector search results
            List<String> similarBooks = similarDocs.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());

            log.info("    Step 1: Found {} seed reviews from vector search", similarBooks.size());

            // Step 2: Use graph relationships to enrich recommendations
            List<Book> graphRecommendations = bookRepository.findGraphRAGRecommendations(
                    similarBooks,
                    userId != null ? userId : "user123"
            );

            log.info("<<< TOOL RESULT: Found {} graph-enriched recommendations", graphRecommendations.size());
            return graphRecommendations;
        } catch (Exception e) {
            log.error("<<< TOOL ERROR: GraphRAG failed - {}", e.getMessage());
            return List.of();
        }
    }
}
