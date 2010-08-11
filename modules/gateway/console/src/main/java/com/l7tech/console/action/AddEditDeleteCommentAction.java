/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.AssertionCommentDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.util.Map;
import java.util.logging.Level;

/**
 * Add, edit or delete a comment on a assertion
 */
public class AddEditDeleteCommentAction extends SecureAction{

    private final AssertionTreeNode node;
    private final boolean isDelete;

    public AddEditDeleteCommentAction(AssertionTreeNode node) {
        super(null, (String)null, true);
        this.node = node;
        this.isDelete = false;
        setActionValues();
    }

    public AddEditDeleteCommentAction(AssertionTreeNode node, boolean delete) {
        super(null, (String)null, true);
        this.node = node;
        isDelete = delete;
        setActionValues();
    }

    @Override
    public String getName() {

        Assertion assertion = node.asAssertion();
        Assertion.Comment comment = assertion.getAssertionComment();

        if(comment != null && !isDelete){
            return "Edit Comment";
        } else if(comment != null){
            return "Delete Comment";            
        } else {
            return "Add Comment";
        }
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/About16.gif";
    }

    @Override
    protected void performAction() {
        final Assertion assertion = node.asAssertion();

        if (isDelete) {
            final Assertion.Comment comment = assertion.getAssertionComment();
            boolean hasBoth = false;
            if(comment != null){
                //should never be null
                final Map<String,String> props = comment.getProperties();
                if(props.containsKey(Assertion.Comment.LEFT_COMMENT) && props.containsKey(Assertion.Comment.RIGHT_COMMENT)){
                    hasBoth = true;
                }
            }
            DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                    (hasBoth) ? "Delete Both Comments?" : "Delete Comment?",
                    "Confirm Delete",
                    JOptionPane.OK_CANCEL_OPTION,
                    new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if (option == JOptionPane.YES_OPTION) {
                        assertion.setAssertionComment(null);
                        updateTreeNode();
                    }
                }
            });
            return;
        }
        
        final Assertion.Comment comment = (assertion.getAssertionComment() != null) ? assertion.getAssertionComment() : new Assertion.Comment();

        final AssertionCommentDialog commentDlg = new AssertionCommentDialog(comment);
        commentDlg.pack();
        Utilities.centerOnScreen(commentDlg);

        DialogDisplayer.display(commentDlg, new Runnable() {
            @Override
            public void run() {
                if(commentDlg.isConfirmed()) {
                    if (!comment.hasComment()) {
                        assertion.setAssertionComment(null);
                    } else {
                        assertion.setAssertionComment(comment);
                    }
                    updateTreeNode();
                }
            }
        });
    }

    private void updateTreeNode() {
        //Update the node in the tree whose display text may have changed following the addition / modification of a comment
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged(node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
