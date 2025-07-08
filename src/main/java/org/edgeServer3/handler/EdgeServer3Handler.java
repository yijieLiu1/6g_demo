package org.edgeServer3.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.edgeServer3.utils.EdgeServer3Manager;

public class EdgeServer3Handler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        String response;
        if (path.equals("/get/totalclientNum")) {
            response = "totalclientNum:" + String.valueOf(EdgeServer3Manager.getClientCount());
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
                    String result = org.edgeServer3.utils.EdgeServer3Manager.findExtremesByInterval();
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
}