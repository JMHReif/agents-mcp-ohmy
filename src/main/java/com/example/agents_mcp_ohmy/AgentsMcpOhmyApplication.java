package com.example.agents_mcp_ohmy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AgentsMcpOhmyApplication {

	private static final Logger log = LoggerFactory.getLogger(AgentsMcpOhmyApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AgentsMcpOhmyApplication.class, args);
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
