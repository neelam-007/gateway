package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.XslTransformationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xml.XslTransformation;

import javax.swing.*;
import java.util.logging.Logger;
import java.awt.*;

/**
 * Invoked when a Xsl Transformation assertion is dropped in the policy tree.
 * Prevents the insertion of a Xsl Transformation assertion with no xsl defined.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 11, 2004<br/>
 * $Id$<br/>
 *
 */
public class AddXslTransformationAssertionAdvice extends AddContextSensitiveAssertionAdvice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof XslTransformation)) {
            throw new IllegalArgumentException();
        }
        super.proceed(pc);
        XslTransformation assertion = (XslTransformation)assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(mw, true, false, assertion);
        // show the dialog
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                // make sure a xslt was entered
                if (dlg.wasOKed()) {
                    pc.proceed();
                } else {
                    log.info("Xsl Transformation must have been canceled");
                }
            }
        });
    }

    protected void notifyPostRouting(PolicyChange pc, Assertion assertion) {
        XslTransformation xslTransformation = (XslTransformation)assertion;
        if (xslTransformation.getTarget() != TargetMessageType.OTHER) xslTransformation.setTarget(TargetMessageType.RESPONSE);
    }

    protected void notifyPreRouting(PolicyChange pc, Assertion assertion) {
        XslTransformation xslTransformation = (XslTransformation)assertion;
        if (xslTransformation.getTarget() != TargetMessageType.OTHER) xslTransformation.setTarget(TargetMessageType.REQUEST);
    }

    private final Logger log = Logger.getLogger(getClass().getName());
}
