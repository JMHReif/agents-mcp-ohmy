package com.example.agents_mcp_ohmy.controller;

import com.example.agents_mcp_ohmy.service.BookToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

/**
 * ACT 2 STAGE 2: AI Agent with MCP Tools
 * 
 * Demonstrates the power of MCP (Model Context Protocol):
 * - Tools automatically discovered from Neo4j MCP server
 * - Agent can execute ANY Cypher query dynamically
 * - No manual tool definition required
 * - Full database access through standardized protocol
 * - This is the "aha!" moment - MCP standardizes tool access
 */
@RestController
@RequestMapping("/act2/stage2")
public class Act2Stage2McpToolsController {
    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider mcpProvider;

    public Act2Stage2McpToolsController(
            ChatClient.Builder chatClientBuilder,
            SyncMcpToolCallbackProvider mcpProvider,
            BookToolService bookToolService) {
        // Build ChatClient with MCP tools (automatically discovered!)
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a useful assistant that calls tools to reply to questions.")
                .defaultToolCallbacks(mcpProvider.getToolCallbacks())
                .defaultTools(bookToolService)
                .build();
        this.mcpProvider = mcpProvider;
    }

    /**
     * Test endpoint to ensure MCP connection
     */
    @GetMapping("/debug/tools")
    public String debugTools() {
        var callbacks = mcpProvider.getToolCallbacks();
        StringBuilder sb = new StringBuilder("Available MCP Tools:\n");
        for (var callback : callbacks) {
            sb.append("- ").append(callback.getToolDefinition().name()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Natural language query endpoint - Agent uses MCP tools
     * ONE endpoint, MANY capabilities through MCP
     */
    @GetMapping("/query")
    public String queryWithMcpTools(
            @RequestParam(defaultValue = "8842281e1d1347389f2ab93d60773d4d") String userId,
            @RequestParam String userQuery) {
        
        String systemPrompt = """
            Current UserId: %s
            
            MANDATORY RESPONSE FORMAT when using query_neo4j:
                1. First line: "Executing Cypher query:"
                2. Show the Cypher in a code block: ```cypher\\n[YOUR QUERY]\\n```
                3. Then show the results
                    
                Example response format:
                 "Executing Cypher query:
                  ```cypher
                  MATCH (u:User {id: 'user123'})-[r:READ]->(b:Book)
                  WHERE r.rating = 5
                  RETURN b.title, b.avg_rating
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
