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
    private static final Map<String, Integer> clientCountMap = new ConcurrentHashMap<>();
    // 新增：用于存储所有收到的方差密文，key为serverType，value为密文
    private static final Map<String, String> varianceCipherTextMap = new ConcurrentHashMap<>();
    private static String varianceDecryptedText = "";
    // 新增：存储均值
    // 新增：存储极值密文和clientId
    private static String maxClientId1 = null, maxCipherText1 = null;
    private static String maxClientId3 = null, maxCipherText3 = null;
    private static String minClientId1 = null, minCipherText1 = null;
    private static String minClientId3 = null, minCipherText3 = null;
    private static String extremeMaxId = null, extremeMinId = null;
    private static boolean compared = false;

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

    // 新增：处理方差密文
    public static void processVarianceCipherText(String serverType, String encryptedValue, int clientCount) {
        varianceCipherTextMap.put(serverType, encryptedValue);
        // 如果收到的密文数大于等于2，才进行聚合
        if (varianceCipherTextMap.size() >= 2) {
            try {
                java.util.Collection<String> values = varianceCipherTextMap.values();
                java.math.BigInteger result = null;
                java.math.BigInteger n2 = org.centerServer.utils.ImprovePaillier.getN()
                        .multiply(org.centerServer.utils.ImprovePaillier.getN());
                for (String v : values) {

                    java.math.BigInteger c = new java.math.BigInteger(v);
                    System.out.println("当前的sumX2密文" + c);
                    if (result == null) {
                        result = c;
                    } else {
                        result = result.multiply(c).mod(n2);
                    }
                }
                // 解密
                java.math.BigInteger decryptedBigInt = org.centerServer.utils.ImprovePaillier.decrypt(result);
                System.out.println("Center Server方差密文解密结果: " + decryptedBigInt.toString());
                java.math.BigDecimal m = new java.math.BigDecimal(decryptedBigInt)
                        .divide(java.math.BigDecimal.TEN.pow(SCALE), SCALE, java.math.RoundingMode.HALF_UP);
                varianceDecryptedText = m.setScale(8, java.math.RoundingMode.HALF_UP).toPlainString();
                System.out.println("Center Server方差E(x^2)解密结果: " + varianceDecryptedText);
            } catch (Exception e) {
                System.err.println("Center Server处理方差密文错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 处理极值密文
    public static void processExtremeCipherText(String serverType, String maxClientId, String maxCipherText,
            String minClientId, String minCipherText) {
        if (serverType.equals("edgeServer1")) {
            maxClientId1 = maxClientId;
            maxCipherText1 = maxCipherText;
            minClientId1 = minClientId;
            minCipherText1 = minCipherText;
        } else if (serverType.equals("edgeServer3")) {
            maxClientId3 = maxClientId;
            maxCipherText3 = maxCipherText;
            minClientId3 = minClientId;
            minCipherText3 = minCipherText;
        }
        compared = false;
    }

    // 极值比较逻辑
    private static void compareExtreme() {
        if (compared)
            return;
        if (maxCipherText1 != null && maxCipherText3 != null) {
            // max: server1用Paillier，server3用NEW_PAILLIER
            BigDecimal max1 = Paillier
                    .decrypt(new BigInteger(maxCipherText1));
            BigDecimal max3 = Paillier.NEW_PAILLIER
                    .decryptInst(new BigInteger(maxCipherText3));
            BigDecimal diff = max1.subtract(max3); // r1(m1-m2)+r2+r3
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                extremeMaxId = maxClientId1;
            } else {
                extremeMaxId = maxClientId3;
            }
        }
        if (minCipherText1 != null && minCipherText3 != null) {
            // min: server1用Paillier，server3用NEW_PAILLIER
            BigDecimal min1 = Paillier
                    .decrypt(new BigInteger(minCipherText1));
            BigDecimal min3 = Paillier.NEW_PAILLIER
                    .decryptInst(new BigInteger(minCipherText3));
            BigDecimal diff = min1.subtract(min3); // r1(m1-m2)+r2+r3
            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                extremeMinId = minClientId1;
            } else {
                extremeMinId = minClientId3;
            }
        }
        compared = true;
    }

    // 获取极值结果
    public static String getExtremeResult() {
        compareExtreme();
        if (extremeMaxId == null || extremeMinId == null) {
            return "尚未收到足够的极值密文";
        }
        return "最大值 clientId: " + extremeMaxId + ", 最小值 clientId: " + extremeMinId;
    }

    // 获取方差结果
    public static String getVarianceResult() {
        if (varianceDecryptedText.isEmpty() || decryptedText.isEmpty()) {
            return "尚未收到足够的密文或明文结果未生成";
        }
        try {
            int totalClientCount = clientCountMap.values().stream().mapToInt(Integer::intValue).sum();

            BigDecimal sumX2 = new BigDecimal(varianceDecryptedText);
            System.out.println("方差x^2解密结果: " + sumX2.toPlainString());
            System.out.println("sumX2");
            BigDecimal ex2 = sumX2.divide(BigDecimal.valueOf(totalClientCount), SCALE, RoundingMode.HALF_UP);
            System.out.println("方差E(x^2): " + ex2.toPlainString());
            // 均值
            BigDecimal sum = new BigDecimal(decryptedText);

            BigDecimal mean = sum.divide(BigDecimal.valueOf(totalClientCount), SCALE, RoundingMode.HALF_UP);
            BigDecimal mean2 = mean.pow(2);
            BigDecimal variance = ex2.subtract(mean2);
            return "E(x^2): " + ex2.toPlainString() + "\nmean^2: " + mean2.toPlainString() + "\n方差: "
                    + variance.toPlainString();
        } catch (Exception e) {
            return "方差计算出错: " + e.getMessage();
        }
    }
}