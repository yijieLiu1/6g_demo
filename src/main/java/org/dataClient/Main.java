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

            // 比较大小：
            // edgeServer2-->En(m1)-->En(r1*m1+r2)-->centerServer
            // edgeServer4-->En(m2)-->En(r1*m2)^-1*En(r3)-->centerServer

            // cneterServer-->De【En(r1*m1+r2)】=result_server2 !SK_Paillier_01
            // centerServer-->De【En(r1*m2)^-1*En(r3)】=result_server4 !SK_Paillier_02
            // centerServer-->result_server2+result_server4=final_result !条件r1>r2+r3

            // 求方差--edgeServer2此时已经拥有聚合值m1+m2。以及对应的均值。edgeServer2对聚合值进行平方，得到m1^2+m2^2+2*m1*m2
            // client-->En(m1^2)-->edgeServer1
            // edgeServer1-->edgeServer2

            // 启动服务器
            server.start();
            System.out.println("Server started on port " + PORT);

            // 示例：注册测试客户端，包括正数、负数和小数
            DataHandler.registerClient("test-client", new BigDecimal("22.45"));
            DataHandler.registerClient("test-client2", new BigDecimal("13.45"));
            DataHandler.registerClient("test-client3", new BigDecimal("123.01"));
            DataHandler.registerClient("test-client4", new BigDecimal("212.45"));

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}