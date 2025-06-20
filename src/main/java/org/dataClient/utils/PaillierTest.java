package org.dataClient.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public class PaillierTest {
    public static void main(String[] args) {
        // 生成两个大素数p和q
        Random rng = new SecureRandom();
        BigInteger p = BigInteger.probablePrime(1024, rng);
        BigInteger q = BigInteger.probablePrime(1024, rng);

        // 计算n = p * q
        BigInteger n = p.multiply(q);

        // 计算lambda = lcm(p-1, q-1)
        BigInteger pMinus1 = p.subtract(BigInteger.ONE);
        BigInteger qMinus1 = q.subtract(BigInteger.ONE);
        BigInteger lambda = lcm(pMinus1, qMinus1);

        // 计算g = n + 1
        BigInteger g = n.add(BigInteger.ONE);

        // 计算mu
        BigInteger n2 = n.multiply(n);
        BigInteger g_lambda = g.modPow(lambda, n2);
        BigInteger L = g_lambda.subtract(BigInteger.ONE).divide(n);
        BigInteger mu = L.modInverse(n);

        // 输出所有参数
        System.out.println("Paillier参数:");
        System.out.println("p = " + p);
        System.out.println("q = " + q);
        System.out.println("n = " + n);
        System.out.println("lambda = " + lambda);
        System.out.println("g = " + g);
        System.out.println("mu = " + mu);

        // 验证参数
        System.out.println("\n验证参数:");
        System.out.println("p和q是否互质: " + p.gcd(q).equals(BigInteger.ONE));
        System.out.println("L和n是否互质: " + L.gcd(n).equals(BigInteger.ONE));

        // 测试加密和解密
        System.out.println("\n测试加密和解密:");
        BigInteger m = new BigInteger("12345");
        BigInteger c = encrypt(m, n, g);
        BigInteger m_decrypted = decrypt(c, n, lambda, mu);
        System.out.println("原始消息: " + m);
        System.out.println("加密后: " + c);
        System.out.println("解密后: " + m_decrypted);
        System.out.println("解密是否正确: " + m.equals(m_decrypted));
    }

    // 计算最小公倍数
    private static BigInteger lcm(BigInteger a, BigInteger b) {
        return a.multiply(b).divide(a.gcd(b));
    }

    // 加密函数
    private static BigInteger encrypt(BigInteger m, BigInteger n, BigInteger g) {
        Random rng = new SecureRandom();
        BigInteger r = new BigInteger(1024, rng);
        BigInteger n2 = n.multiply(n);
        BigInteger gm = g.modPow(m, n2);
        BigInteger rn = r.modPow(n, n2);
        return gm.multiply(rn).mod(n2);
    }

    // 解密函数
    private static BigInteger decrypt(BigInteger c, BigInteger n, BigInteger lambda, BigInteger mu) {
        BigInteger n2 = n.multiply(n);
        BigInteger c_lambda = c.modPow(lambda, n2);
        BigInteger L = c_lambda.subtract(BigInteger.ONE).divide(n);
        return L.multiply(mu).mod(n);
    }
}