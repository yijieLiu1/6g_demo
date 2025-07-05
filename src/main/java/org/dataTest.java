package org;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class dataTest {
    public static void main(String[] args) {
        List<BigDecimal> dataList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("data.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    dataList.add(new BigDecimal(line));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading data.csv: " + e.getMessage());
            return;
        }
        if (dataList.isEmpty()) {
            System.out.println("data.csv is empty.");
            return;
        }
        int total = dataList.size();
        int half = total / 2;
        List<BigDecimal> firstHalf = dataList.subList(0, half);
        List<BigDecimal> secondHalf = dataList.subList(half, total);

        System.out.println("前一半数据统计:");
        printStats(firstHalf);
        System.out.println("后一半数据统计:");
        printStats(secondHalf);
        System.out.println("全部数据统计:");
        printStats(dataList);
    }

    private static void printStats(List<BigDecimal> list) {
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
        System.out.println("最大值: " + max + " (第" + (maxIndex + 1) + "行)");
        System.out.println("最小值: " + min + " (第" + (minIndex + 1) + "行)");
        System.out.println();
    }
}
