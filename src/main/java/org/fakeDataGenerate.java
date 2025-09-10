package org;

import java.io.*;
import java.util.Random;

public class fakeDataGenerate {
    private static final String CSV_FILE_PATH = "data.csv";
    private static final int NUM_ROWS = 100000; // 可以根据需要修改行数
    private static final int MIN_AGE = 0;
    private static final int MAX_AGE = 100;
    private static final int INTERVAL = 10;
    private static final String[] INTERVAL_LABELS = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" }; // a:0-10,
                                                                                                          // ...,
                                                                                                          // j:90-100

    // 新增：收入/支出相关常量
    private static final int MIN_INCOME = 6000;
    private static final int MAX_INCOME = 12000;
    private static final int INCOME_INTERVAL = 600; // (12000-6000)/10 = 600
    private static final String[] INCOME_INTERVAL_LABELS = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" }; // a:6000-6600,
                                                                                                                 // ...,
                                                                                                                 // j:11400-12000
    private static final int MIN_EXPENSE = 4000;
    private static final int MAX_EXPENSE = 10000;
    private static final int EXPENSE_INTERVAL = 600; // (10000-4000)/10 = 600
    private static final String[] EXPENSE_INTERVAL_LABELS = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" }; // a:4000-4600,
                                                                                                                  // ...,
                                                                                                                  // j:9400-10000

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
        double mean = 35.0; // 年龄均值
        double stdDev = 15.0; // 年龄标准差

        // 收入相关参数
        double[] incomeMeans = { 9000.0, 9500.0, 10000.0, 10500.0 }; // 四个季度收入均值
        double[] incomeStdDevs = { 1500.0, 1500.0, 1500.0, 1500.0 }; // 四个季度收入标准差
        // 支出相关参数
        double[] expenseMeans = { 7000.0, 7200.0, 7500.0, 7800.0 }; // 四个季度支出均值
        double[] expenseStdDevs = { 1200.0, 1200.0, 1200.0, 1200.0 }; // 四个季度支出标准差

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
                // 计算年龄区间
                int intervalIndex = Math.min((age == 100 ? 9 : age / INTERVAL), 9); // 100岁归到j
                String intervalLabel = INTERVAL_LABELS[intervalIndex];

                // 四个季度收入和区间
                StringBuilder sb = new StringBuilder();
                sb.append("client-").append(clientId).append(",").append(intervalLabel).append(",").append(age);
                for (int q = 0; q < 4; q++) {
                    int income = (int) Math.round(incomeMeans[q] + incomeStdDevs[q] * random.nextGaussian());
                    if (income < MIN_INCOME)
                        income = MIN_INCOME;
                    if (income > MAX_INCOME)
                        income = MAX_INCOME;
                    int incomeIntervalIndex = Math.min((income - MIN_INCOME) / INCOME_INTERVAL, 9);
                    String incomeIntervalLabel = INCOME_INTERVAL_LABELS[incomeIntervalIndex];
                    sb.append(",").append(incomeIntervalLabel).append(",").append(income);
                }
                // 四个季度支出和区间
                for (int q = 0; q < 4; q++) {
                    int expense = (int) Math.round(expenseMeans[q] + expenseStdDevs[q] * random.nextGaussian());
                    if (expense < MIN_EXPENSE)
                        expense = MIN_EXPENSE;
                    if (expense > MAX_EXPENSE)
                        expense = MAX_EXPENSE;
                    int expenseIntervalIndex = Math.min((expense - MIN_EXPENSE) / EXPENSE_INTERVAL, 9);
                    String expenseIntervalLabel = EXPENSE_INTERVAL_LABELS[expenseIntervalIndex];
                    sb.append(",").append(expenseIntervalLabel).append(",").append(expense);
                }
                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

}
