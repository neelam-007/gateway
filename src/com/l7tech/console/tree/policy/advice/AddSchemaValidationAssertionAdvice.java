package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.SchemaValidationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;

import java.util.logging.Logger;

/**
 * Invoked when a schema validation assertion is dropped in the policy tree.
 * Prevents the instertion of a schema validation assertion with no schema defined.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 9, 2004<br/>
 * $Id$<br/>
 *
 */
public class AddSchemaValidationAssertionAdvice implements Advice {
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SchemaValidation)) {
            throw new IllegalArgumentException();
        }
        SchemaValidation assertion = (SchemaValidation)assertions[0];
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        //SchemaValidationTreeNode fakenode = new SchemaValidationTreeNode(assertion);
        SchemaValidationPropertiesDialog dlg = new SchemaValidationPropertiesDialog(mw, assertion, pc.getService());
        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        // make sure a schema was entered
        if (assertion.getSchema() != null && assertion.getSchema().length() > 0) {
            pc.proceed();
        } else {
            log.info("schema validation must have been canceled " + assertion.getSchema());
        }
    }

    private final Logger log = Logger.getLogger(getClass().getName());
}
