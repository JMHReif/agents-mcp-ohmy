package com.example.agents_mcp_ohmy.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Author entity as a Java Record
 */
@Node
public record Author(
    @Id @Property("author_id") String id,
    String name
) {}
