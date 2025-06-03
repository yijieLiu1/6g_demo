package org.edgeServer2.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.edgeServer2.utils.EdgeServer2Manager;

public class EdgeServer2Handler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String response;

        if (path.equals("/get/decryptedText")) {
            response = EdgeServer2Manager.getDecryptedText();
        } else if (path.equals("/get/receivedCipherText")) {
            response = EdgeServer2Manager.getReceivedCipherText();
        } else if (path.equals("/post/aggregatedCipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
                // 读取请求体中的聚合密文
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String aggregatedCipherText = reader.readLine();

                // 处理接收到的聚合密文
                EdgeServer2Manager.processAggregatedCipherText(aggregatedCipherText);
                response = "Success";
            } else {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
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