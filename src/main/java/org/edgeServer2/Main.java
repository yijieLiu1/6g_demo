package org.edgeServer2;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.edgeServer2.handler.EdgeServer2Handler;

public class Main {
    private static final int PORT = 33456;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            EdgeServer2Handler edgeServer2Handler = new EdgeServer2Handler();

            // 注册路由
            // 第一层，dataClient-->edgeServer，获取聚合值，极值，均值，方差
            server.createContext("/get/decryptedText", edgeServer2Handler);
            server.createContext("/get/compareResult", edgeServer2Handler);
            server.createContext("/get/meanResult", edgeServer2Handler);
            server.createContext("/get/varianceResult", edgeServer2Handler);
            // 第二层edgeServer2-->centerServer,获取并发送Impaillier密文。执行后，centerServer可以求聚合值，均值。
            server.createContext("/get/impaillierCipherText", edgeServer2Handler);
            // 获取并发送。edgeServer2解密得到了client的x^2的聚合值。然后再使用Impaillier加密，发送给centerServer。
            server.createContext("/get/impaillierVarianceCipherText", edgeServer2Handler);

            server.createContext("/post/twoClientCompareResult", edgeServer2Handler);
            server.createContext("/post/aggregatedCipherText", edgeServer2Handler);
            server.createContext("/post/comparisonData", edgeServer2Handler);
            server.createContext("/post/finalCompareResult", edgeServer2Handler);

            // 设置线程池
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(56));

            // 启动服务器
            server.start();
            System.out.println("Edge Server 2 started on port " + PORT);

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
