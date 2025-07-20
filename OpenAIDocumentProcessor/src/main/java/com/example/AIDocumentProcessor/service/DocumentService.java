package com.example.AIDocumentProcessor.service;

import com.example.AIDocumentProcessor.util.LlamaUtil;
import com.example.AIDocumentProcessor.util.OpenAIUtil;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
public class DocumentService {
    private  EmbeddingModel embeddingModel;
    private  SimpleVectorStore vectorStore;

    @Tool(description = "Upload a document by providing its path")
    public String upload(@RequestParam MultipartFile file) throws Exception {
        try {
            SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
            String text = LlamaUtil.parse(file);
            List<String> chunks = chunkText(text);
            for (String chunk : chunks) {
                float[] embedding;
                try {
                    embedding = OpenAIUtil.getEmbedding(chunk);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error generating embedding: " + e.getMessage();
                }
            }
            // You can now save it, extract data, or send to embedding engine
            // For now, let's simulate storing the file
          //  return "File successfully read from: " + filePath ;

        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading file: " + e.getMessage();
        }
        return "Document uploaded successfully.";
    }



    private List<String> chunkText(String text) {
        System.out.println("Chunking text into 500-character segments."+text);
        return List.of(text.split("(?<=\\G.{500})"));
    }

}