package org;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class dataTest {
    public static void main(String[] args) {
        List<String> machineIds = new ArrayList<>();
        List<BigDecimal> temperatures = new ArrayList<>();
        List<BigDecimal> vibrations = new ArrayList<>();
        List<BigDecimal> humidities = new ArrayList<>();
        List<BigDecimal> pressures = new ArrayList<>();
        List<BigDecimal> energies = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("smart_manufacturing_data_preprocessed.csv"))) {
            String line;
            boolean isFirst = true;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                // 跳过表头
                if (isFirst) {
                    isFirst = false;
                    if (line.startsWith("machine_id"))
                        continue;
                }
                String[] parts = line.split(",");
                // 预处理后的格式：11 列
                // 0:machine_id, 1:t_label, 2:temperature, 3:v_label, 4:vibration,
                // 5:h_label, 6:humidity, 7:p_label, 8:pressure, 9:e_label,
                // 10:energy_consumption
                if (parts.length < 11)
                    continue;
                String id = parts[0];
                try {
                    BigDecimal temp = new BigDecimal(parts[2]);
                    BigDecimal vib = new BigDecimal(parts[4]);
                    BigDecimal hum = new BigDecimal(parts[6]);
                    BigDecimal pres = new BigDecimal(parts[8]);
                    BigDecimal ene = new BigDecimal(parts[10]);
                    machineIds.add(id);
                    temperatures.add(temp);
                    vibrations.add(vib);
                    humidities.add(hum);
                    pressures.add(pres);
                    energies.add(ene);
                } catch (NumberFormatException ex) {
                    // 跳过无法解析的行
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading smart_manufacturing_data_preprocessed.csv: " + e.getMessage());
            return;
        }

        if (machineIds.isEmpty()) {
            System.out.println("smart_manufacturing_data_preprocessed.csv is empty or format error.");
            return;
        }

        int total = machineIds.size();
        int half = total / 2;

        System.out.println("== Temperature 温度 ==");
        printAllScopes(temperatures, machineIds, half);
        System.out.println("== Vibration 震动 ==");
        printAllScopes(vibrations, machineIds, half);
        System.out.println("== Humidity 湿度 ==");
        printAllScopes(humidities, machineIds, half);
        System.out.println("== Pressure 压力 ==");
        printAllScopes(pressures, machineIds, half);
        System.out.println("== Energy Consumption 效率 ==");
        printAllScopes(energies, machineIds, half);
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
        // 方差：使用 E(x^2) - E(x)^2 公式
        BigDecimal sumSquares = BigDecimal.ZERO;
        for (BigDecimal val : list) {
            sumSquares = sumSquares.add(val.multiply(val));
        }
        BigDecimal avgSquares = sumSquares.divide(BigDecimal.valueOf(list.size()), 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal variance = avgSquares.subtract(avg.multiply(avg));
        System.out.println("数量: " + list.size());
        System.out.println("和: " + sum);
        System.out.println("平均值: " + avg);
        System.out.println("方差: " + variance);
        System.out.println("最大值: " + max + " (" + clients.get(maxIndex) + ")");
        System.out.println("最小值: " + min + " (" + clients.get(minIndex) + ")");
        System.out.println();
    }

    private static void printAllScopes(List<BigDecimal> values, List<String> ids, int half) {
        int total = values.size();
        List<BigDecimal> firstHalf = values.subList(0, half);
        List<BigDecimal> secondHalf = values.subList(half, total);
        List<String> firstHalfIds = ids.subList(0, half);
        List<String> secondHalfIds = ids.subList(half, total);
        System.out.println("前一半数据统计:");
        printStats(firstHalf, firstHalfIds);
        System.out.println("后一半数据统计:");
        printStats(secondHalf, secondHalfIds);
        System.out.println("全部数据统计:");
        printStats(values, ids);
    }
}
