package com.l7tech.uddi;

import org.junit.Test;
import static junit.framework.Assert.*;

/**
 *
 */
public class UDDIClientFactoryTest {

    @Test
    public void testCreateClient() {
        UDDIClient client = UDDIClientFactory.getInstance().newUDDIClient(
                "http://127.0.0.1/uddi/inquiry",
                "http://127.0.0.1/uddi/publish",
                "http://127.0.0.1/uddi/subscription",
                "http://127.0.0.1/uddi/security",
                "username",
                "password",
                PolicyAttachmentVersion.v1_2,
                null );
        assertNotNull("Client not null", client);
    }
    @Test
    public void testCreateClientFromConfig() {
        UDDIClientConfig config = new UDDIClientConfig(
                "http://127.0.0.1/uddi/inquiry",
                "http://127.0.0.1/uddi/publish",
                "http://127.0.0.1/uddi/subscription",
                "http://127.0.0.1/uddi/security",
                "username",
                "password",
                null
        );

        UDDIClient client = UDDIClientFactory.getInstance().newUDDIClient( config );
        assertNotNull("Client not null", client);
    }

    @Test
    public void testCreateClientWithUDDIRegistryInfo() {
        UDDIRegistryInfo info = new UDDIRegistryInfo(){
            @Override
            public String getName() {
                return "test";
            }

            @Override
            public String getInquiry() {
                return "inquiry";
            }

            @Override
            public String getPublication() {
                return "publish";
            }

            @Override
            public String getSecurityPolicy() {
                return "security";
            }

            @Override
            public String getSubscription() {
                return "subscription";
            }
        };

        UDDIClient client = UDDIClientFactory.getInstance().newUDDIClient(
                "http://127.0.0.1/uddi/",
                info,
                "username",
                "password",
                null,
                null );
        assertNotNull("Client not null", client);
    }

    @Test
    public void testDefaultWsPolicyAttachmentVersion() {
        assertEquals( "WS Policy Attachment Version",
                PolicyAttachmentVersion.v1_2,
                UDDIClientFactory.getDefaultPolicyAttachmentVersion() );
    }
}
