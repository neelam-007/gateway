package com.l7tech.external.assertions.jsonschema;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.jsonschema.console.JSONSchemaPropertiesDialog;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.json.JsonSchemaVersion;
import com.l7tech.policy.assertion.Assertion;

import java.awt.*;
import java.util.Arrays;

/**
 * <p>Advice class that gets triggered when a {@link JSONSchemaAssertion} gets dragged from the policy palette into
 * a service policy window.</p>
 *
 * <p>Its purpose is to default assertions newly dragged into the service policy window to DRAFT_V4
 * JSON Schema version</p>
 *
 */
public class JSONSchemaAssertionAdvice implements Advice {

    public static final String EXCEPTION_MESSAGE = "Expected one " + JSONSchemaAssertion.class.getSimpleName()
            + " but received: ";

    @Override
    public void proceed(final PolicyChange pc) {
        if (pc == null) {
            return;
        }

        final JSONSchemaAssertion jsonSchemaAssertion = extractAssertion(pc);
        jsonSchemaAssertion.setJsonSchemaVersion(JsonSchemaVersion.DRAFT_V4);

        final Frame mw = TopComponents.getInstance().getTopParent();
        final JSONSchemaPropertiesDialog dlg = new JSONSchemaPropertiesDialog(mw, jsonSchemaAssertion);

        // show the dialog
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, () -> {
            if (dlg.isConfirmed()) {
                dlg.getData(jsonSchemaAssertion);
                pc.proceed();
            }
        });
    }

    private JSONSchemaAssertion extractAssertion(final PolicyChange pc) {
        final Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof JSONSchemaAssertion)) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE + Arrays.toString(assertions));
        }

        return (JSONSchemaAssertion) assertions[0];
    }

}
