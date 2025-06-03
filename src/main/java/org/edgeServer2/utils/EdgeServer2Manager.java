package org.edgeServer2.utils;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class EdgeServer2Manager {
    private static String receivedCipherText = "";
    private static String decryptedText = "";

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