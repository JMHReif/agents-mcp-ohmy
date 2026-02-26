package com.example.agents_mcp_ohmy.repository;

import com.example.agents_mcp_ohmy.domain.Book;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data Neo4j Repository for Book queries.
 * Includes basic queries, multi-hop graph traversals, and GraphRAG support.
 */
@Repository
public interface BookRepository extends Neo4jRepository<Book, String> {

    /**
     * Get books rated 5 stars
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
     * Get well-rated books that the user hasn't read
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
     * Count books user has read
     */
    @Query("""
        MATCH (u:User {user_id: $userId})-[rel:PUBLISHED]->(r:Review)-[rel2:WRITTEN_FOR]->(b:Book)
        RETURN count(DISTINCT b);
        """)
    Long countBooksReadByUser(@Param("userId") String userId);

    /**
     * GraphRAG: Enrich seed reviews from vector search with graph-based recommendations
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

    /**
     * Multi-hop graph traversal: Find other books by the same author.
     * Path: Book <- AUTHORED <- Author -> AUTHORED -> OtherBook
     *
     * This is a graph-native query that LLMs cannot reason about on their own.
     */
    @Query("""
        MATCH (b:Book)<-[:AUTHORED]-(a:Author)-[:AUTHORED]->(other:Book)
        WHERE toLower(b.title) CONTAINS toLower($keyword)
        AND other <> b
        WITH DISTINCT other
        OPTIONAL MATCH (other)<-[rel:AUTHORED]-(otherAuthor:Author)
        RETURN other, collect(rel), collect(otherAuthor)
        ORDER BY other.average_rating DESC
        LIMIT 5
        """)
    List<Book> findOtherBooksBySameAuthor(@Param("keyword") String keyword);

    /**
     * Multi-hop graph traversal: Collaborative filtering via graph relationships.
     * Path: User -> Review -> Book <- Review <- OtherUser -> Review -> RecommendedBook
     *
     * Finds users who share highly-rated books with the given user,
     * then recommends books those similar users also rated highly.
     * This is a 6-hop traversal — pure graph-native reasoning.
     */
    @Query("""
        MATCH (u:User {user_id: $userId})-[:PUBLISHED]->(r:Review)-[:WRITTEN_FOR]->(b:Book)
              <-[:WRITTEN_FOR]-(r2:Review)<-[:PUBLISHED]-(other:User)
        WHERE r.rating >= 4 AND r2.rating >= 4 AND other <> u
        WITH u, other, collect(DISTINCT b) as sharedBooks
        WHERE size(sharedBooks) >= 2
        MATCH (other)-[:PUBLISHED]->(or2:Review)-[:WRITTEN_FOR]->(rec:Book)
        WHERE or2.rating >= 4
        AND NOT (u)-[:PUBLISHED]->(:Review)-[:WRITTEN_FOR]->(rec)
        WITH DISTINCT rec
        OPTIONAL MATCH (rec)<-[rel:AUTHORED]-(a:Author)
        RETURN rec, collect(rel), collect(a)
        ORDER BY rec.average_rating DESC
        LIMIT 5
        """)
    List<Book> findCollaborativeRecommendations(@Param("userId") String userId);
}
