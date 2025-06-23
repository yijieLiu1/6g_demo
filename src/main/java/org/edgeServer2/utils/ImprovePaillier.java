package org.edgeServer2.utils;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ImprovePaillier {
    // 固定密钥参数（示例，实际可替换为你生成的安全参数）
    private static final int KEY_LENGTH = 1024;
    // 以下参数请用你实际生成的密钥替换
    private static final BigInteger N = new BigInteger(
            "90913130451501070932748481772397382236595400999767665225756425196793045289596573635244644806527590596036271583704029795718397869466106666892892275736126608528415509903215455745878821317296387522127708648543179995865048169541568940067306362529200405975807838264041582506658754163254252528729244462399122305743");
    private static final BigInteger g = N.add(BigInteger.ONE);
    private static final BigInteger lambda = new BigInteger(
            "45456565225750535466374240886198691118297700499883832612878212598396522644798286817622322403263795298018135791852014897859198934733053333446446137868063294424461037071449150838286400181149899308261418066246622618192309278803167190357010628817448425314684508887260469397053760478154602842977833535203207464380");
    private static final BigInteger u = new BigInteger(
            "2197176502261358720108867809955964857695875096396908942375134965728816997497270518767649850324679197754750563624096491383607898578883982567608299854649881103939676082693258781381018765861633489104194542101719636281741927884627776454766085860478971497409053145409941019656189561926539414767831144743000065757");
    private static final BigInteger y = new BigInteger(
            "2948796819198410718004423930603723946313666176541427078116795290658418061936356029023515296393384827047");
    private static final BigInteger h = g.modPow(y, N.multiply(N));
    // 示例DO密钥（实际可扩展为数组）
    private static final BigInteger[] SK_DO = new BigInteger[] {
            new BigInteger(
                    "1069451829824803122376880573742682965832404725362034454011474465193708417421931030272190871340551275134859160851655766542172806010525839224826514174651337388841346558575250271026730257759485084080222006903431640369568064174075081971944669551551413246812723138388183074810842845629423900707029803521756207660832947055499505656898194284280334028760108992855883357754358134892347287879344402572445480157469808151412397692813105521851499564770884513736278239752788280431280443864663099902625627090422532324532826771453900289985477375824507407113115052732081361889352167571147892770190650330172027824356013641586040685539"),
            new BigInteger(
                    "6426624360668386839255621877281008320684668525878426507411436195330509155987228740276808080871247567828515853108246393390670723823191344410120056504871531938848721194411755146463744137801243148344672509137228302942096403049214923772648349690245730079267704213225983163468376226157971767167968243055807744206628932525134643995167860357826611126061699160170343173560841288386846690433758072211669368847659802455548478346046892762969572961502413673721964317278325380502814180680170572846120827720805095675944571971927706238493331865106954415396321549380335718473381239069906631622815522822347119822220541474986528166592")
    };
    private static final SecureRandom random = new SecureRandom();

    // 加密
    public static BigInteger encrypt(BigInteger x, int doIndex) {
        BigInteger r = new BigInteger(KEY_LENGTH / 2, random);
        BigInteger N2 = N.multiply(N);
        return g.modPow(x, N2)
                .multiply(h.modPow(r, N2))
                .multiply(SK_DO[doIndex])
                .mod(N2);
    }

    // 聚合
    public static BigInteger aggregate(BigInteger[] encryptedData) {
        BigInteger N2 = N.multiply(N);
        BigInteger aggregated = BigInteger.ONE;
        for (BigInteger encrypted : encryptedData) {
            aggregated = aggregated.multiply(encrypted).mod(N2);
        }
        return aggregated;
    }

    // 解密
    public static BigInteger decrypt(BigInteger aggregatedData) {
        BigInteger N2 = N.multiply(N);
        BigInteger L = aggregatedData.modPow(lambda, N2).subtract(BigInteger.ONE).divide(N);
        BigInteger De = L.multiply(u).mod(N).mod(y);
        // 若De > y/2，返回y-De，否则返回De本身
        if (De.compareTo(y.divide(BigInteger.TWO)) > 0) {
            return De.subtract(y);
        } else {
            return De;
        }
    }

    // 获取密钥参数
    public static BigInteger getN() {
        return N;
    }

    public static BigInteger getG() {
        return g;
    }

    public static BigInteger getLambda() {
        return lambda;
    }

    public static BigInteger getU() {
        return u;
    }

    public static BigInteger getY() {
        return y;
    }

    public static BigInteger getH() {
        return h;
    }

    public static BigInteger getSK_DO(int i) {
        return SK_DO[i];
    }

    public static void main(String[] args) {
        // 测试加密和解密
        // 加密
        BigInteger c1 = ImprovePaillier.encrypt(BigInteger.valueOf(-51), 0); // DO1
        BigInteger c2 = ImprovePaillier.encrypt(BigInteger.valueOf(-41), 1); // DO2

        // 聚合
        BigInteger agg = ImprovePaillier.aggregate(new BigInteger[] { c1, c2 });

        // 解密
        BigInteger result = ImprovePaillier.decrypt(agg);
        System.out.println("解密结果: " + result);
    }
}
