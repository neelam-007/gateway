package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.console.util.Registry;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.LicenseException;

import java.util.Iterator;
import java.util.logging.Level;
import java.rmi.RemoteException;

/**
 * The class represents an gui node element in the TreeModel that
 * represents the audit assertions folder.
 */
public class AuditFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public AuditFolderNode() {
        super("Logging, Auditing and Alerts");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert( new AuditAssertionPaletteNode(), index++ );
        insert( new AuditDetailAssertionPaletteNode(), index++ );
        insert( new SnmpTrapAssertionPaletteNode(), index++ );
        insert( new EmailAlertAssertionPaletteNode(), index++ );
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            Iterator it = cr.getAssertions(Category.AUDIT_ALERT).iterator();
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
