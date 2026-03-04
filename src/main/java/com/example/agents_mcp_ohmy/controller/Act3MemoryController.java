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

                MANDATORY RESPONSE FORMAT when using query_neo4j:
                    1. Always call get_schema before constructing the query.
                    2. Limit results to avoid too much data returning.
                    3. First line: "Executing Cypher query:"
                    4. Show the Cypher in a code block: ```cypher\\n[YOUR QUERY]\\n```
                    5. Then show the results

                CRITICAL: You MUST include the Cypher query in every response that uses query_neo4j.
                Show the query before the results.
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
