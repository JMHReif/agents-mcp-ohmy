package com.example.agents_mcp_ohmy.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * ACT 1: LLM Only — "The Brain Alone"
 *
 * Demonstrates the limitations of an LLM without access to tools or data:
 * - The LLM will hallucinate or admit it cannot access the database
 * - This sets the stage for why tools are necessary
 *
 * Key demo moment: Ask the same question here and in Act 2 —
 * see the difference between hallucination and real data.
 */
@RestController
@RequestMapping("/act1")
public class Act1LlmOnlyController {

    private final ChatClient chatClient;

    public Act1LlmOnlyController(ChatClient.Builder chatClientBuilder) {
        // NO TOOLS — intentionally! The LLM is on its own.
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a helpful reading assistant for a book recommendation service.")
                .build();
    }

    /**
     * Natural language query — LLM only, no tools.
     * The LLM will try to answer but cannot access real data.
     */
    @GetMapping("/query")
    public String queryLlmOnly(@RequestParam String userQuery) {
        return chatClient.prompt()
                .user(userQuery)
                .call()
                .content();
    }
}
