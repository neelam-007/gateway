package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Config;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;

public class CassandraConnectionManagerTestStub extends CassandraConnectionManagerImpl implements CassandraConnectionManager{

    public CassandraConnectionManagerTestStub(CassandraConnectionEntityManager cassandraEntityManager, Config config,
                                              SecurePasswordManager securePasswordManager, TrustManager trustManager,
                                              SecureRandom secureRandom) {
        super(cassandraEntityManager, config, securePasswordManager, trustManager, secureRandom);
    }

    @Override
    protected CassandraConnectionHolder createConnection(final CassandraConnection cassandraConnectionEntity){
        return new CassandraConnectionHolderImpl(cassandraConnectionEntity, null, null);
    }
}
