package org.dataClient.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.dataClient.utils.DataManager;
import org.dataClient.utils.Paillier;

public class DataHandler implements HttpHandler {
    private static final ConcurrentHashMap<String, DataManager> clients = new ConcurrentHashMap<>();
    private static final String EDGE_SERVER_URL = "http://localhost:23456";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    // 新增：新Paillier密钥参数
    private static final BigInteger NEW_PAILLIER_N = new BigInteger(
            "17533783372108747920106125575318249359137735333005194441578747782393599787433943202952875760351329363857766976818251563500179216831309900947642732506035409652407968274784075323905219171820618745597343515557230611843344083186766176830424963734181436121771476891198077692143518128112930243186451897679296638047763174948689579891087827291519296363166049458478037878221226897746081327356627177998988868776953849647298447359560237430805518849369332381966727817161934002192070589843924492843526803012801399384257853005982792651324457735564496069314368048364828610804784990929091174060634507968837501831802488335819971428229");
    private static final BigInteger NEW_PAILLIER_LAMBDA = new BigInteger(
            "8766891686054373960053062787659124679568867666502597220789373891196799893716971601476437880175664681928883488409125781750089608415654950473821366253017704826203984137392037661952609585910309372798671757778615305921672041593383088415212481867090718060885738445599038846071759064056465121593225948839648319023748724516560638332471720556829811482618564785528548958240885045832101311879858417656999854250588188980777498176054206795424191964131583952776613405974034275703943683721005831989405354680444467149475089305831951192975935576659660554136087215776419185570907385980496876118312366031453648041582515057835761919542");
    private static final BigInteger NEW_PAILLIER_G = new BigInteger(
            "17533783372108747920106125575318249359137735333005194441578747782393599787433943202952875760351329363857766976818251563500179216831309900947642732506035409652407968274784075323905219171820618745597343515557230611843344083186766176830424963734181436121771476891198077692143518128112930243186451897679296638047763174948689579891087827291519296363166049458478037878221226897746081327356627177998988868776953849647298447359560237430805518849369332381966727817161934002192070589843924492843526803012801399384257853005982792651324457735564496069314368048364828610804784990929091174060634507968837501831802488335819971428230");
    private static final BigInteger NEW_PAILLIER_MU = new BigInteger(
            "9824035577960780375144357233395169247730164380409155999379812259969057644099782976339470679419066120047982004073642379926355199686805107665597865927459962153427585211240063216638433400393109638277214578212896845978716384826168842206240888344049483009044489159901753819107474939766917234018159773987892362813711205397873373629333582901739280903559536247702289746773031425501905534956074006094693803490077514963055464724608667782680516687335985145082359760637443793774490818429467831197070828177913657662397312791686959521904653435726289490684182399244211392307398829417281786500060491794032219253420896764658036028005");
    private static final int NEW_PAILLIER_SCALE = 8;

    private static final Paillier NEW_PAILLIER = new Paillier(NEW_PAILLIER_N, NEW_PAILLIER_G, NEW_PAILLIER_LAMBDA,
            NEW_PAILLIER_MU, NEW_PAILLIER_SCALE);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        if (clientId == null) {
            sendResponse(exchange, 400, "Missing Client-ID header");
            return;
        }

        DataManager dataManager = clients.get(clientId);
        if (dataManager == null) {
            sendResponse(exchange, 404, "Client not found");
            return;
        }

        String response;
        if (path.equals("/get/plainText")) {
            response = "plainText:" + dataManager.getPlainData().toString();
        } else if (path.equals("/get/cipherText")) {
            String cipherText = dataManager.getCipherData().toString();
            response = "cipherText:" + cipherText;

            // 发送密文到edgeServer
            sendCipherTextToEdgeServer(clientId, cipherText);
        } else {
            sendResponse(exchange, 404, "Path not found");
            return;
        }

        sendResponse(exchange, 200, response);
    }

    private void sendCipherTextToEdgeServer(String clientId, String cipherText) {
        try {
            String url;
            if (clientId.equals("test-client3") || clientId.equals("test-client4")) {
                url = "http://localhost:24567/post/cipherText";
            } else {
                url = EDGE_SERVER_URL + "/post/cipherText";
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Client-ID", clientId)
                    .POST(HttpRequest.BodyPublishers.ofString(cipherText))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Failed to send cipher text to edge server: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending cipher text to edge server: " + e.getMessage());
        }
    }

    public static void registerClient(String clientId, BigDecimal initialData) {
        if (clientId.equals("test-client3") || clientId.equals("test-client4")) {
            clients.put(clientId, DataManager.getInstance(clientId, initialData, NEW_PAILLIER));
        } else {
            clients.put(clientId, DataManager.getInstance(clientId, initialData, null));
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}