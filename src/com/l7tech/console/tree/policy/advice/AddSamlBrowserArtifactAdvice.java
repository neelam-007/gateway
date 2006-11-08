package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.SamlBrowserArtifactPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;

import javax.swing.*;
import java.awt.*;

/**
 * Invoked when a SAML Browser Artifact Assertion is dropped to a policy tree to
 * initiate the properties dialog.
 * <p/>
 */
public class AddSamlBrowserArtifactAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SamlBrowserArtifact)) {
            throw new IllegalArgumentException();
        }
        SamlBrowserArtifact assertion = (SamlBrowserArtifact) assertions[0];
        Frame f = TopComponents.getInstance().getTopParent();

        SamlBrowserArtifactPropertiesDialog dlg = new SamlBrowserArtifactPropertiesDialog(assertion, f, true);
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
//        dlg.setVisible(true);
        dlg.setVisible(true);
        // check that user oked this dialog
        if (dlg.isAssertionChanged()) {
            pc.proceed();
        }
    }
}
