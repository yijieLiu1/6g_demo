package org.centerServer.utils;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class CenterServerManager {
    // 用于存储所有收到的密文，key为serverType，value为密文
    private static final Map<String, String> receivedCipherTextMap = new ConcurrentHashMap<>();
    private static String aggregatedCipherText = "";
    private static String decryptedText = "";
    private static final int SCALE = 8; // 保留8位小数
    // 新增：存储来自edgeServer2和4的比较密文
    private static String compareCipherTextFromServer2 = "";
    private static String compareCipherTextFromServer4 = "";
    private static final Map<String, Integer> clientCountMap = new ConcurrentHashMap<>();

    public static void processAggregatedCipherText(String serverType, String cipherText, int clientCount) {
        receivedCipherTextMap.put(serverType, cipherText);
        clientCountMap.put(serverType, clientCount);
        // 如果收到的密文数大于等于2，才进行聚合
        if (receivedCipherTextMap.size() >= 2) {
            try {
                java.util.Collection<String> values = receivedCipherTextMap.values();
                java.math.BigInteger result = null;
                java.math.BigInteger n2 = org.centerServer.utils.ImprovePaillier.getN()
                        .multiply(org.centerServer.utils.ImprovePaillier.getN());
                for (String v : values) {
                    java.math.BigInteger c = new java.math.BigInteger(v);
                    if (result == null) {
                        result = c;
                    } else {
                        result = result.multiply(c).mod(n2);
                    }
                }
                aggregatedCipherText = result.toString();
                // 解密
                java.math.BigInteger decryptedBigInt = org.centerServer.utils.ImprovePaillier.decrypt(result);
                java.math.BigDecimal m = new java.math.BigDecimal(decryptedBigInt)
                        .divide(java.math.BigDecimal.TEN.pow(SCALE), SCALE, java.math.RoundingMode.HALF_UP);
                decryptedText = m.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
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
        for (Map.Entry<String, String> entry : receivedCipherTextMap.entrySet()) {
            response.append(entry.getKey()).append("密文: ")
                    .append(entry.getValue().isEmpty() ? "未收到" : entry.getValue()).append("\n");
        }
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

    // 新增：获取均值方法
    public static String getMeanResult() {
        int totalClientCount = clientCountMap.values().stream().mapToInt(Integer::intValue).sum();
        if (totalClientCount == 0 || decryptedText.isEmpty()) {
            return "尚未收到足够的密文或明文结果未生成";
        }
        try {
            java.math.BigDecimal sum = new java.math.BigDecimal(decryptedText);
            java.math.BigDecimal mean = sum.divide(java.math.BigDecimal.valueOf(totalClientCount), 8,
                    java.math.RoundingMode.HALF_UP);
            return "当前均值: " + mean.toPlainString();
        } catch (Exception e) {
            return "均值计算出错: " + e.getMessage();
        }
    }
}