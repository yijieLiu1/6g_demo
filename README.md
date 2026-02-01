# 云边端协同的安全联合计算示例（6G Demo）

​	本项目实现了“数据端加密 → 边缘聚合与比较 → 中心汇总解密”的多方协同计算流程，支持求和、均值、方差与极值（最大/最小）等统计指标的隐私计算。系统包含 6 个参与方：1 个中心服务器、2 组边缘服务器（每组 2 个:12一组，34一组）、以及数据客户端。

​	数据集为smart_manufacturing_data.csv。通过dataPreprocess.java (支持处理处理少量数据集，进行流程测试。)预处理为 smart_manufacturing_data_preprocessed.csv【原始10w条】

​	推荐启动顺序：centerServer.Main——edgeServer1,2.Main——edgeServer3,4.Main——dataClient.ServerMain

​	**演示思路：**

​	1）【密文发送】客户端把密文均分给edgeServer1和2（1组），edgeServer3和4（2组，两组功能完全一致）。

​	2）【边缘服务器侧计算】edgeServer1发起密文聚合请求完成，则edgeServer2侧可获取求和、均值、方差计算。极值计算：edgeServer1先触发极值协作计算的POST请求，组内协作完成后，edgeServer2侧可获取极大/小值。**（另有一个极值协作计算-遍历的POST请求，主要用于方案效率对比，选择前者的POST即可。）**

​	3）2组需要同样完成上述的操作，以确保边缘服务器侧数据收集完成。从而向中心服务器发送密文数据，在中心服务器侧开展计算。

​	4）【中心服务器侧计算】edgeServer2，向中心服务器发送ImPaillier的密文和平方密文。edgeServer1，向中心服务器发送极值信息。3，4也进行同样的操作。完成后，即可在中心服务器侧获取全局的和、均值、方差、极值计算结果。

## 参与方与核心功能

- Center Server（中心服务器）
  - 汇聚来自两组边缘的二次加密结果（Improve Paillier）。
  - 进行最终解密与统计：总和、均值、方差、极值结果汇总。

- Edge Server 1（边缘组 A：上游聚合与极值计算）
  - 接收 DataClient 的密文与平方密文。
  - 在组内进行密文乘法聚合（对应明文求和、平方和）。
  - 触发极值比较流程并生成“极值比较密文”。

- Edge Server 2（边缘组 A：下游解密与二次加密）
  - 接收 Edge Server 1 聚合后的密文。
  - 解密得到组内求和结果；可计算均值/方差。
  - 使用 Improve Paillier 对结果进行二次加密后发送至 Center Server。
  - 负责解密比较密文，生成组内极值比较结果。

- Edge Server 3（边缘组 B：上游聚合与极值计算）
  - 与 Edge Server 1 同步流程，但使用“新 Paillier 密钥”。

- Edge Server 4（边缘组 B：下游解密与二次加密）
  - 与 Edge Server 2 同步流程，但使用“新 Paillier 密钥”。

- Data Client（数据客户端）
  - 读取 CSV 数据并进行加密。
  - 将密文发送至 Edge Server 1 或 Edge Server 3（按数据划分）。
  - 可输出 `encrypted_data.csv` 便于复现实验。

## 核心工作流

### 1) 加密并上报数据（DataClient → Edge Server 1/3）
- DataClient 读取 `smart_manufacturing_data_preprocessed.csv`。
- 按数据分片：前半使用旧 Paillier 密钥发送到 Edge Server 1；后半使用新 Paillier 密钥发送到 Edge Server 3。
- 发送接口：`POST /post/cipherText`。

### 2) 边缘聚合（Edge Server 1/3 → Edge Server 2/4）
- 触发 `GET /get/sumcipherText`，计算密文乘法聚合（求和与平方和）。
- 发送接口：`POST /post/aggregatedCipherText` 到下游边缘（Edge Server 2/4）。

### 3) 均值/方差流程（Edge Server 2/4 → Center Server）
- Edge Server 2/4 解密聚合密文，计算本组求和与均值。
- 调用 `GET /get/impaillierCipherText` 进行二次加密并上送 Center Server。
- 方差流程：调用 `GET /get/impaillierVarianceCipherText` 发送二次加密的平方和。
- Center Server 汇聚两组二次加密结果，解密得到全局求和、均值、方差。

### 4) 极值流程（Edge Server 1/3 → Edge Server 2/4 → Center Server）
- 触发 Edge Server 1/3 的极值比较（`POST /post/comparePair` 或 `POST /post/allComparePair`）。
- Edge Server 2/4 解密比较密文并保存组内最大/最小 clientId。
- Edge Server 1/3 调用 `GET /get/extremeCipherText` 将极值密文上送 Center Server。
- Center Server 进行跨组极值比较，输出最终极值 clientId。

## 启动方式

### 环境要求
- Java 11（JDK 11）
- Maven 3.6+

### 构建

```bash
mvn -q -DskipTests package
```

### 启动顺序（推荐）

```bash
# 1) Center Server
java -cp target/classes org.centerServer.Main

# 2) Edge Server 2 / 4（下游边缘）
java -cp target/classes org.edgeServer2.Main
java -cp target/classes org.edgeServer4.Main

# 3) Edge Server 1 / 3（上游边缘）
java -cp target/classes org.edgeServer1.Main
java -cp target/classes org.edgeServer3.Main
```

### 启动 Data Client

两种模式：

1) 批量模式（一次性读文件并发送）：
```bash
java -cp target/classes org.dataClient.Main
```

2) 服务模式（HTTP 触发，**推荐**）：
```bash
java -cp target/classes org.dataClient.ServerMain
# 触发发送（GET）
# http://localhost:13456/get/sendData
```

## 端口与角色

- Center Server: `33333`
- Edge Server 1: `23456`
- Edge Server 2: `33456`
- Edge Server 3: `24567`
- Edge Server 4: `34567`
- Data Client（服务模式）: `13456`

## APIfox 接口发送规范

### 通用约定
- 请求体为 JSON 时需设置：`Content-Type: application/json`。
- 比较接口需要在 Header 中携带客户端标识。
- 绝大多数接口返回 `text/plain`，少数返回 JSON（如 Data Client 服务模式）。

### Data Client → Edge Server（数据上报）
- `POST http://localhost:23456/post/cipherText`
- `POST http://localhost:24567/post/cipherText`

Headers：
- `Client-ID: client-xxx`
- `Content-Type: application/json`

Body：
```json
{
  "cipherText": "<加密密文>",
  "squareCipherText": "<加密平方密文>",
  "interval": "<可选区间标签>"
}
```

### Edge Server 1/3 → Edge Server 2/4（聚合密文上送）
- `POST http://localhost:33456/post/aggregatedCipherText`
- `POST http://localhost:34567/post/aggregatedCipherText`

Body：
```json
{
  "cipherText": "<聚合密文>",
  "squareCipherText": "<聚合平方密文>",
  "clientCount": 123
}
```

### Edge Server 2/4 → Center Server（Improve Paillier 二次加密上送）
- `POST http://localhost:33333/post/aggregatedCipherText`
- `POST http://localhost:33333/post/varianceCipherText`

Headers：
- `Server-Type: server2 | server4`
- `Content-Type: application/json`

Body：
```json
{
  "encryptedValue": "<Improve Paillier 密文>",
  "clientCount": 123
}
```

### 极值结果上送（Edge Server 1/3 → Center Server）
- `POST http://localhost:33333/post/extremeCipherText`

Body：
```json
{
  "maxClientId": "client-xxx",
  "maxCipherText": "<max 密文>",
  "minClientId": "client-yyy",
  "minCipherText": "<min 密文>",
  "serverId": "edgeServer1 | edgeServer3"
}
```

### 边缘比较（Edge Server 1/3 → Edge Server 2/4）
- `POST http://localhost:33456/post/comparisonData`
- `POST http://localhost:34567/post/comparisonData`

Headers：
- `Client-ID1: client-xxx`
- `Client-ID2: client-yyy`

Body（纯文本一行）：
```
<比较密文>
```

### 手动触发类接口（GET/POST）
- `GET /get/sumcipherText`：触发上游边缘聚合并下发。
- `GET /get/decryptedText`：解密查看聚合结果。
- `GET /get/meanResult`：均值计算结果。
- `GET /get/varianceResult`：方差计算结果。
- `GET /get/impaillierCipherText`：二次加密后上送 Center Server。
- `GET /get/impaillierVarianceCipherText`：二次加密的平方和上送 Center Server。
- `GET /get/extremeResult`：Center Server 极值结果。
- `POST /post/comparePair`：区间优化比较。
- `POST /post/allComparePair`：全量比较。

## 数据文件说明

- `smart_manufacturing_data_preprocessed.csv`：主输入数据。
- `encrypted_data.csv`：运行 DataClient 后输出的加密结果。

## 常见问题

- 如果请求返回 404：请确认端口与路径是否匹配对应参与方。
- 若均值/方差为空：通常表示尚未完成聚合或未触发二次加密上送流程。
