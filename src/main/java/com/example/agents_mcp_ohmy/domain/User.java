package com.example.agents_mcp_ohmy.domain;

import org.springframework.data.neo4j.core.schema.Id;

public record User(
        @Id String userId
) {
}
