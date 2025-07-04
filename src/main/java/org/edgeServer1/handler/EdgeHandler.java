package org.edgeServer1.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.edgeServer1.utils.EdgeManager;

public class EdgeHandler implements HttpHandler {
    private static volatile boolean alreadyTriggered = false;
    private static final ConcurrentHashMap<String, EdgeManager> clients = new ConcurrentHashMap<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        String response;
        if (path.equals("/get/totalclientNum")) {
            response = "totalclientNum:" + String.valueOf(EdgeManager.getClientCount());
        } else if (path.equals("/get/sumcipherText")) {
            // 输出两个聚合密文
            String cipherText = EdgeManager.getAggregatedCipherText();
            String squareCipherText = EdgeManager.getAggregatedSquareCipherText();
            EdgeManager.sendAggregatedCipherTextToEdgeServer2(cipherText, squareCipherText);
            response = "sumcipherText:{\"cipherText\":\"" + cipherText + "\",\"squareCipherText\":\"" + squareCipherText
                    + "\"}";
        } else if (path.equals("/post/cipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
                // 读取请求体中的密文
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
                    EdgeManager.registerClient(clientId, cipherText, squareCipherText);

                    response = "Success";
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON or missing fields");
                    return;
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
        } else if (path.equals("/post/triggerCompare")) {
            System.out.println("开始执行/post/triggerCompare.....");
            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    java.util.List<org.edgeServer1.ComparisonCipherTextBatchSender.ComparisonCipherText> cmpList = new java.util.ArrayList<>();
                    java.util.List<String> clientIds = org.edgeServer1.utils.EdgeManager.getAllClientIds();
                    for (int i = 0; i < clientIds.size(); i++) {
                        for (int j = i + 1; j < clientIds.size(); j++) {
                            String c1 = clientIds.get(i);
                            String c2 = clientIds.get(j);
                            String cmpCipher = org.edgeServer1.utils.EdgeManager.generateAndSendComparisonCipherText(c1,
                                    c2);
                            if (!cmpCipher.startsWith("Error:")) {
                                cmpList.add(new org.edgeServer1.ComparisonCipherTextBatchSender.ComparisonCipherText(c1,
                                        c2, cmpCipher));
                            }
                        }
                    }
                    org.edgeServer1.ComparisonCipherTextBatchSender.sendBatch(cmpList);
                    sendResponse(exchange, 200, "批量比较已触发");
                } catch (Exception e) {
                    sendResponse(exchange, 500, "服务端异常: " + e.getMessage());
                }
            } else {
                sendResponse(exchange, 405, "Method not allowed");
            }
            return;
        } else if (path.equals("/post/comparePair")) {
            System.out.println("开始执行/post/comparePair.....");
            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    java.util.List<String> clientIds = org.edgeServer1.utils.EdgeManager.getAllClientIds();
                    if (clientIds.size() < 2) {
                        sendResponse(exchange, 400, "Not enough clients to compare");
                        return;
                    }
                    int n = clientIds.size();
                    java.util.List<String> maxCandidates = new java.util.ArrayList<>();
                    java.util.List<String> minCandidates = new java.util.ArrayList<>();
                    // 1. 两两分组比较
                    for (int i = 0; i < n - 1; i += 2) {
                        String a = clientIds.get(i);
                        String b = clientIds.get(i + 1);
                        System.out.println("[comparePair] 分组: " + a + " vs " + b);
                        String cmpCipher = org.edgeServer1.utils.EdgeManager.generateAndSendComparisonCipherText(a, b);
                        String compareResult = org.edgeServer1.utils.ComparePairClient.sendComparePairToEdgeServer2(a,
                                b, cmpCipher);
                        while (compareResult == null) {
                            System.out.println("[comparePair] 网络或服务端异常，重试: " + a + " vs " + b);
                            compareResult = org.edgeServer1.utils.ComparePairClient.sendComparePairToEdgeServer2(a, b,
                                    cmpCipher);
                        }
                        org.json.JSONObject resultJson = new org.json.JSONObject(compareResult);
                        String bigger = resultJson.getString("bigger");
                        String smaller = resultJson.getString("smaller");
                        System.out.println("[comparePair] 结果: bigger=" + bigger + ", smaller=" + smaller);
                        maxCandidates.add(bigger);
                        minCandidates.add(smaller);
                    }
                    if (n % 2 == 1) {
                        String last = clientIds.get(n - 1);
                        System.out.println("[comparePair] 奇数个，最后一个 " + last + " 进入两个候选区");
                        maxCandidates.add(last);
                        minCandidates.add(last);
                    }
                    // 2. 在maxCandidates中找最大
                    String maxId = maxCandidates.get(0);
                    for (int i = 1; i < maxCandidates.size(); i++) {
                        String challenger = maxCandidates.get(i);
                        System.out.println("[comparePair] 最大候选区比较: " + maxId + " vs " + challenger);
                        String cmpCipher = org.edgeServer1.utils.EdgeManager.generateAndSendComparisonCipherText(maxId,
                                challenger);
                        String compareResult = org.edgeServer1.utils.ComparePairClient
                                .sendComparePairToEdgeServer2(maxId, challenger, cmpCipher);
                        while (compareResult == null) {
                            System.out.println("[comparePair] 网络或服务端异常，重试: " + maxId + " vs " + challenger);
                            compareResult = org.edgeServer1.utils.ComparePairClient.sendComparePairToEdgeServer2(maxId,
                                    challenger, cmpCipher);
                        }
                        org.json.JSONObject resultJson = new org.json.JSONObject(compareResult);
                        String bigger = resultJson.getString("bigger");
                        System.out.println("[comparePair] 最大候选区结果: bigger=" + bigger);
                        maxId = bigger;
                    }
                    // 3. 在minCandidates中找最小
                    String minId = minCandidates.get(0);
                    for (int i = 1; i < minCandidates.size(); i++) {
                        String challenger = minCandidates.get(i);
                        System.out.println("[comparePair] 最小候选区比较: " + minId + " vs " + challenger);
                        String cmpCipher = org.edgeServer1.utils.EdgeManager.generateAndSendComparisonCipherText(minId,
                                challenger);
                        String compareResult = org.edgeServer1.utils.ComparePairClient
                                .sendComparePairToEdgeServer2(minId, challenger, cmpCipher);
                        while (compareResult == null) {
                            System.out.println("[comparePair] 网络或服务端异常，重试: " + minId + " vs " + challenger);
                            compareResult = org.edgeServer1.utils.ComparePairClient.sendComparePairToEdgeServer2(minId,
                                    challenger, cmpCipher);
                        }
                        org.json.JSONObject resultJson = new org.json.JSONObject(compareResult);
                        String smaller = resultJson.getString("smaller");
                        System.out.println("[comparePair] 最小候选区结果: smaller=" + smaller);
                        minId = smaller;
                    }
                    System.out.println("[comparePair] 最终最大值 clientId: " + maxId + ", 最小值 clientId: " + minId);
                    // 通知edgeServer2保存极值
                    org.edgeServer1.utils.ComparePairClient.notifyEdgeServer2FinalResult(maxId, minId);
                    sendResponse(exchange, 200, "极值比较完成");
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