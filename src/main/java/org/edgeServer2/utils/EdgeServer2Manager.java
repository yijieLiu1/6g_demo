package org.edgeServer2.utils;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.json.JSONObject;

public class EdgeServer2Manager {

    private static String decryptedText = "";
    private static final String CENTER_SERVER_URL = "http://localhost:33333";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final int SCALE = 8; // 保留8位小数
    // 新增：保存最大最小id
    private static String maxId = null;
    private static String minId = null;
    private static String lastImpaillierCipherText = "";
    private static BigDecimal meanValue = null;
    private static BigDecimal ex2Value = null;
    private static BigDecimal varianceValue = null;

    // 新增：保存sumX2
    private static BigDecimal sumX2 = null;

    private static final java.util.Map<String, String> compareMap = new java.util.LinkedHashMap<>();
    private static final java.util.Set<String> clientIdSet = new java.util.HashSet<>();

    // 在求极值时的调试信息（可选）
    private static int compareCount = 0;

    // 新增：只保存密文和clientCount
    private static String savedCipherText = null;
    private static String savedSquareCipherText = null;
    private static int savedClientCount = 0;

    public static void saveAggregatedCipherText(String cipherText, String squareCipherText, int clientCount) {
        savedCipherText = cipherText;
        savedSquareCipherText = squareCipherText;
        savedClientCount = clientCount;
    }

    public static void processAggregatedCipherText(String cipherText) {
        System.out.println("edgeServer2开始解密聚合密文......\n密文：" + cipherText);
        if (!cipherText.isEmpty()) {
            try {
                BigInteger c = new BigInteger(cipherText);
                // 使用Paillier解密算法
                BigDecimal m = Paillier.decrypt(c);
                // 保留2位小数，确保正确显示负数
                decryptedText = m.setScale(2, RoundingMode.HALF_UP).toPlainString();
                System.out.println("edgeServer2解密完成......结果: " + decryptedText);

            } catch (Exception e) {
                decryptedText = "Error decrypting: " + e.getMessage();
                System.out.println("解密错误: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            decryptedText = "No cipher text received";
            System.out.println("没有接收到密文");
        }
    }

    // 用于处理计算方差值。
    public static void processVarianceData(String squareCipherText, int clientCount) {
        System.out.println("edgeServer2开始计算方差......sum^2密文: " + squareCipherText + ", 当前client数: " + clientCount);
        if (squareCipherText == null || squareCipherText.isEmpty() || clientCount == 0) {
            ex2Value = null;
            varianceValue = null;
            sumX2 = null;
            return;
        }
        try {
            BigInteger c = new BigInteger(squareCipherText);
            sumX2 = Paillier.decrypt(c);
            System.out.println("edgeServer2解密平方密文结束......sum^2结果 " + sumX2);
            ex2Value = sumX2.divide(new BigDecimal(clientCount), 8, RoundingMode.HALF_UP);

            if (meanValue != null && ex2Value != null) {
                BigDecimal meanValueSquared = meanValue.pow(2);
                varianceValue = ex2Value.subtract(meanValueSquared);
                System.out.println("edgeServer2计算E(x^2)-E(x)^2;E(x^2): " + ex2Value + ", E(x)^2: " + meanValueSquared);
                System.out.println("edgeServer2方差计算结束......方差结果" + varianceValue);
            } else {
                varianceValue = null;
            }
        } catch (Exception e) {
            ex2Value = null;
            varianceValue = null;
        }
    }

    public static void processComparisonData(String cipherText, String clientId1, String clientId2) {
        try {

            String key = clientId1 + "," + clientId2;
            if (compareMap.containsKey(key)) {
                // 已处理过，直接丢弃
                return;
            }
            BigInteger c = new BigInteger(cipherText);
            BigDecimal m_blinded = Paillier.decrypt(c);
            BigInteger M = m_blinded.toBigInteger();
            String outcome;

            // === 记录相邻比较结果 ===
            clientIdSet.add(clientId1);
            clientIdSet.add(clientId2);
            if (M.compareTo(BigInteger.ZERO) < 0) {
                compareMap.put(key, "lt"); // clientId1 < clientId2
            } else {
                compareMap.put(key, "gt"); // clientId1 > clientId2
            }

            if (M.compareTo(BigInteger.ZERO) < 0) {
                outcome = String.format("\n客户端 %s 的数小于客户端 %s.", clientId1, clientId2);
            } else {
                outcome = String.format("\n客户端 %s 的数大于客户端 %s.", clientId1, clientId2);
            }

            System.out.println("Comparison processed. Decrypted value: " + M + ". Outcome: " + outcome);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 解密聚合值在processAggregatedCipherText中已经实现。所以直接使用decryptedText即可。
    public static void processMeanData(int clientCount) {
        System.out.println("edgeServer2开始计算均值......clientCount: " + clientCount);
        if (clientCount > 0) {
            BigDecimal m = new BigDecimal(decryptedText);
            meanValue = m.divide(new BigDecimal(clientCount), 8,
                    RoundingMode.HALF_UP);
            System.out.println("edgeServer2计算均值完成......结果: " + meanValue);
        } else {
            meanValue = null;
        }
    }

    private static void sendEncryptedValueToCenterServer(String encryptedValue, int clientCount) {
        try {
            System.out.println("正在发送数据到Center Server...");
            System.out.println("URL: " + CENTER_SERVER_URL + "/post/aggregatedCipherText");
            System.out.println("数据: " + encryptedValue + ", clientCount: " + clientCount);
            JSONObject json = new JSONObject();
            json.put("encryptedValue", encryptedValue);
            json.put("clientCount", clientCount);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CENTER_SERVER_URL + "/post/aggregatedCipherText"))
                    .header("Content-Type", "application/json")
                    .header("Server-Type", "server2")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Center Server响应状态码: " + response.statusCode());
            System.out.println("Center Server响应内容: " + response.body());
            if (response.statusCode() != 200) {
                System.err.println("Failed to send encrypted value to center server: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending encrypted value to center server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 新增：获取ImprovePaillier密文并上传centerServer
    public static String getImpaillierCipherText() {
        // 使用ImprovePaillier的SK_DO密钥对解密结果进行加密
        BigDecimal scaled = new BigDecimal(decryptedText).setScale(SCALE,
                RoundingMode.HALF_UP);
        BigInteger valueToEncrypt = scaled.multiply(BigDecimal.TEN.pow(SCALE)).toBigInteger();
        BigInteger encryptedValue = ImprovePaillier.encrypt(valueToEncrypt, 0);
        // // 只保存，不自动上传
        lastImpaillierCipherText = encryptedValue.toString();
        sendEncryptedValueToCenterServer(lastImpaillierCipherText, savedClientCount);
        return lastImpaillierCipherText;
    }

    // /get/decryptedText时才解密
    public static String decryptAndGetDecryptedText() {
        if (savedCipherText == null || savedCipherText.isEmpty()) {
            return "No cipher text saved.";
        }

        processAggregatedCipherText(savedCipherText); // 每次都解密

        return "聚合值结果：" + decryptedText;
    }

    // /get/meanResult时才计算均值
    public static String processAndGetMeanResult() {
        if (savedCipherText == null || savedCipherText.isEmpty()) {
            return "No cipher text saved.";
        }
        processAggregatedCipherText(savedCipherText); // 每次都解密
        processMeanData(savedClientCount);
        if (meanValue == null) {
            return "Mean Result: 未计算或clientCount为0\n";
        }
        return "Mean Result: " + meanValue.setScale(8, RoundingMode.HALF_UP).toPlainString() + "\n";
    }

    // /get/varianceResult时才计算方差
    public static String processAndGetVarianceResult() {
        if (savedCipherText == null || savedCipherText.isEmpty() || savedSquareCipherText == null
                || savedSquareCipherText.isEmpty()) {
            return "No cipher text or square cipher text saved.";
        }
        processAggregatedCipherText(savedCipherText); // 解密

        processMeanData(savedClientCount);// 求均值

        processVarianceData(savedSquareCipherText, savedClientCount);
        if (varianceValue == null) {
            return "Variance Result: 未计算或数据不足\n";
        }
        return "方差结果: " + varianceValue.setScale(8, RoundingMode.HALF_UP).toPlainString() + "\n";
    }

    public static String compareAndGetBigger(String clientId1, String clientId2, String cmpCipher) {
        try {

            compareCount++;
            System.out.println("[compare] 第 " + compareCount + " 次解密: " + clientId1 + " vs " + clientId2);
            BigInteger c = new BigInteger(cmpCipher);
            BigDecimal m_blinded = Paillier.decrypt(c);
            BigInteger M = m_blinded.toBigInteger();
            if (M.compareTo(BigInteger.ZERO) < 0) {
                return clientId2;
            } else {
                return clientId1;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveCompareResult(String max, String min) {
        maxId = max;
        minId = min;

        System.out.println("[compare] 总共解密 " + compareCount + " 次");
        compareCount = 0;

    }

    // getCompareResult返回最大最小id
    public static String getCompareResult() {
        return String.format("最大值 clientId: %s, 最小值 clientId: %s", maxId, minId);
    }

    // 新增：获取sumX2的ImprovePaillier密文并上传centerServer
    public static String getAndsendImpaillierVarianceCipherText() {
        if (sumX2 == null) {
            return "No sumX2 value available.";
        }
        BigDecimal scaled = sumX2.setScale(SCALE, RoundingMode.HALF_UP);
        System.out.println("sumx2:" + scaled);
        BigInteger valueToEncrypt = scaled.multiply(BigDecimal.TEN.pow(SCALE)).toBigInteger();
        BigInteger encryptedValue = ImprovePaillier.encrypt(valueToEncrypt, 0);
        // 发送到centerServer
        try {
            JSONObject json = new JSONObject();
            json.put("encryptedValue", encryptedValue.toString());
            json.put("clientCount", savedClientCount);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CENTER_SERVER_URL + "/post/varianceCipherText"))
                    .header("Content-Type", "application/json")
                    .header("Server-Type", "server2")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Center Server响应状态码: " + response.statusCode());
            System.out.println("Center Server响应内容: " + response.body());
            if (response.statusCode() != 200) {
                System.err.println("Failed to send variance cipher text to center server: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending variance cipher text to center server: " + e.getMessage());
            e.printStackTrace();
        }
        return encryptedValue.toString();
    }
}