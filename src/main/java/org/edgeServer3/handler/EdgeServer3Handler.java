package org.edgeServer3.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import org.edgeServer3.utils.EdgeServer3Manager;
import java.util.Map;

public class EdgeServer3Handler implements HttpHandler {
    private static final ConcurrentHashMap<String, EdgeServer3Manager> clients = new ConcurrentHashMap<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        String response;
        if (path.equals("/get/totalclientNum")) {
            response = "totalclientNum:" + String.valueOf(EdgeServer3Manager.getClientCount());
        } else if (path.equals("/get/compareCipherText")) {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String clientId1 = params.get("client1");
            String clientId2 = params.get("client2");

            if (clientId1 == null || clientId2 == null) {
                sendResponse(exchange, 400, "Missing client1 or client2 query parameter.");
                return;
            }
            response = EdgeServer3Manager.generateAndSendComparisonCipherText(clientId1, clientId2);
        } else if (path.equals("/get/sumcipherText")) {
            // 输出两个聚合密文
            String cipherText = EdgeServer3Manager.getAggregatedCipherText();
            String squareCipherText = EdgeServer3Manager.getAggregatedSquareCipherText();
            EdgeServer3Manager.sendAggregatedCipherTextToEdgeServer4(cipherText, squareCipherText);
            response = "sumcipherText:{\"cipherText\":\"" + cipherText + "\",\"squareCipherText\":\"" + squareCipherText
                    + "\"}";

        } else if (path.equals("/post/cipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
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
                    EdgeServer3Manager.registerClient(clientId, cipherText, squareCipherText);
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
                java.util.List<org.edgeServer3.ComparisonCipherTextBatchSender.ComparisonCipherText> cmpList = new java.util.ArrayList<>();
                java.util.List<String> clientIds = org.edgeServer3.utils.EdgeServer3Manager.clientCipherTexts == null
                        ? new java.util.ArrayList<>()
                        : new java.util.ArrayList<>(
                                org.edgeServer3.utils.EdgeServer3Manager.clientCipherTexts.keySet());
                // 按 client-数字 升序排序，确保顺序一致
                clientIds.sort(java.util.Comparator.comparingInt(id -> Integer.parseInt(id.replace("client-", ""))));
                // 全排列两两比较
                for (int i = 0; i < clientIds.size(); i++) {
                    for (int j = i + 1; j < clientIds.size(); j++) {
                        String c1 = clientIds.get(i);
                        String c2 = clientIds.get(j);
                        String cmpCipher = org.edgeServer3.utils.EdgeServer3Manager
                                .generateAndSendComparisonCipherText(c1, c2);
                        if (!cmpCipher.startsWith("Error:")) {
                            cmpList.add(new org.edgeServer3.ComparisonCipherTextBatchSender.ComparisonCipherText(c1, c2,
                                    cmpCipher));
                        }
                    }
                }
                org.edgeServer3.ComparisonCipherTextBatchSender.sendBatch(cmpList);
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

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // 用于将URL中的查询字符串转为Map，便于获取client1和client2参数
    private Map<String, String> queryToMap(String query) {
        if (query == null) {
            return new java.util.HashMap<>();
        }
        Map<String, String> result = new java.util.HashMap<>();
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
}