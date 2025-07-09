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
    private static String receivedCipherText = "";
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
    private static int lastClientCount = 0;
    // 新增：保存sumX2
    private static BigDecimal sumX2 = null;

    private static final java.util.Map<String, String> compareMap = new java.util.LinkedHashMap<>();
    private static final java.util.Set<String> clientIdSet = new java.util.HashSet<>();

    // 在求极值时的调试信息（可选）
    private static int compareCount = 0;
    private static long compareStartTime = 0;

    public static void processAggregatedCipherText(String cipherText, int clientCount) {
        lastClientCount = clientCount;
        receivedCipherText = cipherText;
        if (!cipherText.isEmpty()) {
            try {
                BigInteger c = new BigInteger(cipherText);
                // 使用Paillier解密算法
                BigDecimal m = Paillier.decrypt(c);
                // 保留2位小数，确保正确显示负数
                decryptedText = m.setScale(2, RoundingMode.HALF_UP).toPlainString();

                // 使用ImprovePaillier的SK_DO密钥对解密结果进行加密
                BigDecimal scaled = new BigDecimal(decryptedText).setScale(SCALE,
                        RoundingMode.HALF_UP);
                BigInteger valueToEncrypt = scaled.multiply(BigDecimal.TEN.pow(SCALE)).toBigInteger();
                BigInteger encryptedValue = ImprovePaillier.encrypt(valueToEncrypt, 0);
                // // 只保存，不自动上传
                lastImpaillierCipherText = encryptedValue.toString();
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
        if (squareCipherText == null || squareCipherText.isEmpty() || clientCount == 0) {
            ex2Value = null;
            varianceValue = null;
            sumX2 = null;
            return;
        }
        try {
            BigInteger c = new BigInteger(squareCipherText);
            sumX2 = Paillier.decrypt(c);
            System.out.println("Sum of squares decrypted: " + sumX2);
            ex2Value = sumX2.divide(new BigDecimal(clientCount), 8, RoundingMode.HALF_UP);
            System.out.println("Ex2 Value: " + ex2Value);

            if (meanValue != null && ex2Value != null) {
                BigDecimal meanValueSquared = meanValue.pow(2);
                System.out.println("Mean Value Squared: " + meanValueSquared);
                varianceValue = ex2Value.subtract(meanValueSquared);
                System.out.println("Variance Value: " + varianceValue);
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
        if (clientCount > 0) {
            BigDecimal m = new BigDecimal(decryptedText);
            meanValue = m.divide(new BigDecimal(clientCount), 8,
                    RoundingMode.HALF_UP);
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

    public static String getDecryptedText() {
        String debugInfo = String.format(
                "当前状态:\n" +
                        "是否接收到密文: %s\n" +
                        "密文内容: %s\n" +
                        "解密结果: %s",
                !receivedCipherText.isEmpty() ? "是" : "否",
                receivedCipherText.isEmpty() ? "无" : receivedCipherText,
                decryptedText.isEmpty() ? "无" : decryptedText);

        if (!receivedCipherText.isEmpty()) {
            try {
                BigInteger c = new BigInteger(receivedCipherText);
                BigDecimal m = Paillier.decrypt(c);
                BigInteger n = Paillier.getPublicKey();
                BigInteger halfN = n.divide(BigInteger.TWO);

                debugInfo += String.format(
                        "\n\n解密详细信息:\n" +
                                "接收到的密文: %s\n" +
                                "n的值: %s\n" +
                                "n/2的值: %s\n" +
                                "解密后的原始值: %s\n" +
                                "解密后的整数: %s\n" +
                                "最终结果: %s",
                        c,
                        n,
                        halfN,
                        m,
                        m.multiply(BigDecimal.TEN.pow(8)).toBigInteger(),
                        m.setScale(2, RoundingMode.HALF_UP).toPlainString());
            } catch (Exception e) {
                debugInfo += "\n\n解密错误: " + e.getMessage();
            }
        }

        return debugInfo;
    }

    // 新增：获取ImprovePaillier密文并上传centerServer
    public static String getImpaillierCipherText() {
        if (lastImpaillierCipherText == null || lastImpaillierCipherText.isEmpty()) {
            return "No ImprovePaillier cipher text available.";
        }
        sendEncryptedValueToCenterServer(lastImpaillierCipherText, lastClientCount);
        return lastImpaillierCipherText;
    }

    // // 生成En(r1*m1+r2)密文并发送到centerServer,废弃
    // public static String generateAndSendCompareCipherText() {
    // try {
    // java.security.SecureRandom random = new java.security.SecureRandom();
    // java.math.BigDecimal m1 = decryptedText.isEmpty() ? java.math.BigDecimal.ZERO
    // : new java.math.BigDecimal(decryptedText);
    // java.math.BigInteger r1 = new java.math.BigInteger(
    // "106825203108678901282936524768508786416970440522324880302033274827400270090769");
    // java.math.BigInteger r2 = new java.math.BigInteger(128, random);
    // java.math.BigDecimal blinded = m1.multiply(new
    // java.math.BigDecimal(r1)).add(new java.math.BigDecimal(r2));
    // java.math.BigInteger cipher =
    // org.edgeServer2.utils.Paillier.encrypt(blinded);
    // // 发送到centerServer
    // sendCompareCipherTextToCenterServer(cipher.toString());
    // return cipher.toString();
    // } catch (Exception e) {
    // return "Error generating compare cipher text: " + e.getMessage();
    // }
    // }

    // private static void sendCompareCipherTextToCenterServer(String cipherText) {
    // try {
    // java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
    // .uri(java.net.URI.create(CENTER_SERVER_URL + "/post/compareCipherText"))
    // .header("Content-Type", "text/plain")
    // .header("Server-Type", "server2")
    // .POST(java.net.http.HttpRequest.BodyPublishers.ofString(cipherText))
    // .build();
    // java.net.http.HttpResponse<String> response = httpClient.send(request,
    // java.net.http.HttpResponse.BodyHandlers.ofString());
    // if (response.statusCode() != 200) {
    // System.err.println("Failed to send compare cipher text to center server: " +
    // response.body());
    // }
    // } catch (Exception e) {
    // System.err.println("Error sending compare cipher text to center server: " +
    // e.getMessage());
    // }
    // }

    public static String getMeanResult() {
        if (meanValue == null) {
            return "Mean Result: 未计算或clientCount为0\n";
        }
        return "Mean Result: " + meanValue.setScale(8, RoundingMode.HALF_UP).toPlainString() + "\n";
    }

    // 获取当前所有client值的方差
    public static String getVarianceResult() {
        if (varianceValue == null) {
            return "Variance Result: 未计算或数据不足\n";
        }
        return "方差结果: " + varianceValue.setScale(8, RoundingMode.HALF_UP).toPlainString() + "\n";
    }

    public static String compareAndGetBigger(String clientId1, String clientId2, String cmpCipher) {
        try {
            if (compareCount == 0) {
                compareStartTime = System.currentTimeMillis();
            }
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
        long cost = System.currentTimeMillis() - compareStartTime;
        System.out.println("[compare] 总共解密 " + compareCount + " 次，总耗时 " + cost + " ms");
        compareCount = 0;
        compareStartTime = 0;
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
            json.put("clientCount", lastClientCount);
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