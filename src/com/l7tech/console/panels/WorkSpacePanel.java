package com.l7tech.console.panels;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.Preferences;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <CODE>WorkSpacePanel</CODE> represents the main editing panel
 * for elements such as policies.
 */
public class WorkSpacePanel extends JPanel {
    static final Logger log = Logger.getLogger(WorkSpacePanel.class.getName());

    private final JPanel listPane = new JPanel();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private JScrollPane scrollPane = new JScrollPane();
    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();


    /**
     * default constructor
     */
    public WorkSpacePanel() {
        try {
            layoutComponents();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initializePropertiesListener();
    }

    /**
     * Set the active component that the work bench.
     * The {@link JComponent#getName() } sets the tab name.
     *
     * @param jc the new component to host
     */
    public void setComponent(JComponent jc) {
        scrollPane.setViewportView(jc);
        if (jc !=null) {
            String name = jc.getName();
            if (name != null)
                tabbedPane.setTitleAt(0, jc.getName());
        }
    }

    /**
     * layout components on this panel
     */
    private void layoutComponents() throws IOException {
        addHierarchyListener(hierarchyListener);
        setLayout(new BorderLayout());

        listPane.setLayout(new BorderLayout());
        listPane.add(scrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("", listPane);

        Font f = tabbedPane.getFont();
        tabbedPane.setFont(new Font(f.getName(), Font.BOLD, 12));
        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * initialize properties listener
     */
    private void initializePropertiesListener() {
        // look and feel listener
        PropertyChangeListener l =
                new PropertyChangeListener() {
                    /** This method gets called when a property is changed.*/
                    public void propertyChange(final PropertyChangeEvent evt) {
                        if ("lookAndFeel".equals(evt.getPropertyName())) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    SwingUtilities.updateComponentTreeUI(WorkSpacePanel.this);

                                }
                            });
                        }
                    }
                };

        UIManager.addPropertyChangeListener(l);
        try {
            Preferences pref = Preferences.getPreferences();
            l = new PropertyChangeListener() {
                /** This method gets called when a property is changed.*/
                public void propertyChange(PropertyChangeEvent evt) {
                    log.info("toolbar view changed to " + evt.getNewValue());
                }
            };

            // toolbars (icon, text etc)
            pref.
                    addPropertyChangeListener(Preferences.STATUS_BAR_VISIBLE, l);

        } catch (IOException e) {
            // java.util.Logging does not specify explicit 'level' methods with
            // throwables as params. why?
            log.log(Level.WARNING, "error instantiaitng preferences", e);
        }
    }

    /**
     * Return the ToolBarForTable instance for a given node or null.
     * @return ToolBarForTable
     */
    private ToolBarForTable getToolBar(AbstractTreeNode bn) {
        ToolBarForTable tb = new ToolBarForTable();
        tb.setFloatable(false);
        return tb;
    }

    /**
     * the empty toolbar for table class. The users of the
     * populate the class with components (typically buttons).
     */
    final class ToolBarForTable
            extends JToolBar implements ListSelectionListener {
        JButton buttonUp;
        JButton buttonOpen;
        JButton buttonNew;
        JButton buttonEdit;
        JButton buttonDelete;

        public ToolBarForTable() {
            super();
            this.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
            initComponents();
        }

        private void initComponents() {
            buttonUp = new JButton();
            buttonUp.setIcon(IconManager.getInstance().getUpOneLevelIcon());
            buttonUp.setFont(new Font("Dialog", 1, 10));
            buttonUp.setText("Up");
            buttonUp.setMargin(new Insets(0, 0, 0, 0));

            this.add(buttonUp);
            buttonUp.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {

                        }
                    });


            buttonOpen = new JButton();
            buttonOpen.setIcon(IconManager.getInstance().getOpenFolderIcon());
            buttonOpen.setFont(new Font("Dialog", 1, 10));
            buttonOpen.setText("Open");
            buttonOpen.setMargin(new Insets(0, 0, 0, 0));

            this.add(buttonOpen);
            buttonOpen.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {
                        }
                    });

            buttonOpen.setEnabled(false);

            buttonNew = new JButton();
            buttonNew.setIcon(IconManager.getInstance().getDefaultNewIcon());
            buttonNew.setFont(new Font("Dialog", 1, 10));
            buttonNew.setText("New");
            buttonNew.setMargin(new Insets(0, 0, 0, 0));
            this.add(buttonNew);


            buttonNew.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {

                        }
                    });


            buttonEdit = new JButton();
            buttonEdit.setIcon(IconManager.getInstance().getDefaultEditIcon());
            buttonEdit.setFont(new Font("Dialog", 1, 10));
            buttonEdit.setText("Edit");
            buttonEdit.setMargin(new Insets(0, 0, 0, 0));
            this.add(buttonEdit);
            buttonEdit.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {
                        }
                    });

            buttonEdit.setEnabled(false);

            buttonDelete = new JButton();
            buttonDelete.setIcon(IconManager.getInstance().getDefaultDeleteIcon());
            buttonDelete.setFont(new Font("Dialog", 1, 10));
            buttonDelete.setText("Delete");
            buttonDelete.setMargin(new Insets(0, 0, 0, 0));
            this.add(buttonDelete);
            buttonDelete.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {
                        }
                    });
            buttonDelete.setEnabled(false);
            Utilities.
                    equalizeComponentSizes(
                            new JComponent[]{
                                buttonDelete,
                                buttonEdit,
                                buttonNew,
                                buttonOpen,
                                buttonUp
                            });
        }


        /**
         * Called whenever the value of the selection changes.
         * @param e the event that characterizes the change.
         */
        public void valueChanged(ListSelectionEvent e) {

        }
    }

    // hierarchy listener
    private final
    HierarchyListener hierarchyListener =
            new HierarchyListener() {
                /** Called when the hierarchy has been changed.*/
                public void hierarchyChanged(HierarchyEvent e) {
                    long flags = e.getChangeFlags();
                    if ((flags & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                        if (WorkSpacePanel.this.isShowing()) {
                        } else {
                        }
                    }
                }
            };
}
