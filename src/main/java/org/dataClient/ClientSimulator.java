package org.dataClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientSimulator {

    private static final String SERVER_URL = "http://localhost:13456/get/cipherText";
    private static final String DATA_FILE = "data.csv";
    private static final int NUM_THREADS = 100; // 并发线程数

    public static void main(String[] args) {
        List<String> clientIds = getClientIdsFromFile(DATA_FILE);
        if (clientIds.isEmpty()) {
            System.out.println("No client IDs found. Exiting.");
            return;
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        System.out.println("Starting client simulation for " + clientIds.size() + " clients...");

        for (String clientId : clientIds) {
            Runnable task = () -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(SERVER_URL))
                            .header("Client-ID", clientId)
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        System.out.println(
                                "Client " + clientId + " successfully sent data. Server response: ");
                    } else {
                        System.err.println("Client " + clientId + " failed. Status code: " + response.statusCode()
                                + ", Response: " + response.body());
                    }
                } catch (IOException | InterruptedException e) {
                    System.err.println("Exception for client " + clientId + ": " + e.getMessage());
                }
            };
            executor.submit(task);
        }

        // Shutdown the executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        System.out.println("Client simulation finished.");
    }

    private static List<String> getClientIdsFromFile(String filePath) {
        List<String> clientIds = new ArrayList<>();
        int clientCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                clientCount++;
                clientIds.add("client-" + clientCount);
            }
        } catch (IOException e) {
            System.err.println("Error reading client data file: " + e.getMessage());
            e.printStackTrace();
        }
        return clientIds;
    }
}