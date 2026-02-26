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

    private static final Logger log = LoggerFactory.getLogger(Act4McpController.class);
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
     * Log discovered MCP tools at startup — demonstrates dynamic tool discovery.
     * This is the "aha!" moment: tools loaded via protocol, not hardcoded.
     */
    @PostConstruct
    public void logDiscoveredTools() {
        var callbacks = mcpProvider.getToolCallbacks();
        log.info("========================================");
        log.info("  Act 4: MCP Tool Discovery");
        log.info("  Discovered {} tools:", callbacks.length);
        for (var cb : callbacks) {
            log.info("    - {}", cb.getToolDefinition().name());
        }
        log.info("  Tools loaded dynamically via protocol!");
        log.info("========================================");
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

                MANDATORY RESPONSE FORMAT when using query_neo4j:
                    1. Always call get_schema before constructing the query.
                    2. Limit results to avoid too much data returning.
                    3. First line: "Executing Cypher query:"
                    4. Show the Cypher in a code block: ```cypher\\n[YOUR QUERY]\\n```
                    5. Then show the results

                        Example response format:
                         "Executing Cypher query:
                          ```cypher
                          MATCH (u:User {user_id: 'user123'})-[:PUBLISHED]->(r:Review)-[:WRITTEN_FOR]->(b:Book)
                          WHERE r.rating = 5
                          RETURN b.title, b.average_rating
                          LIMIT 10
                          ```

                          Results: Here are the 5-star rated books..."

                CRITICAL: You MUST include the Cypher query in every response that uses query_neo4j.
                Show the query before the results.
                """.formatted(userId);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userQuery)
                .call()
                .content();
    }
}
