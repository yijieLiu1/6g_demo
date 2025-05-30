package org.dataClient.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.dataClient.utils.DataManager;

public class DataHandler implements HttpHandler {
    private static final ConcurrentHashMap<String, DataManager> clients = new ConcurrentHashMap<>();
    private static final String EDGE_SERVER_URL = "http://localhost:23456";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        if (clientId == null) {
            sendResponse(exchange, 400, "Missing Client-ID header");
            return;
        }

        DataManager dataManager = clients.get(clientId);
        if (dataManager == null) {
            sendResponse(exchange, 404, "Client not found");
            return;
        }

        String response;
        if (path.equals("/get/plainText")) {
            response = "plainText:" + dataManager.getPlainData().toString();
        } else if (path.equals("/get/cipherText")) {
            String cipherText = dataManager.getCipherData().toString();
            response = "cipherText:" + cipherText;

            // 发送密文到edgeServer
            sendCipherTextToEdgeServer(clientId, cipherText);
        } else {
            sendResponse(exchange, 404, "Path not found");
            return;
        }

        sendResponse(exchange, 200, response);
    }

    private void sendCipherTextToEdgeServer(String clientId, String cipherText) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER_URL + "/post/cipherText"))
                    .header("Client-ID", clientId)
                    .POST(HttpRequest.BodyPublishers.ofString(cipherText))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Failed to send cipher text to edge server: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending cipher text to edge server: " + e.getMessage());
        }
    }

    public static void registerClient(String clientId, BigDecimal initialData) {
        clients.put(clientId, DataManager.getInstance(clientId, initialData));
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}