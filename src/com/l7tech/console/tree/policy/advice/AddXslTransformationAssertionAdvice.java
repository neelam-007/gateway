package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.XslTransformationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xml.XslTransformation;

import java.util.logging.Logger;
import java.util.Iterator;
import javax.swing.*;

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
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.setSize(600, 800);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        // make sure a xslt was entered
        Assertion ass = dlg.getAssertion();
        if (ass != null) {
            pc.proceed();
        } else {
            log.info("Xsl Transformation must have been canceled " + assertion.getXslSrc());
        }
    }

    private boolean isInsertPostRouting(PolicyChange pc) {
        Assertion ass = pc.getParent().asAssertion();
        if (ass instanceof AllAssertion) {
            AllAssertion parent = (AllAssertion)ass;
            Iterator i = parent.children();
            int pos = 0;
            while (i.hasNext()) {
                Assertion child = (Assertion)i.next();
                if (pos < pc.getChildLocation()) {
                    if (child instanceof RoutingAssertion) {
                        return true;
                    }
                }
                pos++;
            }
        }
        Assertion previous = ass;
        ass = ass.getParent();
        while (ass != null) {
            if (ass instanceof AllAssertion) {
                AllAssertion parent = (AllAssertion)ass;
                Iterator i = parent.children();
                while (i.hasNext()) {
                    Assertion child = (Assertion)i.next();
                    System.out.println(child.getClass().getName());
                    if (child instanceof RoutingAssertion) {
                        return true;
                    }
                    if (child == previous) break;
                }
            }
            previous = ass;
            ass = ass.getParent();
        }
        return false;
    }

    private final Logger log = Logger.getLogger(getClass().getName());
}
