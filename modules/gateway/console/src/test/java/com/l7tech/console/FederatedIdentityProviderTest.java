package com.l7tech.console;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import static org.junit.Assert.*;

import com.l7tech.objectmodel.Goid;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author alex
 */
@Ignore
public class FederatedIdentityProviderTest {
    private static Registry registry;
    private FederatedIdentityProviderConfig config;

    /**
     * create the <code>TestSuite</code> for the FederatedIdentityProviderTest <code>TestCase</code>
     */
    @BeforeClass
    public static void init() {
        try {
            new SsgAdminSession();
            registry = Registry.getDefault();
        } catch (Exception e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    @Before
    public void setUp() throws Exception {
        this.config = new FederatedIdentityProviderConfig();
        config.setX509Supported(true);
        config.setSamlSupported(true);
        config.setName("Example FIP");
    }

    @Test
    public void testSaveConfig() throws Exception {
        final Goid oid = registry.getIdentityAdmin().saveIdentityProviderConfig(config);
        System.err.println("Saved Federated IDPC #" + oid);

        FederatedIdentityProviderConfig conf = (FederatedIdentityProviderConfig) registry.getIdentityAdmin().findIdentityProviderConfigByID(oid);

        assertNotNull(conf);
        System.err.println("Loaded FIPC " + conf);
        registry.getIdentityAdmin().deleteIdentityProviderConfig(oid);
    }

    @Test
    public void testSaveVirtualGroup() throws Exception {
        IdentityProviderConfig config = null;

        final IdentityAdmin identityAdmin = registry.getIdentityAdmin();
        EntityHeader[] configs = identityAdmin.findAllIdentityProviderConfig();
        for (int i = 0; i < configs.length; i++) {
            EntityHeader entityHeader = configs[i];
            if (entityHeader.getType() == EntityType.ID_PROVIDER_CONFIG) {
                config = identityAdmin.findIdentityProviderConfigByID(entityHeader.getGoid());
                if (config.type() == IdentityProviderType.FEDERATED) {
                    break;
                } else {
                    config = null;
                }
            }
        }

        assertNotNull("There must already be a Federated Identity Provider", config);

        final VirtualGroup vg = new VirtualGroup(config.getGoid(), "CN is anything");
        vg.setX509SubjectDnPattern("CN=*");
        String soid = identityAdmin.saveGroup(config.getGoid(), vg, null);

        assertNotNull("Couldn't save virtual group", soid);
    }
}