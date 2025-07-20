package com.example.AIDocumentProcessor.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class OpenAIUtil  {
    private static final String API_KEY = "YOUR_OPENAI_API_KEY"; // Replace with your OpenAI API key
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static float[] getEmbedding(String input) throws Exception {
        Map<String, Object> payload = Map.of("input", input, "model", "text-embedding-ada-002");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body()).get("data").get(0).get("embedding")
                .traverse(mapper).readValueAs(float[].class);
    }



    public static String askWithContext(String question, String context) throws Exception {
        Map<String, Object> messages = Map.of(
                "model", "gpt-4",
                "messages", List.of(
                        Map.of("role", "system", "content", "Use the provided context to answer the question."),
                        Map.of("role", "user", "content", context + "\n\nQuestion: " + question)
                )
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(messages)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body()).get("choices").get(0).get("message").get("content").asText();
    }

    public static String askWithPrompt(String prompt) throws Exception {
        Map<String, Object> messages = Map.of(
                "model", "gpt-4",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(messages)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body()).get("choices").get(0).get("message").get("content").asText();
    }
}
