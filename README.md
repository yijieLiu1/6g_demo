src/java/main/org/目录中，有对应的不同参与方。

不同参与方端口情况，请逆序启动Main：
DataClient：13456
edgeServer:23456
edgeServer2:33456

目前支持：
1.多个DataClient加密数据并发送
2.edgeServer接收密文并聚合
3.edgeServer2接收聚合密文并正确解密【支持浮点数，负数】


Updating...
