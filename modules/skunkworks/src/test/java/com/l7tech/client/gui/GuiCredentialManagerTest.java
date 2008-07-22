package com.l7tech.client.gui;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.proxy.datamodel.CredentialManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.SsgManagerStub;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.test.util.GuiTestMethod;
import com.l7tech.test.util.InteractiveGuiTester;

import javax.swing.*;
import java.net.PasswordAuthentication;

/**
 * Standalone test for GuiCredentialManager
 */
public class GuiCredentialManagerTest {
    private static SsgManager ssgManager = new SsgManagerStub();
    private Ssg ssg = new Ssg();
    private GuiCredentialManager gcm = GuiCredentialManager.createGuiCredentialManager(ssgManager);

    public static void main(String[] args) throws Exception {
        Gui.GuiParams gp = new Gui.GuiParams(ssgManager, 7700);
        Gui.setInstance(Gui.createGui(gp));
        InteractiveGuiTester.startTest(new GuiCredentialManagerTest());
    }

    public GuiCredentialManagerTest() {
    }

    @GuiTestMethod
    public void testGetAuxiliaryCredentials(JFrame frame) throws OperationCanceledException {
        PasswordAuthentication pw = gcm.getAuxiliaryCredentials(ssg, SecurityTokenType.SAML_ASSERTION, "sts.example.com", CredentialManager.ReasonHint.TOKEN_SERVICE, false);
        JOptionPane.showMessageDialog(frame, "Got pw: " + pw.getUserName() + ":" + new String(pw.getPassword()));
    }
}
