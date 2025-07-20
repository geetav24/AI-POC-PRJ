package com.example.AIDocumentProcessor.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

public class LlamaUtil {

    private static final String API_KEY = "ll";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();



    public static String parse(MultipartFile file) throws Exception {
        Path tempFile = Files.createTempFile("llama-", "-" + file.getOriginalFilename());
        file.transferTo(tempFile.toFile());

        String boundary = "----WebKitFormBoundary" + UUID.randomUUID();
        HttpRequest.BodyPublisher bodyPublisher = ofMimeMultipartData(tempFile, boundary);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloud.llamaindex.ai/api/v1/parsing/upload"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(bodyPublisher)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String id;
        if(response.statusCode() != 200) {
            throw new RuntimeException("Failed to upload file: " + response.body());
        }else{
            id = mapper.readTree(response.body()).get("id").asText();
           waitForLlamaResult(id);
       }
        request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloud.llamaindex.ai/api/v1/parsing/job/"+id+  "/result/text"))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Files.deleteIfExists(tempFile);
        return (mapper.readTree(response.body()).get("text").asText());
    }



    private static HttpRequest.BodyPublisher ofMimeMultipartData(Path filePath, String boundary) throws IOException {
        var byteArrays = new ArrayList<byte[]>();

        // Multipart form field: file
        byteArrays.add(("--" + boundary + "\r\n").getBytes());
        byteArrays.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filePath.getFileName() + "\"\r\n").getBytes());
        byteArrays.add(("Content-Type: application/pdf\r\n\r\n").getBytes());
        byteArrays.add(Files.readAllBytes(filePath));
        byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    private static String waitForLlamaResult(String id) throws Exception {
        int retries = 10;
        int delaySeconds = 2;

        for (int i = 0; i < retries; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.cloud.llamaindex.ai/api/v1/parsing/job/" + id))
                    .header("Authorization", "Bearer " + API_KEY)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonNode = mapper.readTree(response.body());

            String status = jsonNode.get("status").asText();
            if ("SUCCESS".equalsIgnoreCase(status)) {
                return "SUCCESS";
            }

            Thread.sleep(delaySeconds * 1000);
        }

        throw new RuntimeException("Llama parsing timed out");
    }
}