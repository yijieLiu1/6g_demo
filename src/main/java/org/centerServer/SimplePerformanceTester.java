package org.centerServer;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SimplePerformanceTester {
    private static final String CENTER_SERVER_URL = "http://localhost:33333";
    private static final String EDGE_SERVER1_URL = "http://localhost:23456";
    private static final String EDGE_SERVER2_URL = "http://localhost:33456";
    private static final String EDGE_SERVER3_URL = "http://localhost:24567";
    private static final String EDGE_SERVER4_URL = "http://localhost:34567";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        int testCount = 10; // 默认测试10次

        if (args.length > 0) {
            try {
                testCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("使用默认测试次数: 10");
            }
        }

        System.out.println("=== CenterServer API 性能测试 ===");
        System.out.println("测试次数: " + testCount);
        System.out.println();

        // 在执行所有测试之前，先准备数据
        System.out.println("准备数据：调用各个EdgeServer的API...");
        prepareData();
        System.out.println("数据准备完成！\n");

        // 测试各个API
        testAPI("/get/decryptedText", "解密聚合值", testCount);
        testAPI("/get/meanResult", "计算平均值", testCount);
        testAPI("/get/extremeResult", "获取极值结果", testCount);
        testAPI("/get/varianceResult", "计算方差", testCount);

        System.out.println("=== 测试完成 ===");
    }

    // 准备数据：调用各个EdgeServer的API
    private static void prepareData() {
        try {
            System.out.println("调用edgeServer1的/get/extremeCipherText...");
            callEdgeServerAPI(EDGE_SERVER1_URL + "/get/extremeCipherText", "edgeServer1极值密文");

            System.out.println("调用edgeServer2的/get/impaillierCipherText...");
            callEdgeServerAPI(EDGE_SERVER2_URL + "/get/impaillierCipherText", "edgeServer2聚合密文");

            System.out.println("调用edgeServer2的/get/impaillierVarianceCipherText...");
            callEdgeServerAPI(EDGE_SERVER2_URL + "/get/impaillierVarianceCipherText", "edgeServer2方差密文");

            System.out.println("调用edgeServer3的/get/extremeCipherText...");
            callEdgeServerAPI(EDGE_SERVER3_URL + "/get/extremeCipherText", "edgeServer3极值密文");

            System.out.println("调用edgeServer4的/get/impaillierCipherText...");
            callEdgeServerAPI(EDGE_SERVER4_URL + "/get/impaillierCipherText", "edgeServer4聚合密文");

            System.out.println("调用edgeServer4的/get/impaillierVarianceCipherText...");
            callEdgeServerAPI(EDGE_SERVER4_URL + "/get/impaillierVarianceCipherText", "edgeServer4方差密文");

            // 等待一段时间确保数据处理完成
            Thread.sleep(2000);

        } catch (Exception e) {
            System.err.println("准备数据时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 调用EdgeServer API的通用方法
    private static void callEdgeServerAPI(String url, String description) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println(description + " 调用成功");
            } else {
                System.err.println(description + " 调用失败，状态码: " + response.statusCode());
                System.err.println("错误信息: " + response.body());
            }
        } catch (Exception e) {
            System.err.println(description + " 调用异常: " + e.getMessage());
        }
    }

    private static void testAPI(String endpoint, String description, int testCount) {
        System.out.println("测试: " + description + " (" + endpoint + ")");
        System.out.println("----------------------------------------");

        List<Long> times = new ArrayList<>();

        for (int i = 1; i <= testCount; i++) {
            try {
                long startTime = System.currentTimeMillis();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(CENTER_SERVER_URL + endpoint))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                times.add(duration);

                System.out.printf("第 %2d 次: %4d ms%n", i, duration);

                // 短暂延迟
                Thread.sleep(200);

            } catch (Exception e) {
                System.err.printf("第 %d 次测试失败: %s%n", i, e.getMessage());
            }
        }

        // 计算统计结果
        calculateStatistics(description, times);
        System.out.println();
    }

    // 计算统计结果（去掉一个最大值和一个最小值）
    private static void calculateStatistics(String description, List<Long> times) {
        if (times.isEmpty()) {
            System.out.println("没有有效的测试结果");
            return;
        }

        // 如果数据量少于3个，无法去掉最大最小值
        if (times.size() < 3) {
            System.out.println("数据量不足，无法去掉最大最小值");
            return;
        }

        // 排序并去掉一个最大值和一个最小值
        List<Long> sortedTimes = new ArrayList<>(times);
        sortedTimes.sort(Long::compareTo);

        // 去掉最小值和最大值
        List<Long> filteredTimes = sortedTimes.subList(1, sortedTimes.size() - 1);

        // 计算平均值（去掉最大最小值后）
        long total = filteredTimes.stream().mapToLong(Long::longValue).sum();
        double average = (double) total / filteredTimes.size();
        long min = filteredTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = filteredTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("统计结果:");
        System.out.printf("  原始测试次数: %d%n", times.size());
        System.out.printf("  去掉最大最小值后的测试次数: %d%n", filteredTimes.size());
        System.out.printf("  平均耗时: %.2f ms (去掉最大最小值)%n", average);
        System.out.printf("  最小耗时: %d ms%n", min);
        System.out.printf("  最大耗时: %d ms%n", max);
        System.out.printf("  总耗时: %d ms%n", total);
    }
}
