package org.edgeServer3.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.edgeServer3.utils.EdgeServer3Manager;
import org.json.JSONObject;

public class EdgeServer3Handler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        String response;
        if (path.equals("/get/totalclientNum")) {
            response = "totalclientNum:" + String.valueOf(EdgeServer3Manager.getClientCount());
        } else if (path.equals("/get/sumcipherText")) {
            System.out.println("\n\nedgeServer3收到/get/sumcipherText请求，开始聚合密文......");
            long startTime = System.currentTimeMillis();
            // 输出两个聚合密文
            String cipherText = EdgeServer3Manager.getAggregatedCipherText();
            long endTime = System.currentTimeMillis();
            System.out.println("\nedgeServer3聚合普通密文结束......共耗时" + (endTime - startTime) + "ms");
            System.out.println("\n\nedgeServer3收到/get/sumcipherText请求，开始聚合平方密文......");
            long startTime2 = System.currentTimeMillis();
            String squareCipherText = EdgeServer3Manager.getAggregatedSquareCipherText();
            long endTime2 = System.currentTimeMillis();
            System.out.println("\nedgeServer3聚合平方密文结束......共耗时" + (endTime2 - startTime2) + "ms");
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
                    JSONObject json = new JSONObject(body);
                    String cipherText = json.getString("cipherText");
                    String squareCipherText = json.getString("squareCipherText");
                    String interval = json.optString("interval", "");
                    EdgeServer3Manager.registerClient(clientId, cipherText, squareCipherText, interval);
                    response = "Success";
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON or missing fields");
                    return;
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
        } else if (path.equals("/post/comparePair")) {
            System.out.println("开始执行/post/comparePair.....");
            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    // 只在最大区间找最大值，最小区间找最小值
                    String result = EdgeServer3Manager.findExtremesByInterval();
                    // 该方法只是测试edgeServer2解密比较密文的性能。
                    // String result = org.edgeServer1.utils.EdgeManager.findExtremes();
                    sendResponse(exchange, 200, result);
                } catch (Exception e) {
                    sendResponse(exchange, 500, "服务端异常: " + e.getMessage());
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
            }
            return;
        } else if (path.equals("/get/extremeCipherText")) {
            String maxId = EdgeServer3Manager.getLastMaxClientId();
            String minId = EdgeServer3Manager.getLastMinClientId();
            String maxCipherText = EdgeServer3Manager.generateExtremeCipherTextforCenterServer(maxId);
            String minCipherText = EdgeServer3Manager.generateExtremeCipherTextforCenterServer(minId);
            System.out.println("maxId: " + maxId + ", maxCipherText: " + maxCipherText);
            System.out.println("minId: " + minId + ", minCipherText: " + minCipherText);
            // 发送到centerServer
            if (maxId != null && maxCipherText != null && minId != null && minCipherText != null) {
                EdgeServer3Manager.sendExtremeCipherTextToCenterServer(maxId, maxCipherText, minId, minCipherText);
                Map<String, Object> ordered = new LinkedHashMap<>();
                ordered.put("maxId", maxId);
                ordered.put("maxCipherText", maxCipherText);
                ordered.put("minId", minId);
                ordered.put("minCipherText", minCipherText);
                JSONObject json = new JSONObject(ordered);
                response = "ExtremeCipherText:" + json.toString();
            } else {
                response = "错误: 极值信息不完整";
            }

        }

        else {
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
}