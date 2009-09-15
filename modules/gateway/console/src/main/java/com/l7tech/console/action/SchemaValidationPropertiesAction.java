package com.l7tech.console.action;

import com.l7tech.console.panels.SchemaValidationPropertiesDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a Schema Validation Assertion node.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 * $Id$<br/>
 */
public class SchemaValidationPropertiesAction extends NodeActionWithMetaSupport {

    public SchemaValidationPropertiesAction(AssertionTreeNode node) {
        super(null, SchemaValidation.class, node.asAssertion());
        this.node = node;
    }

    @Override
    protected void performAction() {
        try {
            //test here before trying to create the dialog, which will fail if not found
            node.getService();
        } catch (FindException e) {
            log.log(Level.WARNING, "cannot get service", e);
            //minic the old behaviour in it's TreeNode, no action if service not found
            return;
        }

        SchemaValidation me = (SchemaValidation) node.asAssertion();
        TargetMessageType inferredTarget = null;
        if (me.getTarget() == null) {
            // Backward compatibility--if no target is set, figure out how to configure the UI based on whether the first
            // routing assertion is before us.
            final TreeNode[] pathNodes = node.getPath();
            final AssertionTreeNode rootnode = (AssertionTreeNode)pathNodes[0];
            final Assertion rootass = rootnode.asAssertion();

            int firstRoutingPos = -1, myPos = -1, i = 0;
            for (Iterator iter = rootass.preorderIterator(); iter.hasNext(); i++) {
                Assertion ass = (Assertion)iter.next();
                if (firstRoutingPos == -1 && ass instanceof RoutingAssertion) {
                    firstRoutingPos = i;
                } else if (ass == me) {
                    myPos = i;
                }
            }

            if (myPos == -1) throw new IllegalStateException("Couldn't find SchemaValidation assertion");

            inferredTarget = firstRoutingPos == -1 || myPos <= firstRoutingPos ? TargetMessageType.REQUEST : TargetMessageType.RESPONSE;
        }

        final SchemaValidationPropertiesDialog dlg = new SchemaValidationPropertiesDialog(TopComponents.getInstance().getTopParent(), node, inferredTarget);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (!dlg.isChangesCommitted())
                    return;

                final JTree tree = TopComponents.getInstance().getPolicyTree();
                if (tree != null) {
                    PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                    model.assertionTreeNodeChanged(node);
                    log.finest("model invalidated");
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        });
    }

    private final Logger log = Logger.getLogger(getClass().getName());
    private AssertionTreeNode node;
}
