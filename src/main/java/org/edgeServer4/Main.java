package org.edgeServer4;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.edgeServer4.handler.EdgeServer4Handler;

public class Main {
    private static final int PORT = 34567;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            EdgeServer4Handler edgeServer4Handler = new EdgeServer4Handler();

            // 注册路由
            server.createContext("/get/decryptedText", edgeServer4Handler);
            server.createContext("/get/receivedCipherText", edgeServer4Handler);
            server.createContext("/post/aggregatedCipherText", edgeServer4Handler);

            // 设置线程池
            server.setExecutor(null);

            // 启动服务器
            server.start();
            System.out.println("Edge Server 4 started on port " + PORT);

        } catch (IOException e) {
            System.err.println("Error starting server4: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
