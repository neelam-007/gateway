package com.l7tech.console.panels;

import com.l7tech.console.util.Preferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <CODE>WorkSpacePanel</CODE> represents the main editing panel
 * for elements such as policies.
 */
public class WorkSpacePanel extends JPanel {
    static final Logger log = Logger.getLogger(WorkSpacePanel.class.getName());

    private final JTabbedPane tabbedPane = new JTabbedPane();
    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();


    /**
     * default constructor
     */
    public WorkSpacePanel() {
        layoutComponents();
        initializePropertiesListener();
    }

    /**
     * Set the active component for the work space.
     *
     * @param jc the new component to host
     */
    public void setComponent(JComponent jc) {
        tabbedPane.removeAll();
        tabbedPane.addTab(jc.getName(), jc);
    }

    /**
     * Remove the active component that the work bench.
     * The {@link JComponent#getName() } sets the tab name.
     */
    public void clearWorskpace() {
        tabbedPane.removeAll();
    }


    /**
     * layout components on this panel
     */
    private void layoutComponents() {
        addHierarchyListener(hierarchyListener);
        setLayout(new BorderLayout());
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
