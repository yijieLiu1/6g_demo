package org.centerServer;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.centerServer.handler.CenterServerHandler;

public class Main {
    private static final int PORT = 33333;

    public static void main(String[] args) {
        try {
            System.out.println("正在启动Center Server...");
            System.out.println("监听端口: " + PORT);

            InetSocketAddress address = new InetSocketAddress("0.0.0.0", PORT);
            HttpServer server = HttpServer.create(address, 0);
            CenterServerHandler centerServerHandler = new CenterServerHandler();

            // 注册路由
            System.out.println("注册路由...");
            server.createContext("/get/decryptedText", centerServerHandler);
            server.createContext("/post/aggregatedCipherText", centerServerHandler);
            System.out.println("路由注册完成");

            // 设置线程池
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));

            // 启动服务器
            server.start();
            System.out.println("Center Server启动成功!");
            System.out.println("可以通过以下地址访问:");
            System.out.println("- http://localhost:" + PORT + "/get/decryptedText");
            System.out.println("- http://localhost:" + PORT + "/post/aggregatedCipherText");

        } catch (IOException e) {
            System.err.println("Center Server启动失败!");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
