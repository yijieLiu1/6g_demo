package org.edgeServer2.utils;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

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

    public static void processAggregatedCipherText(String cipherText) {
        receivedCipherText = cipherText;
        if (!cipherText.isEmpty()) {
            try {
                BigInteger c = new BigInteger(cipherText);
                System.out.println("接收到的密文: " + c);

                // 使用Paillier解密算法
                BigDecimal m = Paillier.decrypt(c);
                System.out.println("解密后的原始值: " + m);

                // 保留2位小数，确保正确显示负数
                decryptedText = m.setScale(2, RoundingMode.HALF_UP).toPlainString();
                System.out.println("最终显示结果: " + decryptedText);

                // 使用ImprovePaillier的SK_DO密钥对解密结果进行加密
                BigDecimal scaled = new BigDecimal(decryptedText).setScale(SCALE, RoundingMode.HALF_UP);
                BigInteger valueToEncrypt = scaled.multiply(BigDecimal.TEN.pow(SCALE)).toBigInteger();
                BigInteger encryptedValue = ImprovePaillier.encrypt(valueToEncrypt, 0);

                // 发送加密后的结果到CenterServer
                sendEncryptedValueToCenterServer(encryptedValue.toString());

                System.out.println("已发送加密数据到Center Server: " + encryptedValue.toString());
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

    private static void sendEncryptedValueToCenterServer(String encryptedValue) {
        try {
            System.out.println("正在发送数据到Center Server...");
            System.out.println("URL: " + CENTER_SERVER_URL + "/post/aggregatedCipherText");
            System.out.println("数据: " + encryptedValue);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CENTER_SERVER_URL + "/post/aggregatedCipherText"))
                    .header("Content-Type", "text/plain")
                    .header("Server-Type", "server2")
                    .POST(HttpRequest.BodyPublishers.ofString(encryptedValue))
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
}