package com.l7tech.console.action;

import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default properties action for assertions that do not have custom properties actions, but which do provide
 * an AssertionPropertiesEditor subclass just for them.
 */
public class DefaultAssertionPropertiesAction
        <AT extends Assertion,
         NT extends AssertionTreeNode<AT>>
    extends SecureAction
{
    protected static final Logger logger = Logger.getLogger(DefaultAssertionPropertiesAction.class.getName());

    private final NT subject;
    private final Functions.Binary< AssertionPropertiesEditor<AT>, Frame, AT> apeFactory;


    /**
     * Create a DefaultAssertionPropertiesAction using the specified subject policy tree node and using
     * new instances of the specified AssertionPropertiesEditor concrete class to edit the assertion properties.
     *
     * @param subject  the AssertionTreeNode whose properties are to be edited.  Must not be null.
     *                 Must return a valid, non-null value from subject.asAssertion().
     * @param apeFactory the AssertionPropertiesEditor factory to use when the action is fired.  Must not be null.
     */
    public DefaultAssertionPropertiesAction(NT subject, Functions.Binary< AssertionPropertiesEditor<AT>, Frame, AT> apeFactory) {
        super(null, subject.asAssertion().getClass());
        this.subject = subject;
        this.apeFactory = apeFactory;
        setActionValues(); // reset values
    }

    @Override
    public String getName() {
        if (subject == null) return "Properties";
        return (String)subject.asAssertion().meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME);
    }

    @Override
    public String getDescription() {
        if (subject == null) return "";
        return (String)subject.asAssertion().meta().get(AssertionMetadata.PROPERTIES_ACTION_DESC);
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        try {
            final AT ass = subject.asAssertion();
            final AssertionPropertiesEditor<AT> ape = apeFactory.call(TopComponents.getInstance().getTopParent(), ass);
            ape.setParameter( AssertionPropertiesEditor.PARAM_READONLY, !subject.canEdit() );
            //todo calling setData again here means assertions may initialize themselves a second time as its likely apeFactory.call caused a call to setData
            ape.setData(ass);
            final JDialog dlg = ape.getDialog();
            if (Boolean.TRUE.equals(ass.meta().get(AssertionMetadata.PROPERTIES_EDITOR_SUPPRESS_SHEET_DISPLAY)))
                DialogDisplayer.suppressSheetDisplay(dlg);
            dlg.pack();
            Utilities.centerOnParentWindow(dlg);
            Frame f = TopComponents.getInstance().getTopParent();
            DialogDisplayer.display(dlg, f, new Runnable() {
                @Override
                public void run() {
                    if (ape.isConfirmed()) {
                        AT updatedAssertion = ape.getData(ass);
                        subject.setUserObject(updatedAssertion);
                        PolicyTree tree = (PolicyTree) TopComponents.getInstance().getPolicyTree();
                        if (tree != null) {
                            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                            model.assertionTreeNodeChanged(subject);
                            if (updatedAssertion != ass) {
                                // a new assertion instance was returned, we must update our parent's assertion references
                                TreeNode parent = subject.getParent();
                                if (parent instanceof CompositeAssertionTreeNode) {
                                    CompositeAssertionTreeNode compNode = (CompositeAssertionTreeNode) parent;
                                    int index = compNode.getIndex(subject);
                                    PolicyTreeModel policyTreeModel = (PolicyTreeModel) tree.getModel();
                                    tree.treeNodesInserted(new PolicyTreeModelEvent(this,
                                            policyTreeModel.getPathToRoot(parent), new int[]{index}, new Object[]{subject}));
                                }
                            }
                        } else {
                            log.log(Level.WARNING, "Unable to reach the policy tree.");
                        }
                    }
                }
            });
        } catch (RuntimeException re) {
            final PermissionDeniedException pde = ExceptionUtils.getCauseIfCausedBy(re, PermissionDeniedException.class);
            if (pde != null) {
                PermissionDeniedErrorHandler.showMessageDialog(pde, log);
            } else {
                throw re;
            }
        }
    }

}
