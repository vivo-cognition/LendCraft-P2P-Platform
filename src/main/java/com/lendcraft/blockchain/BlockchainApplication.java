package com.lendcraft.blockchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // Импортировать это

@SpringBootApplication
@EnableScheduling
public class BlockchainApplication {
	public static void main(String[] args) {
		SpringApplication.run(BlockchainApplication.class, args);
	}
}
