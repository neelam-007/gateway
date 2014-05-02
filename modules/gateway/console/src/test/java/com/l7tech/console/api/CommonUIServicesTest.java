package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.commonui.CommonUIServices;
import com.l7tech.policy.assertion.ext.commonui.CustomSecurePasswordPanel;
import com.l7tech.policy.assertion.ext.commonui.CustomTargetVariablePanel;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommonUIServicesTest {

    @Mock
    private Registry registry;

    @Mock
    private TrustedCertAdmin trustedCertAdmin;

    @Mock
    private GuidBasedEntityManager<Policy> policyFinder;

    @Test
    @Ignore("Developer Test")
    public void testCreateTargetVariablePanel() throws Exception {
        Registry.setDefault(registry);

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, new CustomAssertionHolder(), null);
        CommonUIServices commonUIServices = (CommonUIServices) consoleContext.get(CommonUIServices.CONSOLE_CONTEXT_KEY);

        CustomTargetVariablePanel targetVariablePanel = commonUIServices.createTargetVariablePanel();
        Assert.assertNotNull(targetVariablePanel);
        Assert.assertNotNull(targetVariablePanel.getPanel());
    }

    @Test
    @Ignore("Developer Test")
    public void testCreatePasswordComboBoxPanel() throws Exception {
        Registry.setDefault(registry);
        when(registry.getTrustedCertManager()).thenReturn(trustedCertAdmin);
        List<SecurePassword> passwords = this.createSecurePasswordList();
        when(trustedCertAdmin.findAllSecurePasswords()).thenReturn(passwords);

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, new CustomAssertionHolder(), null);
        CommonUIServices commonUIServices = (CommonUIServices) consoleContext.get(CommonUIServices.CONSOLE_CONTEXT_KEY);

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
        Registry.setDefault(registry);
        when(registry.getTrustedCertManager()).thenReturn(trustedCertAdmin);
        List<SecurePassword> passwords = this.createSecurePasswordList();
        when(trustedCertAdmin.findAllSecurePasswords()).thenReturn(passwords);

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, new CustomAssertionHolder(), null);
        CommonUIServices commonUIServices = (CommonUIServices) consoleContext.get(CommonUIServices.CONSOLE_CONTEXT_KEY);

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
    public void testGetVariablesSetByPredecessorsAddAssertion() throws Exception {
        // Test scenario when an assertion is added to policy.
        //
        Registry.setDefault(registry);
        when(registry.getPolicyFinder()).thenReturn(policyFinder);

        // Setup test data.
        //
        SetVariableAssertion previousAssertion = new SetVariableAssertion();
        previousAssertion.setDataType(DataType.STRING);
        previousAssertion.setVariableToSet("test_var_name");
        CustomAssertionHolder cah = new CustomAssertionHolder();

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, cah, previousAssertion);
        CommonUIServices commonUIServices = (CommonUIServices) consoleContext.get(CommonUIServices.CONSOLE_CONTEXT_KEY);

        Map<String, VariableMetadata> vars = commonUIServices.getVariablesSetByPredecessors();
        Assert.assertNotNull(vars);
        Assert.assertEquals(1, vars.size());
        VariableMetadata var = vars.get("test_var_name");
        Assert.assertNotNull(var);
        Assert.assertEquals(DataType.STRING, var.getType());
    }

    @Test
    public void testGetVariablesSetByPredecessorsExistingAssertion() throws Exception {
        // Test scenario when an assertion is already in policy.
        //
        Registry.setDefault(registry);
        when(registry.getPolicyFinder()).thenReturn(policyFinder);

        // Setup test assertion.
        //
        SetVariableAssertion previousAssertion = new SetVariableAssertion();
        previousAssertion.setDataType(DataType.MESSAGE);
        previousAssertion.setVariableToSet("test_var_name");
        CustomAssertionHolder cah = new CustomAssertionHolder();

        List<Assertion> children = new ArrayList<>(3);
        children.add(previousAssertion);
        children.add(cah);

        @SuppressWarnings("UnusedDeclaration")
        AllAssertion allAssertion = new AllAssertion(children);

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, cah, previousAssertion);
        CommonUIServices commonUIServices = (CommonUIServices) consoleContext.get(CommonUIServices.CONSOLE_CONTEXT_KEY);

        Map<String, VariableMetadata> vars = commonUIServices.getVariablesSetByPredecessors();
        Assert.assertNotNull(vars);
        Assert.assertEquals(1, vars.size());
        VariableMetadata var = vars.get("test_var_name");
        Assert.assertNotNull(var);
        Assert.assertEquals(DataType.MESSAGE, var.getType());
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
}