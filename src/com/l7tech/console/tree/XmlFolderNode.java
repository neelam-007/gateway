package com.l7tech.console.tree;

import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.LicenseException;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * The class represents an gui node element in the TreeModel that
 * represents a routing folder.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class XmlFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public XmlFolderNode() {
        super("Message Validation/Transformation");
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert(new RequestXpathPaletteNode(), index++);
        insert(new ResponseXpathPaletteNode(), index++);
        insert(new SchemaValidationPaletteNode(), index++);
        insert(new XslTransformationPaletteNode(), index++);
        insert(new RequestSwAAssertionPaletteNode(), index++);
        insert(new RegexNode(), index++);
        insert(new OperationPaletteNode(), index++);
        insert(new HtmlFormDataAssertionPaletteNode(), index++);
        insert(new HttpFormPostNode(), index++);
        insert(new InverseHttpFormPostNode(), index++);
        insert(new WsiBspPaletteNode(), index++);
        insert(new WsiSamlPaletteNode(), index++);
        insert(new WsspPaletteNode(), index++);
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            Iterator it = cr.getAssertions(Category.MESSAGE).iterator();
            while (it.hasNext()) {
                CustomAssertionHolder a = (CustomAssertionHolder)it.next();
                insert(new CustomAccessControlNode(a), index++);
            }
            it = cr.getAssertions(Category.MSG_VAL_XSLT).iterator();
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