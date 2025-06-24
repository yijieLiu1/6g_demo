src/java/main/org/目录中，有对应的不同参与方。

不同参与方端口情况，请顺序启动Main：
centerServer:33333

DataClient：13456
edgeServer1:23456
edgeServer2:33456

edgeServer3:24567
edgeServer4:34567

目前支持：
1.多个DataClient加密数据并发送
2.edgeServer接收密文并聚合
3.edgeServer2接收聚合密文并正确解密【支持浮点数，负数】
4.edgeServer2和4对解密的结果，再次执行ImprovePaillier再次加密发送，centerServer对结果进行聚合解密


Updating...
