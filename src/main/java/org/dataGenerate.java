package org;

import java.io.*;
import java.util.Random;
import java.text.DecimalFormat;

public class dataGenerate {
    private static final String CSV_FILE_PATH = "data.csv";
    private static final int NUM_ROWS = 100000; // 可以根据需要修改行数
    private static final int MIN_AGE = 0;
    private static final int MAX_AGE = 100;
    private static final int INTERVAL = 10;
    private static final String[] INTERVAL_LABELS = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" }; // a:0-10,
                                                                                                          // ...,
                                                                                                          // j:90-100

    public static void main(String[] args) {
        try {
            // 清空文件并生成新数据
            generateRandomData();
            System.out.println("数据生成完成！共生成 " + NUM_ROWS + " 行数据到 " + CSV_FILE_PATH);
        } catch (IOException e) {
            System.err.println("数据生成失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generateRandomData() throws IOException {
        Random random = new Random();
        double mean = 35.0; // 均值
        double stdDev = 15.0; // 标准差
        // 清空文件并写入新数据
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE_PATH))) {
            for (int i = 0; i < NUM_ROWS; i++) {
                int clientId = i + 1;
                // 生成正态分布的年龄
                int age = (int) Math.round(mean + stdDev * random.nextGaussian());
                if (age < MIN_AGE)
                    age = MIN_AGE;
                if (age > MAX_AGE)
                    age = MAX_AGE;
                // 计算区间
                int intervalIndex = Math.min((age == 100 ? 9 : age / INTERVAL), 9); // 100岁归到j
                String intervalLabel = INTERVAL_LABELS[intervalIndex];
                // 写入格式：client-x, 区间, 年龄
                writer.write("client-" + clientId + "," + intervalLabel + "," + age);
                writer.newLine();
            }
        }
    }

}
