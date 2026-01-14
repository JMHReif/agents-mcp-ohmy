package com.example.agents_mcp_ohmy.repository;

import com.example.agents_mcp_ohmy.domain.Book;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data Neo4j Repository for Book queries
 * Demonstrates the traditional approach with hardcoded queries
 */
@Repository
public interface BookRepository extends Neo4jRepository<Book, String> {

    /**
     * Hardcoded query: Get books rated 5 stars
     */
    @Query("""
        MATCH (u:User)-[rel:PUBLISHED]->(r:Review)-[rel2:WRITTEN_FOR]->(b:Book)
        WHERE r.rating = 5
        OPTIONAL MATCH (b)<-[rel3:AUTHORED]-(a:Author)
        WITH *
        ORDER BY r.date_updated DESC
        LIMIT 10
        RETURN b, collect(rel), collect(r), collect(rel2), collect(u), collect(rel3), collect(a);
        """)
    List<Book> findFiveStarBooks();

    /**
     * For Acts 1 and 2
     * Hardcoded query: Get well-rated books that the user hasn't read
     */
    @Query("""
        MATCH (b:Book)<-[rel:AUTHORED]-(a:Author)
        WHERE b.average_rating >= 4
        AND NOT (:User {user_id: $userId})-[:PUBLISHED]->(:Review)-[:WRITTEN_FOR]->(b)
        RETURN b, collect(rel), collect(a)
        LIMIT 3;
        """)
    List<Book> findBooksNotRead(@Param("userId") String userId);

    /**
     * For Acts 1 and 2
     * Hardcoded query: Count books user has read
     */
    @Query("""
        MATCH (u:User {user_id: $userId})-[rel:PUBLISHED]->(r:Review)-[rel2:WRITTEN_FOR]->(b:Book)
        RETURN count(DISTINCT b);
        """)
    Long countBooksReadByUser(@Param("userId") String userId);

    /**
     * For Act 2 GraphRAG: Enrich seed books with graph-based recommendations
     * Takes seed book titles from vector search and expands via graph relationships
     */
    @Query("""
        MATCH (b:Book)<-[rel:WRITTEN_FOR]-(r:Review)
        WHERE r.id IN $reviewIds
        AND NOT (:User {user_id: $userId})-[:PUBLISHED]->(:Review)-[:WRITTEN_FOR]->(b)
        OPTIONAL MATCH (b)<-[rel2:AUTHORED]-(a:Author)
        RETURN b, collect(rel), collect(r), collect(rel2), collect(a);
        """)
    List<Book> findGraphRAGRecommendations(
        @Param("reviewIds") List<String> reviewIds,
        @Param("userId") String userId
    );
}
