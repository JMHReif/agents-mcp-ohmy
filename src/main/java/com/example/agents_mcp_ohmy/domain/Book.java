package com.example.agents_mcp_ohmy.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

/**
 * Book entity as a Java Record
 */
@Node
public record Book(
        @Id @Property("book_id") String id,
        String title,
        String isbn,
        String isbn13,
        @Property("publication_year") String publicationYear,
        @Property("average_rating") Double avgRating,
        @Property("num_pages") String numPages,
        @Property("ratings_count") Integer ratingsCount,

        @Relationship(value = "AUTHORED", direction = Relationship.Direction.INCOMING) List<Author> authors,
        @Relationship(value = "WRITTEN_FOR", direction = Relationship.Direction.INCOMING) List<Review> reviews
) {}
