package com.l7tech.console.util;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gui.util.Utilities;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.*;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;

/**
 * A simple utility widget for displaying XML in a way that makes it easy to click on individual nodes.
 */
public class XmlViewer extends JTree {
    Document document;
    NodeSelectionListener selectionListener;

    public XmlViewer() {
        getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                Node node = null;

                TreePath path = e.getPath();
                if (path != null) {
                    node = (Node) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                }

                if (selectionListener != null) {
                    selectionListener.nodeSelected(node);
                }
            }
        });
        setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode mutableTreeNode = (DefaultMutableTreeNode) value;
                    Object obj = mutableTreeNode.getUserObject();
                    if (obj instanceof Node) {
                        Node node = (Node) obj;
                        StringBuilder sb = new StringBuilder("<HTML>");

                        sb.append("<HTML><font color='blue'>&lt;</font>");
                        sb.append(node.getNodeName());

                        NamedNodeMap attrs = node.getAttributes();
                        if (attrs != null && attrs.getLength() > 0) {
                            for (int i = 0; i < attrs.getLength(); ++i) {
                                Attr attr = (Attr) attrs.item(i);
                                sb.append(' ');
                                sb.append(attr.getName());
                                sb.append("<font color='blue'>=\"</font>");
                                sb.append(attr.getValue());
                                sb.append("<font color='blue'>\"</font>");
                            }
                        }

                        sb.append("<font color='blue'>&gt;</font>");

                        value = sb.toString();
                    }
                }
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                setIcon(null);
                return this;
            }
        });
        setDocument(XmlUtil.createEmptyDocument());
    }

    public void setDocument(Document document) {
        this.document = document;
        setModel(makeTreeModel(document));
        Utilities.expandTree(this);
    }

    public Document getDocument() {
        return this.document;
    }

    private TreeModel makeTreeModel(Document document) {
        if (document == null)
            return new DefaultTreeModel(new DefaultMutableTreeNode("", false));

        return new DefaultTreeModel(makeNode(document), true);
    }

    private MutableTreeNode makeNode(Node node) {
        if (node == null)
            return new DefaultMutableTreeNode("", false);

        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE:
                return makeNode(((Document)node).getDocumentElement());

            case Node.ELEMENT_NODE:
                DefaultMutableTreeNode ret = new DefaultMutableTreeNode(node, true);
                for (Element element : DomUtils.getChildElements(((Element) node)))
                    ret.add(makeNode(element));
                return ret;

            default:
                throw new IllegalStateException("Can't happen");
        }
    }

    public Node getSelectedNode() {
        DefaultMutableTreeNode tn = (DefaultMutableTreeNode)getLastSelectedPathComponent();
        return tn == null ? null : ((Node)tn.getUserObject());
    }

    public void setSelectionListener(NodeSelectionListener listener) {
        this.selectionListener = listener;
    }

    public interface NodeSelectionListener {
        /**
         * @param node selected node, or null if selection is cleared.
         */
        void nodeSelected(@Nullable Node node);
    }
}
