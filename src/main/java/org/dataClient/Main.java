package org.dataClient;

import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

            // 从文件加载和注册客户端
            registerClientsFromFile("data.txt");

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerClientsFromFile(String filePath) {
        List<String> clientData = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                clientData.add(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error reading client data file: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        org.dataClient.handler.DataHandler.totalClientCount = clientData.size();
        for (int i = 0; i < clientData.size(); i++) {
            try {
                BigDecimal data = new BigDecimal(clientData.get(i));
                String clientId = "client-" + (i + 1);
                DataHandler.registerClient(clientId, data);
                System.out.println("Registered client: " + clientId + " with data: " + data);
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format for client " + (i + 1) + ": " + clientData.get(i));
            }
        }
    }
}