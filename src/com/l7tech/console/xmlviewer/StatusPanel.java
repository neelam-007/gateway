package com.l7tech.console.xmlviewer;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Insert comments here.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public class StatusPanel extends JPanel {
    private ViewerMenuBar menuBar;
    private JLabel universalNameLabel = null;
    private JLabel namespacesLabel = null;
    private JLabel valuesLabel = null;
    private JLabel attributesLabel = null;
    private JLabel commentsLabel = null;
    private final Border bevelBorder = new CompoundBorder(new BevelBorder(BevelBorder.LOWERED, Color.white, new Color(204, 204, 204), new Color(204, 204, 204), new Color(102, 102, 102)),
      new EmptyBorder(0, 2, 0, 0));
    private final CompoundBorder bevelCompoundBorder = new CompoundBorder(new EmptyBorder(2, 0, 0, 0),
      bevelBorder);
    private final Viewer viewer;


    public StatusPanel(BorderLayout borderLayout, ViewerMenuBar menu, Viewer v) {
        super(borderLayout);
        menuBar = menu;
        this.viewer = v;
        universalNameLabel = new JLabel();
        universalNameLabel.setForeground(Color.black);
        universalNameLabel.setPreferredSize(new Dimension(100, 18));
        universalNameLabel.setFont(universalNameLabel.getFont().deriveFont(Font.PLAIN));
        universalNameLabel.setBorder(new EmptyBorder(0, 2, 0, 0));

        commentsLabel = new JLabel();
        commentsLabel.setFont(universalNameLabel.getFont());
        commentsLabel.setHorizontalAlignment(JLabel.CENTER);
        commentsLabel.setForeground(Color.black);
        commentsLabel.setPreferredSize(new Dimension(35, 18));
        commentsLabel.setBorder(bevelCompoundBorder);
        commentsLabel.setText(menu.showCommentsItem.isSelected() ? "COM" : "");

        namespacesLabel = new JLabel();
        namespacesLabel.setHorizontalAlignment(JLabel.CENTER);
        namespacesLabel.setFont(universalNameLabel.getFont());
        namespacesLabel.setForeground(Color.black);
        namespacesLabel.setPreferredSize(new Dimension(25, 18));
        namespacesLabel.setBorder(bevelCompoundBorder);
        namespacesLabel.setText(menu.showNamespacesItem.isSelected() ? "NS" : "");

        valuesLabel = new JLabel();
        valuesLabel.setFont(universalNameLabel.getFont());
        valuesLabel.setHorizontalAlignment(JLabel.CENTER);
        valuesLabel.setForeground(Color.black);
        valuesLabel.setPreferredSize(new Dimension(35, 18));
        valuesLabel.setBorder(bevelCompoundBorder);
        valuesLabel.setText(menu.showValuesItem.isSelected() ? "VAL" : "");

        attributesLabel = new JLabel();
        attributesLabel.setHorizontalAlignment(JLabel.CENTER);
        attributesLabel.setFont(universalNameLabel.getFont());
        attributesLabel.setForeground(Color.black);
        attributesLabel.setPreferredSize(new Dimension(35, 18));
        attributesLabel.setBorder(bevelCompoundBorder);
        attributesLabel.setText(menu.showAttributesItem.isSelected() ? "ATT" : "");

        JPanel elementsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        elementsPanel.add(attributesLabel);
        elementsPanel.add(namespacesLabel);
        elementsPanel.add(valuesLabel);
        elementsPanel.add(commentsLabel);

        add(universalNameLabel, BorderLayout.CENTER);
        add(elementsPanel, BorderLayout.EAST);

        menuBar.showAttributesItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (menuBar.showAttributesItem.isSelected()) {
                    attributesLabel.setText("ATT");
                } else {
                    attributesLabel.setText("");
                }
            }
        });

        menuBar.showNamespacesItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (menuBar.showNamespacesItem.isSelected()) {
                    namespacesLabel.setText("NS");
                } else {
                    namespacesLabel.setText("");
                }
            }
        });

        menuBar.showValuesItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (menuBar.showValuesItem.isSelected()) {
                    valuesLabel.setText("VAL");
                } else {
                    valuesLabel.setText("");
                }
            }
        });

        menuBar.showCommentsItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (menuBar.showCommentsItem.isSelected()) {
                    commentsLabel.setText("COM");
                } else {
                    commentsLabel.setText("");
                }
            }
        });

        viewer.tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = viewer.tree.getSelectionPath();
                if (path != null) {
                    XmlElementNode node = (XmlElementNode)path.getLastPathComponent();
                    ExchangerElement element = node.getElement();

                    if (element != null) {
                        universalNameLabel.setText(node.getElement().getUniversalName());
                    } else {
                        universalNameLabel.setText("");
                    }
                } else {
                    universalNameLabel.setText(null);
                }
            }
        });


    }
}
