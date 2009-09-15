package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.SchemaValidationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xml.SchemaValidation;

import java.util.logging.Logger;
import java.awt.*;

/**
 * Invoked when a schema validation assertion is dropped in the policy tree.
 * Prevents the instertion of a schema validation assertion with no schema defined.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 * $Id$<br/>
 *
 */
public class AddSchemaValidationAssertionAdvice extends AddContextSensitiveAssertionAdvice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SchemaValidation)) {
            throw new IllegalArgumentException();
        }
        super.proceed(pc);
        
        SchemaValidation assertion = (SchemaValidation)assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final SchemaValidationPropertiesDialog dlg = new SchemaValidationPropertiesDialog(mw, assertion, pc.getService());
        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                // make sure a schema was entered
                if (dlg.isChangesCommitted()) {
                    pc.proceed();
                } else {
                    log.info("Addition of SchemaValidation must have been canceled");
                }
            }
        });
    }

    @Override
    protected void notifyPreRouting(PolicyChange pc, Assertion assertion) {
        if (!(assertion instanceof SchemaValidation)) throw new IllegalStateException();
        SchemaValidation sv = (SchemaValidation)assertion;
        sv.setTarget(TargetMessageType.REQUEST);
    }

    @Override
    protected void notifyPostRouting(PolicyChange pc, Assertion assertion) {
        if (!(assertion instanceof SchemaValidation)) throw new IllegalStateException();
        SchemaValidation sv = (SchemaValidation)assertion;
        sv.setTarget(TargetMessageType.RESPONSE);
    }

    private final Logger log = Logger.getLogger(getClass().getName());
}
