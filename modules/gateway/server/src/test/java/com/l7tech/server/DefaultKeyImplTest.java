package com.l7tech.server;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.GoidUpgradeMapperTestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.security.KeyStoreException;

/**
 * This was created: 8/16/13 as 1:32 PM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultKeyImplTest {

    ServerConfigStub serverConfig = new ServerConfigStub();
    @Mock
    ClusterPropertyManager clusterPropertyManager;
    @Mock
    SsgKeyStoreManager keyStoreManager;
    @Mock
    PlatformTransactionManager transactionManager;

    DefaultKeyImpl defaultKey;
    private Goid keystoreGoid = new Goid(0, 3);
    private long keystoreOid = 3;
    private String alias = "mycaKey";

    @Before
    public void before() throws FindException, KeyStoreException {
        defaultKey = new DefaultKeyImpl(serverConfig, clusterPropertyManager, keyStoreManager, transactionManager);

        SsgKeyEntry key = SsgKeyEntry.createDummyEntityForAuditing(keystoreGoid, alias);
        Mockito.when(keyStoreManager.lookupKeyByKeyAlias(alias, keystoreGoid)).thenReturn(key);
        GoidUpgradeMapperTestUtil.addPrefix("keystore_file", 0);
    }

    @Test
    public void testPropertyWithOid() {
        System.setProperty("com.l7tech.server.keyStore.defaultCa.alias", keystoreOid+":"+alias);

        SsgKeyEntry key = defaultKey.getCaInfo();

        Assert.assertNotNull(key);
        Assert.assertEquals(keystoreGoid, key.getKeystoreId());
        Assert.assertEquals(alias, key.getAlias());
        Assert.assertEquals(keystoreGoid+":"+alias, key.getId());
    }

    @Test
    public void testPropertyWithGoid() {
        System.setProperty("com.l7tech.server.keyStore.defaultCa.alias", keystoreGoid+":"+alias);

        SsgKeyEntry key = defaultKey.getCaInfo();

        Assert.assertNotNull(key);
        Assert.assertEquals(keystoreGoid, key.getKeystoreId());
        Assert.assertEquals(alias, key.getAlias());
        Assert.assertEquals(keystoreGoid+":"+alias, key.getId());
    }
}
