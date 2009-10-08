package com.l7tech.identity.ldap;

import org.junit.Test;
import org.junit.Assert;
import com.l7tech.security.types.CertificateValidationType;

/**
 *
 */
public class LdapIdentityProviderConfigTest {

    @Test
    public void testConfigUpgradeCertificateValidation() throws Exception {
        LdapIdentityProviderConfig config = new LdapIdentityProviderConfig();
        config.setSerializedProps( REV_CONFIG_OLD );
        Assert.assertEquals( "Revocation type 1", CertificateValidationType.REVOCATION, config.getCertificateValidationType() );

        config.setSerializedProps( REV_CONFIG_NEW );
        Assert.assertEquals( "Revocation type 2", CertificateValidationType.REVOCATION, config.getCertificateValidationType() );
    }

    @Test
    public void testConfigUpgradeCertificateIndexing() throws Exception {
        LdapIdentityProviderConfig config = new LdapIdentityProviderConfig();
        Assert.assertEquals( "Cert index type 1", LdapIdentityProviderConfig.UserCertificateUseType.NONE, config.getUserCertificateUseType() );

        config.setSerializedProps( INDEX_CONFIG_OLD_ENABLE );
        Assert.assertEquals( "Cert index type 2", LdapIdentityProviderConfig.UserCertificateUseType.INDEX, config.getUserCertificateUseType() );

        config.setSerializedProps( INDEX_CONFIG_OLD_DISABLE );
        Assert.assertEquals( "Cert index type 3", LdapIdentityProviderConfig.UserCertificateUseType.NONE, config.getUserCertificateUseType() );
    }

    private final String REV_CONFIG_OLD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<java version=\"1.6.0_16\" class=\"java.beans.XMLDecoder\"> \n" +
            " <object class=\"java.util.HashMap\"> \n" +
            "  <void method=\"put\"> \n" +
            "   <string>certificateValidationType</string> \n" +
            "   <object class=\"com.l7tech.security.types.CertificateValidationType\" method=\"valueOf\"> \n" +
            "    <string>REVOCATION</string> \n" +
            "   </object> \n" +
            "  </void> \n" +
            " </object> \n" +
            "</java> ";

    private final String REV_CONFIG_NEW = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<java version=\"1.6.0_16\" class=\"java.beans.XMLDecoder\"> \n" +
            " <object class=\"java.util.HashMap\"> \n" +
            "  <void method=\"put\"> \n" +
            "   <string>certificateValidationType</string> \n" +
            "   <string>REVOCATION</string> \n" +
            "  </void> \n" +
            " </object> \n" +
            "</java>";

    private final String INDEX_CONFIG_OLD_ENABLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<java version=\"1.6.0_16\" class=\"java.beans.XMLDecoder\"> \n" +
            " <object class=\"java.util.HashMap\"> \n" +
            "  <void method=\"put\"> \n" +
            "   <string>userCertsEnabled</string> \n" +
            "   <boolean>true</boolean> \n" +
            "  </void> \n" +
            " </object> \n" +
            "</java>";

    private final String INDEX_CONFIG_OLD_DISABLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<java version=\"1.6.0_16\" class=\"java.beans.XMLDecoder\"> \n" +
            " <object class=\"java.util.HashMap\"> \n" +
            "  <void method=\"put\"> \n" +
            "   <string>userCertsEnabled</string> \n" +
            "   <boolean>false</boolean> \n" +
            "  </void> \n" +
            " </object> \n" +
            "</java>";
}
