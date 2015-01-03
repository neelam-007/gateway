package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Config;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;

public class CassandraConnectionManagerTestStub extends CassandraConnectionManagerImpl implements CassandraConnectionManager{

    private boolean isReturnNull;


    public CassandraConnectionManagerTestStub(CassandraConnectionEntityManager cassandraEntityManager, Config config,
                                              SecurePasswordManager securePasswordManager, TrustManager trustManager,
                                              SecureRandom secureRandom, boolean isReturnNull) {
        super(cassandraEntityManager, config, securePasswordManager, trustManager, secureRandom);
        this.isReturnNull = isReturnNull;
    }

    public void setReturnNull(boolean isReturnNull) {
        this.isReturnNull = isReturnNull;
    }

    @Override
    protected CassandraConnectionHolder createConnection(final CassandraConnection cassandraConnectionEntity){
        return !isReturnNull? new CassandraConnectionHolderImpl(cassandraConnectionEntity, null, null) : null;
    }
}
