package com.l7tech.console.xmlviewer;

import com.l7tech.common.gui.widgets.SquigglyTextField;
import com.l7tech.console.xmlviewer.properties.ViewerProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * Insert comments here.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public class ViewerToolBar extends JToolBar {
    Viewer viewer;
    ViewerProperties properties;
    private JTextField xpathField = null;
    private CollapseAllAction collapse = null;
    private ExpandAllAction expand = null;

    public ViewerToolBar(ViewerProperties props, Viewer v) {
        viewer = v;
        properties = props;

        setFloatable(false);
        expand = new ExpandAllAction(v);
        collapse = new CollapseAllAction(v);

        xpathField = new SquigglyTextField();

        JLabel xpathLabel = new JLabel("XPath:");
        xpathLabel.setForeground(new Color(102, 102, 102));

        JPanel xpathPanel = new JPanel(new BorderLayout(5, 0));
        xpathPanel.setBorder(new EmptyBorder(2, 5, 2, 2));
        JPanel xpanel = new JPanel(new BorderLayout());
        xpanel.setBorder(new EmptyBorder(1, 0, 1, 0));

        xpathField.setFont(xpathField.getFont().deriveFont(Font.PLAIN, 12));
        xpathField.setPreferredSize(new Dimension(100, 19));
        xpathField.setEditable(true);

        xpanel.add(xpathField, BorderLayout.CENTER);
        xpathPanel.add(xpathLabel, BorderLayout.WEST);
        xpathPanel.add(xpanel, BorderLayout.CENTER);

        add(xpathPanel, BorderLayout.CENTER);

        add(expand);
        add(collapse);
        addSeparator();

        viewer.tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = viewer.tree.getSelectionPath();
                if (path != null) {
                    XmlElementNode node = (XmlElementNode)path.getLastPathComponent();
                    ExchangerElement element = node.getElement();

                    if (element != null) {
                        xpathField.setText(node.getElement().getPath());
                    } else {
                        xpathField.setText(null);
                    }
                }
            }
        });
    }

    /**
     * Access the xpath combo box component. This accesses
     *
     * @return the xpath combo box
     */
    public JTextField getxpathField() {
        return xpathField;
    }

    /**
     * Sets whether or not this component controls are enabled.
     *
     * @param enabled true if this component controls should be enabled, false otherwise
     */
    public void setToolbarEnabled(boolean enabled) {
        xpathField.setEnabled(enabled);
        collapse.setEnabled(enabled);
        expand.setEnabled(enabled);
    }

    /**
     * @return the currently selected xpath or <b>null</b> if none selected
     */
    public String getXPath() {
        return xpathField.getText();
    }
}
