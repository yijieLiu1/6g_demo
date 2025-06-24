package org.edgeServer2.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Random;

public class Paillier {
    // 密钥长度（比特）
    private static final int KEY_LENGTH = 1024;
    private static final int SCALE = 8; // 保留8位小数

    // 公钥参数
    private static final BigInteger n = new BigInteger(
            "19091888195394899167269594393458492531853606908098661974981932232174362480932870121177422681565852355942316893458264239123205757783521313989728135417239609931669044790196589974621628520658553488843168300419738790896207431267628052889695276828315583209034031674009352010098816064965475882551498518609451460688510955601277809159539842731998726267134581077487469849737998600970618438086506409416323211411744402479180328912528698383282079682086577297871622644547182334991372579769082113744710382126063919342566231158423210156778088884270206427515413241109998748971884777114569048830864514992115103606282992341075546173389");
    private static final BigInteger g = n.add(BigInteger.ONE);

    // 私钥参数
    private static final BigInteger lambda = new BigInteger(
            "4772972048848724791817398598364623132963401727024665493745483058043590620233217530294355670391463088985579223364566059780801439445880328497432033854309902482917261197549147493655407130164638372210792075104934697724051857816907013222423819207078895802258507918502338002524704016241368970637874629652362865172057323815385703401795995081165387695802179319978094646065773102617463190152773329699740030717813623535131428348284722845162821326416047809788913519674111224752009676223800907162844718246028871077901090107768181249932408899893292291590559258754024003421931244666555944383358495180820328753503847993959708340200");

    // 计算mu = (L(g^lambda mod n^2))^(-1) mod n
    private static final BigInteger mu = new BigInteger(
            "8762676180648866948085148644234730052347919324217610501013816935146258541373924240355909212564004780247412142265048042932718102622305634998539396453659682473096442377092092279648358408257825756018027101094754895725127303773028507798199443110954840588922826633996306772228330371112836166585165382399304760194446860287800476750827758992178975847066479114302194741687968238136533191506768261308432870633986229523043778002319067610607377778752299247304574268354808695117368913460914126972408888241677645174911954253692124253404109409953359349818836109611645267630097753240536879987485162847562638307684186781753119152164");

    public static BigInteger encrypt(BigInteger m) {
        // 直接用BigDecimal包装，调用原有的encrypt(BigDecimal)
        return encrypt(new BigDecimal(m));
    }

    public static BigInteger encrypt(BigDecimal m) {
        // 将小数转换为整数（乘以10^8保留8位小数）
        BigDecimal scaled = m.setScale(SCALE, RoundingMode.HALF_UP);
        BigInteger m_int = scaled.multiply(BigDecimal.TEN.pow(SCALE)).toBigInteger();

        // 只对负数使用模n对称表示
        if (m_int.compareTo(BigInteger.ZERO) < 0) {
            m_int = m_int.mod(n);
        }

        // 生成随机数r
        Random rng = new SecureRandom();
        BigInteger r = new BigInteger(KEY_LENGTH, rng);

        // 计算密文 c = g^m * r^n mod n^2
        BigInteger n2 = n.multiply(n);
        BigInteger gm = g.modPow(m_int, n2);
        BigInteger rn = r.modPow(n, n2);
        return gm.multiply(rn).mod(n2);
    }

    public static BigDecimal decrypt(BigInteger c) {
        BigInteger n2 = n.multiply(n);
        BigInteger c_lambda = c.modPow(lambda, n2);
        BigInteger L = c_lambda.subtract(BigInteger.ONE).divide(n);
        BigInteger m = L.multiply(mu).mod(n);

        // 处理负数情况
        BigInteger halfN = n.divide(BigInteger.TWO);
        if (m.compareTo(halfN) > 0) {
            m = m.subtract(n);
        }

        // 将整数转换回小数（除以10^8）
        return new BigDecimal(m).divide(BigDecimal.TEN.pow(SCALE), SCALE, RoundingMode.HALF_UP);
    }

    // 获取公钥参数
    public static BigInteger getPublicKey() {
        return n;
    }

    // 获取生成元
    public static BigInteger getGenerator() {
        return g;
    }

    // 获取模数n的平方
    public static BigInteger getN2() {
        return n.multiply(n);
    }
}