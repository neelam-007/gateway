package com.ca.siteminder;

import com.l7tech.util.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/21/13
 */
public class SiteMinderAgentTest {
    private static final Logger logger = Logger.getLogger(SiteMinderAgent.class.getName());

    static String AGENT_CONFIG = "#Annie's host on 10.7.34.32 - COMPAT\n" +
            "aw80.name = layer7-agent\n" +
            "aw80.secret = {RC2}LwXoqG3BnL8PDQqoTcOk7WGgkKRCKxOBoVon4b9ofNMzd8L9OpHc4djGUEzueLb80+341xujT/n+jC1wS9DKizZ9og6NkMbepYtDnPEp7G738n10EY0LWctk/2pBHTwu/RQVeaU3NrilxuO2pdLa9rle5rn3TVczPQvnUrQLu9SFnZC4e9l2mhMq/lEA3H2g\n" +
            "aw80.address = 127.0.0.1\n" +
            "aw80.ipcheck = false\n" +
            "aw80.hostname = aw80Compat\n" +
            "aw80.fipsmode = COMPAT\n" +
            "aw80.update_cookie = true\n" +
            "aw80.noncluster_failover = false\n" +
            "aw80.cluster_threshold = 50\n" +
            "aw80.server.0.0.address = 10.7.34.32\n" +
            "aw80.server.0.0.authentication.port = 44442\n" +
            "aw80.server.0.0.authorization.port = 44443\n" +
            "aw80.server.0.0.accounting.port = 44441\n" +
            "aw80.server.0.0.connection.min = 1\n" +
            "aw80.server.0.0.connection.max = 3\n" +
            "aw80.server.0.0.connection.step = 1\n" +
            "aw80.server.0.0.timeout = 75\n";

    SiteMinderAgent fixture;
    SiteMinderAgentApiClassHelper classHelper = new SiteMinderAgentApiClassHelper(true);



    @Before
    public void setUp() throws Exception {
       fixture = getAgent(AGENT_CONFIG, "aw80");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSiteMinderLowLeveAgent() throws Exception {
        Config config = new Config(AGENT_CONFIG);


        SiteMinderAgentConfig agentConfig = config.getAgentConfig("aw80");

        SiteMinderLowLevelAgent lowLevelAgent = new SiteMinderLowLevelAgent(agentConfig);
        assertTrue(lowLevelAgent.isInitialized());
    }

    @Test
    public void testSiteMinderHighLevelAgent() throws Exception {
        SiteMinderHighLevelAgent hla = new SiteMinderHighLevelAgent(AGENT_CONFIG, "aw80");
//        hla.checkAndInitialize(AGENT_CONFIG, "aw80");
        SiteMinderContext context = new SiteMinderContext();

        assertTrue(hla.checkProtected("aw80", "127.0.0.1", "/resfilter*", "POST", context));

        SiteMinderCredentials testCredentials = new SiteMinderCredentials("wssker_tacoma", "7layer");
        assertEquals(1, hla.processAuthenticationRequest(testCredentials, "127.0.0.1", null, context));
        for(Pair<String, Object> attr : context.getAttrList()) {
            System.out.println(attr.getKey() + ": " + attr.getValue());
        }
        String smsession = context.getSsoToken();
        assertEquals(1, hla.processAuthenticationRequest(new SiteMinderCredentials(), "127.0.0.1", smsession, context));
        System.out.println("SMSESSION=" + context.getSsoToken());
    }

    @Test
    public void testSiteMinderAuthentication() throws Exception {
        HashMap<String,Object> attrMap = new HashMap();
        Subject testSubject = new Subject();
        testSubject.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                return "wssker_tacoma";  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        testSubject.getPrivateCredentials().add(new String("7layer"));
        String smSessionCookie = fixture.authenticateAndAuthorize(testSubject, "127.0.0.1", "/resfilter*", "POST", null, false, new HashMap(), attrMap);
        System.out.println("SMSESSION: " + smSessionCookie);
        for(String key: attrMap.keySet()) {
            System.out.println(key + ": " + attrMap.get(key));
        }
    }


    private SiteMinderAgent getAgent(final String currentConfig, final String agentId)
            throws SiteMinderAgentConfigurationException, SiteMinderApiClassException {


       Config config = new Config(currentConfig);


        SiteMinderAgentConfig agentConfig = config.getAgentConfig(agentId);

        logger.log(Level.INFO, "Initializing SiteMinder agent: " + agentId);
        SiteMinderAgent agent = new SiteMinderAgent(agentConfig, classHelper);

        return agent;

    }
}
