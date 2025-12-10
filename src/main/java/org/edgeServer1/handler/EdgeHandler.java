package org.edgeServer1.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.edgeServer1.utils.EdgeManager;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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
            System.out.println("\nedgeServer1: 收到/get/sumcipherText请求，开始聚合密文......");
            long startTime = System.currentTimeMillis();
            String cipherText = EdgeManager.getAggregatedCipherText();
            long endTime = System.currentTimeMillis();
            System.out.println("edgeServer1: 聚合普通密文结束......共耗时" + (endTime - startTime) + "ms");

            long startTime2 = System.currentTimeMillis();
            String squareCipherText = EdgeManager.getAggregatedSquareCipherText();
            long endTime2 = System.currentTimeMillis();
            System.out.println("edgeServer1: 聚合平方密文结束......共耗时" + (endTime2 - startTime2) + "ms");

            EdgeManager.sendAggregatedCipherTextToEdgeServer2(cipherText, squareCipherText);
            response = "所有密文(普通密文和平方密文)和密文总数已顺利聚合并发送完成...";
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
            System.out.println("\nedgeServer1: 收到/get/extremeCipherText请求，开始向centerServer发送极值密文......");
            String maxId = EdgeManager.getLastMaxClientId();
            String minId = EdgeManager.getLastMinClientId();
            String maxCipherText = EdgeManager.generateExtremeCipherTextforCenterServer(maxId);
            String minCipherText = EdgeManager.generateExtremeCipherTextforCenterServer(minId);
            System.out.println("maxId: " + maxId + ", maxCipherText: " + maxCipherText);
            System.out.println("minId: " + minId + ", minCipherText: " + minCipherText);
            // 发送到centerServer
            if (maxId != null && maxCipherText != null && minId != null && minCipherText != null) {
                EdgeManager.sendExtremeCipherTextToCenterServer(maxId, maxCipherText, minId, minCipherText);
                // 构建响应
                Map<String, Object> ordered = new LinkedHashMap<>();
                ordered.put("maxId", maxId);
                ordered.put("maxCipherText", maxCipherText);
                ordered.put("minId", minId);
                ordered.put("minCipherText", minCipherText);
                JSONObject json = new JSONObject(ordered);
                response = "边缘节点1的极值密文已顺利发送至中心服务器:" + json.toString();
            } else {
                response = "错误: 极值信息不完整";
            }

        }

        else if (path.equals("/post/comparePair")) {

            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    // 只在最大区间找最大值，最小区间找最小值
                    String result = EdgeManager.findExtremesByInterval();
                    // 该方法只是测试edgeServer2解密比较密文的性能。
                    // String result = EdgeManager.findExtremes();
                    System.out.println("Post comparePair result: " + result);
                    // 确保正确返回200状态码
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

        else if (path.equals("/post/allComparePair")) {
            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    // 全量比较
                    String result = EdgeManager.findExtremesByFullComparison();
                    System.out.println("edgeServer1 :比较密文遍历构建结果 " + result);
                    // 确保正确返回200状态码
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
        } else {
            sendResponse(exchange, 404, "Path not found");
            return;
        }

        sendResponse(exchange, 200, response);
    }

    // 发送响应
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