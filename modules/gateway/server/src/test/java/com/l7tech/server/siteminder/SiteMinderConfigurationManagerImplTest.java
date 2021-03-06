package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SiteMinderConfigurationManagerImplTest {

    private SiteMinderConfigurationManager manager;

    @Before
    public void setup() throws Exception {
        manager = new SiteMinderConfigurationManagerImpl() {
            @Override
            public SiteMinderConfiguration findByUniqueName(String name) throws FindException {
                SiteMinderConfiguration config = new SiteMinderConfiguration();
                config.setGoid(PersistentEntity.DEFAULT_GOID);
                config.setName("aw80");
                config.setAddress("127.0.0.1");
                config.setSecret("{RC2}kZiLwNZObmPrkGIxDIr+wjLr92OMIXTySRn0YWaQ8uMEHNgyN6BqDYMDq434h37fVCRBGT/nM1gDWZDq9SG/TAx12EAJbItKxyj7SH4Obq25R8gwxXl/Xh2WvoYOfpX0SbxyYAdiWyE1rwZVhpnmm1ToIEZjypTbNtM3VQS7PflzMeoXDMkNlP0Na9PGxB+X");
                config.setIpcheck(false);
                config.setHostname("aw80Compat");
                config.setFipsmode(1);
                config.setNonClusterFailover(false);
                config.setClusterThreshold(50);
                Map<String, String> properties = new HashMap();
                properties.put("server.0.0.address", "10.7.34.32");
                properties.put("server.0.0.authentication.port", "44442");
                properties.put("server.0.0.authorization.port", "44443");
                properties.put("server.0.0.accounting.port", "44441");
                properties.put("server.0.0.connection.min", "1");
                properties.put("server.0.0.connection.max", "3");
                properties.put("server.0.0.connection.step", "1");
                properties.put("server.0.0.timeout", "75");

                properties.put("server.1.0.address", "10.7.34.33");
                properties.put("server.1.0.authentication.port", "44442");
                properties.put("server.1.0.authorization.port", "44443");
                properties.put("server.1.0.accounting.port", "44441");
                properties.put("server.1.0.connection.min", "1");
                properties.put("server.1.0.connection.max", "3");
                properties.put("server.1.0.connection.step", "1");
                properties.put("server.1.0.timeout", "75");

                properties.put("server.0.1.address", "10.7.34.34");
                properties.put("server.0.1.authentication.port", "44442");
                properties.put("server.0.1.authorization.port", "44443");
                properties.put("server.0.1.accounting.port", "44441");
                properties.put("server.0.1.connection.min", "1");
                properties.put("server.0.1.connection.max", "3");
                properties.put("server.0.1.connection.step", "1");
                properties.put("server.0.1.timeout", "75");
                config.setProperties(properties);
                return config;
            }
        };
    }

    @Ignore("Require siteminder connection")
    @Test
    public void testGetSiteMinderLowLevelAgent() throws Exception {
        SiteMinderLowLevelAgent agent = manager.getSiteMinderLowLevelAgent(PersistentEntity.DEFAULT_GOID);
        assertTrue(agent.isInitialized());
    }

    @Test
    public void testSiteMinderAgentConfig() throws FindException {

        SiteMinderConfiguration config = manager.getSiteMinderConfiguration("aw80");
        SiteMinderAgentConfig smConfig = new SiteMinderAgentConfig(config);
        assertEquals(3, smConfig.getServers().size());
        assertEquals("10.7.34.32", smConfig.getServers().get(0).serverIpAddress);
        assertEquals("10.7.34.34", smConfig.getServers().get(1).serverIpAddress);
        assertEquals("10.7.34.33", smConfig.getServers().get(2).serverIpAddress);

    }

    @Ignore("Requires SiteMinder connection")
    @Test
    public void testValidSiteMinderAgentConfig() throws FindException, SiteMinderApiClassException {
        manager.validateSiteMinderConfiguration(manager.getSiteMinderConfiguration("aw80"));
    }

    @Ignore("Requires SiteMinder connection")
    @Test (expected = SiteMinderApiClassException.class)
    public void testInvalidSiteMinderAgentConfig() throws FindException, SiteMinderApiClassException {
        SiteMinderConfiguration smConfig = manager.getSiteMinderConfiguration("aw80");
        smConfig.setSecret("This is invalid secret");
        manager.validateSiteMinderConfiguration(smConfig);
    }

}
