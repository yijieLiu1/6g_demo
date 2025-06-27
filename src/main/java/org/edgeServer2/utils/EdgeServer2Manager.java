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

    private static class ComparisonResult {
        String decryptedValue;
        String outcomeMessage;
    }

    private static ComparisonResult lastComparisonResult = null;
    private static String lastImpaillierCipherText = "";
    private static BigDecimal meanValue = null;
    private static BigDecimal ex2Value = null;
    private static Double varianceValue = null;
    private static int lastClientCount = 0;

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
                // 计算均值
                processMeanData(m, clientCount);
                // 使用ImprovePaillier的SK_DO密钥对解密结果进行加密
                BigDecimal scaled = new BigDecimal(decryptedText).setScale(SCALE, RoundingMode.HALF_UP);
                BigInteger valueToEncrypt = scaled.multiply(BigDecimal.TEN.pow(SCALE)).toBigInteger();
                BigInteger encryptedValue = ImprovePaillier.encrypt(valueToEncrypt, 0);
                // 只保存，不自动上传
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
            return;
        }
        try {
            BigInteger c = new BigInteger(squareCipherText);
            BigDecimal sumX2 = Paillier.decrypt(c);
            System.out.println("Sum of squares decrypted: " + sumX2);
            ex2Value = sumX2.divide(new BigDecimal(clientCount), 8, RoundingMode.HALF_UP);
            if (meanValue != null && ex2Value != null) {
                varianceValue = ex2Value.doubleValue() - Math.pow(meanValue.doubleValue(), 2);
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
            BigInteger c = new BigInteger(cipherText);
            BigDecimal m_blinded = Paillier.decrypt(c);
            BigInteger M = m_blinded.toBigInteger();
            BigInteger n = Paillier.getPublicKey();
            BigInteger halfN = n.divide(BigInteger.TWO);
            String outcome;

            if (M.compareTo(BigInteger.ZERO) < 0) {
                outcome = String.format("\n客户端 %s 的数小于客户端 %s.", clientId1, clientId2);
            } else {
                outcome = String.format("\n客户端 %s 的数大于客户端 %s.", clientId1, clientId2);
            }

            lastComparisonResult = new ComparisonResult();
            lastComparisonResult.decryptedValue = M.toString();
            lastComparisonResult.outcomeMessage = outcome;

            System.out.println("Comparison processed. Decrypted value: " + M + ". Outcome: " + outcome);
        } catch (Exception e) {
            lastComparisonResult = new ComparisonResult();
            lastComparisonResult.decryptedValue = "Error";
            lastComparisonResult.outcomeMessage = "Error processing comparison: " + e.getMessage();
            e.printStackTrace();
        }
    }

    public static String getCompareResult() {
        if (lastComparisonResult == null) {
            return "No comparison has been performed yet.";
        }
        return String.format(
                "Decrypted Value (De[En(r1*(m1-m2)+r2)]): %s\nComparison Result: %s",
                lastComparisonResult.decryptedValue,
                lastComparisonResult.outcomeMessage);
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

    public static String getReceivedCipherText() {
        return receivedCipherText.isEmpty() ? "No cipher text received" : receivedCipherText;
    }

    // 新增：获取ImprovePaillier密文并上传centerServer
    public static String getImpaillierCipherText() {
        if (lastImpaillierCipherText == null || lastImpaillierCipherText.isEmpty()) {
            return "No ImprovePaillier cipher text available.";
        }
        sendEncryptedValueToCenterServer(lastImpaillierCipherText, lastClientCount);
        return lastImpaillierCipherText;
    }

    // 新增：生成En(r1*m1+r2)密文并发送到centerServer
    public static String generateAndSendCompareCipherText() {
        try {
            java.security.SecureRandom random = new java.security.SecureRandom();
            java.math.BigDecimal m1 = decryptedText.isEmpty() ? java.math.BigDecimal.ZERO
                    : new java.math.BigDecimal(decryptedText);
            java.math.BigInteger r1 = new java.math.BigInteger(256, random);
            java.math.BigInteger r2 = new java.math.BigInteger(128, random);
            java.math.BigDecimal blinded = m1.multiply(new java.math.BigDecimal(r1)).add(new java.math.BigDecimal(r2));
            java.math.BigInteger cipher = org.edgeServer2.utils.Paillier.encrypt(blinded);
            // 发送到centerServer
            sendCompareCipherTextToCenterServer(cipher.toString());
            return cipher.toString();
        } catch (Exception e) {
            return "Error generating compare cipher text: " + e.getMessage();
        }
    }

    private static void sendCompareCipherTextToCenterServer(String cipherText) {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(CENTER_SERVER_URL + "/post/compareCipherText"))
                    .header("Content-Type", "text/plain")
                    .header("Server-Type", "server2")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(cipherText))
                    .build();
            java.net.http.HttpResponse<String> response = httpClient.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Failed to send compare cipher text to center server: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending compare cipher text to center server: " + e.getMessage());
        }
    }

    public static String getMeanResult() {
        if (meanValue == null) {
            return "Mean Result: 未计算或clientCount为0\n";
        }
        return "Mean Result: " + meanValue.setScale(8, RoundingMode.HALF_UP).toPlainString() + "\n";
    }

    public static void processMeanData(BigDecimal decryptedValue, int clientCount) {
        if (clientCount > 0) {
            meanValue = decryptedValue.divide(new BigDecimal(clientCount), 8, RoundingMode.HALF_UP);
        } else {
            meanValue = null;
        }
    }

    // 获取当前所有client值的方差
    public static String getVarianceResult() {
        if (varianceValue == null) {
            return "Variance Result: 未计算或数据不足\n";
        }
        return "Variance Result: " + String.format("%.8f", varianceValue) + "\n";
    }
}