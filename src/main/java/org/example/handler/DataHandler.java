package org.example.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

import org.example.utils.DataManager;

public class DataHandler implements HttpHandler {
    private static final ConcurrentHashMap<String, DataManager> clients = new ConcurrentHashMap<>();

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
            response = "cipherText:" + dataManager.getCipherData().toString();
        } else {
            sendResponse(exchange, 404, "Path not found");
            return;
        }

        sendResponse(exchange, 200, response);
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