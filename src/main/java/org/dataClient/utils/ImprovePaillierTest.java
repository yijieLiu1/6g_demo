package org.dataClient.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public class ImprovePaillierTest {
    private BigInteger k;
    private BigInteger N;
    private BigInteger g;
    private BigInteger h;
    private BigInteger lambda;
    private BigInteger u;
    private static BigInteger y;
    private BigInteger[] n_i;
    public BigInteger[] SK_DO;
    private BigInteger SK_CSP;
    private int bitLength = 1024;
    private SecureRandom random = new SecureRandom();

    // 构造函数，接收每个DO的模型参数哈希数组
    public ImprovePaillierTest() {
        keyGeneration(2);
    }

    // 密钥生成，SK_DO与模型参数哈希相关
    private void keyGeneration(int m) {
        BigInteger p = BigInteger.probablePrime(bitLength / 2, random);
        BigInteger q = BigInteger.probablePrime(bitLength / 2, random);
        N = p.multiply(q);
        k = new BigInteger(bitLength, random);
        g = N.add(BigInteger.ONE);
        do {
            y = new BigInteger(bitLength / 3, random);
        } while (!y.gcd(k).equals(BigInteger.ONE));
        h = g.modPow(y, N.multiply(N));
        lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))
                .divide(p.subtract(BigInteger.ONE).gcd(q.subtract(BigInteger.ONE)));
        u = g.modPow(lambda, N.multiply(N)).subtract(BigInteger.ONE).divide(N).modInverse(N);
        n_i = new BigInteger[m];
        SK_DO = new BigInteger[m];
        BigInteger sum = BigInteger.ZERO;
        for (int i = 0; i < m - 1; i++) {
            n_i[i] = new BigInteger(bitLength / 2, random);
            sum = sum.add(n_i[i]);
        }
        n_i[m - 1] = N.subtract(sum);
        BigInteger R_t;
        do {
            R_t = new BigInteger(bitLength, random);
        } while (!R_t.gcd(N).equals(BigInteger.ONE));
        for (int i = 0; i < m; i++) {
            SK_DO[i] = R_t.modPow(n_i[i], N.multiply(N));
        }
        SK_CSP = lambda;
    }

    public BigInteger encrypt(BigInteger x, BigInteger SK_DO_i) {
        BigInteger r = new BigInteger(bitLength / 2, random);
        return g.modPow(x, N.multiply(N))
                .multiply(h.modPow(r, N.multiply(N)))
                .multiply(SK_DO_i)
                .mod(N.multiply(N));
    }

    public BigInteger aggregate(BigInteger[] encryptedData) {
        BigInteger aggregated = BigInteger.ONE;
        for (BigInteger encrypted : encryptedData) {
            aggregated = aggregated.multiply(encrypted).mod(N.multiply(N));
        }
        return aggregated;
    }

    public BigInteger decrypt(BigInteger aggregatedData) {
        BigInteger L = aggregatedData.modPow(lambda, N.multiply(N)).subtract(BigInteger.ONE).divide(N);
        BigInteger De = L.multiply(u).mod(N).mod(y);
        // 若De > y/2，返回y-De，否则返回De本身
        if (De.compareTo(y.divide(BigInteger.TWO)) > 0) {
            return y.subtract(De);
        } else {
            return De;
        }
    }

    // 单独输出CSP密钥组和每个DO的密钥组
    public void printKeys() {
        System.out.println("N: " + N);
        System.out.println("CSP密钥组: ");
        System.out.println("lambda = " + lambda);
        System.out.println("u = " + u);
        System.out.println("y = " + y);
        System.out.println("\n每个DO的密钥组:");
        for (int i = 0; i < SK_DO.length; i++) {
            System.out.println("n_i: " + n_i[i]);
            System.out.println("SK_DO: " + SK_DO[i]);
        }
    }

    public static void main(String[] args) {
        int numDO = 2;
        // 构造 ImprovedPaillierTest 对象
        ImprovePaillierTest paillier = new ImprovePaillierTest();

        // 1. 输出CSP密钥组和DO密钥组（只输出一次）
        System.out.println("===== CSP密钥组和DO密钥组 =====");
        paillier.printKeys();
        System.out.println("=====================================");

        // 2. 四种测试情况
        BigInteger[][] testCases = {
                { new BigInteger("5"), new BigInteger("-4") },
                { new BigInteger("5"), new BigInteger("-7") },

        };
        for (BigInteger[] testCase : testCases) {
            // 加密每个 DO 的数据
            BigInteger[] encryptedMessages = new BigInteger[numDO];
            for (int i = 0; i < numDO; i++) {
                encryptedMessages[i] = paillier.encrypt(testCase[i], paillier.SK_DO[i]);
            }
            // 聚合加密后的数据
            BigInteger aggregated = paillier.aggregate(encryptedMessages);
            // 解密聚合后的数据
            BigInteger decrypted = paillier.decrypt(aggregated);
            // 输出测试结果
            System.out.println("原始数据: " + Arrays.toString(testCase));
            System.out.println("正确求和结果：" + testCase[0].add(testCase[1]));
            System.out.println("聚合后的密文: " + aggregated);
            System.out.println("解密后的结果: " + decrypted);
            System.out.println("=====================================");
        }
    }
}
