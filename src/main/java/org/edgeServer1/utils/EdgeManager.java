package org.edgeServer1.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.net.URI;
import org.edgeServer2.utils.Paillier;
import org.json.JSONObject;

public class EdgeManager {
    static final ConcurrentHashMap<String, String> clientCipherTexts = new ConcurrentHashMap<>();
    private static String aggregatedCipherText = "";
    private static final ConcurrentHashMap<String, String> clientSquareCipherTexts = new ConcurrentHashMap<>();
    private static String aggregatedSquareCipherText = "";
    private static final String EDGE_SERVER2_URL = "http://localhost:33456";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ConcurrentHashMap<String, String> clientIntervals = new ConcurrentHashMap<>();

    // 极值结果本地保存，用于centerServer求极值。
    private static String lastMaxClientId = null;
    private static String lastMinClientId = null;

    // 在Post/cipherText请求中注册客户端
    // clientId: 客户端ID
    // cipherText: 客户端加密的明文
    // squareCipherText: 客户端加密的明文平方-求方差
    // interval: 客户端所属区间标签
    public static void registerClient(String clientId, String cipherText, String squareCipherText, String interval) {
        clientCipherTexts.put(clientId, cipherText);
        clientSquareCipherTexts.put(clientId, squareCipherText);
        clientIntervals.put(clientId, interval);
    }

    // 在执行/get/sumcipherText时更新聚合密文和平分密文,进行连乘。
    private static void updateAggregatedCipherText() {
        if (clientCipherTexts.isEmpty()) {
            aggregatedCipherText = "";
            return;
        }

        // 初始化聚合结果为第一个密文
        BigInteger result = new BigInteger(clientCipherTexts.values().iterator().next());

        // 对剩余的密文进行乘法运算（对应明文加法）
        for (String cipherText : clientCipherTexts.values()) {
            if (cipherText.equals(clientCipherTexts.values().iterator().next())) {
                continue; // 跳过第一个已经使用的密文
            }
            BigInteger currentCipher = new BigInteger(cipherText);
            // 在模n^2下进行乘法运算
            result = result.multiply(currentCipher).mod(Paillier.getN2());
        }

        aggregatedCipherText = result.toString();
        // 发送聚合后的密文到edgeServer2

    }

    // 在执行/get/sumcipherText时更新聚合密文和平分密文,进行连乘。
    private static void updateAggregatedSquareCipherText() {
        if (clientSquareCipherTexts.isEmpty()) {
            aggregatedSquareCipherText = "";
            return;
        }
        BigInteger result = new BigInteger(clientSquareCipherTexts.values().iterator().next());
        for (String squareCipherText : clientSquareCipherTexts.values()) {
            if (squareCipherText.equals(clientSquareCipherTexts.values().iterator().next())) {
                continue;
            }
            BigInteger currentCipher = new BigInteger(squareCipherText);
            result = result.multiply(currentCipher).mod(Paillier.getN2());
        }
        aggregatedSquareCipherText = result.toString();
    }

    // 在执行/get/sumcipherText时把聚合密文+平分密文发送到edgeServer2
    public static void sendAggregatedCipherTextToEdgeServer2(String cipherText, String squareCipherText) {
        try {
            JSONObject json = new JSONObject();
            json.put("cipherText", cipherText);
            json.put("squareCipherText", squareCipherText);
            json.put("clientCount", getClientCount());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER2_URL + "/post/aggregatedCipherText"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Failed to send aggregated cipher text to edge server 2: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending aggregated cipher text to edge server 2: " + e.getMessage());
        }
    }

    // 生成两个客户端密文的比较密文
    // En(r1*(m1-m2)+r2)
    public static String generateComparisonCipherText(String clientId1, String clientId2) {
        String cipherText1Str = clientCipherTexts.get(clientId1);
        String cipherText2Str = clientCipherTexts.get(clientId2);
        SecureRandom random = new SecureRandom();

        if (cipherText1Str == null || cipherText2Str == null) {
            return "Error: Client cipher not found.";
        }

        BigInteger cipherText1 = new BigInteger(cipherText1Str);
        BigInteger cipherText2 = new BigInteger(cipherText2Str);
        BigInteger nSquared = Paillier.getN2();
        BigInteger r1 = new BigInteger(512, random);
        BigInteger r2 = new BigInteger(256, random);
        if (r1.compareTo(r2) <= 0) {
            r1 = r1.add(r2);
        }
        // 构建出En(r1*(m1-m2)+r2)的密文
        BigInteger cipherText2Inv = cipherText2.modInverse(nSquared);
        BigInteger diffCipherText = cipherText1.multiply(cipherText2Inv).mod(nSquared);
        BigInteger blindedDiffCipherText = diffCipherText.modPow(r1, nSquared);

        BigInteger r2CipherText = Paillier.encrypt(r2);
        BigInteger finalCipherText = blindedDiffCipherText.multiply(r2CipherText).mod(nSquared);

        return finalCipherText.toString();
    }

    public static String getAggregatedCipherText() {
        updateAggregatedCipherText();
        return aggregatedCipherText;
    }

    public static String getAggregatedSquareCipherText() {
        updateAggregatedSquareCipherText();
        return aggregatedSquareCipherText;
    }

    public static int getClientCount() {
        return clientCipherTexts.size();
    }

    public static java.util.List<String> getAllClientIds() {
        return new java.util.ArrayList<>(clientCipherTexts.keySet());
    }

    // 新增：最小最大对法查找极值
    public static String findExtremes() {
        java.util.List<String> clientIds = getAllClientIds();
        if (clientIds.size() < 2) {
            return "Not enough clients to compare";
        }

        int n = clientIds.size();
        java.util.List<String> maxCandidates = new java.util.ArrayList<>();
        java.util.List<String> minCandidates = new java.util.ArrayList<>();

        // 1. 两两分组比较
        for (int i = 0; i < n - 1; i += 2) {
            String a = clientIds.get(i);
            String b = clientIds.get(i + 1);
            System.out.println("[EdgeManager] 分组: " + a + " vs " + b);

            String cmpCipher = generateComparisonCipherText(a, b);
            String compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(a, b,
                    cmpCipher);
            while (compareResult == null) {
                System.out.println("[EdgeManager] 网络或服务端异常，重试: " + a + " vs " + b);
                compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(a, b,
                        cmpCipher);
            }

            org.json.JSONObject resultJson = new org.json.JSONObject(compareResult);
            String bigger = resultJson.getString("bigger");
            String smaller = resultJson.getString("smaller");
            System.out.println("[EdgeManager] 结果: bigger=" + bigger + ", smaller=" + smaller);
            maxCandidates.add(bigger);
            minCandidates.add(smaller);
        }

        if (n % 2 == 1) {
            String last = clientIds.get(n - 1);
            System.out.println("[EdgeManager] 奇数个，最后一个 " + last + " 进入两个候选区");
            maxCandidates.add(last);
            minCandidates.add(last);
        }

        // 2. 在maxCandidates中找最大
        String maxId = maxCandidates.get(0);
        for (int i = 1; i < maxCandidates.size(); i++) {
            String challenger = maxCandidates.get(i);
            System.out.println("[EdgeManager] 最大候选区比较: " + maxId + " vs " + challenger);

            String cmpCipher = generateComparisonCipherText(maxId, challenger);
            String compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(maxId,
                    challenger, cmpCipher);
            while (compareResult == null) {
                System.out.println("[EdgeManager] 网络或服务端异常，重试: " + maxId + " vs " + challenger);
                compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(maxId,
                        challenger, cmpCipher);
            }

            org.json.JSONObject resultJson = new org.json.JSONObject(compareResult);
            String bigger = resultJson.getString("bigger");
            System.out.println("[EdgeManager] 最大候选区结果: bigger=" + bigger);
            maxId = bigger;
        }

        // 3. 在minCandidates中找最小
        String minId = minCandidates.get(0);
        for (int i = 1; i < minCandidates.size(); i++) {
            String challenger = minCandidates.get(i);
            System.out.println("[EdgeManager] 最小候选区比较: " + minId + " vs " + challenger);

            String cmpCipher = generateComparisonCipherText(minId, challenger);
            String compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(minId,
                    challenger, cmpCipher);
            while (compareResult == null) {
                System.out.println("[EdgeManager] 网络或服务端异常，重试: " + minId + " vs " + challenger);
                compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(minId,
                        challenger, cmpCipher);
            }

            org.json.JSONObject resultJson = new org.json.JSONObject(compareResult);
            String smaller = resultJson.getString("smaller");
            System.out.println("[EdgeManager] 最小候选区结果: smaller=" + smaller);
            minId = smaller;
        }

        System.out.println("[EdgeManager] 最终最大值 clientId: " + maxId + ", 最小值 clientId: " + minId);

        // 通知edgeServer2保存极值
        org.edgeServer1.utils.ComparePairClient.notifyEdgeServer2FinalResult(maxId, minId);

        return "极值比较完成";
    }

    // 新增：只在最大区间找最大值，只在最小区间找最小值
    // 提前划分好区间。相比于之前的方案，省去了找最大client的候选区和最小client候选区的代码。
    // 直接在最大区间内找最大值，在最小区间内找最小值。数据量通常更小
    public static String findExtremesByInterval() {
        java.util.List<String> clientIds = getAllClientIds();
        if (clientIds.size() < 2) {
            return "Not enough clients to compare";
        }
        // 一次遍历分组所有client
        java.util.Map<String, java.util.List<String>> intervalToClients = new java.util.HashMap<>();
        for (String clientId : clientIds) {
            String interval = clientIntervals.getOrDefault(clientId, "");
            intervalToClients.computeIfAbsent(interval, k -> new java.util.ArrayList<>()).add(clientId);
        }
        if (intervalToClients.isEmpty()) {
            return "No interval data";
        }
        // 直接找最小和最大区间标签
        java.util.Set<String> intervalSet = intervalToClients.keySet();
        String minInterval = java.util.Collections.min(intervalSet);
        String maxInterval = java.util.Collections.max(intervalSet);
        java.util.List<String> minClients = intervalToClients.get(minInterval);
        java.util.List<String> maxClients = intervalToClients.get(maxInterval);
        if (minClients == null || minClients.isEmpty() || maxClients == null || maxClients.isEmpty()) {
            return "No clients in min or max interval";
        }
        // 在maxClients中找最大
        String maxId = maxClients.get(0);
        for (int i = 1; i < maxClients.size(); i++) {
            String challenger = maxClients.get(i);
            String cmpCipher = generateComparisonCipherText(maxId, challenger);
            String compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(maxId,
                    challenger, cmpCipher);
            while (compareResult == null) {
                compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(maxId,
                        challenger, cmpCipher);
            }
            org.json.JSONObject resultJson = new org.json.JSONObject(compareResult);
            String bigger = resultJson.getString("bigger");
            maxId = bigger;
        }
        // 在minClients中找最小
        String minId = minClients.get(0);
        for (int i = 1; i < minClients.size(); i++) {
            String challenger = minClients.get(i);
            String cmpCipher = generateComparisonCipherText(minId, challenger);
            String compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(minId,
                    challenger, cmpCipher);
            while (compareResult == null) {
                compareResult = org.edgeServer1.utils.ComparePairClient.sendComparisonDataToEdgeServer2(minId,
                        challenger, cmpCipher);
            }
            org.json.JSONObject resultJson = new org.json.JSONObject(compareResult);
            String smaller = resultJson.getString("smaller");
            minId = smaller;
        }
        // 通知edgeServer2保存极值
        org.edgeServer1.utils.ComparePairClient.notifyEdgeServer2FinalResult(maxId, minId);
        // 本地保存极值
        lastMaxClientId = maxId;
        lastMinClientId = minId;
        return "极值比较完成（区间优化/排序优化）";
    }

    // 获取最近一次极值结果
    public static String getLastMaxClientId() {
        return lastMaxClientId;
    }

    public static String getLastMinClientId() {
        return lastMinClientId;
    }

}