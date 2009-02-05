package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;

/**
 * The class represents an gui node element in the TreeModel that
 * represents the misc assertions folder.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 19, 2004<br/>
 * $Id$
 */
public class MiscFolderNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public MiscFolderNode() {
        super("Service Availability", "misc");
    }

    /**
     * subclasses override this method
     */
    protected void doLoadChildren() {
        int index = 0;
        children = null;
        insert( new TimeRangePaletteNode(), index++ );
        insert( new RemoteIpRangePaletteNode(), index++ );
        insert( new ThroughputQuotaPaletteNode(), index++ );
        index = insertMatchingModularAssertions(index);
        insertMatchingCustomAssertions(index, Category.AVAILABILITY);
    }
}
