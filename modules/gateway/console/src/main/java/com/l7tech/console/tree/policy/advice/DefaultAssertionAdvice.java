package com.l7tech.console.tree.policy.advice;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Default advice for any assertion.  This displays the properties dialog, if there is one.
 */
public class DefaultAssertionAdvice<AT extends Assertion> implements Advice {
    protected static final Logger logger = Logger.getLogger(DefaultAssertionAdvice.class.getName());

    @Override
    public void proceed(final PolicyChange pc) {
        //noinspection unchecked
        final AssertionTreeNode<AT> subject = pc.getNewChild();
        final AT ass = subject.asAssertion();
        final AssertionMetadata meta = ass.meta();

        //noinspection unchecked
        final Functions.Binary<AssertionPropertiesEditor< AT >, Frame, AT > apeFactory =
                (Functions.Binary<AssertionPropertiesEditor< AT >, Frame, AT>)
                        meta.get(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);

        if (apeFactory == null) {
            // No properties editor for this assertion, so no default advice
            pc.proceed();
            return;
        }

        final AssertionPropertiesEditor<AT> ape = apeFactory.call(TopComponents.getInstance().getTopParent(), ass);
        if ( ape instanceof AssertionPropertiesEditorSupport ) {
            ((AssertionPropertiesEditorSupport)ape).setPolicyPosition( pc.getParent().asAssertion(), pc.getChildLocation() );            
        }
        ape.setData(ass);
        final JDialog dlg = ape.getDialog();
        if (Boolean.TRUE.equals(ass.meta().get(AssertionMetadata.PROPERTIES_EDITOR_SUPPRESS_SHEET_DISPLAY)))
            DialogDisplayer.suppressSheetDisplay(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        Frame f = TopComponents.getInstance().getTopParent();
        DialogDisplayer.display(dlg, f, new Runnable() {
            @Override
            public void run() {
                if (ape.isConfirmed()) {
                    subject.setUserObject(ape.getData(ass));
                    pc.proceed();
                }
            }
        });
    }
}
