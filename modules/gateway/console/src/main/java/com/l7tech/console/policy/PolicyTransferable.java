/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.policy;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ResourceUtils;
import org.w3c.dom.Document;

import java.awt.datatransfer.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a snippet of policy XML that can be placed on the clipboard.
 */
public class PolicyTransferable implements Transferable, ClipboardOwner {
    private static final Logger logger = Logger.getLogger(PolicyTransferable.class.getName());

    public static final DataFlavor ASSERTION_DATAFLAVOR;
    public static final DataFlavor HEADLESS_GROUP_DATAFLAVOR;
    static {
        DataFlavor assFlavor;
        DataFlavor headFlavor;
        try {
            assFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + AbstractTreeNode.class.getName());
            headFlavor = new DataFlavor(String.class, "Ungrouped Policy Assertions");
        } catch (ClassNotFoundException e) {
            assFlavor = null;
            headFlavor = null;
        }
        ASSERTION_DATAFLAVOR = assFlavor;
        HEADLESS_GROUP_DATAFLAVOR = headFlavor;
    }


    //
    // Instance fields
    //

    private transient AbstractTreeNode[] treeNodes = null; // in-process only; clipboard gets only plain text
    private String policyXml = null;
    private boolean ignoreRoot = false;

    //
    // Constructors
    //

    /**
     * Create a PolicyTransferable from one or more AbstractTreeNodes.
     *
     * @param treeNodes  tree nodes to transfer.  Must be non-empty.  Nodes must be siblings.
     */
    public PolicyTransferable(AbstractTreeNode[] treeNodes) {
        if (treeNodes == null || treeNodes.length < 1 || treeNodes[0] == null) throw new NullPointerException("No policy tree nodes provided");
        this.treeNodes = treeNodes;
        this.policyXml = null;
    }

    public PolicyTransferable(String policyXml) {
        if (policyXml == null) throw new NullPointerException("No policy XML snippet provided");
        this.policyXml = policyXml;
        this.treeNodes = null;
    }


    //
    // Public methods
    //

    public DataFlavor[] getTransferDataFlavors() {
        List<DataFlavor> ret = new ArrayList<DataFlavor>();
        if (treeNodes != null) ret.add(ASSERTION_DATAFLAVOR);
        ret.add(HEADLESS_GROUP_DATAFLAVOR);
        ret.add(DataFlavor.stringFlavor);
        return ret.toArray(new DataFlavor[ret.size()]);
    }

    /**
     * Get the policy XML string representation.
     *
     * @return the policy XML string, or null if this transferable contains an assertion tree node that (somehow)
     *         lacks an actual assertion.
     */
    public String getPolicyXml() {
        if (policyXml != null) return policyXml;

        if (treeNodes != null) {
            AllAssertion ass = new AllAssertion();
            for(AbstractTreeNode node : treeNodes) {
                if(node instanceof PolicyTemplateNode){
                    FileInputStream fis = null;
                    PolicyTemplateNode template = (PolicyTemplateNode)node;
                    try{
                        fis = new FileInputStream(template.getFile());
                        Document doc = XmlUtil.parse(fis);
                        return XmlUtil.nodeToString(doc);
                    }
                    catch(Exception e){
                        logger.warning("Error reading Policy Template " + template.getName());
                        return null;
                    }
                    finally {
                        if(fis != null) ResourceUtils.closeQuietly(fis);
                    }
                }
                else {
                    Assertion assertion = node.asAssertion();
                    if(assertion != null) ass.addChild(node.asAssertion());
                }
            }
            ignoreRoot = true;
            return WspWriter.getPolicyXml(ass);
        }

        // can't happen
        throw new IllegalStateException("No policyXml or treeNodes");
    }

    /**
     * @return true if the root assertion of the policy tree represented by this transferable is just a
     * grouping placeholder and should not be considered part of the information being transferred. 
     */
    public boolean isIgnoreRoot() {
        return ignoreRoot;
    }

    void setIgnoreRoot(boolean ignoreRoot) {
        this.ignoreRoot = ignoreRoot;
    }

    /**
     * Convert this PolicyTransferable into a version that is safe to export to the system clipboard.
     * Advertising the ASSERTION_DATAFLAVOR seems to prevent clipboard export from succeeding, possibly
     * because it is javaJVMLocalObjectMimeType which is documented as working only "within the same JVM".
     *
     * @return a new PolicyTransferable instance that offers the same data, but only in XML string format.  Never null.
     */
    public PolicyTransferable asClipboardSafe() {
        PolicyTransferable ret = new PolicyTransferable(getPolicyXml());
        ret.setIgnoreRoot(this.ignoreRoot);
        return ret;
    }

    /**
     * Get the AbstractTreeNode representation, if any.
     *
     * @return the assertion tree node, or null if there isn't one.
     */
    public AbstractTreeNode[] getTreeNodes() {
        return treeNodes;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return HEADLESS_GROUP_DATAFLAVOR.equals(flavor) || (ASSERTION_DATAFLAVOR.equals(flavor) ? treeNodes != null : DataFlavor.stringFlavor.equals(flavor));
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor == null) throw new NullPointerException("No DataFlavor provided");
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);

        // Try assertion tree node
        if (ASSERTION_DATAFLAVOR.equals(flavor)) {
            if (treeNodes != null)
                return treeNodes;
            throw new UnsupportedFlavorException(flavor);
        }

        Class wantClass = flavor.getRepresentationClass();

        if ((HEADLESS_GROUP_DATAFLAVOR.equals(flavor) || flavor.isFlavorTextType()) &&
               wantClass == null || CharSequence.class.equals(wantClass) || String.class.equals(wantClass))
            return getPolicyXml();

        // Nothing we can do for them
        throw new UnsupportedFlavorException(flavor);
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // No action required
    }
}
