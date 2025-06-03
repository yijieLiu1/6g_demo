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
            server.createContext("/get/decryptedText", edgeServer2Handler);
            server.createContext("/get/receivedCipherText", edgeServer2Handler);
            server.createContext("/post/aggregatedCipherText", edgeServer2Handler);

            // 设置线程池
            server.setExecutor(null);

            // 启动服务器
            server.start();
            System.out.println("Edge Server 2 started on port " + PORT);

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
