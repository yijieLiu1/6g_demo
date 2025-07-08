package org.edgeServer4.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.edgeServer4.utils.EdgeServer4Manager;

public class EdgeServer4Handler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String response;

        if (path.equals("/get/decryptedText")) {
            response = EdgeServer4Manager.getDecryptedText();

        }
        // 获取到来自edgeServer3的密文，然后进行聚合。
        else if (path.equals("/post/aggregatedCipherText")) {
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
                    String aggregatedCipherText = json.getString("cipherText");
                    String squareCipherText = json.getString("squareCipherText");
                    int clientCount = json.getInt("clientCount");
                    EdgeServer4Manager.processAggregatedCipherText(aggregatedCipherText, clientCount);
                    EdgeServer4Manager.processMeanData(clientCount);
                    EdgeServer4Manager.processVarianceData(squareCipherText, clientCount);
                    response = "Success";
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON or missing fields");
                    return;
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
        } else if (path.equals("/get/compareResult")) {
            response = EdgeServer4Manager.getCompareResult();
        } else if (path.equals("/post/comparisonData")) {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String clientId1 = exchange.getRequestHeaders().getFirst("Client-ID1");
                String clientId2 = exchange.getRequestHeaders().getFirst("Client-ID2");

                if (clientId1 == null || clientId2 == null) {
                    sendResponse(exchange, 400, "Missing Client-ID1 or Client-ID2 header.");
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String cipherText = reader.readLine();
                System.out.println("[comparisonData] 收到比较请求: " + clientId1 + " vs " + clientId2);
                String bigger = org.edgeServer4.utils.EdgeServer4Manager.compareAndGetBigger(clientId1, clientId2,
                        cipherText);
                String smaller = clientId1.equals(bigger) ? clientId2 : clientId1;
                System.out.println("[comparisonData] 结果: bigger=" + bigger + ", smaller=" + smaller);
                org.json.JSONObject result = new org.json.JSONObject();
                result.put("bigger", bigger);
                result.put("smaller", smaller);
                sendResponse(exchange, 200, result.toString());
                return;
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
        } // 接收来自edgeServer1的最终极值保存请求,只接收最后一轮的比较结果。
        else if (path.equals("/post/finalCompareResult")) {
            System.out.println("[finalCompareResult] 收到最终极值保存请求");
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString();
                try {
                    org.json.JSONObject json = new org.json.JSONObject(body);
                    String maxId = json.getString("maxId");
                    String minId = json.getString("minId");
                    System.out.println("[finalCompareResult] 保存极值: maxId=" + maxId + ", minId=" + minId);
                    org.edgeServer4.utils.EdgeServer4Manager.saveCompareResult(maxId, minId);
                    sendResponse(exchange, 200, "Final result saved");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON or missing fields");
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
            }
            return;
        } else if (path.equals("/get/impaillierCipherText")) {
            response = EdgeServer4Manager.getImpaillierCipherText();
        } else if (path.equals("/get/compareCipherText")) {
            response = org.edgeServer4.utils.EdgeServer4Manager.generateAndSendCompareCipherText();
        } else if (path.equals("/get/meanResult")) {
            response = EdgeServer4Manager.getMeanResult();
        } else if (path.equals("/get/varianceResult")) {
            response = EdgeServer4Manager.getVarianceResult();
        } else {
            sendResponse(exchange, 404, "Path not found");
            return;
        }

        sendResponse(exchange, 200, response);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        // 设置响应头
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        // 发送响应头
        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        // 发送响应体
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        }
    }
}