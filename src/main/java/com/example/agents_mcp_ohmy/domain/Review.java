package com.example.agents_mcp_ohmy.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node
public record Review(
        @Id String id,
        String text,
        Integer rating,
        @Relationship(value = "PUBLISHED", direction = Relationship.Direction.INCOMING) User user
) {}
