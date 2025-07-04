package org.edgeServer3;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.time.Duration;

public class ComparisonCipherTextBatchSender {
    private static final String TARGET_URL = "http://localhost:34567/post/comparisonData"; // edgeServer4

    public static void sendBatch(List<ComparisonCipherText> comparisonList) {
        int THREADS = 16; // 可根据CPU核数调整
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<?>> futures = new ArrayList<>();
        final HttpClient httpClient = HttpClient.newHttpClient();

        for (ComparisonCipherText cmp : comparisonList) {
            futures.add(pool.submit(() -> {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(TARGET_URL))
                        .header("Content-Type", "text/plain")
                        .header("Client-ID1", cmp.clientId1)
                        .header("Client-ID2", cmp.clientId2)
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(cmp.comparisonCipherText))
                        .build();
                int maxRetries = 3;
                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    try {
                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return;
                        } else {
                            System.err.println(
                                    "发送失败: " + cmp.clientId1 + ", " + cmp.clientId2 + " 状态码:" + response.statusCode());
                        }
                    } catch (Exception e) {
                        System.err.println("发送异常: " + e.getMessage());
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.err.println("最终发送失败: " + cmp.clientId1 + ", " + cmp.clientId2);
            }));
        }
        // 等待所有任务完成
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        pool.shutdown();
        System.out.println("全部比较密文已批量发送完成。");
    }

    // 比较密文数据结构
    public static class ComparisonCipherText {
        public final String clientId1;
        public final String clientId2;
        public final String comparisonCipherText;

        public ComparisonCipherText(String clientId1, String clientId2, String comparisonCipherText) {
            this.clientId1 = clientId1;
            this.clientId2 = clientId2;
            this.comparisonCipherText = comparisonCipherText;
        }
    }
}