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
 * It represents the folder with XML security elements.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class XmlSecurityFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     *
     */
    public XmlSecurityFolderNode() {
        super("XML Security", "xmlSecurity");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        insert(new RequestWssIntegrityNode(), index++);
        insert(new RequestWssConfidentialityNode(), index++);
        insert(new ResponseWssIntegrityNode(), index++);
        insert(new ResponseWssConfidentialityNode(), index++);
        insert(new RequestWssReplayProtectionNode(), index++);
        insert(new RequestWssTimestampPaletteNode(), index++);
        insert(new ResponseWssTimestampPaletteNode(), index++);
        insert(new ResponseWssSecurityTokenPaletteNode(), index++);
        index = insertMatchingModularAssertions(index);
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            Iterator it = cr.getAssertions(Category.XML_SEC).iterator();
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
