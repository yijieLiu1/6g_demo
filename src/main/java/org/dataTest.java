package org;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class dataTest {
    public static void main(String[] args) {
        List<BigDecimal> ageList = new ArrayList<>();
        List<String> clientList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("data.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length == 5) {
                        clientList.add(parts[0]); // client编号
                        ageList.add(new BigDecimal(parts[4])); // 年龄
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading data.csv: " + e.getMessage());
            return;
        }
        if (ageList.isEmpty()) {
            System.out.println("data.csv is empty or format error.");
            return;
        }
        int total = ageList.size();
        int half = total / 2;
        List<BigDecimal> firstHalf = ageList.subList(0, half);
        List<BigDecimal> secondHalf = ageList.subList(half, total);
        List<String> firstHalfClients = clientList.subList(0, half);
        List<String> secondHalfClients = clientList.subList(half, total);

        System.out.println("前一半数据统计:");
        printStats(firstHalf, firstHalfClients);
        System.out.println("后一半数据统计:");
        printStats(secondHalf, secondHalfClients);
        System.out.println("全部数据统计:");
        printStats(ageList, clientList);
    }

    @SuppressWarnings("deprecation")
    private static void printStats(List<BigDecimal> list, List<String> clients) {
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal min = list.get(0);
        BigDecimal max = list.get(0);
        int maxIndex = 0; // 记录最大值的位置
        int minIndex = 0; // 记录最小值的位置

        for (int i = 0; i < list.size(); i++) {
            BigDecimal val = list.get(i);
            sum = sum.add(val);
            if (val.compareTo(min) < 0) {
                min = val;
                minIndex = i;
            }
            if (val.compareTo(max) > 0) {
                max = val;
                maxIndex = i;
            }
        }

        BigDecimal avg = sum.divide(BigDecimal.valueOf(list.size()), 8, BigDecimal.ROUND_HALF_UP);
        // 方差
        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal val : list) {
            BigDecimal diff = val.subtract(avg);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(list.size()), 8, BigDecimal.ROUND_HALF_UP);
        System.out.println("数量: " + list.size());
        System.out.println("和: " + sum);
        System.out.println("平均值: " + avg);
        System.out.println("方差: " + variance);
        System.out.println("最大值: " + max + " (" + clients.get(maxIndex) + ")");
        System.out.println("最小值: " + min + " (" + clients.get(minIndex) + ")");
        System.out.println();
    }
}
