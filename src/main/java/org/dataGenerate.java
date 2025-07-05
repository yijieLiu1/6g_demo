package org;

import java.io.*;
import java.util.Random;
import java.text.DecimalFormat;

public class dataGenerate {
    private static final String CSV_FILE_PATH = "data.csv";
    private static final double MIN_VALUE = -1000.00;
    private static final double MAX_VALUE = 1000.00;
    private static final int NUM_ROWS = 1000; // 可以根据需要修改行数

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
        DecimalFormat df = new DecimalFormat("0.00");

        // 清空文件并写入新数据
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE_PATH))) {
            for (int i = 0; i < NUM_ROWS; i++) {
                // 生成-1000.00到1000.00之间的随机数
                double randomValue = MIN_VALUE + (MAX_VALUE - MIN_VALUE) * random.nextDouble();
                // 格式化为两位小数
                String formattedValue = df.format(randomValue);
                // 写入文件
                writer.write(formattedValue);
                writer.newLine();
            }
        }
    }

}
