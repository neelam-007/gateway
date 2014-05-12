package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.console.beaneditor.BeanEditor;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.AssertionEditor;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.assertion.ext.EditListener;
import com.l7tech.policy.assertion.ext.cei.UsesConsoleContext;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.console.api.CustomConsoleContext.*;

/**
 * Invoked when a Custom Assertion is dropped to a policy tree to
 * display the properties dialog.
 */
public class CustomAssertionHolderAdvice implements Advice {
    protected static final Logger logger = Logger.getLogger(CustomAssertionHolderAdvice.class.getName());

    private PolicyChange pc;
    private CustomAssertionHolder cah;

    @Override
    public void proceed(final PolicyChange pc) {

        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof CustomAssertionHolder)) {
            throw new IllegalArgumentException();
        }

        this.pc = pc;
        this.cah = (CustomAssertionHolder) assertions[0];

        // Display custom assertion dialog.
        //
        AssertionEditor editor = this.getCustomEditor();
        if (editor != null) {
            editor.addEditListener(new EditListener() {
                public void onEditAccepted(Object source, Object bean) {
                    assertionChanged();
                }

                public void onEditCancelled(Object source, Object bean) {}
            });
            if (editor instanceof Window) {
                Utilities.centerOnScreen((Window)editor);
            }
            editor.edit();
        } else {
            performGenericEditorAction();
        }
    }

    private void performGenericEditorAction() {
        final JDialog dialog = new JDialog(TopComponents.getInstance().getTopParent(), true);
        final CustomAssertion ca = cah.getCustomAssertion();
        BeanEditor.Options options = new BeanEditor.Options();
        options.setDescription(ca.getName());
        options.setExcludeProperties(new String[]{"name", "variablesUsed", "variablesSet"});
        BeanEditor be = new BeanEditor(dialog, ca, Object.class, options);
        be.addBeanListener(new BeanAdapter() {
            public void onEditAccepted(Object source, Object bean) {
                dialog.dispose();
                assertionChanged();
            }
        });
        dialog.setTitle(ca.getName());
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.suppressSheetDisplay(dialog);
        DialogDisplayer.display(dialog);
    }

    private AssertionEditor getCustomEditor() {
        CustomAssertionsRegistrar registrar = Registry.getDefault().getCustomAssertionsRegistrar();
        CustomAssertionUI customAssertionUI = registrar.getUI(cah.getCustomAssertion().getClass().getName());

        AssertionEditor editor = null;
        if (customAssertionUI != null) {
            if (customAssertionUI instanceof UsesConsoleContext) {
                Map<String, Object> consoleContext = new HashMap<>(4);
                addCustomExtensionInterfaceFinder(consoleContext);
                Assertion previousAssertion = pc.getChildLocation() == 0 ? pc.getParent().asAssertion() : ((AssertionTreeNode) (pc.getParent().getChildAt(pc.getChildLocation()-1))).asAssertion();
                addCommonUIServices(consoleContext, cah, previousAssertion);
                addKeyValueStoreServices(consoleContext);
                addVariableServices(consoleContext, cah, previousAssertion);
                ((UsesConsoleContext) customAssertionUI).setConsoleContextUsed(consoleContext);
            }
            editor = customAssertionUI.getEditor(cah.getCustomAssertion());
        }

        return editor;
    }

    private void assertionChanged() {
        pc.proceed();
    }
}