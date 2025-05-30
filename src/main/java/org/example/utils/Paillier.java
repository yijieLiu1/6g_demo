package org.example.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;

public class Paillier {
    private BigInteger p;
    private BigInteger q;
    private BigInteger n;
    private BigInteger lambda;
    private BigInteger g;
    private BigInteger mu;
    private int bitLength;
    private static final int SCALE = 10; // 小数位数

    public Paillier(int bitLength) {
        this.bitLength = bitLength;
        generateKeys();
    }

    private void generateKeys() {
        SecureRandom random = new SecureRandom();
        p = BigInteger.probablePrime(bitLength, random);
        q = BigInteger.probablePrime(bitLength, random);
        n = p.multiply(q);
        lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        g = n.add(BigInteger.ONE);
        mu = lambda.modInverse(n);
    }

    public BigInteger encrypt(BigInteger m) {
        SecureRandom random = new SecureRandom();
        BigInteger r = new BigInteger(bitLength, random);
        return g.modPow(m, n.multiply(n)).multiply(r.modPow(n, n.multiply(n))).mod(n.multiply(n));
    }

    public BigInteger encrypt(BigDecimal m) {
        // 将BigDecimal转换为BigInteger，保留SCALE位小数
        BigInteger scaledValue = m.multiply(BigDecimal.TEN.pow(SCALE)).toBigInteger();
        return encrypt(scaledValue);
    }

    public BigInteger decrypt(BigInteger c) {
        BigInteger u = c.modPow(lambda, n.multiply(n)).subtract(BigInteger.ONE).divide(n);
        return u.multiply(mu).mod(n);
    }

    public BigDecimal decryptToDecimal(BigInteger c) {
        BigInteger decrypted = decrypt(c);
        return new BigDecimal(decrypted).divide(BigDecimal.TEN.pow(SCALE), SCALE, RoundingMode.HALF_UP);
    }

    public BigInteger getN() {
        return n;
    }

    public BigInteger getG() {
        return g;
    }
}