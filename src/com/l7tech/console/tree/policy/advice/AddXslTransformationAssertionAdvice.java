package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.XslTransformationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.XslTransformation;

import java.util.logging.Logger;

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
public class AddXslTransformationAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof XslTransformation)) {
            throw new IllegalArgumentException();
        }
        XslTransformation assertion = (XslTransformation)assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(mw, true, assertion);
        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        // make sure a xslt was entered
        if (assertion.getXslSrc() != null && assertion.getXslSrc().length() > 0) {
            pc.proceed();
        } else {
            log.info("Xsl Transformation must have been canceled " + assertion.getXslSrc());
        }
    }

    private final Logger log = Logger.getLogger(getClass().getName());
}
