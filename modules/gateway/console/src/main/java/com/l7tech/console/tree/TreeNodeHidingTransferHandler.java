/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree;

import com.l7tech.console.policy.PolicyTransferable;

import javax.swing.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;

/**
 * A TransferHandler wrapper for the policy trees (palette tree and policy tree) to allow copy and drag out (and,
 * optionally, cut, paste, and drag-in) of policy assertion tree nodes, while hiding the assertion tree node
 * data flavors from the system clipboard (since they aren't serializable).
 */
public class TreeNodeHidingTransferHandler extends TransferHandler {
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        int clipboardAction = getSourceActions(comp) & action;
        if (clipboardAction != NONE) {
            Transferable t = createTransferable(comp);
            if (t != null) {
                try {
                    if (!((comp instanceof  AssertionsTree) && (((AssertionsTree)comp).getLastSelectedPathComponent() instanceof PolicyTemplateNode)) && t instanceof PolicyTransferable) {
                    //if (t instanceof PolicyTransferable) {
                        // Strip the tree nodes, since they aren't all serializable and the attempt
                        // to put them onto the system clipboard will fail.
                        t = ((PolicyTransferable)t).asClipboardSafe();
                    }
                    clip.setContents(t, null);
                    exportDone(comp, t, clipboardAction);
                    return;
                } catch (IllegalStateException ise) {
                    exportDone(comp, t, NONE);
                    throw ise;
                }
            }
        }

        exportDone(comp, null, NONE);
    }
}
