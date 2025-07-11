package org.edgeServer3.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class ComparePairClient {
    private static final String EDGE_SERVER4_URL = "http://localhost:34567";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    // 发送比较密文到edgeServer2，返回比较结果json字符串
    public static String sendComparisonDataToEdgeServer4(String clientId1, String clientId2, String cmpCipher) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER4_URL + "/post/comparisonData"))
                    .header("Content-Type", "text/plain")
                    .header("Client-ID1", clientId1)
                    .header("Client-ID2", clientId2)
                    .POST(HttpRequest.BodyPublishers.ofString(cmpCipher))
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
    public static void notifyEdgeServer4FinalResult(String maxId, String minId, long computeTime) {
        try {
            JSONObject json = new JSONObject();
            json.put("maxId", maxId);
            json.put("minId", minId);
            json.put("computeTime", computeTime);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER4_URL + "/post/finalCompareResult"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // 忽略异常
        }
    }
}