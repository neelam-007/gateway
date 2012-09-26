package com.l7tech.external.assertions.adaptiveloadbalancing.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.variable.VariableMetadata;
import org.apache.commons.lang.StringUtils;

public class TargetVariablePanelValidationRule extends InputValidator.ComponentValidationRule {
    private final TargetVariablePanel component;
    private final String componentName;

    public TargetVariablePanelValidationRule(TargetVariablePanel component, String componentName) {
        super(component);
        this.component = component;
        this.componentName = componentName;
    }

    @Override
    public String getValidationError() {
        if (StringUtils.isBlank(component.getVariable())) {
            return componentName + " must not be empty!";
        }
        else if(!VariableMetadata.isNameValid(component.getVariable())) {
            return componentName + " must have valid name";
        }
        return null;
    }
}
