package com.example.agents_mcp_ohmy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// TODO: Spring AI 2.0.0-M6 bug — Neo4jChatMemoryRepository passes Optional values from OpenAI response
//  metadata to the Neo4j driver, which can't serialize them. Workaround: override with in-memory storage.
//  Remove once fixed upstream: https://github.com/spring-projects/spring-ai/issues
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class AgentsMcpOhmyApplication {

	private static final Logger log = LoggerFactory.getLogger(AgentsMcpOhmyApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AgentsMcpOhmyApplication.class, args);
	}

	@Bean
	@Primary
	ChatMemoryRepository chatMemoryRepository() {
		return new InMemoryChatMemoryRepository();
	}

	@Bean
	CommandLineRunner startupBanner() {
		return args -> {
			log.info("========================================");
			log.info("  Agents, Tools, and MCP, oh my!");
			log.info("  Act 1: /act1  — LLM Only");
			log.info("  Act 2: /act2  — Spring AI Tools");
			log.info("  Act 3: /act3  — MCP Protocol");
			log.info("  Act 4: /act4  — Memory (Neo4j)");
			log.info("========================================");
		};
	}
}
