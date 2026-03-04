package com.example.agents_mcp_ohmy.controller;

import com.example.agents_mcp_ohmy.service.BookToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * ACT 2: AI Agent with Spring AI Tool Calling — "The Brain Gets Hands"
 *
 * Demonstrates how AI agents use manually-defined tools:
 * - Agent can call specific @Tool methods from BookToolService
 * - Tools provide deterministic, real database results
 * - Includes multi-hop graph traversals (author's other books, collaborative filtering)
 * - Shows the progression toward MCP standardization (Act 3)
 */
@RestController
@RequestMapping("/act2")
public class Act2ToolsController {
    private final ChatClient chatClient;

    public Act2ToolsController(ChatClient.Builder chatClientBuilder, BookToolService bookToolService) {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a helpful reading assistant. You have access to tools that can query a book database.")
                .defaultTools(bookToolService)
                .build();
    }

    /**
     * Natural language query — Agent uses manual @Tool methods.
     * Watch the logs to see which tools execute!
     */
    @GetMapping("/query")
    public String queryWithTools(
            @RequestParam(defaultValue = "8842281e1d1347389f2ab93d60773d4d") String userId,
            @RequestParam String userQuery) {

        String systemPrompt = """
                You are a helpful reading assistant. You have access to tools that can:
                1. Get highly rated (5-star) books
                2. Find well-rated books a user hasn't read yet
                3. Count how many books a user has read
                4. Find other books by the same author (multi-hop graph traversal)
                5. Get personalized recommendations based on readers with similar taste (collaborative filtering)
                6. RAG: Find book reviews similar to a description using semantic search
                7. GraphRAG: Find books using semantic search + graph relationships

                Use these tools to answer the user's question.
                The userId is: %s
                """.formatted(userId);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userQuery)
                .call()
                .content();
    }
}
