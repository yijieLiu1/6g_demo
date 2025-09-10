package org;

import java.io.*;
import java.util.*;

/**
 * 数据预处理
 * 1. 数据清洗
 * 2. 区间划分
 * 该方法主要针对真实数据进行预处理。smart_manufacturing_data.csv
 */
public class dataPreprocess {
    public static void main(String[] args) {
        String csvFilePath = "smart_manufacturing_data.csv";
        List<String> cleaned = cleanData(csvFilePath);
        List<String> processed = intervalDivision(cleaned);
        // 可选：写出到新文件，方便后续使用
        writeToFile("smart_manufacturing_data_preprocessed.csv", processed);
    }

    private static final String[] INTERVAL_LABELS = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" };

    /**
     * 数据清洗：
     * 1) 仅保留 failure_type == "Normal" 的行
     * 2) 修改 machine_id = 原值 + 日期时间(到分钟)，如 machineId_2025-01-01 00:00；并移除空格
     * 3) 删除多余列，只保留 machine_id 以及
     * temperature,vibration,humidity,pressure,energy_consumption 共 6 列
     * 返回：包含上述 6 列的行（逗号分隔字符串）
     */
    public static List<String> cleanData(String csvFilePath) {
        List<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String header = br.readLine();
            if (header == null)
                return result;
            String[] cols = header.split(",");
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < cols.length; i++) {
                idx.put(cols[i].trim(), i);
            }

            Integer idxTimestamp = idx.get("timestamp");
            Integer idxMachineId = idx.get("machine_id");
            Integer idxTemp = idx.get("temperature");
            Integer idxVib = idx.get("vibration");
            Integer idxHum = idx.get("humidity");
            Integer idxPres = idx.get("pressure");
            Integer idxEnergy = idx.get("energy_consumption");
            Integer idxFailureType = idx.get("failure_type");

            if (idxTimestamp == null || idxMachineId == null || idxTemp == null || idxVib == null || idxHum == null ||
                    idxPres == null || idxEnergy == null || idxFailureType == null) {
                return result;
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty())
                    continue;
                String[] parts = line.split(",");
                if (parts.length < cols.length)
                    continue;

                String failureType = parts[idxFailureType].trim();
                if (!"Normal".equals(failureType))
                    continue;

                String timestamp = parts[idxTimestamp].trim(); // 例如 2025-01-01 00:00:00
                String machineId = parts[idxMachineId].trim();
                String tsToMinute = extractToMinute(timestamp); // 2025-01-01 00:00
                String newMachineId = (machineId + "_" + tsToMinute).replace(" ", "");

                String temperature = parts[idxTemp].trim();
                String vibration = parts[idxVib].trim();
                String humidity = parts[idxHum].trim();
                String pressure = parts[idxPres].trim();
                String energy = parts[idxEnergy].trim();

                result.add(String.join(",", Arrays.asList(
                        newMachineId, temperature, vibration, humidity, pressure, energy)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 区间划分：对每列数据按最小-最大等宽分 10 个区间，生成对应的区间标签 a-j，
     * 并在每个数据前插入其标签。
     * 输入：仅包含 temperature,vibration,humidity,pressure,energy 的行
     * 输出：每行变为 (label,value) 重复 5 次，总计 10 列
     */
    public static List<String> intervalDivision(List<String> rows) {
        if (rows == null || rows.isEmpty())
            return Collections.emptyList();

        // 第 0 列为 machine_id（字符串），后续 5 列为数值
        int n = rows.size();
        int m = 5;
        String[] ids = new String[n];
        double[][] values = new double[n][m];
        for (int i = 0; i < n; i++) {
            String[] parts = rows.get(i).split(",");
            if (parts.length < m + 1)
                continue;
            ids[i] = parts[0].trim();
            for (int j = 0; j < m; j++) {
                try {
                    values[i][j] = Double.parseDouble(parts[j + 1].trim());
                } catch (NumberFormatException e) {
                    values[i][j] = Double.NaN;
                }
            }
        }

        // 计算每列的 min/max
        double[] min = new double[m];
        double[] max = new double[m];
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                double v = values[i][j];
                if (Double.isNaN(v))
                    continue;
                if (v < min[j])
                    min[j] = v;
                if (v > max[j])
                    max[j] = v;
            }
        }

        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<String> cols = new ArrayList<>(m * 2 + 1);
            cols.add(ids[i] == null ? "" : ids[i]);
            for (int j = 0; j < m; j++) {
                double v = values[i][j];
                String label = "";
                if (!Double.isNaN(v)) {
                    int bin = computeBin(v, min[j], max[j]);
                    label = INTERVAL_LABELS[bin];
                }
                cols.add(label);
                cols.add(Double.isNaN(v) ? "" : String.valueOf(v));
            }
            out.add(String.join(",", cols));
        }
        return out;
    }

    private static int computeBin(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isNaN(min) || Double.isNaN(max) || max <= min)
            return 0;
        if (value <= min)
            return 0;
        if (value >= max)
            return 9;
        double ratio = (value - min) / (max - min);
        int bin = (int) Math.floor(ratio * 10.0);
        if (bin < 0)
            bin = 0;
        if (bin > 9)
            bin = 9;
        return bin;
    }

    private static String extractToMinute(String timestamp) {
        // 假设格式为 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd HH:mm
        if (timestamp == null || timestamp.isEmpty())
            return "";
        // 分割到分钟
        int idx = timestamp.indexOf(":");
        if (idx >= 0) {
            // 找到第一个冒号（小时与分钟之间），再找第二个冒号（分钟与秒之间）
            int secondColon = timestamp.indexOf(":", idx + 1);
            if (secondColon > 0) {
                return timestamp.substring(0, secondColon);
            }
        }
        return timestamp;
    }

    private static void writeToFile(String path, List<String> lines) {
        if (lines == null)
            return;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            // 写出列头：为清晰，标注每个值前有标签列，并包含 machine_id
            bw.write(
                    "machine_id,t_label,temperature,v_label,vibration,h_label,humidity,p_label,pressure,e_label,energy_consumption");
            bw.newLine();
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
