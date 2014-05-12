package com.l7tech.console.action;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;
import com.l7tech.policy.assertion.ext.cei.UsesConsoleContext;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.console.api.CustomConsoleContext.*;

/**
 * Custom action for Custom Assertions. This action is displayed in the Task->Additional Actions menu
 * in the Policy Manager UI.
 */
public class CustomAssertionHolderAction extends BaseAction {

    private final CustomTaskActionUI customTaskActionUi;

    public CustomAssertionHolderAction(CustomTaskActionUI customTaskActionUi) {

        // ***IMPORTANT***
        // This class extends BaseAction class. If you want to extend SecureAction class instead,
        // the super constructor should be called like this:
        //
        // super(
        //    new AttemptedAnyOperation(EntityType.GENERIC), // or any other entity type.
        //    customAction.getName(),
        //    customAction.getDescription(),
        //    customAction.getIcon() != null ? customAction.getIcon().getImage() : null);
        //

        super(
            customTaskActionUi.getName(),
            customTaskActionUi.getDescription(),
            customTaskActionUi.getIcon() != null ? customTaskActionUi.getIcon().getImage() : null);

        this.customTaskActionUi = customTaskActionUi;
    }

    @Override
    public String getName() {
        // ***IMPORTANT***
        // This method is never called. This is because the constructor of this class
        // calls super class constructor with name, description, and resources passed it.
        // Consider throwing an exception instead. throw new RuntimeException("This method should not be called.");
        //
        return customTaskActionUi.getName();
    }

    @Override
    public String getDescription() {
        // ***IMPORTANT***
        // This method is never called. This is because the constructor of this class
        // calls super class constructor with name, description, and resources passed it.
        // Consider throwing an exception instead. throw new RuntimeException("This method should not be called.");
        //
        return customTaskActionUi.getDescription();
    }

    @Override
    protected String iconResource() {
        // ***IMPORTANT***
        // This method is never called. This is because the constructor of this class
        // calls super class constructor with name, description, and resources passed it.
        // Consider throwing an exception instead. throw new RuntimeException("This method should not be called.");
        //
        return customTaskActionUi.getIcon() != null ? customTaskActionUi.getIcon().toString() : null;
    }

    @Override
    protected void performAction() {
        if (customTaskActionUi instanceof UsesConsoleContext) {
            Map<String, Object> consoleContext = new HashMap<>(4);
            addCustomExtensionInterfaceFinder(consoleContext);
            addCommonUIServices(consoleContext, null, null);
            addKeyValueStoreServices(consoleContext);
            addVariableServices(consoleContext, null, null);
            ((UsesConsoleContext) customTaskActionUi).setConsoleContextUsed(consoleContext);
        }

        final JDialog dialog = customTaskActionUi.getDialog(TopComponents.getInstance().getTopParent());
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);
        DialogDisplayer.display(dialog);
    }
}