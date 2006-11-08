package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.XslTransformationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
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
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof XslTransformation)) {
            throw new IllegalArgumentException();
        }
        super.proceed(pc);
        XslTransformation assertion = (XslTransformation)assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(mw, true, assertion);
        // show the dialog
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        // make sure a xslt was entered
        if (dlg.wasOKed()) {
            pc.proceed();
        } else {
            log.info("Xsl Transformation must have been canceled");
        }
    }

    protected void notifyPostRouting(PolicyChange pc, Assertion assertion) throws PolicyException {
        XslTransformation xslTransformation = (XslTransformation)assertion;
        xslTransformation.setDirection(XslTransformation.APPLY_TO_RESPONSE);
    }

    protected void notifyPreRouting(PolicyChange pc, Assertion assertion) throws PolicyException {
        XslTransformation xslTransformation = (XslTransformation)assertion;
        xslTransformation.setDirection(XslTransformation.APPLY_TO_REQUEST);
    }

    private final Logger log = Logger.getLogger(getClass().getName());
}
