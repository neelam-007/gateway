package com.l7tech.external.assertions.mysqlcounter.server;

import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterProvider;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterStore;
import org.hibernate.SessionFactory;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.ConcurrentHashMap;

public class MysqlCounterProvider implements SharedCounterProvider {

    public static final String KEY = "ssgdb";

    private final ConcurrentHashMap<String, MysqlCounterStore> counters;
    private final PlatformTransactionManager transactionManager;
    private final SessionFactory sessionFactory;

    MysqlCounterProvider(PlatformTransactionManager transactionManager, SessionFactory sessionFactory) {
        this.counters = new ConcurrentHashMap<>();
        this.transactionManager = transactionManager;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public SharedCounterStore getCounterStore(String name) {
        return counters.computeIfAbsent(name, key -> {
            MysqlCounterStore counterStore = new MysqlCounterStore(transactionManager);
            counterStore.setSessionFactory(sessionFactory);
            return counterStore;
        });
    }

    @Override
    public String getName() {
        return KEY;
    }
}
