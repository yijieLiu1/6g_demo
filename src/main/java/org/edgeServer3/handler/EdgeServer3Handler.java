package org.edgeServer3.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.edgeServer3.utils.EdgeServer3Manager;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class EdgeServer3Handler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        String response;
        if (path.equals("/get/totalclientNum")) {
            response = "totalclientNum:" + String.valueOf(EdgeServer3Manager.getClientCount());
        } else if (path.equals("/get/sumcipherText")) {
            System.out.println("\nedgeServer3: 收到/get/sumcipherText请求，开始聚合密文......");
            long startTime = System.currentTimeMillis();
            // 输出两个聚合密文
            String cipherText = EdgeServer3Manager.getAggregatedCipherText();
            long endTime = System.currentTimeMillis();
            System.out.println("edgeServer3聚合普通密文结束......共耗时" + (endTime - startTime) + "ms");

            long startTime2 = System.currentTimeMillis();
            String squareCipherText = EdgeServer3Manager.getAggregatedSquareCipherText();
            long endTime2 = System.currentTimeMillis();
            System.out.println("edgeServer3聚合平方密文结束......共耗时" + (endTime2 - startTime2) + "ms");
            EdgeServer3Manager.sendAggregatedCipherTextToEdgeServer4(cipherText, squareCipherText);
            response = "所有密文(普通密文和平方密文)和密文总数已顺利聚合并发送完成...";

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
            System.out.println("\nedgeServer3: 收到/post/comparePair请求，开始发送比较密文(区间比较)......");

            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    // 只在最大区间找最大值，最小区间找最小值
                    String result = EdgeServer3Manager.findExtremesByInterval();
                    exchange.sendResponseHeaders(200, result.getBytes("UTF-8").length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(result.getBytes("UTF-8"));
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 500, "服务端异常: " + e.getMessage());
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
            }
            return;
        } else if (path.equals("/post/allComparePair")) {
            System.out.println("\nedgeServer3: 收到/post/allComparePair请求，开始发送比较密文(全密文遍历比较)......");

            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    // 全密文遍历比较找极值
                    String result = EdgeServer3Manager.findExtremesByFullComparison();
                    exchange.sendResponseHeaders(200, result.getBytes("UTF-8").length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(result.getBytes("UTF-8"));
                    }

                } catch (Exception e) {
                    sendResponse(exchange, 500, "服务端异常: " + e.getMessage());
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
            }
            return;
        }

        else if (path.equals("/get/extremeCipherText")) {
            System.out.println("\nedgeServer3: 收到/get/extremeCipherText请求，开始向centerServer发送极值密文......");
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
                response = "边缘节点3的极值密文已顺利发送至中心服务器:" + json.toString();
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
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}