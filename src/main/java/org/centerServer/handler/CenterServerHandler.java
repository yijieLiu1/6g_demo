package org.centerServer.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.centerServer.utils.CenterServerManager;

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

        if (path.equals("/get/decryptedText")) {
            System.out.println("处理获取解密文本请求");
            response = CenterServerManager.getDecryptedText();
            System.out.println("响应内容: " + response);
        } else if (path.equals("/post/aggregatedCipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
                System.out.println("处理聚合密文POST请求");
                // 读取请求体中的聚合密文
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String aggregatedCipherText = reader.readLine();
                String serverType = exchange.getRequestHeaders().getFirst("Server-Type");

                System.out.println("收到来自 " + serverType + " 的密文");
                System.out.println("密文内容: " + aggregatedCipherText);

                // 处理接收到的聚合密文
                CenterServerManager.processAggregatedCipherText(serverType, aggregatedCipherText);
                response = "Success";
            } else {
                System.out.println("非法的HTTP方法: " + exchange.getRequestMethod());
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
        } else if (path.equals("/get/compareResult")) {
            response = org.centerServer.utils.CenterServerManager.getCompareResult();
        } else if (path.equals("/post/compareCipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String compareCipherText = reader.readLine();
                String serverType = exchange.getRequestHeaders().getFirst("Server-Type");
                org.centerServer.utils.CenterServerManager.processCompareCipherText(serverType, compareCipherText);
                response = "Success";
            } else {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
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