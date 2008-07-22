package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.XpathCredentialSourcePropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;

import java.awt.*;

/**
 * Invoked when a Xpath Credential Source Assertion is dropped to a policy tree to
 * initiate the properties dialog.
 * <p/>
 */
public class AddXpathCredentialSourceAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof XpathCredentialSource)) {
            throw new IllegalArgumentException();
        }
        XpathCredentialSource assertion = (XpathCredentialSource)assertions[0];
        Frame f = TopComponents.getInstance().getTopParent();

        final XpathCredentialSourcePropertiesDialog dlg = new XpathCredentialSourcePropertiesDialog(assertion, f, true, false);
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
