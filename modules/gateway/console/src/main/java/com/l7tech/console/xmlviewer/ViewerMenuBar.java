package com.l7tech.console.xmlviewer;

import com.l7tech.console.xmlviewer.properties.ViewerProperties;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Insert comments here.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public class ViewerMenuBar extends JMenuBar {
    private ViewerProperties properties;


    JCheckBoxMenuItem showAttributesItem = null;
    JCheckBoxMenuItem showNamespacesItem = null;
    JCheckBoxMenuItem showValuesItem = null;
    JCheckBoxMenuItem showCommentsItem = null;

    CollapseAllAction collapse = null;
    ExpandAllAction expand = null;
    private final ViewerFrame viewerFrame;

    public ViewerMenuBar(final ViewerFrame v, ViewerProperties props) {
        properties = props;
        this.viewerFrame = v;

        // >>> File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        CloseAction close = new CloseAction(viewerFrame);
        JMenuItem closeItem = new JMenuItem(close);
        fileMenu.add(closeItem);

        add(fileMenu);
        // <<< File Menu

        // >>> View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');

        expand = new ExpandAllAction(viewerFrame.getViewer());
        JMenuItem expandItem = new JMenuItem(expand);
        expandItem.setAccelerator((KeyStroke)expand.getValue(Action.ACCELERATOR_KEY));
        viewMenu.add(expandItem);

        collapse = new CollapseAllAction(viewerFrame.getViewer());
        JMenuItem collapseItem = new JMenuItem(collapse);
        collapseItem.setAccelerator((KeyStroke)collapse.getValue(Action.ACCELERATOR_KEY));
        viewMenu.add(collapseItem);

        JMenu showMenu = new JMenu("Show");
        showMenu.setMnemonic('S');
        viewMenu.add(showMenu);

        add(viewMenu);
        // <<< View menu


        showAttributesItem = new JCheckBoxMenuItem("Attributes", properties.isShowAttributes());
        showMenu.add(showAttributesItem);
        showAttributesItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                properties.showAttributes(showAttributesItem.isSelected());
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        viewerFrame.getViewer().tree.update();
                    }
                });
            }
        });


        showNamespacesItem = new JCheckBoxMenuItem("Namespaces", properties.isShowNamespaces());
        showMenu.add(showNamespacesItem);
        showNamespacesItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                properties.showNamespaces(showNamespacesItem.isSelected());
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        viewerFrame.getViewer().tree.update();
                    }
                });
            }
        });

        showValuesItem = new JCheckBoxMenuItem("Values", properties.isShowValues());
        showMenu.add(showValuesItem);
        showValuesItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                properties.showValues(showValuesItem.isSelected());
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        viewerFrame.getViewer().tree.update();
                    }
                });
            }
        });

        showCommentsItem = new JCheckBoxMenuItem("Comments", properties.isShowComments());
        showMenu.add(showCommentsItem);
        showCommentsItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                properties.showComments(showCommentsItem.isSelected());
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        viewerFrame.getViewer().tree.update();
                    }
                });
            }
        });
    }
}
