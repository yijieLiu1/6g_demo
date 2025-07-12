package org.centerServer.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.centerServer.utils.CenterServerManager;
import org.json.JSONObject;

public class CenterServerHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("收到请求: " + exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());

        // 添加CORS头
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Server-Type");

        // 处理OPTIONS请求
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String response;
        // 解密聚合值
        if (path.equals("/get/decryptedText")) {
            System.out.println("\n\ncenterServer收到/get/decryptedText请求，开始解密聚合值......");
            long startTime = System.currentTimeMillis();
            response = CenterServerManager.getDecryptedText();
            long endTime = System.currentTimeMillis();
            System.out.println("\ncenterServer解密聚合值结束......共耗时" + (endTime - startTime) + "ms");
        }
        // 获取均值
        else if (path.equals("/get/meanResult")) {
            System.out.println("\n\nCenterServer收到/get/meanResult请求，开始获取均值结果......");
            long startTime = System.currentTimeMillis();
            response = CenterServerManager.getMeanResult();
            long endTime = System.currentTimeMillis();
            System.out.println("\nCenterServer获取均值结果结束......共耗时" + (endTime - startTime) + "ms");

        }
        // 处理聚合密文POST请求，聚合
        else if (path.equals("/post/aggregatedCipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
                System.out.println("处理聚合密文POST请求");
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString();
                try {
                    org.json.JSONObject json = new org.json.JSONObject(body);
                    String encryptedValue = json.getString("encryptedValue");
                    int clientCount = json.getInt("clientCount");
                    String serverType = exchange.getRequestHeaders().getFirst("Server-Type");
                    org.centerServer.utils.CenterServerManager.processAggregatedCipherText(serverType, encryptedValue,
                            clientCount);
                    response = "Success";
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON or missing fields");
                    return;
                }
            } else {
                System.out.println("非法的HTTP方法: " + exchange.getRequestMethod());
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
        }
        // 获取极值结果
        else if (path.equals("/get/extremeResult")) {
            System.out.println("\n\nCenterServer收到/get/extremeResult请求，开始获取极值结果......");
            long startTime = System.currentTimeMillis();
            response = CenterServerManager.getExtremeResult();
            long endTime = System.currentTimeMillis();
            System.out.println("\nCenterServer获取极值结果结束......共耗时" + (endTime - startTime) + "ms");
        }
        // 处理极值比较的密文。
        else if (path.equals("/post/extremeCipherText")) {
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
                    String maxClientId = json.getString("maxClientId");
                    String maxCipherText = json.getString("maxCipherText");
                    String minClientId = json.getString("minClientId");
                    String minCipherText = json.getString("minCipherText");
                    String serverType = json.getString("serverId");
                    org.centerServer.utils.CenterServerManager.processExtremeCipherText(serverType, maxClientId,
                            maxCipherText, minClientId, minCipherText);
                    response = "Success";
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON or missing fields");
                    return;
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
        }
        // 处理方差密文的POST请求，计算方差
        else if (path.equals("/post/varianceCipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString();
                try {
                    JSONObject json = new JSONObject(body);
                    String encryptedValue = json.getString("encryptedValue");
                    int clientCount = json.getInt("clientCount");
                    String serverType = exchange.getRequestHeaders().getFirst("Server-Type");
                    CenterServerManager.processVarianceCipherText(serverType, encryptedValue,
                            clientCount);
                    response = "Success";
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON or missing fields");
                    return;
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
        }
        // 获取方差结果
        else if (path.equals("/get/varianceResult")) {
            System.out.println("\n\nCenterServer收到/get/varianceResult请求，开始获取方差结果......");
            long startTime = System.currentTimeMillis();
            response = CenterServerManager.getVarianceResult();
            long endTime = System.currentTimeMillis();
            System.out.println("\nCenterServer获取方差结果结束......共耗时" + (endTime - startTime) + "ms");

        } else {
            System.out.println("未找到路径: " + path);
            sendResponse(exchange, 404, "Path not found");
            return;
        }

        sendResponse(exchange, 200, response);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        System.out.println("已发送响应, 状态码: " + statusCode);
    }
}