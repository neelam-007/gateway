package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.panels.PolicyEditorPanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.ComponentRegistry;

import javax.swing.*;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyValidatorAdvice implements Advice {
    /**
     * Intercepts a policy change.
     * 
     * @param pc The policy change.
     */
    public void proceed(PolicyChange pc) throws PolicyException {
        pc.proceed();
        final ComponentRegistry creg = ComponentRegistry.getInstance();
        final WorkSpacePanel cws = creg.getCurrentWorkspace();
        JComponent jc = cws.getComponent();
        if (jc == null || !(jc instanceof PolicyEditorPanel)) {
            return;
        }
        PolicyEditorPanel pe = (PolicyEditorPanel)jc;
        pe.validatePolicy();
    }
}
