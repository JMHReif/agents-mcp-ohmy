package com.example.agents_mcp_ohmy.controller;

import com.example.agents_mcp_ohmy.service.BookToolService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

/**
 * ACT 4: AI Agent with MCP (Model Context Protocol) — "The Standard"
 *
 * Demonstrates the power of MCP:
 * - Tools automatically discovered from Neo4j MCP server
 * - Agent can execute ANY Cypher query dynamically
 * - Schema-aware: agent calls get_schema before constructing queries
 * - No manual tool definition required for database access
 *
 * Key demo line: "I didn't rewrite my business logic. I changed the integration layer."
 */
@RestController
@RequestMapping("/act4")
public class Act4McpController {
    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider mcpProvider;

    public Act4McpController(
            ChatClient.Builder chatClientBuilder,
            SyncMcpToolCallbackProvider mcpProvider,
            BookToolService bookToolService) {

        this.chatClient = chatClientBuilder
                .defaultSystem("You are a helpful reading assistant with access to tools for querying a book database.")
                .defaultToolCallbacks(mcpProvider.getToolCallbacks())
                .defaultTools(bookToolService)
                .build();
        this.mcpProvider = mcpProvider;
    }

    /**
     * Debug endpoint — List all available MCP tools.
     * Demonstrates dynamic tool discovery at runtime.
     */
    @GetMapping("/debug/tools")
    public String debugTools() {
        var callbacks = mcpProvider.getToolCallbacks();
        StringBuilder sb = new StringBuilder("Discovered MCP Tools:\n");
        for (var callback : callbacks) {
            sb.append("  - ").append(callback.getToolDefinition().name()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Natural language query — Agent uses MCP tools + manual tools.
     * ONE endpoint, unlimited capabilities through MCP.
     */
    @GetMapping("/query")
    public String queryWithMcpTools(
            @RequestParam(defaultValue = "8842281e1d1347389f2ab93d60773d4d") String userId,
            @RequestParam String userQuery) {

        String systemPrompt = """
                Current UserId: %s

                You have access to book tools for common queries, and a query_neo4j MCP tool for custom queries.
                Prefer the book tools when they can answer the question.
                Keep responses concise and conversational.

                Only when using query_neo4j:
                    1. Call get_schema once to understand the data model, then construct your query.
                    2. Limit results of up to 10 to avoid returning too much data.
                    3. Show the Cypher query in a code block, then the results.
                    4. If the data needed to answer the question is not in the schema, say so and stop.
                    5. Do not retry more than once if a query returns no results.
                """.formatted(userId);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userQuery)
                .call()
                .content();
    }
}
