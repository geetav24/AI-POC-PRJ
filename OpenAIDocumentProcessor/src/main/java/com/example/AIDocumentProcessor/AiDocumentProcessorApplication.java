package com.example.AIDocumentProcessor;

import com.example.AIDocumentProcessor.service.DocumentService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiDocumentProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiDocumentProcessorApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider weatherTools(DocumentService documentService) {
		return MethodToolCallbackProvider.builder().toolObjects(documentService).build();
	}


}
