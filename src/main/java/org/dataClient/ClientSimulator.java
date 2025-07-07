// package org.dataClient;

// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.IOException;
// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.TimeUnit;

// public class ClientSimulator {

// private static final String SERVER_URL =
// "http://localhost:13456/get/cipherText";
// private static final String DATA_FILE = "data.csv";
// private static final int NUM_THREADS = 100; // 并发线程数

// public static void main(String[] args) {
// List<String> clientIds = getClientIdsFromFile(DATA_FILE);
// if (clientIds.isEmpty()) {
// System.out.println("No client IDs found. Exiting.");
// return;
// }

// HttpClient httpClient = HttpClient.newHttpClient();
// ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

// System.out.println("Starting client simulation for " + clientIds.size() + "
// clients...");

// for (String clientId : clientIds) {
// Runnable task = () -> {
// try {
// HttpRequest request = HttpRequest.newBuilder()
// .uri(URI.create(SERVER_URL))
// .header("Client-ID", clientId)
// .GET()
// .build();

// HttpResponse<String> response = httpClient.send(request,
// HttpResponse.BodyHandlers.ofString());

// if (response.statusCode() == 200) {
// System.out.println(
// "Client " + clientId + " successfully sent data. Server response: ");
// } else {
// System.err.println("Client " + clientId + " failed. Status code: " +
// response.statusCode()
// + ", Response: " + response.body());
// }
// } catch (IOException | InterruptedException e) {
// System.err.println("Exception for client " + clientId + ": " +
// e.getMessage());
// }
// };
// executor.submit(task);
// }

// // Shutdown the executor
// executor.shutdown();
// try {
// if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
// executor.shutdownNow();
// }
// } catch (InterruptedException e) {
// executor.shutdownNow();
// }

// System.out.println("Client simulation finished.");
// }

// private static List<String> getClientIdsFromFile(String filePath) {
// List<String> clientIds = new ArrayList<>();
// int clientCount = 0;
// try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
// String line;
// while ((line = br.readLine()) != null) {
// clientCount++;
// clientIds.add("client-" + clientCount);
// }
// } catch (IOException e) {
// System.err.println("Error reading client data file: " + e.getMessage());
// e.printStackTrace();
// }
// return clientIds;
// }
// }

// package org.dataClient;

// import com.sun.net.httpserver.HttpServer;

// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.IOException;
// import java.net.InetSocketAddress;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.TimeUnit;

// import org.dataClient.handler.DataHandler;

// import java.math.BigDecimal;
// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import org.dataClient.utils.Paillier;
// import org.json.JSONObject;
// import java.math.BigInteger;
// import java.io.BufferedWriter;
// import java.io.FileWriter;

// public class Main {
// public static void main(String[] args) {
// try {
// long starttime = System.currentTimeMillis();
// encryptAndSendToEdgeServers("data.csv");
// long endtime = System.currentTimeMillis();
// System.out.println("所有客户端注册和请求发送完成，耗时: " + (endtime - starttime) + " 毫秒");

// } catch (Exception e) {
// System.err.println("Error starting server: " + e.getMessage());
// e.printStackTrace();
// }
// }

// private static void encryptAndSendToEdgeServers(String filePath) {
// List<String> clientData = new ArrayList<>();
// try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
// String line;
// while ((line = br.readLine()) != null) {
// clientData.add(line.trim());
// }
// } catch (IOException e) {
// System.err.println("Error reading client data file: " + e.getMessage());
// e.printStackTrace();
// return;
// }
// int total = clientData.size();
// System.out.println("读取到 " + total + " 条客户端数据。开始加密并发送请求...");
// int THREADS = 120;
// ExecutorService pool = Executors.newFixedThreadPool(THREADS);
// final HttpClient httpClient = HttpClient.newHttpClient();

// // 新密钥参数（与DataHandler保持一致）
// BigInteger NEW_PAILLIER_N = new BigInteger(
// "17533783372108747920106125575318249359137735333005194441578747782393599787433943202952875760351329363857766976818251563500179216831309900947642732506035409652407968274784075323905219171820618745597343515557230611843344083186766176830424963734181436121771476891198077692143518128112930243186451897679296638047763174948689579891087827291519296363166049458478037878221226897746081327356627177998988868776953849647298447359560237430805518849369332381966727817161934002192070589843924492843526803012801399384257853005982792651324457735564496069314368048364828610804784990929091174060634507968837501831802488335819971428229");
// BigInteger NEW_PAILLIER_LAMBDA = new BigInteger(
// "8766891686054373960053062787659124679568867666502597220789373891196799893716971601476437880175664681928883488409125781750089608415654950473821366253017704826203984137392037661952609585910309372798671757778615305921672041593383088415212481867090718060885738445599038846071759064056465121593225948839648319023748724516560638332471720556829811482618564785528548958240885045832101311879858417656999854250588188980777498176054206795424191964131583952776613405974034275703943683721005831989405354680444467149475089305831951192975935576659660554136087215776419185570907385980496876118312366031453648041582515057835761919542");
// BigInteger NEW_PAILLIER_G = new BigInteger(
// "17533783372108747920106125575318249359137735333005194441578747782393599787433943202952875760351329363857766976818251563500179216831309900947642732506035409652407968274784075323905219171820618745597343515557230611843344083186766176830424963734181436121771476891198077692143518128112930243186451897679296638047763174948689579891087827291519296363166049458478037878221226897746081327356627177998988868776953849647298447359560237430805518849369332381966727817161934002192070589843924492843526803012801399384257853005982792651324457735564496069314368048364828610804784990929091174060634507968837501831802488335819971428230");
// BigInteger NEW_PAILLIER_MU = new BigInteger(
// "9824035577960780375144357233395169247730164380409155999379812259969057644099782976339470679419066120047982004073642379926355199686805107665597865927459962153427585211240063216638433400393109638277214578212896845978716384826168842206240888344049483009044489159901753819107474939766917234018159773987892362813711205397873373629333582901739280903559536247702289746773031425501905534956074006094693803490077514963055464724608667782680516687335985145082359760637443793774490818429467831197070828177913657662397312791686959521904653435726289490684182399244211392307398829417281786500060491794032219253420896764658036028005");
// int NEW_PAILLIER_SCALE = 8;
// Paillier newPaillier = new Paillier(NEW_PAILLIER_N, NEW_PAILLIER_G,
// NEW_PAILLIER_LAMBDA, NEW_PAILLIER_MU,
// NEW_PAILLIER_SCALE);

// final List<String> dataForThreads = clientData;
// int BATCH_SIZE = 300;

// // 新增：写密文到endata.csv，先清空
// // try (BufferedWriter encWriter = new BufferedWriter(new
// // FileWriter("endata.csv"))) {
// for (int batchStart = 0; batchStart < dataForThreads.size(); batchStart +=
// BATCH_SIZE) {
// int batchEnd = Math.min(batchStart + BATCH_SIZE, dataForThreads.size());
// List<java.util.concurrent.CompletableFuture<String>> batchFutures = new
// ArrayList<>();
// for (int i = batchStart; i < batchEnd; i++) {
// final int idx = i;
// batchFutures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
// try {
// String[] parts = dataForThreads.get(idx).split(",");
// String clientId = parts[0];
// String interval = parts[1];
// BigDecimal data = new BigDecimal(parts[2]);
// BigInteger cipherText;
// if (idx < total / 2) {
// cipherText = Paillier.encrypt(data);
// } else {
// cipherText = newPaillier.encryptInst(data);
// }
// // 返回密文行
// return clientId + "," + interval + "," + cipherText.toString();
// } catch (Exception e) {
// System.err.println("Error for client " + (idx + 1) + ": " + e.getMessage());
// return null;
// }
// }));
// }
// // 写入密文到endata.csv
// // for (java.util.concurrent.CompletableFuture<String> f : batchFutures) {
// // String encLine = f.join();
// // if (encLine != null) {
// // encWriter.write(encLine);
// // encWriter.newLine();
// // }
// // }
// // 发送密文到服务端
// List<java.util.concurrent.CompletableFuture<Void>> sendFutures = new
// ArrayList<>();
// for (int i = batchStart; i < batchEnd; i++) {
// final int idx = i;
// sendFutures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
// try {
// String[] parts = dataForThreads.get(idx).split(",");
// String clientId = parts[0];
// String interval = parts[1];
// BigDecimal data = new BigDecimal(parts[2]);
// BigInteger cipherText, squareCipherText;
// String url;
// if (idx < total / 2) {
// cipherText = Paillier.encrypt(data);
// squareCipherText = Paillier.encrypt(data.multiply(data));
// url = "http://localhost:23456/post/cipherText";
// } else {
// cipherText = newPaillier.encryptInst(data);
// squareCipherText = newPaillier.encryptInst(data.multiply(data));
// url = "http://localhost:24567/post/cipherText";
// }
// JSONObject json = new JSONObject();
// json.put("cipherText", cipherText.toString());
// json.put("squareCipherText", squareCipherText.toString());
// json.put("interval", interval);
// HttpRequest request = HttpRequest.newBuilder()
// .uri(URI.create(url))
// .header("Client-ID", clientId)
// .header("Content-Type", "application/json")
// .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
// .build();
// sendWithRetry(httpClient, request, 3, clientId);
// if ((idx + 1) % 1000 == 0) {
// System.out.println("已加密并发送: " + clientId);
// }
// } catch (Exception e) {
// System.err.println("Error for client " + (idx + 1) + ": " + e.getMessage());
// }
// }));
// }
// sendFutures.forEach(java.util.concurrent.CompletableFuture::join);
// try {
// Thread.sleep(200); // 给服务端缓冲
// } catch (Exception e) {
// System.out.println("线程休眠异常: " + e.getMessage());
// }
// }
// // } catch (IOException e) {
// // System.err.println("Error writing endata.csv: " + e.getMessage());
// // }
// clientData = null;
// System.gc();
// System.out.println("全部加密并发送完成，资源已释放。可以安全退出。");
// }

// private static void sendWithRetry(HttpClient httpClient, HttpRequest request,
// int maxRetries, String clientId) {
// int attempt = 0;
// while (attempt < maxRetries) {
// try {
// HttpResponse<String> response = httpClient.send(request,
// HttpResponse.BodyHandlers.ofString());
// int code = response.statusCode();
// if (code >= 200 && code < 300) {
// return;
// } else {
// System.err.println("发送失败，状态码:" + code + " clientId: " + clientId + "，重试...");
// }
// } catch (Exception e) {
// System.err.println("发送异常: " + e.getMessage() + " clientId: " + clientId +
// "，重试...");
// }
// attempt++;
// try {
// Thread.sleep(200);
// } catch (InterruptedException e) {
// Thread.currentThread().interrupt();
// }
// }
// System.err.println("最终发送失败 clientId: " + clientId);
// }
// }