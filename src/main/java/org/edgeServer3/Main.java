package org.edgeServer3;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.edgeServer3.handler.EdgeServer3Handler;

public class Main {
    private static final int PORT = 24567;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            EdgeServer3Handler edgeServer3Handler = new EdgeServer3Handler();

            // 注册路由
            server.createContext("/get/totalclientNum", edgeServer3Handler);
            server.createContext("/get/sumcipherText", edgeServer3Handler);
            server.createContext("/get/compareCipherText", edgeServer3Handler);
            server.createContext("/post/cipherText", edgeServer3Handler);
            server.createContext("/post/triggerCompare", edgeServer3Handler);

            server.createContext("/post/comparePair", edgeServer3Handler);
            server.createContext("/post/finalCompareResult", edgeServer3Handler);

            // 设置线程池
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(56));

            // 启动服务器
            server.start();
            System.out.println("Edge Server3 started on port " + PORT);

        } catch (IOException e) {
            System.err.println("Error starting server3: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
