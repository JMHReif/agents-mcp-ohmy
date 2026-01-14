package com.example.agents_mcp_ohmy.service;

import com.example.agents_mcp_ohmy.domain.Book;
import com.example.agents_mcp_ohmy.repository.BookRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that exposes repository methods as AI Tools for function calling
 * These tools will be registered with Spring AI for the agent to use in Act 2 Stage 1
 *
 * Includes RAG and GraphRAG tools for semantic search capabilities
 */
@Component
public class BookToolService {

    private final BookRepository bookRepository;
    private final VectorStore vectorStore;

    public BookToolService(BookRepository bookRepository,
                           VectorStore vectorStore) {
        this.bookRepository = bookRepository;
        this.vectorStore = vectorStore;
    }

    /**
     * Tool 1: Get 5-star books
     */
    @Tool(description = "Get books rated highly (5 stars). Returns list of Book objects.")
    public List<Book> getHighlyRatedBooks() {
        return bookRepository.findFiveStarBooks();
    }

    /**
     * Tool 2: Get books rated highly (4-5 stars) that user hasn't read
     */
    @Tool(description = "Get books rated highly (4 or 5 stars) that user hasn't read. Returns list of Book objects.")
    public List<Book> getQualityRecommendations(String userId) {
        return bookRepository.findBooksNotRead(userId);
    }

    /**
     * Tool 3: Count how many books user has read
     */
    @Tool(description = "Count the total number of books a user has read. Returns a Long count.")
    public Long countBooksRead(String userId) {
        return bookRepository.countBooksReadByUser(userId);
    }

    /**
     * Tool 4: RAG - Find books similar to a description using vector search
     * Uses semantic similarity to find books matching a description
     */
    @Tool(description = "Find reviews similar to a given description or theme using semantic search. " +
            "Use this when the user is searching for book reviews with general themes or topics. " +
            "Returns list of reviews as documents.")
    public List<Document> findBookReviewsByDescription(String description) {
        try {
            // Perform vector similarity search
            List<Document> similarReviews = vectorStore.similaritySearch(description);

            System.out.println("----- RAG: Found " + similarReviews.size() + " book reviews from vector search -----");
            System.out.println("Results: " + similarReviews);

            return similarReviews;
        } catch (Exception e) {
            System.err.println("Vector RAG failed: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Tool 5: GraphRAG - Combine vector search with graph traversal
     * First finds similar books via semantic search, then uses graph relationships
     * to find additional relevant books (author, reviews, etc)
     */
    @Tool(description = "Find book information semantically similar to user's topics or themes. " +
            "Use this when the user is looking for book information similar to their theme or topic. " +
            "Returns a list of books.")
    public List<Book> findBooksWithGraphRAG(String description, String userId) {
        try {
            // Step 1: Vector search to find semantically similar books
            List<Document> similarDocs = vectorStore.similaritySearch(description);

            if (similarDocs.isEmpty()) {
                return List.of();
            }

            System.out.println("SimilarDocs: " + similarDocs);

            // Extract ids from vector search results
            List<String> similarBooks = similarDocs.stream()
//                    .map(doc -> (String) doc.getMetadata().getOrDefault("title", ""))
                    .map(document -> document.getId())
                    .collect(Collectors.toList());

            System.out.println("----- GraphRAG: Found " + similarBooks.size() + " book reviews from vector search -----");
            System.out.println("Seed reviewIds: " + similarBooks);

            // Step 2: Use repository to get graph-enriched recommendations
            List<Book> graphRecommendations = bookRepository.findGraphRAGRecommendations(
                    similarBooks,
                    userId != null ? userId : "user123"
            );

            System.out.println("----- GraphRAG: Found " + graphRecommendations.size() + " enriched recommendations -----");
            System.out.println("Recommendations: " + graphRecommendations);

            return graphRecommendations;
        } catch (Exception e) {
            System.err.println("GraphRAG failed: " + e.getMessage());
            return List.of();
        }
    }
}
