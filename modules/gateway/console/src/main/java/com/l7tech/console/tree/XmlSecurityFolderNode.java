package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken;


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
    @Override
    protected void doLoadChildren() {
        int index = 0;
        // Assertions are inserted by type for now until we have a way to define the
        // desired ordering in metadata.
        index = insertModularAssertionByType( index, RequestWssIntegrity.class );
        index = insertModularAssertionByType( index, RequestWssConfidentiality.class );
        index = insertModularAssertionByType( index, ResponseWssIntegrity.class );
        index = insertModularAssertionByType( index, ResponseWssConfidentiality.class );
        index = insertModularAssertionByType( index, ResponseWssSecurityToken.class );
        index = insertMatchingModularAssertions(index);
        insertMatchingCustomAssertions(index, Category.XML_SEC);
    }
}
