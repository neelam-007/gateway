package com.l7tech.console.panels;

import com.l7tech.console.action.ActionVetoException;
import com.l7tech.console.event.ContainerVetoException;
import com.l7tech.console.event.VetoableContainerListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.util.Preferences;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventListener;
import java.util.logging.Logger;

/**
 * <CODE>WorkSpacePanel</CODE> represents the main editing panel
 * for elements such as policies.
 */
public class WorkSpacePanel extends JPanel {
    static public final String NAME = "workspace.panel";
    static final Logger log = Logger.getLogger(WorkSpacePanel.class.getName());
    private final TabbedPane tabbedPane = new TabbedPane();

    /** TLS helper for container veto excpetions */
    private static ThreadLocal containerVetoException = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return null;
        }
    };

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
    public void setComponent(JComponent jc) throws ActionVetoException {
        tabbedPane.removeAll();
        if (containerVetoException.get() != null) {
            ContainerVetoException cve = (ContainerVetoException)containerVetoException.get();
            containerVetoException.set(null);
            throw new ActionVetoException(null, "workspace change vetoed", cve);
        }

        tabbedPane.addTab(jc.getName(), jc);
        jc.addPropertyChangeListener(new PropertyChangeListener() {
            /**
             * This method gets called when a bound property is changed.
             * 
             * @param evt A PropertyChangeEvent object describing the event source
             *            and the property that has changed.
             */
            public void propertyChange(PropertyChangeEvent evt) {
                if ("name".equals(evt.getPropertyName())) {
                    tabbedPane.setTitleAt(0, (String)evt.getNewValue());
                }
            }
        });
    }

    /**
     * get the component that the workspace panel is currently
     * hosting or null.
     * 
     * @return the workspace panel component or null
     */
    public JComponent getComponent() {
        int tabCount = tabbedPane.getTabCount();
        if (tabCount == 0) return null;
        return (JComponent)tabbedPane.getComponentAt(tabCount - 1);
    }


    /**
     * Remove the active component that the workspace.
     * The {@link JComponent#getName() } sets the tab name.
     */
    public void clearWorkspace() throws ActionVetoException {
        tabbedPane.removeAll();
        if (containerVetoException.get() != null) {
            ContainerVetoException cve = (ContainerVetoException)containerVetoException.get();
            containerVetoException.set(null);
            throw new ActionVetoException(null, "workspace change vetoed", cve);
        }
    }


    /**
     * Adds the specified container listener to receive container events
     * from this container.
     * If l is null, no exception is thrown and no action is performed.
     * This is a specialized version of the container listener, and it is
     * delegated to the Container that hosts the <i>workspace</i> component.
     * 
     * @param l the container listener
     */
    public synchronized void addWorkspaceContainerListener(ContainerListener l) {
        tabbedPane.addContainerListener(l);
    }

    /**
     * Removes the specified container listener so it no longer receives
     * container events from this container.
     * If l is null, no exception is thrown and no action is performed.
     * This is a specialized version of the container listener and it is
     * delegated to the Container that hosts the <i>workspace</i> component.
     * 
     * @param l the container listener
     */
    public synchronized void removeWorkspaceContainerListener(ContainerListener l) {
        tabbedPane.removeContainerListener(l);
    }


    /**
     * layout components on this panel
     */
    private void layoutComponents() {
        setBorder(null);
        addHierarchyListener(hierarchyListener);
        setLayout(new BorderLayout());
        Font f = tabbedPane.getFont();
        tabbedPane.setBorder(null);
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
              /**
               * This method gets called when a property is changed.
               */
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
        Preferences pref = Preferences.getPreferences();
        l = new PropertyChangeListener() {
            /**
             * This method gets called when a property is changed.
             */
            public void propertyChange(PropertyChangeEvent evt) {
                log.info("toolbar view changed to " + evt.getNewValue());
            }
        };

        // toolbars (icon, text etc)
        pref.
          addPropertyChangeListener(Preferences.STATUS_BAR_VISIBLE, l);


        tabbedPane.addContainerListener(new ContainerAdapter() {

            public void componentRemoved(ContainerEvent e) {
                final Component c = e.getChild();
                if (c instanceof ContainerListener) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            log.fine("Removig container listener of type " + c.getClass());
                            tabbedPane.
                              removeContainerListener((ContainerListener)c);
                        }
                    });
                }
            }
        });
    }

    // hierarchy listener
    private final
    HierarchyListener hierarchyListener =
      new HierarchyListener() {
          /**
           * Called when the hierarchy has been changed.
           */
          public void hierarchyChanged(HierarchyEvent e) {
              long flags = e.getChangeFlags();
              if ((flags & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                  if (WorkSpacePanel.this.isShowing()) {
                  } else {
                  }
              }
          }
      };

    /**
     * The tabbed pane with veto support (for removing/adding tabs)
     */
    private static class TabbedPane extends JTabbedPane {
        EventListenerList listenerList = new WeakEventListenerList();

        public TabbedPane() {
        }

        /**
         * Adds the specified container listener to receive container events
         * from this container.
         * If l is null, no exception is thrown and no action is performed.
         * This is a specialized version of the container listener, that
         * support the VetoableContainerListener
         * 
         * @param l the container listener
         */
        public synchronized void addContainerListener(ContainerListener l) {
            if (l instanceof VetoableContainerListener) {
                listenerList.add(VetoableContainerListener.class, (VetoableContainerListener)l);
            }
            super.addContainerListener(l);
        }

        /**
         * Removes the specified container listener so it no longer receives
         * container events from this container.
         * If l is null, no exception is thrown and no action is performed.
         * This is a specialized version of the container listener, that
         * supports the VetoableContainerListener
         * 
         * @param l the container listener
         */
        public synchronized void removeContainerListener(ContainerListener l) {
            if (l instanceof VetoableContainerListener) {
                listenerList.remove(VetoableContainerListener.class, (VetoableContainerListener)l);
            }
            super.removeContainerListener(l);
        }

        /**
         * Removes the tab at <code>index</code>. This method is overriden
         * for veto support.
         * 
         * @see #addTab
         * @see #insertTab
         */
        public void removeTabAt(int index) {
            containerVetoException.set(null);
            EventListener[] listeners =
              listenerList.getListeners(VetoableContainerListener.class);
            ContainerEvent e =
              new ContainerEvent(this,
                ContainerEvent.COMPONENT_REMOVED,
                getComponentAt(index));
            try {
                for (int i = 0; i < listeners.length; i++) {
                    EventListener listener = listeners[i];
                    ((VetoableContainerListener)listener).componentWillRemove(e);
                }
                super.removeTabAt(index);
            } catch (ContainerVetoException e1) {
                containerVetoException.set(e1);
            }
        }

        /**
         * Inserts a <code>component</code>, at <code>index</code>. This method
         * is overriden for veto uspprt.
         */
        public void insertTab
          (String
          title, Icon
          icon, Component
          component, String
          tip, int index) {
            containerVetoException.set(null);
            EventListener[] listeners =
              listenerList.getListeners(VetoableContainerListener.class);
            ContainerEvent e =
              new ContainerEvent(this,
                ContainerEvent.COMPONENT_ADDED,
                component);
            try {
                for (int i = 0; i < listeners.length; i++) {
                    EventListener listener = listeners[i];
                    ((VetoableContainerListener)listener).componentWillAdd(e);
                }
                super.insertTab(title, icon, component, tip, index);
            } catch (ContainerVetoException e1) {
                containerVetoException.set(e1);
            }
        }

        /**
         * Removes all the tabs and their corresponding components
         * from the <code>tabbedpane</code>.
         * 
         * @see #addTab
         * @see #removeTabAt
         */
        public void removeAll() {
            //setSelectedIndexImpl(-1);

            int tabCount = getTabCount();
            // We invoke removeTabAt for each tab, otherwise we may end up
            // removing CredentialsLocation added by the UI.
            while (tabCount-- > 0) {
                removeTabAt(tabCount);
            }
        }
    }
}
    