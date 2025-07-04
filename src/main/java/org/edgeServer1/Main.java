package org.edgeServer1;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.edgeServer1.handler.EdgeHandler;

public class Main {
    private static final int PORT = 23456;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            EdgeHandler edgeHandler = new EdgeHandler();

            // 注册路由
            // 获取client数
            server.createContext("/get/totalclientNum", edgeHandler);
            // 获取聚合密文并发送
            server.createContext("/get/sumcipherText", edgeHandler);
            // 为了接收来自client的密文
            server.createContext("/post/cipherText", edgeHandler);
            // 触发比较
            server.createContext("/post/triggerCompare", edgeHandler);
            // 多轮通信比较密文发送
            server.createContext("/post/comparePair", edgeHandler);
            server.createContext("/post/finalCompareResult", edgeHandler);

            // 设置线程池
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(56));

            // 启动服务器
            server.start();
            System.out.println("Edge Server started on port " + PORT);

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
