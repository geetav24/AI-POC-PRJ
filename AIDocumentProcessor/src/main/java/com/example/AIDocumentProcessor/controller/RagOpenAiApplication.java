package com.example.AIDocumentProcessor.controller;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

@RestController
@RequestMapping("/api/rag-openai")
public class RagOpenAiApplication {

    private static final String OPENAI_API_KEY = "YOUR_OPENAI_API_KEY"; // Replace with your OpenAI API key
    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final String CHAT_MODEL = "gpt-4o";
    private static final String JSON_MODEL = "gpt-4o";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final List<VectorRecord> vectorStore = new ArrayList<>();
    private static JsonNode documentJson = NullNode.getInstance();

    @PostMapping("/upload")
    public ResponseEntity<String> uploadAndProcess(@RequestParam("file") MultipartFile file) throws IOException, InterruptedException {
        File convFile = new File("./uploads/" + file.getOriginalFilename());
        convFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }

        String extractedText = extractText(convFile);
        documentJson = textToJsonViaOpenAI(extractedText);
        List<String> chunks = chunkText(extractedText, 500);

        for (String chunk : chunks) {
            double[] embedding = getOpenAiEmbedding(chunk);
            vectorStore.add(new VectorRecord(UUID.randomUUID().toString(), chunk, embedding));
        }

        return ResponseEntity.ok("File processed, converted to JSON, and vectors stored with json."+documentJson.get("Name").asText() +" end of json");
    }

    @PostMapping("/query")
    public ResponseEntity<JsonNode> queryJson(@RequestBody Map<String, String> request) {
        String key = request.get("key");
        JsonNode result = documentJson.at("/" + key);
        return ResponseEntity.ok(result.isMissingNode() ? TextNode.valueOf("Key not found") : result);
    }

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody Map<String, String> request) throws IOException, InterruptedException {
        String query = request.get("question");
        double[] queryEmbedding = getOpenAiEmbedding(query);

        List<String> topChunks = similaritySearch(queryEmbedding, 3)
                .stream()
                .map(vr -> vr.chunk)
                .collect(Collectors.toList());

        String prompt = String.join("\n---\n", topChunks) + "\n\nAnswer the user's query: " + query;
        String answer = callOpenAiChat(prompt);

        return ResponseEntity.ok(answer);
    }

    private String extractText(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                return pdfStripper.getText(document);
            }
        } else if (name.endsWith(".docx")) {
            try (FileInputStream fis = new FileInputStream(file); XWPFDocument doc = new XWPFDocument(fis)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph p : doc.getParagraphs()) {
                    sb.append(p.getText()).append("\n");
                }
                return sb.toString();
            }
        }
        throw new IOException("Unsupported file type: " + name);
    }

    private JsonNode textToJson(String text) {
        ObjectNode json = mapper.createObjectNode();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                json.put(parts[0].trim().replaceAll("\\s+", "_"), parts[1].trim());
            }
        }
        return json;
    }

    private List<String> chunkText(String text, int maxTokens) {
        String[] sentences = text.split("\\. ");
        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        for (String sentence : sentences) {
            if (chunk.length() + sentence.length() > maxTokens * 5) {
                chunks.add(chunk.toString());
                chunk = new StringBuilder();
            }
            chunk.append(sentence).append(". ");
        }
        if (chunk.length() > 0) chunks.add(chunk.toString());
        return chunks;
    }

    record VectorRecord(String id, String chunk, double[] embedding) {}

    private List<VectorRecord> similaritySearch(double[] query, int topK) {
        return vectorStore.stream()
                .sorted(Comparator.comparingDouble(v -> -cosineSimilarity(query, v.embedding)))
                .limit(topK)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double[] getOpenAiEmbedding(String text) throws IOException, InterruptedException {
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", EMBEDDING_MODEL);
        requestJson.putArray("input").add(text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestJson)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("response response: " + response);
        JsonNode json = mapper.readTree(response.body());
        System.out.println("Embedding response: " + json.toString());
        JsonNode vector = json.get("data").get(0).get("embedding");
        return mapper.convertValue(vector, double[].class);
    }

    private String callOpenAiChat(String prompt) throws IOException, InterruptedException {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt);

        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", CHAT_MODEL);
        ArrayNode messages = mapper.createArrayNode();
        messages.add(message);
        requestJson.set("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestJson), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = mapper.readTree(response.body());
        return json.get("choices").get(0).get("message").get("content").asText();
    }
    private JsonNode textToJsonViaOpenAI(String text) throws IOException, InterruptedException {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        message.put("content", "Return structured JSON format:\n\n" + text);

        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", JSON_MODEL);
        ArrayNode messages = mapper.createArrayNode();
        messages.add(message);
        requestJson.set("messages", messages);
        System.out.println("text: " + text);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestJson), StandardCharsets.UTF_8))
                .build();
        System.out.println("request json: " + request);
        String content="";
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("response json: " + response.body());

        JsonNode json = mapper.readTree(response.body());
        String jscontent = json.get("choices").get(0).get("message").get("content").asText();
        if(jscontent.indexOf("{") >0 && jscontent.indexOf("}") > 0){
            content = jscontent.substring(jscontent.indexOf("{"),jscontent.indexOf("}")+1);
            System.out.println("response content: " +content);
        }
        return mapper.readTree(content);
    }

}
