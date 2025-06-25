package org.centerServer.utils;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CenterServerManager {
    private static String receivedCipherTextFromServer2 = "";
    private static String receivedCipherTextFromServer4 = "";
    private static String aggregatedCipherText = "";
    private static String decryptedText = "";
    private static final int SCALE = 8; // 保留8位小数

    public static void processAggregatedCipherText(String serverType, String cipherText) {
        if (serverType.equals("server2")) {
            receivedCipherTextFromServer2 = cipherText;
        } else if (serverType.equals("server4")) {
            receivedCipherTextFromServer4 = cipherText;
        }

        // 如果两个服务器的密文都已收到，则进行聚合
        if (!receivedCipherTextFromServer2.isEmpty() && !receivedCipherTextFromServer4.isEmpty()) {
            try {
                BigInteger c1 = new BigInteger(receivedCipherTextFromServer2);
                BigInteger c2 = new BigInteger(receivedCipherTextFromServer4);

                // 在模n^2下进行乘法运算（对应明文加法）
                BigInteger n2 = ImprovePaillier.getN().multiply(ImprovePaillier.getN());
                BigInteger result = c1.multiply(c2).mod(n2);
                aggregatedCipherText = result.toString();

                // 使用SK_DO解密
                BigInteger decryptedBigInt = ImprovePaillier.decrypt(result);
                // 将BigInteger转换为BigDecimal（除以10^8）
                BigDecimal m = new BigDecimal(decryptedBigInt).divide(BigDecimal.TEN.pow(SCALE), SCALE,
                        RoundingMode.HALF_UP);
                decryptedText = m.setScale(2, RoundingMode.HALF_UP).toPlainString();

                System.out.println("Center Server解密结果: " + decryptedText);
            } catch (Exception e) {
                System.err.println("Center Server处理密文错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static String getDecryptedText() {
        StringBuilder response = new StringBuilder();
        response.append("Center Server状态:\n");
        response.append("Server2密文: ")
                .append(receivedCipherTextFromServer2.isEmpty() ? "未收到" : receivedCipherTextFromServer2).append("\n");
        response.append("Server4密文: ")
                .append(receivedCipherTextFromServer4.isEmpty() ? "未收到" : receivedCipherTextFromServer4).append("\n");
        response.append("聚合密文: ").append(aggregatedCipherText.isEmpty() ? "未生成" : aggregatedCipherText).append("\n");
        response.append("解密结果: ").append(decryptedText.isEmpty() ? "未解密" : decryptedText);
        return response.toString();
    }
}