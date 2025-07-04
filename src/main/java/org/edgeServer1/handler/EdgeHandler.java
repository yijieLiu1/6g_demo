package org.edgeServer1.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.edgeServer1.utils.EdgeManager;

public class EdgeHandler implements HttpHandler {
    private static volatile boolean alreadyTriggered = false;
    private static final ConcurrentHashMap<String, EdgeManager> clients = new ConcurrentHashMap<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        if (clientId == null) {
            sendResponse(exchange, 400, "Missing Client-ID header");
            return;
        }

        String response;
        if (path.equals("/get/totalclientNum")) {
            response = "totalclientNum:" + String.valueOf(EdgeManager.getClientCount());
        } else if (path.equals("/get/compareCipherText")) {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String clientId1 = params.get("client1");
            String clientId2 = params.get("client2");

            if (clientId1 == null || clientId2 == null) {
                sendResponse(exchange, 400, "Missing client1 or client2 query parameter.");
                return;
            }
            response = EdgeManager.generateAndSendComparisonCipherText(clientId1, clientId2);
        } else if (path.equals("/get/sumcipherText")) {
            // 输出两个聚合密文
            String cipherText = EdgeManager.getAggregatedCipherText();
            String squareCipherText = EdgeManager.getAggregatedSquareCipherText();
            response = "sumcipherText:{\"cipherText\":\"" + cipherText + "\",\"squareCipherText\":\"" + squareCipherText
                    + "\"}";
        } else if (path.equals("/post/cipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
                // 读取请求体中的密文
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString();
                try {
                    org.json.JSONObject json = new org.json.JSONObject(body);
                    String cipherText = json.getString("cipherText");
                    String squareCipherText = json.getString("squareCipherText");
                    EdgeManager.registerClient(clientId, cipherText, squareCipherText);

                    response = "Success";
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON or missing fields");
                    return;
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
        } else if (path.equals("/post/triggerCompare")) {

            if (exchange.getRequestMethod().equals("POST")) {
                java.util.List<org.edgeServer1.ComparisonCipherTextBatchSender.ComparisonCipherText> cmpList = new java.util.ArrayList<>();
                java.util.List<String> clientIds = org.edgeServer1.utils.EdgeManager.getAllClientIds();

                // 全排列，构建两两比较的密文。
                for (int i = 0; i < clientIds.size(); i++) {
                    for (int j = i + 1; j < clientIds.size(); j++) {
                        String c1 = clientIds.get(i);
                        String c2 = clientIds.get(j);
                        String cmpCipher = org.edgeServer1.utils.EdgeManager.generateAndSendComparisonCipherText(c1,
                                c2);
                        if (!cmpCipher.startsWith("Error:")) {
                            cmpList.add(new org.edgeServer1.ComparisonCipherTextBatchSender.ComparisonCipherText(c1, c2,
                                    cmpCipher));
                        }
                    }
                }
                org.edgeServer1.ComparisonCipherTextBatchSender.sendBatch(cmpList);
                sendResponse(exchange, 200, "批量比较已触发");
            } else {
                sendResponse(exchange, 405, "Method not allowed");
            }
            return;
        } else {
            sendResponse(exchange, 404, "Path not found");
            return;
        }

        sendResponse(exchange, 200, response);
    }

    // 用于将URL中的查询字符串转为Map ?client1=XXX &client2=YYY，以正确处理Client-ID值。
    private Map<String, String> queryToMap(String query) {
        if (query == null) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}