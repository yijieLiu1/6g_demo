package org.centerServer.utils;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

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

    public static void processAggregatedCipherText(String serverType, String cipherText, int clientCount) {
        receivedCipherTextMap.put(serverType, cipherText);
        clientCountMap.put(serverType, clientCount);
        // 如果收到的密文数大于等于2，才进行聚合
    }

    public static String getDecryptedText() {
        System.out.println("Center Server开始解密聚合值......");
        if (receivedCipherTextMap.size() >= 2) {
            try {
                Collection<String> values = receivedCipherTextMap.values();
                BigInteger result = null;
                BigInteger n2 = ImprovePaillier.getN()
                        .multiply(ImprovePaillier.getN());
                for (String v : values) {
                    BigInteger c = new BigInteger(v);
                    if (result == null) {
                        result = c;
                    } else {
                        result = result.multiply(c).mod(n2);
                    }
                }
                aggregatedCipherText = result.toString();
                // 解密
                BigInteger decryptedBigInt = ImprovePaillier.decrypt(result);
                BigDecimal m = new BigDecimal(decryptedBigInt)
                        .divide(BigDecimal.TEN.pow(SCALE), SCALE, RoundingMode.HALF_UP);
                decryptedText = m.setScale(2, RoundingMode.HALF_UP).toPlainString();
                System.out.println("Center Server聚合密文解密结果: " + decryptedText);
            } catch (Exception e) {
                System.err.println("Center Server处理密文错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
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
        getDecryptedText(); // 确保解密结果已生成
        int totalClientCount = clientCountMap.values().stream().mapToInt(Integer::intValue).sum();
        if (totalClientCount == 0 || decryptedText.isEmpty()) {
            return "尚未收到足够的密文或明文结果未生成";
        }
        try {
            System.out.println("Center Server开始计算均值......");
            java.math.BigDecimal sum = new java.math.BigDecimal(decryptedText);
            java.math.BigDecimal mean = sum.divide(java.math.BigDecimal.valueOf(totalClientCount), 8,
                    java.math.RoundingMode.HALF_UP);
            System.out.println("Center Server均值计算结果: " + mean.toPlainString());
            return "当前均值: " + mean.toPlainString();
        } catch (Exception e) {
            return "均值计算出错: " + e.getMessage();
        }
    }

    // 新增：处理方差密文
    public static void processVarianceCipherText(String serverType, String encryptedValue, int clientCount) {
        varianceCipherTextMap.put(serverType, encryptedValue);
        // 如果收到的密文数大于等于2，才进行聚合

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
    }

    // 极值比较逻辑
    private static void compareExtreme() {
        if (maxCipherText1 != null && maxCipherText3 != null) {
            // max: server1用Paillier，server3用NEW_PAILLIER
            System.out.println("maxCipherText1: " + maxCipherText1);
            System.out.println("maxCipherText3: " + maxCipherText3);
            BigDecimal max1 = Paillier
                    .decrypt(new BigInteger(maxCipherText1));
            System.out.println("max1: " + max1.toPlainString());
            BigDecimal max3 = Paillier.NEW_PAILLIER
                    .decryptInst(new BigInteger(maxCipherText3));
            System.out.println("max3: " + max3.toPlainString());
            BigDecimal diff = max1.subtract(max3); // r1(m1-m2)+r2+r3
            System.out.println("max1 - max3: " + diff.toPlainString());
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                extremeMaxId = maxClientId1;
            } else {
                extremeMaxId = maxClientId3;
            }
        }
        if (minCipherText1 != null && minCipherText3 != null) {
            System.out.println("minCipherText1: " + minCipherText1);
            System.out.println("minCipherText3: " + minCipherText3);
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
        System.out.println("Center Server开始计算方差......");
        getDecryptedText(); // 确保解密结果已生成

        // 直接计算均值，避免重复调用getMeanResult()
        int totalClientCount = clientCountMap.values().stream().mapToInt(Integer::intValue).sum();
        if (totalClientCount == 0 || decryptedText.isEmpty()) {
            return "尚未收到足够的密文或明文结果未生成";
        }

        // 计算均值
        java.math.BigDecimal sum = new java.math.BigDecimal(decryptedText);
        java.math.BigDecimal mean = sum.divide(java.math.BigDecimal.valueOf(totalClientCount), 8,
                java.math.RoundingMode.HALF_UP);
        System.out.println("Center Server均值计算结果: " + mean.toPlainString());

        if (varianceCipherTextMap.size() >= 2) {
            try {
                Collection<String> values = varianceCipherTextMap.values();
                BigInteger result = null;
                BigInteger n2 = ImprovePaillier.getN()
                        .multiply(ImprovePaillier.getN());
                for (String v : values) {

                    BigInteger c = new BigInteger(v);
                    if (result == null) {
                        result = c;
                    } else {
                        result = result.multiply(c).mod(n2);
                    }
                }
                // 解密
                BigInteger decryptedBigInt = ImprovePaillier.decrypt(result);
                BigDecimal m = new BigDecimal(decryptedBigInt)
                        .divide(BigDecimal.TEN.pow(SCALE), SCALE, RoundingMode.HALF_UP);
                varianceDecryptedText = m.setScale(8, RoundingMode.HALF_UP).toPlainString();
            } catch (Exception e) {
                System.err.println("Center Server处理方差密文错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (varianceDecryptedText.isEmpty() || decryptedText.isEmpty()) {
            return "尚未收到足够的密文或明文结果未生成";
        }
        try {
            BigDecimal sumX2 = new BigDecimal(varianceDecryptedText);
            System.out.println("sumX^2解密结果: " + sumX2.toPlainString());
            BigDecimal ex2 = sumX2.divide(BigDecimal.valueOf(totalClientCount), SCALE, RoundingMode.HALF_UP);
            System.out.println("方差E(x^2): " + ex2.toPlainString());
            // 使用已计算的均值
            BigDecimal mean2 = mean.pow(2);
            BigDecimal variance = ex2.subtract(mean2);
            System.out.println("Center Server方差计算结束: " + variance.toPlainString() +
                    "\nE(x^2): " + ex2.toPlainString() +
                    "\nE(x)^2: " + mean2.toPlainString());

            return "E(x^2): " + ex2.toPlainString() + "\nE(x)^2: " + mean2.toPlainString() + "\n方差: "
                    + variance.toPlainString();
        } catch (Exception e) {
            return "方差计算出错: " + e.getMessage();
        }
    }
}