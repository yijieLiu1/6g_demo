package org.edgeServer;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.edgeServer.handler.EdgeHandler;

public class Main {
    private static final int PORT = 23456;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            EdgeHandler edgeHandler = new EdgeHandler();

            // 注册路由
            server.createContext("/get/totalclientNum", edgeHandler);
            server.createContext("/get/sumcipherText", edgeHandler);
            server.createContext("/post/cipherText", edgeHandler);

            // 设置线程池
            server.setExecutor(null);

            // 启动服务器
            server.start();
            System.out.println("Edge Server started on port " + PORT);

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
