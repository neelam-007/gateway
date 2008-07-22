/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.policy;

import com.l7tech.util.CausedIOException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * Represents a snippet of policy XML that can be placed on the clipboard.
 */
public class PolicyTransferable implements Transferable, ClipboardOwner {
    public static final DataFlavor ASSERTION_DATAFLAVOR;
    static {
        DataFlavor df;
        try {
            df = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + AbstractTreeNode.class.getName());
        } catch (ClassNotFoundException e) {
            df = null;
        }
        ASSERTION_DATAFLAVOR = df;
    }


    //
    // Instance fields
    //

    private transient AbstractTreeNode treeNode = null; // in-process only; clipboard gets only plain text
    private String policyXml = null;


    //
    // Constructors
    //

    public PolicyTransferable(AbstractTreeNode treeNode) {
        if (treeNode == null) throw new NullPointerException("No policy tree node provided");
        this.treeNode = treeNode;
        this.policyXml = null;
    }

    public PolicyTransferable(String policyXml) {
        if (policyXml == null) throw new NullPointerException("No policy XML snippet provided");
        this.policyXml = policyXml;
        this.treeNode = null;
    }


    //
    // Public methods
    //

    public DataFlavor[] getTransferDataFlavors() {
        if (treeNode != null) {
            return new DataFlavor[] {
                    ASSERTION_DATAFLAVOR,
                    DataFlavor.stringFlavor,
            };
        }
        return new DataFlavor[] {
                DataFlavor.stringFlavor,
        };
    }

    /**
     * Get the policy XML string representation.
     *
     * @return the policy XML string, or null if this transferable contains an assertion tree node that (somehow)
     *         lacks an actual assertion.
     */
    public String getPolicyXml() {
        if (policyXml != null) return policyXml;

        if (treeNode != null) {
            Assertion ass = treeNode.asAssertion();
            if (ass == null)
                return null;
            return WspWriter.getPolicyXml(ass);
        }

        return null;
    }

    /**
     * Get the AbstractTreeNode representation, if any.
     *
     * @return the assertion tree node, or null if there isn't one.
     */
    public AbstractTreeNode getTreeNode() {
        return treeNode;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return ASSERTION_DATAFLAVOR.equals(flavor) ? treeNode != null : DataFlavor.stringFlavor.equals(flavor);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor == null) throw new NullPointerException("No DataFlavor provided");
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);

        Class wantClass = flavor.getRepresentationClass();

        // Try assertion tree node
        if (ASSERTION_DATAFLAVOR.equals(flavor)) {
            if (treeNode != null)
                return treeNode;
            throw new UnsupportedFlavorException(flavor);
        }

        // Try DOM XML
        if (Node.class.equals(wantClass) || Element.class.equals(wantClass) || Document.class.equals(wantClass)) {
            try {
                Document doc = XmlUtil.stringToDocument(policyXml);
                if (Element.class.equals(wantClass))
                    return doc.getDocumentElement();
                return doc;
            } catch (SAXException e) {
                throw new CausedIOException(e);
            }
        }

        // Try fallback to XML encoded as plain text
        if (flavor.isFlavorTextType() &&
                (wantClass == null || CharSequence.class.equals(wantClass) || String.class.equals(wantClass))) {
            return policyXml;
        }

        // Nothing we can do for them
        throw new UnsupportedFlavorException(flavor);
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // No action required
    }
}
