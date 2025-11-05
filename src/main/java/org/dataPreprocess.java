package org;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据预处理
 * 1. 数据清洗
 * 2. 区间划分
 * 支持截取前 N 条数据
 */
public class dataPreprocess {
    public static void main(String[] args) {
        String csvFilePath = "smart_manufacturing_data.csv";

        // ✅ 这里设置要处理的条数，比如 5000；如果是 -1 就处理全部
        int limit = 10000;

        List<String> cleaned = cleanData(csvFilePath, limit);
        List<String> processed = intervalDivision(cleaned);
        writeToFile("smart_manufacturing_data_preprocessed.csv", processed);
    }

    private static final String[] INTERVAL_LABELS = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" };

    /**
     * 数据清洗
     * limit > 0 时，仅保留前 limit 条；limit <= 0 时，处理所有数据
     */
    public static List<String> cleanData(String csvFilePath, int limit) {
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

            if (idxTimestamp == null || idxMachineId == null || idxTemp == null || idxVib == null ||
                    idxHum == null || idxPres == null || idxEnergy == null || idxFailureType == null) {
                return result;
            }

            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty())
                    continue;
                if (limit > 0 && count >= limit)
                    break; // ✅ 截断

                String[] parts = line.split(",");
                if (parts.length < cols.length)
                    continue;

                // String failureType = parts[idxFailureType].trim();
                // if (!"Normal".equals(failureType))
                //     continue;

                String timestamp = parts[idxTimestamp].trim();
                String machineId = parts[idxMachineId].trim();
                String tsToMinute = extractToMinute(timestamp);
                String newMachineId = (machineId + "_" + tsToMinute).replace(" ", "");

                String temperature = parts[idxTemp].trim();
                String vibration = parts[idxVib].trim();
                String humidity = parts[idxHum].trim();
                String pressure = parts[idxPres].trim();
                String energy = parts[idxEnergy].trim();

                result.add(String.join(",", Arrays.asList(
                        newMachineId, temperature, vibration, humidity, pressure, energy)));
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<String> intervalDivision(List<String> rows) {
        if (rows == null || rows.isEmpty())
            return Collections.emptyList();

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
        return Math.max(0, Math.min(9, bin));
    }

    private static String extractToMinute(String timestamp) {
        if (timestamp == null || timestamp.isEmpty())
            return "";
        int idx = timestamp.indexOf(":");
        if (idx >= 0) {
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
