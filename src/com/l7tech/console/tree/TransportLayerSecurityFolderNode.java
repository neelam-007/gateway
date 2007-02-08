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
 * The class represents a node element in the palette assertion tree.
 * It represents the folder with transport layer securitry.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class TransportLayerSecurityFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     *
     */
    public TransportLayerSecurityFolderNode() {
        super("Transport Layer Security (TLS)", "transportLayerSecurity");
    }


    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        //children = null;
        int index = 0;
        insert(new SslTransportNode(false), index++);
        index = insertMatchingModularAssertions(index);
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            Iterator it = cr.getAssertions(Category.TRANSPORT_SEC).iterator();
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
