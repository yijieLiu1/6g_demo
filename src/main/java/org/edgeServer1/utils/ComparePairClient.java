package org.edgeServer1.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class ComparePairClient {
    private static final String EDGE_SERVER2_URL = "http://localhost:33456";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    // 发送一对比较密文到edgeServer2，返回比较结果json字符串
    public static String sendComparePairToEdgeServer2(String clientId1, String clientId2, String cmpCipher) {
        try {
            JSONObject json = new JSONObject();
            json.put("clientId1", clientId1);
            json.put("clientId2", clientId2);
            json.put("cmpCipher", cmpCipher);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER2_URL + "/post/twoClientCompareResult"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    // 通知edgeServer2保存最终极值
    public static void notifyEdgeServer2FinalResult(String maxId, String minId) {
        try {
            JSONObject json = new JSONObject();
            json.put("maxId", maxId);
            json.put("minId", minId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER2_URL + "/post/finalCompareResult"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // 忽略异常
        }
    }
}