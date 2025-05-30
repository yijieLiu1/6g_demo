package org.example.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private static final ConcurrentHashMap<String, DataManager> instances = new ConcurrentHashMap<>();
    private final String clientId;
    private BigDecimal plainData;
    private BigInteger cipherData;
    private final Paillier paillier;

    private DataManager(String clientId, BigDecimal initialData) {
        this.clientId = clientId;
        this.plainData = initialData;
        this.paillier = new Paillier(1024);
        this.cipherData = paillier.encrypt(initialData);
    }

    public static DataManager getInstance(String clientId, BigDecimal initialData) {
        return instances.computeIfAbsent(clientId, k -> new DataManager(k, initialData));
    }

    public BigDecimal getPlainData() {
        return plainData;
    }

    public BigInteger getCipherData() {
        return cipherData;
    }

    public void updateData(BigDecimal newData) {
        this.plainData = newData;
        this.cipherData = paillier.encrypt(newData);
    }

    public String getClientId() {
        return clientId;
    }
}