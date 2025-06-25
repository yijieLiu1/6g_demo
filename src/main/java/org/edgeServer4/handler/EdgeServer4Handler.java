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

        } else if (path.equals("/get/receivedCipherText")) {
            response = EdgeServer4Manager.getReceivedCipherText();

        }
        // 获取到来自edgeServer3的密文，然后进行聚合。
        else if (path.equals("/post/aggregatedCipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
                // 读取请求体中的聚合密文
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String aggregatedCipherText = reader.readLine();

                // 处理接收到的聚合密文
                EdgeServer4Manager.processAggregatedCipherText(aggregatedCipherText);
                response = "Success";
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
                EdgeServer4Manager.processComparisonData(cipherText, clientId1, clientId2);
                response = "Comparison data received.";
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
        } else if (path.equals("/get/impaillierCipherText")) {
            response = EdgeServer4Manager.getImpaillierCipherText();
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