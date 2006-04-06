package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.console.util.Registry;

import java.util.Iterator;
import java.util.logging.Level;
import java.rmi.RemoteException;

/**
 * The class represents an gui node element in the TreeModel that
 * represents the audit assertions folder.
 */
public class ThreatProtectionFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public ThreatProtectionFolderNode() {
        super("Threat Protection");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert( new SqlAttackAssertionPaletteNode(), index++ );
        insert( new RequestSizeLimitPaletteNode(), index++ );
        insert( new OversizedTextAssertionPaletteNode(), index++ );
        insert( new RequestWssReplayProtectionNode(), index++ );
        insert( new SchemaValidationPaletteNode() {
            public String getName() {
                return "XML Parameter Tampering and XDoS Protection";
            }
        }, index++ );
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            Iterator it = cr.getAssertions(Category.THREAT_PROT).iterator();
            while (it.hasNext()) {
                CustomAssertionHolder a = (CustomAssertionHolder)it.next();
                insert(new CustomAccessControlNode(a), index++);
            }
        } catch (RemoteException e1) {
            logger.log(Level.WARNING, "Unable to retrieve custom assertions", e1);
        }
    }
}