package com.example.agents_mcp_ohmy.controller;

import com.example.agents_mcp_ohmy.service.BookToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * ACT 2 STAGE 1: AI Agent with Manual Tools
 * 
 * Demonstrates how AI agents use manually-defined tools:
 * - Agent can call specific tool methods
 * - Tools are explicitly registered (@Tool annotation)
 * - More flexible than Act 1, but still requires manual tool definition
 * - Shows the progression toward MCP (Stage 2)
 */
@RestController
@RequestMapping("/act2/stage1")
public class Act2Stage1ManualToolsController {

    private final ChatClient chatClient;

    public Act2Stage1ManualToolsController(ChatClient.Builder chatClientBuilder, BookToolService bookToolService) {
        // Build ChatClient with manual tools
        this.chatClient = chatClientBuilder
                .defaultSystem("You are useful assistant that calls tools to reply to questions.")
                .defaultTools(bookToolService)  // Register all @Tool methods
                .build();
    }

    /**
     * Natural language query endpoint - Agent uses manual tools
     */
    @GetMapping("/query")
    public String queryWithManualTools(@RequestParam(defaultValue = "8842281e1d1347389f2ab93d60773d4d") String userId, @RequestParam String userQuery) {
        String systemPrompt = """
            You are a helpful reading assistant. You have access to tools that can:
            1. Get 5-star rated books
            2. Find highly-rated books that a user hasn't read
            3. Count how many books a user has read
            4. RAG: Find book reviews similar to a description using semantic search
            5. GraphRAG: Find books using semantic search + graph relationships

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
