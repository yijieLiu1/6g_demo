package org.edgeServer1.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.edgeServer1.utils.EdgeManager;
import org.json.JSONObject;

public class EdgeHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        String response;

        if (path.equals("/get/totalclientNum")) {
            response = "totalclientNum:" + String.valueOf(EdgeManager.getClientCount());
        }
        // 执行密文连乘，输出密文，并把聚合后的密文发送给edgeServer2
        else if (path.equals("/get/sumcipherText")) {
            long startTime = System.currentTimeMillis();
            // 输出两个聚合密文
            String cipherText = EdgeManager.getAggregatedCipherText();
            String squareCipherText = EdgeManager.getAggregatedSquareCipherText();
            // String squareCipherText = "0"; // 目前没有平方密文

            EdgeManager.sendAggregatedCipherTextToEdgeServer2(cipherText, squareCipherText);
            long endTime = System.currentTimeMillis();
            response = "sumcipherText:{\"cipherText\":\"" + cipherText + "\",\"squareCipherText\":\"" + squareCipherText
                    + "\"}+" + " Time taken: " + (endTime - startTime) + "ms";
        }
        // 接收来自dataClient发送的密文,并注册client
        // cipherText密文，squareCipherText平方密文，interval区间
        else if (path.equals("/post/cipherText")) {
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
                    String interval = json.optString("interval", "");
                    EdgeManager.registerClient(clientId, cipherText, squareCipherText, interval);
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
        // 执行极值比较，需手动触发
        else if (path.equals("/get/extremeCipherText")) {
            // 获取极值密文（包含maxId->密文, minId->密文）
            Map<String, String> extremeMap = EdgeManager.getExtremeCipherText();
            // 取出maxId和minId及其密文
            Iterator<Map.Entry<String, String>> it = extremeMap.entrySet().iterator();
            String maxId = null, maxCipherText = null, minId = null, minCipherText = null;
            if (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                maxId = entry.getKey();
                maxCipherText = entry.getValue();
            }
            if (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                minId = entry.getKey();
                minCipherText = entry.getValue();
            }
            // 发送到centerServer
            if (maxId != null && maxCipherText != null && minId != null && minCipherText != null) {
                EdgeManager.sendExtremeCipherTextToCenterServer(maxId, maxCipherText, minId, minCipherText);
                // 构建响应
                JSONObject json = new JSONObject();
                json.put("maxId", maxId);
                json.put("maxCipherText", maxCipherText);
                json.put("minId", minId);
                json.put("minCipherText", minCipherText);
                response = "ExtremeCipherText:" + json.toString();
            } else {
                response = "错误: 极值信息不完整";
            }

        }

        else if (path.equals("/post/comparePair")) {
            System.out.println("开始执行/post/comparePair.....");
            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    // 只在最大区间找最大值，最小区间找最小值
                    String result = EdgeManager.findExtremesByInterval();
                    // 该方法只是测试edgeServer2解密比较密文的性能。
                    // String result = EdgeManager.findExtremes();
                    sendResponse(exchange, 200, result);
                } catch (Exception e) {
                    sendResponse(exchange, 500, "服务端异常: " + e.getMessage());
                }
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

    // 发送响应
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}