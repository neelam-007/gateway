package com.l7tech.server.admin.ws;

import com.l7tech.gateway.common.GatewayConfiguration;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.admin.AdminLoginImpl;
import com.l7tech.util.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminLoginImplTest {
    private AdminLoginImpl adminLogin;
    @Mock
    private Config config;

    @Before
    public void setup() {
        adminLogin = new AdminLoginImpl(null, null, null, null);
        adminLogin.setServerConfig(config);
    }

    @Test
    public void getNetworkConfigurationDelegatesToConfig() throws Exception {
        when(config.getIntProperty(ServerConfigParams.PARAM_UUID_AMOUNT_MAX, 100)).thenReturn(500);

        final GatewayConfiguration result = adminLogin.getGatewayConfiguration();

        assertEquals(500, result.getUuidAmountMax());
        verify(config).getIntProperty(ServerConfigParams.PARAM_UUID_AMOUNT_MAX, 100);
    }
}
