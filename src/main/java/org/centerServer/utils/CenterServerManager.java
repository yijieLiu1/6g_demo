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
    // 新增：存储来自edgeServer2和4的比较密文
    private static String compareCipherTextFromServer2 = "";
    private static String compareCipherTextFromServer4 = "";

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

    public static void processCompareCipherText(String serverType, String cipherText) {
        if (serverType.equals("server2")) {
            compareCipherTextFromServer2 = cipherText;
        } else if (serverType.equals("server4")) {
            compareCipherTextFromServer4 = cipherText;
        }
    }

    // 新增：解密、求和、判断正负，返回比较结果
    public static String getCompareResult() {
        if (compareCipherTextFromServer2.isEmpty() || compareCipherTextFromServer4.isEmpty()) {
            return "尚未收到全部比较密文";
        }
        try {
            java.math.BigInteger c2 = new java.math.BigInteger(compareCipherTextFromServer2);
            java.math.BigInteger c4 = new java.math.BigInteger(compareCipherTextFromServer4);
            // edgeServer2用默认Paillier，edgeServer4用NEW_PAILLIER
            java.math.BigDecimal dec2 = org.centerServer.utils.Paillier.decrypt(c2);
            java.math.BigDecimal dec4 = org.centerServer.utils.Paillier.NEW_PAILLIER.decryptInst(c4);
            java.math.BigDecimal sum = dec2.add(dec4);
            String result;
            int cmp = sum.compareTo(java.math.BigDecimal.ZERO);
            if (cmp > 0) {
                result = "edgeServer2聚合值大于edgeServer4聚合值";
            } else if (cmp < 0) {
                result = "edgeServer2聚合值小于edgeServer4聚合值";
            } else {
                result = "二者相等";
            }
            return String.format("解密结果：\nedgeServer2: %s\nedgeServer4: %s\n求和: %s\n比较结论: %s", dec2.toPlainString(),
                    dec4.toPlainString(), sum.toPlainString(), result);
        } catch (Exception e) {
            return "比较解密出错: " + e.getMessage();
        }
    }
}