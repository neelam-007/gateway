package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.commonui.CommonUIServices;
import com.l7tech.policy.assertion.ext.commonui.CustomPrivateKeyPanel;
import com.l7tech.policy.assertion.ext.commonui.CustomSecurePasswordPanel;
import com.l7tech.policy.assertion.ext.commonui.CustomTargetVariablePanel;
import com.l7tech.policy.assertion.ext.security.SignerServices;
import com.l7tech.security.cert.TestCertificateGenerator;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommonUIServicesTest {

    @Mock
    private Registry registry;

    @Mock
    private TrustedCertAdmin trustedCertAdmin;

    private CommonUIServices commonUIServices;

    @Before
    public void setup() throws Exception {
        Registry.setDefault(registry);
        when(registry.isAdminContextPresent()).thenReturn(true);

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, new CustomAssertionHolder(), null);
        commonUIServices = (CommonUIServices) consoleContext.get(CommonUIServices.CONSOLE_CONTEXT_KEY);
        Assert.assertNotNull(commonUIServices);
    }

    @Test
    @Ignore("Developer Test")
    public void testCreateTargetVariablePanel() throws Exception {
        CustomTargetVariablePanel targetVariablePanel = commonUIServices.createTargetVariablePanel();
        Assert.assertNotNull(targetVariablePanel);
        Assert.assertNotNull(targetVariablePanel.getPanel());
    }

    @Test
    @Ignore("Developer Test")
    public void testCreatePasswordComboBoxPanel() throws Exception {
        when(registry.getTrustedCertManager()).thenReturn(trustedCertAdmin);
        List<SecurePassword> passwords = this.createSecurePasswordList();
        when(trustedCertAdmin.findAllSecurePasswords()).thenReturn(passwords);

        CustomSecurePasswordPanel securePasswordPanel = commonUIServices.createPasswordComboBoxPanel(new JDialog());
        Assert.assertNotNull(securePasswordPanel);
        Assert.assertNotNull(securePasswordPanel.getPanel());

        // check that only passwords are populated. Not PEM private keys.
        Assert.assertTrue(securePasswordPanel.containsItem(new Goid(0,1000L).toString()));
        Assert.assertTrue(securePasswordPanel.containsItem(new Goid(0,1001L).toString()));
        Assert.assertFalse(securePasswordPanel.containsItem(new Goid(0,1002L).toString()));
        Assert.assertFalse(securePasswordPanel.containsItem(new Goid(0,1003L).toString()));
    }

    @Test
    @Ignore("Developer Test")
    public void testCreatePEMPrivateKeyComboBoxPanel() throws Exception {
        when(registry.getTrustedCertManager()).thenReturn(trustedCertAdmin);
        List<SecurePassword> passwords = this.createSecurePasswordList();
        when(trustedCertAdmin.findAllSecurePasswords()).thenReturn(passwords);

        CustomSecurePasswordPanel securePasswordPanel = commonUIServices.createPEMPrivateKeyComboBoxPanel(new JDialog());
        Assert.assertNotNull(securePasswordPanel);
        Assert.assertNotNull(securePasswordPanel.getPanel());

        // check that only PEM private keys are populated. Not passwords.
        Assert.assertFalse(securePasswordPanel.containsItem(new Goid(0,1000L).toString()));
        Assert.assertFalse(securePasswordPanel.containsItem(new Goid(0,1001L).toString()));
        Assert.assertTrue(securePasswordPanel.containsItem(new Goid(0,1002L).toString()));
        Assert.assertTrue(securePasswordPanel.containsItem(new Goid(0,1003L).toString()));
    }

    @Test
    @Ignore("Developer Test")
    public void testCreateGatewayPrivateKeyComboBoxPanel() throws Exception {
        when(registry.getTrustedCertManager()).thenReturn(trustedCertAdmin);
        List<KeystoreFileEntityHeader> keystores = this.createKeystoresList();
        when(trustedCertAdmin.findAllKeystores(anyBoolean())).thenReturn(keystores);
        List<SsgKeyEntry> keys = this.createKeys();
        when(trustedCertAdmin.findAllKeys(any(Goid.class), anyBoolean())).thenReturn(keys);

        CustomPrivateKeyPanel privateKeyPanel = commonUIServices.createPrivateKeyComboBoxPanel(new JDialog());
        Assert.assertNotNull(privateKeyPanel);
        Assert.assertNotNull(privateKeyPanel.getPanel());

        privateKeyPanel.setSelectedItem(SignerServices.KEY_ID_SSL);
        Assert.assertEquals(SignerServices.KEY_ID_SSL, privateKeyPanel.getSelectedItem());

        privateKeyPanel.setSelectedItem(new Goid(0,2000L).toString()+":"+"alias1");
        Assert.assertEquals(new Goid(0,2000L).toString()+":"+"alias1", privateKeyPanel.getSelectedItem());

        privateKeyPanel.setSelectedItem(new Goid(0,2000L).toString()+":"+"alias2");
        Assert.assertEquals(new Goid(0,2000L).toString()+":"+"alias2", privateKeyPanel.getSelectedItem());
    }

    private List<SecurePassword> createSecurePasswordList() {
        // Add 2 passwords and 2 PEM private keys.
        List<SecurePassword> passwords = new ArrayList<>(4);

        SecurePassword password = new SecurePassword();
        password.setGoid(new Goid(0,1000L));
        password.setName("pass1");
        password.setType(SecurePassword.SecurePasswordType.PASSWORD);
        password.setEncodedPassword("");
        passwords.add(password);

        password = new SecurePassword();
        password.setGoid(new Goid(0,1001L));
        password.setName("pass2");
        password.setType(SecurePassword.SecurePasswordType.PASSWORD);
        password.setEncodedPassword("");
        passwords.add(password);

        password = new SecurePassword();
        password.setGoid(new Goid(0,1002L));
        password.setName("pem1");
        password.setType(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
        password.setEncodedPassword("");
        passwords.add(password);

        password = new SecurePassword();
        password.setGoid(new Goid(0,1003L));
        password.setName("pem2");
        password.setType(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
        password.setEncodedPassword("");
        passwords.add(password);

        return passwords;
    }

    private List<KeystoreFileEntityHeader> createKeystoresList() {
        List<KeystoreFileEntityHeader> keystores = new ArrayList<>();
        KeystoreFileEntityHeader keystore = new KeystoreFileEntityHeader(
            new Goid(0,2000L),
            "name",
            "type",
            false);
        keystores.add(keystore);

        return keystores;
    }

    private List<SsgKeyEntry> createKeys() throws Exception {
        List<SsgKeyEntry> keys = new ArrayList<>();

        SsgKeyEntry key = new SsgKeyEntry(
            new Goid(0,2000L),
            "alias1",
            new X509Certificate[]{new TestCertificateGenerator().subject("CN=test1").generate()},
            new TestCertificateGenerator().getPrivateKey());
        keys.add(key);

        key = new SsgKeyEntry(
            new Goid(0,2000L),
            "alias2",
            new X509Certificate[]{new TestCertificateGenerator().subject("CN=test2").generate()},
            new TestCertificateGenerator().getPrivateKey());
        keys.add(key);

        return keys;
    }
}