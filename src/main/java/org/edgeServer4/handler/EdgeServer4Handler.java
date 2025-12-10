package org.edgeServer4.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.edgeServer4.utils.EdgeServer4Manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class EdgeServer4Handler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String response;

        if (path.equals("/get/decryptedText")) {
            System.out.println("\nedgeServer4: 收到/get/decryptedText请求，开始解密聚合值......");
            long startTime = System.currentTimeMillis();
            response = EdgeServer4Manager.decryptAndGetDecryptedText();
            long endTime = System.currentTimeMillis();
            System.out.println("edgeServer4: 解密聚合值结束......共耗时" + (endTime - startTime) + "ms");

        }
        // 获取到来自edgeServer3的密文，只把密文保存下来。方便后续分开计算，
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
                    System.out.println("edgeServer4: 收到/post/aggregatedCipherText请求：\n已顺利接收到所有的聚合密文并保存...\n密文值"+aggregatedCipherText+"平方密文值"+squareCipherText+"\n"+"密文总数"+clientCount);
                    // 只保存，不做解密和计算
                    EdgeServer4Manager.saveAggregatedCipherText(aggregatedCipherText, squareCipherText, clientCount);
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
            System.out.println("\nedgeServer4: 收到/get/compareResult请求，开始获取极值结果......");
            response = EdgeServer4Manager.getCompareResult();
            System.out.println("edgeServer4: 极值结果获取完成......结果: " + response);
        } else if (path.equals("/post/comparisonData")) {
            System.out.println("\nedgeServer4: 收到/post/comparisonData请求，开始处理比较数据......");
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
            System.out.println("edgeServer4: [finalCompareResult] 收到最终极值保存请求");
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
                    String computeTime = String.valueOf(json.get("computeTime"));
                    System.out.println("[finalCompareResult] 保存极值: 最大值Id=" + maxId + ", 最小值Id=" + minId +
                            ", 计算时间=" + computeTime + "ms");
                    org.edgeServer4.utils.EdgeServer4Manager.saveCompareResult(maxId, minId, computeTime);
                    sendResponse(exchange, 200, "Final result saved");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON or missing fields");
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
            }
            return;
        } else if (path.equals("/get/impaillierCipherText")) {
            System.out.println("\nedgeServer4: 收到/get/impaillierCipherText请求，开始向中心服务器发送impaillier密文......");
            response = EdgeServer4Manager.getImpaillierCipherText();
            System.out.println("edgeServer4发送impaillier密文结束......");
        } else if (path.equals("/get/impaillierVarianceCipherText")) {
            System.out.println("\nedgeServer4: 收到/get/impaillierVarianceCipherText请求，开始向中心服务器发送impaillier方差密文......");
            response = EdgeServer4Manager.getAndsendImpaillierVarianceCipherText();
            System.out.println("edgeServer4发送impaillier方差密文结束......");
        } else if (path.equals("/get/meanResult")) {
            System.out.println("\nedgeServer4: 收到/get/meanResult请求，开始计算平均值......");
            long startTime = System.currentTimeMillis();
            response = EdgeServer4Manager.processAndGetMeanResult();
            long endTime = System.currentTimeMillis();
            System.out.println("edgeServer4计算平均值结束......共耗时" + (endTime - startTime) + "ms");
        } else if (path.equals("/get/varianceResult")) {
            System.out.println("\nedgeServer4收到/get/varianceResult请求，开始计算方差结果......");
            long startTime = System.currentTimeMillis();
            response = EdgeServer4Manager.processAndGetVarianceResult();
            long endTime = System.currentTimeMillis();
            System.out.println("edgeServer4计算方差结果结束......共耗时" + (endTime - startTime) + "ms");
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