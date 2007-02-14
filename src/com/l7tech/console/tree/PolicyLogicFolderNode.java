package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.console.util.Registry;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.LicenseException;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Iterator;
import java.rmi.RemoteException;


/**
 * The class represents a node element in the palette assertion tree.
 * It represents the folder with composite and logical policy assertions.
 */
public class PolicyLogicFolderNode extends AbstractPaletteFolderNode {
    static final Logger log = Logger.getLogger(PolicyLogicFolderNode.class.getName());

    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     */
    public PolicyLogicFolderNode() {
        super("Policy Logic", "policyLogic");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert(new CommentAssertionPaletteNode(), index++);
        insert(new OneOrMoreNode(), index++);
        insert(new AllNode(), index++);
        insert(new TrueAssertionPaletteNode(), index++);
        insert(new FalseAssertionPaletteNode(), index++);
        insert(new SetVariableAssertionPaletteNode(), index++);
        index = insertMatchingModularAssertions(index);
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            Iterator it = cr.getAssertions(Category.LOGIC).iterator();
            while (it.hasNext()) {
                CustomAssertionHolder a = (CustomAssertionHolder)it.next();
                insert(new CustomAccessControlNode(a), index++);
            }
        } catch (RemoteException e1) {
            if (ExceptionUtils.causedBy(e1, LicenseException.class)) {
                logger.log(Level.INFO, "Custom assertions unavailable or unlicensed");
            } else
                logger.log(Level.WARNING, "Unable to retrieve custom assertions", e1);
        }
    }
}
