package com.example.AIDocumentProcessor.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class VectorStoreConfig {

//         // Read the persistence path from application.properties
//        private String vectorStorePath="../ai/output/vector_store.json";
//
//        // Store the created bean instance directly for access in @PreDestroy
//        private SimpleVectorStore vectorStoreBeanInstance:
//        // No longer need to store the File instance here
//
//        @Bean
//        private VectorStor simpleVectorStore(EmbeddingModel embeddingModel) {
//            // Create file object locally for loading check
//            File vectorStoreFile = new File(vectorStorePath);
//
//            // Use the Builder pattern to create the SimpleVectorStore instance
//           SimpleVectorStore.SimpleVectorStoreBuilder builder = SimpleVectorStore.builder(embeddingModel); // Pass EmbeddingModel to builder
//
//            // Potentially configure other builder options here if needed
//
//            val simpleVectorStore = builder.build() // Build the instance
//            // Assign the created bean instance immediately
//            this.vectorStoreBeanInstance = simpleVectorStore
//
//
//            // Handle persistence: Load from file if it exists AFTER creating the instance
//            if (vectorStoreFile.exists() && vectorStoreFile.isFile) {
//                log.info("Loading SimpleVectorStore from file: ${vectorStoreFile.absolutePath}")
//                try {
//                    simpleVectorStore.load(vectorStoreFile)
//                } catch (e: Exception) {
//                    log.error("Error loading vector store from file: ${e.message}. Starting fresh.", e)
//                    // Optionally delete the corrupted file? Be careful here.
//                }
//            } else {
//                log.info("Vector store file not found or not a file, starting fresh: ${vectorStoreFile.absolutePath}")
//            }
//
//            log.info("SimpleVectorStore bean created successfully.")
//            return simpleVectorStore
//        }
//
//        // Explicitly save the vector store on application shutdown
//        @PreDestroy
//        fun saveVectorStore() {
//            // Check if the bean instance was initialized
//            if (::vectorStoreBeanInstance.isInitialized) {
//                // Create the File object directly using the injected path
//                val saveFile = File(vectorStorePath)
//                log.info("Attempting to save SimpleVectorStore to file: ${saveFile.absolutePath}")
//                try {
//                    // Use the stored bean instance to save
//                    vectorStoreBeanInstance.save(saveFile)
//                    log.info("SimpleVectorStore saved successfully.")
//                } catch (e: Exception) {
//                    log.error("Error saving SimpleVectorStore to file: ${e.message}", e)
//                }
//            } else {
//                log.warn("VectorStore bean was not initialized, skipping save.")
//            }
//        }
//    }
}
