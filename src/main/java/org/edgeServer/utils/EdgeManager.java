package org.edgeServer.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;
import java.math.BigDecimal;

public class EdgeManager {
    private static final ConcurrentHashMap<String, String> clientCipherTexts = new ConcurrentHashMap<>();
    private static String aggregatedCipherText = "";
    private static final BigInteger n = new BigInteger(
            "32317006071311007300153513477825163362488057133489075174588434139269806834136210002792056362640164685458556357935330816928829023080573472625273554742461245741026202527916572972862706300325263428213145766931414223654220941111348629991657478268034230553086349050635557712219187890332729569696129743856241741236237225197346402691855797767976823014625397933058015226858730761197532436467475855460715043896844940366130497697812854295958659597567051283852132784468522925504568272879113720098931873959143374175837826000278034973198552060607533234122603254684088120031105907484281003994966956119696956248629032338072839127039");
    private static final BigInteger n2 = n.multiply(n);

    public static void registerClient(String clientId, String cipherText) {
        clientCipherTexts.put(clientId, cipherText);
        updateAggregatedCipherText();
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
            result = result.multiply(currentCipher).mod(n2);
        }

        aggregatedCipherText = result.toString();
    }

    public static String getAggregatedCipherText() {
        return aggregatedCipherText;
    }

    public static int getClientCount() {
        return clientCipherTexts.size();
    }
}