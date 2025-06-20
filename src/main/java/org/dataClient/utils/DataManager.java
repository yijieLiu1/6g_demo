package org.dataClient.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private static final ConcurrentHashMap<String, DataManager> instances = new ConcurrentHashMap<>();
    private final String clientId;
    private BigDecimal plainData;
    private BigInteger cipherData;
    private final Paillier paillier;

    private DataManager(String clientId, BigDecimal initialData, Paillier paillier) {
        this.clientId = clientId;
        this.plainData = initialData;
        this.paillier = paillier;
        if (paillier != null) {
            this.cipherData = paillier.encryptInst(initialData);
        } else {
            this.cipherData = Paillier.encrypt(initialData);
        }
    }

    public static DataManager getInstance(String clientId, BigDecimal initialData, Paillier paillier) {
        return instances.computeIfAbsent(clientId, k -> new DataManager(k, initialData, paillier));
    }

    public BigDecimal getPlainData() {
        return plainData;
    }

    public BigInteger getCipherData() {
        return cipherData;
    }

    public void updateData(BigDecimal newData) {
        this.plainData = newData;
        if (paillier != null) {
            this.cipherData = paillier.encryptInst(newData);
        } else {
            this.cipherData = Paillier.encrypt(newData);
        }
    }

    public String getClientId() {
        return clientId;
    }
}