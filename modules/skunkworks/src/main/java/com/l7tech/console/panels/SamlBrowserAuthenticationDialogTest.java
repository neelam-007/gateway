package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.AuthenticationProperties;

import javax.swing.*;
import java.util.HashMap;

/**
 * User: steve
 * Date: Sep 20, 2005
 * Time: 6:24:46 PM
 * $Id$
 */
public class SamlBrowserAuthenticationDialogTest {

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        AuthenticationProperties ap = new AuthenticationProperties();

        ap.setCopyFormFields(true);
        ap.setUsernameFieldname("user");
        ap.setPasswordFieldname("pass");

        HashMap fields = new HashMap();
        fields.put("foo", "bar");
        fields.put("baz", "quux");
        ap.setAdditionalFields(fields);

        SamlBrowserAuthenticationDialog dlg = new SamlBrowserAuthenticationDialog(ap, null, false);
        dlg.pack();
        dlg.setVisible(true);
    }
}
