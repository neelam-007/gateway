package com.l7tech.console.xmlviewer;

import com.l7tech.console.xmlviewer.properties.ViewerProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Vector;

/**
 * Insert comments here.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public class ViewerToolBar extends JToolBar {
    Viewer viewer;
    ViewerProperties properties;
    private JComboBox xpathComboBox = null;
    private CollapseAllAction collapse = null;
    private ExpandAllAction expand = null;
    private JButton selectXpath;

    public interface XPathSelectFeedback {
        void selected(String xpathSelected);
    }

    public ViewerToolBar(ViewerProperties props, Viewer v, final XPathSelectFeedback selectionFeedback,
                         final JRootPane rootPane) {
        viewer = v;
        properties = props;

        setFloatable(false);
        expand = new ExpandAllAction(v);
        collapse = new CollapseAllAction(v);

        xpathComboBox = new JComboBox();
        xpathComboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String xpathString = (String)xpathComboBox.getEditor().getItem();
                    if (viewer.selectXpath(xpathString)) {
                        setXPaths();
                    }
                }
            }
        });


        selectXpath = new JButton("Select");
        selectXpath.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String xpathString = (String)xpathComboBox.getEditor().getItem();
                if (viewer.selectXpath(xpathString)) {
                    setXPaths();
                    if (selectionFeedback != null) {
                        selectionFeedback.selected(xpathString);
                    }
                }
            }
        });

        if (rootPane != null)
            rootPane.setDefaultButton(selectXpath);        
        setXPaths();

        JLabel xpathLabel = new JLabel("XPath:");
        xpathLabel.setForeground(new Color(102, 102, 102));
        selectXpath.setMargin(new Insets(0, 5, 0, 5));
        selectXpath.setFocusPainted(false);

        JPanel xpathPanel = new JPanel(new BorderLayout(5, 0));
        xpathPanel.setBorder(new EmptyBorder(2, 5, 2, 2));
        JPanel xpanel = new JPanel(new BorderLayout());
        xpanel.setBorder(new EmptyBorder(1, 0, 1, 0));

        xpathComboBox.setFont(xpathComboBox.getFont().deriveFont(Font.PLAIN, 12));
        xpathComboBox.setPreferredSize(new Dimension(100, 19));
        xpathComboBox.setEditable(true);

        xpanel.add(xpathComboBox, BorderLayout.CENTER);
        xpathPanel.add(xpathLabel, BorderLayout.WEST);
        xpathPanel.add(xpanel, BorderLayout.CENTER);
        xpathPanel.add(selectXpath, BorderLayout.EAST);

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
                        xpathComboBox.getEditor().setItem(node.getElement().getPath());
                    } else {
                        xpathComboBox.getEditor().setItem(null);
                    }
                }
            }
        });
    }

    /**
     * Access the xpath combo box component. This accesses
     * @return the xpath combo box
     */
    public JComboBox getXpathComboBox() {
        return xpathComboBox;
    }

    /**
     * Sets whether or not this component controls are enabled.
     *
     * @param enabled true if this component controls should be enabled, false otherwise
     */
    public void setToolbarEnabled(boolean enabled) {
        xpathComboBox.setEnabled(enabled);
        collapse.setEnabled(enabled);
        expand.setEnabled(enabled);
        selectXpath.setEnabled(enabled);
    }

    /**
     * @return the currently selected xpath or <b>null</b> if none selected
     */
    public String getXPath() {
        final Object item = xpathComboBox.getEditor().getItem();
        if (item == null) return null;
        return item.toString();
    }


    private void setXPaths() {
        if (xpathComboBox.getItemCount() > 0) {
            xpathComboBox.removeAllItems();
        }

        Vector xpaths = properties.getXPaths();

        for (int i = 0; i < xpaths.size(); i++) {
            xpathComboBox.addItem(xpaths.elementAt(i));
        }
    }

}
