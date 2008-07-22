package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.SamlBrowserArtifactPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;

import java.awt.*;

/**
 * Invoked when a SAML Browser Artifact Assertion is dropped to a policy tree to
 * initiate the properties dialog.
 * <p/>
 */
public class AddSamlBrowserArtifactAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SamlBrowserArtifact)) {
            throw new IllegalArgumentException();
        }
        SamlBrowserArtifact assertion = (SamlBrowserArtifact) assertions[0];
        Frame f = TopComponents.getInstance().getTopParent();

        final SamlBrowserArtifactPropertiesDialog dlg = new SamlBrowserArtifactPropertiesDialog(assertion, f, true, false);
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // check that user oked this dialog
                if (dlg.isAssertionChanged()) {
                    pc.proceed();
                }
            }
        });
    }
}
