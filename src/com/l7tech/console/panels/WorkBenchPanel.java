package com.l7tech.console.panels;

import com.l7tech.console.MainWindow;
import com.l7tech.console.tree.BasicTreeNode;
import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.Preferences;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;

/**
 * <CODE>WorkBenchPanel</CODE> represents the main editing panel
 * for elements such as policies.
 */
public class WorkBenchPanel extends JPanel {
    private static final Category log = Category.getInstance(WorkBenchPanel.class.getName());

    private final JPanel listPane = new JPanel();
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private JTree tree = null;
    private DefaultMutableTreeNode parentNode = null;
    final String home = MainWindow.RESOURCE_PATH+"/home.html";
    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();

    /**
     * default constructor
     */
    public WorkBenchPanel() {
        try {
            layoutComponents();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initializePropertiesListener();
    }

    /**
     * layout components on this panel
     */
    private void layoutComponents() throws IOException {
        addHierarchyListener(hierarchyListener);
        setLayout(new BorderLayout());

        listPane.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(forTreeNode(null));
        listPane.add(scrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("Home", listPane);

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
                                    SwingUtilities.updateComponentTreeUI(WorkBenchPanel.this);

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
                    log.debug("toolbar view changed to " + evt.getNewValue());
                }
            };

            // toolbars (icon, text etc)
            pref.
                    addPropertyChangeListener(Preferences.TOOLBARS_VIEW, l);

        } catch (IOException e) {
            log.warn("error instantiaitng preferences", e);
        }
    }

    /**
     *
     * @param node the node to examine
     * @return the corresponding component
     */
    private JComponent forTreeNode(TreeNode node) {
        HTMLDocument doc = new HTMLDocument();
        JTextPane htmlPane = new JTextPane(doc);

        URL url = cl.getResource(home);

        htmlPane.setEditable(false);
        htmlPane.addHyperlinkListener( new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                    System.out.println("activated "+e.getURL());
                }
            }
        });
        try {
            htmlPane.setPage(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return htmlPane;

    }

    /**
     * create and returns the tab title for the given
     * <CODE>object</CODE> instance.
     *
     * @param o     the <CODE>Object</CODE> to determine the label for
     * @return the title for the BasicTreeNode instance
     */
    private String tabTitle(Object o) {
        return o.toString();
    }

    /**
     * Return the ToolBarForTable instance for a given node or null.
     * @return ToolBarForTable
     */
    private ToolBarForTable getToolBar(BasicTreeNode bn) {
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
                        if (WorkBenchPanel.this.isShowing()) {
                        } else {
                        }
                    }
                }
            };
}
