package org.dataClient;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.dataClient.handler.DataHandler;

import java.math.BigDecimal;

public class Main {
    private static final int PORT = 13456;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            DataHandler dataHandler = new DataHandler();

            // 注册路由
            server.createContext("/get/plainText", dataHandler);
            server.createContext("/get/cipherText", dataHandler);

            // 设置线程池
            server.setExecutor(null);

            // 启动服务器
            server.start();
            System.out.println("Server started on port " + PORT);

            // 示例：注册测试客户端，包括正数、负数和小数
            DataHandler.registerClient("test-client", new BigDecimal("1123.45"));
            DataHandler.registerClient("test-client2", new BigDecimal("-67.89"));
            DataHandler.registerClient("test-client3", new BigDecimal("-0.01"));
            DataHandler.registerClient("test-client4", new BigDecimal("123.45"));

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}