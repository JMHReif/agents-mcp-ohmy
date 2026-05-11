package com.example.agents_mcp_ohmy.controller;

import com.example.agents_mcp_ohmy.service.BookToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

/**
 * ACT 3: Memory — "The Brain Remembers"
 *
 * Builds on Act 2 (Tools) and adds conversation memory backed by Neo4j:
 * - Prior conversation context persists across requests
 * - Enables follow-up questions that reference earlier results
 * - Memory stored as graph nodes in Neo4j
 * - Demonstrates: Context + Graph + Agent loop
 */
@RestController
@RequestMapping("/act3")
public class Act3MemoryController {
    private final ChatClient chatClient;

    public Act3MemoryController(
            ChatClient.Builder chatClientBuilder,
            SyncMcpToolCallbackProvider mcpProvider,
            BookToolService bookToolService,
            ChatMemory chatMemory) {

        this.chatClient = chatClientBuilder
                .defaultSystem("You are a helpful reading assistant with access to tools for querying a book database.")
                .defaultToolCallbacks(mcpProvider.getToolCallbacks())
                .defaultTools(bookToolService)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * Natural language query with conversation memory.
     * Use the same conversationId across requests to maintain context.
     * Follow-up questions reference prior results automatically.
     */
    @GetMapping("/query")
    public String queryWithMemory(
            @RequestParam(defaultValue = "8842281e1d1347389f2ab93d60773d4d") String userId,
            @RequestParam String userQuery,
            @RequestParam(defaultValue = "demo") String conversationId) {

        String systemPrompt = """
                Current UserId: %s

                You have access to book tools for common queries, and a query_neo4j MCP tool for custom queries.
                Prefer the book tools when they can answer the question.

                Only when using query_neo4j:
                    1. Call get_schema once to understand the data model, then construct your query.
                    2. Limit results of up to 10 to avoid returning too much data.
                    3. Show the Cypher query in a code block, then the results.
                    4. If the data needed to answer the question is not in the schema, say so and stop.
                    5. Do not retry more than once if a query returns no results.
                """.formatted(userId);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userQuery)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        return response;
    }
}
