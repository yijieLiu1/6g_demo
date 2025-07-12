package org.edgeServer1.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class ComparePairClient {
    private static final String EDGE_SERVER2_URL = "http://localhost:33456";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    // 发送比较密文到edgeServer2，返回比较结果json字符串
    public static String sendComparisonDataToEdgeServer2(String clientId1, String clientId2, String cmpCipher) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER2_URL + "/post/comparisonData"))
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
    public static void notifyEdgeServer2FinalResult(String maxId, String minId, long computeTime) {
        System.out.println("edgeServer1通知edgeServer2保存最终极值: maxId=" + maxId + ", minId=" + minId
                + ", computeTime=" + computeTime);
        try {
            JSONObject json = new JSONObject();
            json.put("maxId", maxId);
            json.put("minId", minId);
            json.put("computeTime", computeTime);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER2_URL + "/post/finalCompareResult"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("成功通知edgeServer2保存极值结果");
            } else {
                System.err.println("通知edgeServer2保存极值失败，状态码: " + response.statusCode() + ", 响应: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("通知edgeServer2保存极值异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}