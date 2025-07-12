package org.edgeServer4;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SimplePerformanceTester {
    private static final String EDGE_SERVER4_URL = "http://localhost:34567";
    private static final String EDGE_SERVER3_URL = "http://localhost:24567";
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

        System.out.println("=== EdgeServer4 API 性能测试 ===");
        System.out.println("测试次数: " + testCount);
        System.out.println();

        // 在执行所有测试之前，先执行edgeServer1的/get/sumcipherText
        System.out.println("准备数据：执行edgeServer3的聚合操作...");
        prepareData();
        System.out.println("数据准备完成！\n");

        // 测试各个API
        testAPI("/get/decryptedText", "解密聚合值", testCount);
        testAPI("/get/meanResult", "计算平均值", testCount);
        testAPI("/get/varianceResult", "计算方差", testCount);
        testAPI("/get/compareResult", "获取比较结果", testCount);

        System.out.println("=== 测试完成 ===");
    }

    // 准备数据：执行edgeServer1的聚合操作
    private static void prepareData() {
        try {
            System.out.println("调用edgeServer3的/get/sumcipherText...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER3_URL + "/get/sumcipherText"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("edgeServer3聚合操作成功完成");
                System.out.println("响应内容: " + response.body());
            } else {
                System.err.println("edgeServer3聚合操作失败，状态码: " + response.statusCode());
                System.err.println("错误信息: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("执行edgeServer3聚合操作异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testAPI(String endpoint, String description, int testCount) {
        System.out.println("测试: " + description + " (" + endpoint + ")");
        System.out.println("----------------------------------------");

        List<Long> times = new ArrayList<>();
        List<Long> computeTimes = new ArrayList<>(); // 用于存储实际计算时间

        for (int i = 1; i <= testCount; i++) {
            try {
                // 对于compareResult API，先触发edgeServer1的计算
                if (endpoint.equals("/get/compareResult")) {
                    System.out.printf("第 %2d 次: 先触发edgeServer1计算...%n", i);
                    triggerEdgeServer3Computation();
                    // 等待计算完成
                    Thread.sleep(1000);
                }

                long startTime = System.currentTimeMillis();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(EDGE_SERVER4_URL + endpoint))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                times.add(duration);

                // 对于compareResult API，解析响应中的实际计算时间
                if (endpoint.equals("/get/compareResult")) {
                    String responseBody = response.body();
                    try {
                        // 解析响应中的计算时间
                        if (responseBody.contains("用时:")) {
                            String timePart = responseBody.split("用时:")[1].trim();
                            String timeValue = timePart.replace("ms", "").trim();
                            long computeTime = Long.parseLong(timeValue);
                            computeTimes.add(computeTime);
                            System.out.printf("第 %2d 次: API响应时间=%4d ms, 实际计算时间=%4d ms%n", i, duration, computeTime);
                        } else {
                            System.out.printf("第 %2d 次: %4d ms - 无法解析计算时间%n", i, duration);
                        }
                    } catch (Exception e) {
                        System.out.printf("第 %2d 次: %4d ms - 解析计算时间失败%n", i, duration);
                    }
                } else {
                    System.out.printf("第 %2d 次: %4d ms%n", i, duration);
                }

                // 短暂延迟
                Thread.sleep(200);

            } catch (Exception e) {
                System.err.printf("第 %d 次测试失败: %s%n", i, e.getMessage());
            }
        }

        // 计算统计结果
        if (endpoint.equals("/get/compareResult") && !computeTimes.isEmpty()) {
            System.out.println("\n=== 实际计算时间统计 ===");
            calculateStatistics(description + " (实际计算时间)", computeTimes);
            System.out.println("\n=== API响应时间统计 ===");
            calculateStatistics(description + " (API响应时间)", times);
        } else {
            calculateStatistics(description, times);
        }

        System.out.println();
    }

    // 触发edgeServer1的计算
    private static void triggerEdgeServer3Computation() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER3_URL + "/post/comparePair"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("触发edgeServer3计算失败，状态码: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("触发edgeServer3计算异常: " + e.getMessage());
        }
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