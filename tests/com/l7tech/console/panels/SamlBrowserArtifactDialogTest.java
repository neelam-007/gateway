/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import com.l7tech.policy.assertion.xmlsec.AuthenticationProperties;

import javax.swing.*;
import java.util.HashMap;

/**
 * @author emil
 * @version Mar 22, 2005
 */
public class SamlBrowserArtifactDialogTest {
    public static void main(String[] args)
      throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SamlBrowserArtifact sba = new SamlBrowserArtifact();
        AuthenticationProperties dap = sba.getAuthenticationProperties();
        sba.setSsoEndpointUrl("http://foo");
        //sba.setLoginUrl("http://bar");
        dap.setMethod(AuthenticationProperties.METHOD_FORM);
        HashMap fields = new HashMap();
        fields.put("foo", "bar");
        fields.put("baz", "quux");
        dap.setAdditionalFields(fields);
        sba.setAuthenticationProperties(dap);

        SamlBrowserArtifactPropertiesDialog dlg = new SamlBrowserArtifactPropertiesDialog(sba, null, false);
        dlg.pack();
        dlg.setVisible(true);
    }
}