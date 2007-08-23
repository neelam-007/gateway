package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.console.util.Registry;
import com.l7tech.console.tree.policy.CodeInjectionProtectionAssertionPaletteNode;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.LicenseException;

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
        super("Threat Protection", "threatProtection");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert( new SqlAttackAssertionPaletteNode(), index++ );
        insert( new CodeInjectionProtectionAssertionPaletteNode(), index++);
        insert( new RequestSizeLimitPaletteNode(), index++ );
        insert( new OversizedTextAssertionPaletteNode(), index++ );
        insert( new RequestWssReplayProtectionNode(), index++ );
        insert( new SchemaValidationPaletteNode(), index++ );
        //insert(new FaultLevelPaletteNode(), index++);
        index = insertMatchingModularAssertions(index);
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            Iterator it = cr.getAssertions(Category.THREAT_PROT).iterator();
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