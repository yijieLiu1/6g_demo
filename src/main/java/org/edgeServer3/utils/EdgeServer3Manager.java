package org.edgeServer3.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.edgeServer4.utils.Paillier;
import org.json.JSONObject;

public class EdgeServer3Manager {
    private static final ConcurrentHashMap<String, String> clientCipherTexts = new ConcurrentHashMap<>();
    private static String aggregatedCipherText = "";
    private static final ConcurrentHashMap<String, String> clientSquareCipherTexts = new ConcurrentHashMap<>();
    private static String aggregatedSquareCipherText = "";

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String EDGE_SERVER4_URL = "http://localhost:34567";

    public static void registerClient(String clientId, String cipherText, String squareCipherText) {
        clientCipherTexts.put(clientId, cipherText);
        clientSquareCipherTexts.put(clientId, squareCipherText);
        updateAggregatedCipherText();
        updateAggregatedSquareCipherText();
    }

    private static void updateAggregatedCipherText() {
        if (clientCipherTexts.isEmpty()) {
            aggregatedCipherText = "";
            return;
        }

        // 初始化聚合结果为第一个密文
        BigInteger result = new BigInteger(clientCipherTexts.values().iterator().next());

        // 对剩余的密文进行乘法运算（对应明文加法）
        for (String cipherText : clientCipherTexts.values()) {
            if (cipherText.equals(clientCipherTexts.values().iterator().next())) {
                continue; // 跳过第一个已经使用的密文
            }
            BigInteger currentCipher = new BigInteger(cipherText);
            // 在模n^2下进行乘法运算
            result = result.multiply(currentCipher).mod(Paillier.getN2());
        }

        aggregatedCipherText = result.toString();

        // 发送聚合后的密文到edgeServer4

    }

    private static void updateAggregatedSquareCipherText() {
        if (clientSquareCipherTexts.isEmpty()) {
            aggregatedSquareCipherText = "";
            return;
        }
        BigInteger result = new BigInteger(clientSquareCipherTexts.values().iterator().next());
        for (String squareCipherText : clientSquareCipherTexts.values()) {
            if (squareCipherText.equals(clientSquareCipherTexts.values().iterator().next())) {
                continue;
            }
            BigInteger currentCipher = new BigInteger(squareCipherText);
            result = result.multiply(currentCipher).mod(Paillier.getN2());
        }
        aggregatedSquareCipherText = result.toString();
    }

    private static void sendAggregatedCipherTextToEdgeServer4(String cipherText) {
        try {
            JSONObject json = new JSONObject();
            json.put("cipherText", cipherText);
            json.put("squareCipherText", getAggregatedSquareCipherText());
            json.put("clientCount", getClientCount());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER4_URL + "/post/aggregatedCipherText"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Failed to send aggregated cipher text to edge server 4: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending aggregated cipher text to edge server 4: " + e.getMessage());
        }
    }

    public static String getAggregatedCipherText() {
        sendAggregatedCipherTextToEdgeServer4(aggregatedCipherText);
        return aggregatedCipherText;
    }

    public static String getAggregatedSquareCipherText() {
        return aggregatedSquareCipherText;
    }

    public static int getClientCount() {
        return clientCipherTexts.size();
    }

    // 构建比较密文
    private static String getComparisonCipherTextInternal(String clientId1, String clientId2) {
        String cipherText1Str = clientCipherTexts.get(clientId1);
        String cipherText2Str = clientCipherTexts.get(clientId2);
        java.security.SecureRandom random = new java.security.SecureRandom();

        if (cipherText1Str == null || cipherText2Str == null) {
            return "Error: Client cipher not found.";
        }

        BigInteger cipherText1 = new BigInteger(cipherText1Str);
        BigInteger cipherText2 = new BigInteger(cipherText2Str);
        BigInteger nSquared = Paillier.getN2();
        BigInteger r1 = new BigInteger(512, random);
        BigInteger r2 = new BigInteger(256, random);
        if (r1.compareTo(r2) <= 0) {
            r1 = r1.add(r2);
        }
        // 构建出En(r1*(m1-m2)+r2)的密文
        BigInteger cipherText2Inv = cipherText2.modInverse(nSquared);
        BigInteger diffCipherText = cipherText1.multiply(cipherText2Inv).mod(nSquared);
        BigInteger blindedDiffCipherText = diffCipherText.modPow(r1, nSquared);

        BigInteger r2CipherText = Paillier.encrypt(new java.math.BigDecimal(r2));
        BigInteger finalCipherText = blindedDiffCipherText.multiply(r2CipherText).mod(nSquared);

        return finalCipherText.toString();
    }

    // 将密文发送给edgeServer4
    private static void sendComparisonDataToEdgeServer4(String cipherText, String clientId1, String clientId2) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER4_URL + "/post/comparisonData"))
                    .header("Content-Type", "text/plain")
                    .header("Client-ID1", clientId1)
                    .header("Client-ID2", clientId2)
                    .POST(HttpRequest.BodyPublishers.ofString(cipherText))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Successfully sent comparison data to EdgeServer4.");
            } else {
                System.err.println("Failed to send data to EdgeServer4. Status: " + response.statusCode() + ", Body: "
                        + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending comparison data to EdgeServer4: " + e.getMessage());
        }
    }

    public static String generateAndSendComparisonCipherText(String clientId1, String clientId2) {
        String comparisonCipherText = getComparisonCipherTextInternal(clientId1, clientId2);

        if (comparisonCipherText.startsWith("Error:")) {
            return comparisonCipherText;
        }

        java.util.concurrent.CompletableFuture
                .runAsync(() -> sendComparisonDataToEdgeServer4(comparisonCipherText, clientId1, clientId2));

        return comparisonCipherText;
    }
}