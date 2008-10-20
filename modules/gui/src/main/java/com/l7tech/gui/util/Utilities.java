/*
 * $Header$
 */
package com.l7tech.gui.util;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * This class is a bag of utilites shared by panels.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @noinspection unchecked,RedundantArrayCreation,UnnecessaryUnboxing,ForLoopReplaceableByForEach
 */
public class Utilities {
    private static final Logger logger = Logger.getLogger(Utilities.class.getName());

    public static final String CONTEXT_CUT = "Cut";
    public static final String CONTEXT_COPY = "Copy";
    public static final String CONTEXT_PASTE = "Paste";
    public static final String CONTEXT_SELECT_ALL = "Select All";

    /** Action map entry for setEscKeyDisposes */
    public static final String KEY_ESCAPE = "escape-action";

    /**
     * private constructor, this class cannot be instantiated
     */
    private Utilities() {
    }

    /**
     * Sets all of the JLabels to be the same size. This is done
     * dynamically by setting each compnents's preferred and maximum
     * sizes.
     *
     * @param labels an array of labels to set the same size.
     */
    public static void equalizeLabelSizes(JLabel[] labels) {

        if (labels == null || labels.length == 0) {
            return;
        }
        // Get the largest width and height
        Dimension maxSize = new Dimension(0, 0);
        Rectangle2D textBounds;
        FontMetrics metrics = labels[0].getFontMetrics(labels[0].getFont());
        Graphics g = labels[0].getGraphics();

        for (int i = 0; i < labels.length; i++) {
            textBounds = metrics.getStringBounds(labels[i].getText(), g);
            maxSize.width =
              Math.max(maxSize.width, (int)textBounds.getWidth());
            maxSize.height =
              Math.max(maxSize.height, (int)textBounds.getHeight());
        }

        // reset preferred and maximum size since GridBaglayout takes both
        // into account
        for (int i = 0; i < labels.length; i++) {
            labels[i].setPreferredSize((Dimension)maxSize.clone());
            labels[i].setMaximumSize((Dimension)maxSize.clone());
        }
    }

    /**
     * Sets all of the buttons to be the same size. This is done
     * dynamically by setting each button's preferred and maximum
     * sizes after the buttons are created.
     * <p/>
     * Limitations:
     * Button images are not considered into the calcualtion, nor
     * the alignment.
     * <p/>
     * The first button is used to determine the font size, that is
     * same font is assumed for all the buttons.
     *
     * @param buttons the array of buttons to eqalize the size for.
     */
    public static void equalizeButtonSizes(javax.swing.AbstractButton[] buttons) {
        if (buttons == null || buttons.length == 0) {
            return;
        }
        // Get the largest width and height
        Dimension maxSize = new Dimension(0, 0);
        Rectangle2D textBounds;
        FontMetrics metrics =
          buttons[0].getFontMetrics(buttons[0].getFont());
        Graphics g = buttons[0].getGraphics();

        for (int i = 0; i < buttons.length; i++) {
            textBounds = metrics.getStringBounds(buttons[i].getText(), g);
            maxSize.width =
              Math.max(maxSize.width, (int)textBounds.getWidth());
            maxSize.height =
              Math.max(maxSize.height, (int)textBounds.getHeight());
        }

        if (buttons[0].getBorder() != null) {
            Insets insets =
              buttons[0].getBorder().getBorderInsets(buttons[0]);
            maxSize.width += insets.left + insets.right;
            maxSize.height += insets.top + insets.bottom;
        }

        // reset preferred and maximum size since GridBaglayout takes both
        // into account
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setPreferredSize((Dimension)maxSize.clone());
            buttons[i].setMaximumSize((Dimension)maxSize.clone());
        }
    }

    /**
     * Get the button size.
     * <p/>
     * The button size is detemrined by the text, font, and insets.
     *
     * @param button the buttons to get the size for.
     * @return the button size dimension
     */
    public static Dimension getButtonSize(javax.swing.AbstractButton button) {
        // Get the largest width and height
        Dimension buttonSize = new Dimension();
        Rectangle2D textBounds;
        FontMetrics metrics = button.getFontMetrics(button.getFont());
        Graphics g = button.getGraphics();

        textBounds = metrics.getStringBounds(button.getText(), g);
        buttonSize.width = (int)textBounds.getWidth();
        buttonSize.height = (int)textBounds.getHeight();

        if (button.getBorder() != null) {
            Insets insets = button.getBorder().getBorderInsets(button);
            buttonSize.width += insets.left + insets.right;
            buttonSize.height += insets.top + insets.bottom;
        }

        return buttonSize;
    }


    /**
     * Sets the JComponents in the array to be the same size.
     * This is done by setting each component's preferred and
     * maximum sizes after the components have been created.
     *
     * @param components instances of JComponent
     */
    public static void equalizeComponentSizes(JComponent[] components) {
        // Get the largest width and height
        int i;
        Dimension maxPreferred = new Dimension(0, 0);
        JComponent oneComponent;
        Dimension thisPreferred;
        for (i = 0; i < components.length; ++i) {
            oneComponent = components[i];
            thisPreferred = oneComponent.getPreferredSize();
            maxPreferred.width =
              Math.max(maxPreferred.width, (int)thisPreferred.getWidth());
            maxPreferred.height =
              Math.max(maxPreferred.height, (int)thisPreferred.getHeight());
        }

        for (i = 0; i < components.length; ++i) {
            oneComponent = components[i];
            oneComponent.setPreferredSize((Dimension)maxPreferred.clone());
            oneComponent.setMaximumSize((Dimension)maxPreferred.clone());
        }
    }

    /**
     * Sets the JComponents in the array to be the same widthe.
     * This is done by setting each component's preferred and
     * maximum sizes after the components have been created.
     *
     * @param components instances of JComponent
     */
    public static void equalizeComponentWidth(JComponent[] components) {
        // Get the largest width and height
        int i;
        Dimension maxPreferred = new Dimension(0, 0);
        JComponent oneComponent;
        Dimension thisPreferred;
        for (i = 0; i < components.length; ++i) {
            oneComponent = components[i];
            thisPreferred = oneComponent.getPreferredSize();
            maxPreferred.width =
              Math.max(maxPreferred.width, (int)thisPreferred.getWidth());
        }

        for (i = 0; i < components.length; ++i) {
            oneComponent = components[i];
            Dimension d = oneComponent.getPreferredSize();
            d.width = maxPreferred.width;
            oneComponent.setPreferredSize((Dimension)d.clone());
            oneComponent.setMaximumSize((Dimension)d.clone());
        }
    }


    /**
     * Sets the CredentialsLocation in the array to be the same size.
     * This is done dynamically by setting each button's
     * preferred and maximum sizes after the components have
     * been created.
     *
     * @param components instances of Component
     */
    public static void equalizeComponentSizes(Component[] components) {
        // Get the largest width and height
        int i;
        Dimension maxPreferred = new Dimension(0, 0);
        Component oneComponent;
        Dimension thisPreferred;
        for (i = 0; i < components.length; ++i) {
            oneComponent = components[i];
            thisPreferred = oneComponent.getPreferredSize();
            maxPreferred.width =
              Math.max(maxPreferred.width, (int)thisPreferred.getWidth());
            maxPreferred.height =
              Math.max(maxPreferred.height, (int)thisPreferred.getHeight());
        }

        for (i = 0; i < components.length; ++i) {
            oneComponent = components[i];
            oneComponent.setSize((Dimension)maxPreferred.clone());
        }
    }

    /**
     * center the frame on the screen
     *
     * @param window the window to center on the screen
     */
    public static void centerOnScreen(Window window) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = window.getSize();
        if (frameSize.height > screenSize.height)
            frameSize.height = screenSize.height;
        if (frameSize.width > screenSize.width)
            frameSize.width = screenSize.width;
        window.setLocation((screenSize.width - frameSize.width) / 2,
                           (screenSize.height - frameSize.height) / 2);
    }

    public static void centerOnParent(Component component) {
        Container parent = component.getParent();
        Dimension parentSize = parent.getSize();
        Dimension compSize = component.getSize();
        if (compSize.height > parentSize.height)
            compSize.height = parentSize.height;
        if (compSize.width > parentSize.width)
            compSize.width = parentSize.width;
        component.setLocation((parentSize.width - compSize.width) / 2,
                           (parentSize.height - compSize.height) / 2);
    }

    public static void centerOnParentWindow(Component component) {
        Window toCenter;
        if (component instanceof Window) {
            toCenter = (Window) component;
        } else {
            toCenter = SwingUtilities.getWindowAncestor(component);
        }
        Window parent = toCenter.getOwner();

        if (parent == null || !parent.isVisible()) {
            centerOnScreen(toCenter);
        }
        else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension parentSize = parent.getSize();
            Dimension compSize = component.getSize();

            // if larger than parent display slightly to the right and below
            if (compSize.height > parentSize.height)
                compSize.height = parentSize.height - 40;
            if (compSize.width > parentSize.width)
                compSize.width = parentSize.width - 40;

            int x = ((int)parent.getLocationOnScreen().getX()) + ((parentSize.width - compSize.width) / 2);
            int y = ((int)parent.getLocationOnScreen().getY()) + ((parentSize.height - compSize.height) / 2);

            // fix offscreen
            if (x < 0) x = 0;
            if (y < 0) y = 0;
            if ((x+compSize.width) > screenSize.width) x = screenSize.width - compSize.width;
            if ((y+compSize.height) > screenSize.height) y = screenSize.height - compSize.height;

            toCenter.setLocation(x, y);
        }
    }



    /**
     * Loads an image from the specified resourceD.
     * Lookup.
     *
     * @param resource resource path of the icon (no initial slash)
     * @return icon's Image, or null, if the icon cannot be loaded.
     */
    public static Image loadImage(String resource) {
        return ImageCache.getInstance().getIcon(resource);
    }

    /**
     * If this property is set to "true" in a control with an edit context menu, it's right-click
     * mouse listener will do an automatic "select all" before popping up the menu.
     */
    public static final String PROPERTY_CONTEXT_MENU_AUTO_SELECT_ALL = "com.l7tech.common.gui.util.Utilities.contextMenuAutoSelectAll";

    /**
     * Update the input map of the JDialog's <code>JLayeredPane</code> so
     * the ESC keystroke  invoke dispose on the dialog.
     *
     * @param d the dialog
     */
    public static void setEscKeyStrokeDisposes(final JDialog d) {
        JLayeredPane layeredPane = d.getLayeredPane();
        final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getActionMap().put(KEY_ESCAPE,
                                       new AbstractAction() {
                                           public void actionPerformed(ActionEvent evt) {
                                               d.dispose();
                                           }
                                       });
    }

    /**
     * Update the input map of the JFrame's <code>JLayeredPane</code> so
     * the ESC keystroke nvoke dispose on the frame.
     *
     * @param f the frame
     */
    public static void setEscKeyStrokeDisposes(final JFrame f) {
        JLayeredPane layeredPane = f.getLayeredPane();
        final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getActionMap().put(KEY_ESCAPE,
                                       new AbstractAction() {
                                           public void actionPerformed(ActionEvent evt) {
                                               f.dispose();
                                           }
                                       });
    }

    /**
     * Update the input map of the JDialog's <code>JLayeredPane</code> so
     * the ESC keystroke invokes the passed action dispose on the dialog.
     *
     * @param d the dialog
     * @param action the dialog action to invoke on Esc key
     */
    public static void setEscAction(final JDialog d, final Action action) {
        JLayeredPane layeredPane = d.getLayeredPane();
        final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getActionMap().put(KEY_ESCAPE, action);
    }

    /**
     * Update the input map of the JDialog's <code>JLayeredPane</code> so
     * the ENTER keystroke invokes the passed action.
     *
     * @param d the dialog
     * @param action the dialog action to invoke on Enter key
     */
    public static void setEnterAction(final JDialog d, final Action action) {
        JLayeredPane layeredPane = d.getLayeredPane();
        final KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enterKeyStroke, "ok-it");
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(enterKeyStroke, "ok-it");
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enterKeyStroke, "ok-it");
        layeredPane.getActionMap().put("ok-it", action);
    }

    /**
     * Update the input map of the JFrame's <code>JLayeredPane</code> so
     * the ESC keystroke invokes the passed action.
     *
     * @param f the frame
     * @param action the frame action to invoke on Esc key
     */
    public static void setEscAction(final JFrame f, final Action action) {
        JLayeredPane layeredPane = f.getLayeredPane();
        final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, KEY_ESCAPE);
        layeredPane.getActionMap().put(KEY_ESCAPE, action);
    }

    /**
     * Set the button to click when a double click is performed on the list.
     *
     * @param list The list.
     * @param button The button whose action should be performed.
     */
    public static void setDoubleClickAction(final JList list, final AbstractButton button) {
        setDoubleClickAction(list, getDispatchingActionListener(button, button.getActionListeners()), button, button.getActionCommand());
    }

    /**
     * Set the button to click when a double click is performed on the list.
     *
     * @param list The list.
     * @param listener The listener for the action
     * @param source The source for the action
     * @param command The command for the action
     */
    public static void setDoubleClickAction(final JList list, final ActionListener listener, final Object source, final String command) {
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = list.locationToIndex(e.getPoint());
                    if ( index >= 0 ) {
                        Rectangle bounds = list.getCellBounds(index, index);
                        if ( bounds.contains(e.getPoint()) ) {
                            ActionEvent actionEvent =
                                    new ActionEvent( source, ActionEvent.ACTION_PERFORMED, command);
                            listener.actionPerformed( actionEvent );
                        }
                    }
                 }
            }
        };
        list.addMouseListener(mouseListener);
    }

    /**
     * Set the button to click when a double click is performed on the table.
     *
     * @param table The table.
     * @param button The button whose action should be performed.
     */
    public static void setDoubleClickAction(final JTable table, final AbstractButton button) {
        setDoubleClickAction(table, getDispatchingActionListener(button, button.getActionListeners()), button, button.getActionCommand());
    }

    /**
     * Set the button to click when a double click is performed on the table.
     *
     * @param table The table.
     * @param listener The listener for the action
     * @param source The source for the action
     * @param command The command for the action
     */
    public static void setDoubleClickAction(final JTable table, final ActionListener listener, final Object source, final String command) {
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = table.rowAtPoint(e.getPoint());
                    if ( index >= 0 ) {
                        ActionEvent actionEvent =
                                new ActionEvent( source, ActionEvent.ACTION_PERFORMED, command);
                        listener.actionPerformed( actionEvent );
                    }
                 }
            }
        };
        table.addMouseListener(mouseListener);
    }

    public static void doWithConfirmation(Component parent, String title, String message, Runnable runnable) {
        int result = JOptionPane.showConfirmDialog(
            parent, message, title, JOptionPane.YES_NO_CANCEL_OPTION);

        if (result == JOptionPane.YES_OPTION) runnable.run();
    }

    /**
     * Fully expand all possible paths in the specified tree.
     * The tree will be expanded as though the user had patiently clicked every "Expand branch" grapple from the
     * top down until there were no more left to click on.
     * <p/>
     * This must never be invoked on trees with infinite (or merely enormous)
     * dynamic tree models, for hopefully-obvious reasons.
     *
     * @param tree the tree to expand.  Must not be null.
     */
    public static void expandTree(JTree tree) {
        int erow = 0;
        while (erow < tree.getRowCount())
            tree.expandRow(erow++);
    }

    /**
     * Fully collapse all open paths in the specified tree.
     * The tree will be collapsed as though the user had patiently clicked every "Collapse branch" grapple from the
     * bottom up until there were no more left to click on.
     *
     * @param tree the tree to collapse.  Must not be null.
     */
    public static void collapseTree(JTree tree) {
        int erow = tree.getRowCount() - 1;
        while (erow >= 0)
            tree.collapseRow(erow--);
    }

    /**
     * Try to set alwaysOnTop for the specified dialog, taking no action if the security manager doesn't allow us
     * to do so.
     *
     * @param dialog  the dialog that should be always on top (if possible).
     * @param b       the desired new value for the alwaysOnTop property
     */
    public static void setAlwaysOnTop(JDialog dialog, boolean b) {
        try {
            dialog.setAlwaysOnTop(b);
        } catch (AccessControlException e) {
            // Probably running as applet
        }
    }

    /**
     * Get the closest JInternalFrame ancestor of the specified component, if any.
     *
     * @param c the component to examine.  Must not be null.
     * @return the nearest enclosing JInternalFrame, or null if not inside a JInternalFrame.
     */
    public static JInternalFrame getInternalFrameAncestor(Component c) {
        for (Container p = c.getParent(); p != null; p = p.getParent()) {
            if (p instanceof JInternalFrame) return (JInternalFrame)p;
        }
        return null;
    }

    /**
     * Get the closest RootPaneContainer ancestor of the specified component, if any.
     *
     * @param c the component to examine.  Must not be null.
     * @return the nearest enclosing RootPaneContainer, or null if not inside a RootPaneContainer.
     */
    public static RootPaneContainer getRootPaneContainerAncestor(Component c) {
        for (Container p = c.getParent(); p != null; p = p.getParent()) {
            if (p instanceof RootPaneContainer) return (RootPaneContainer)p;
        }
        return null;
    }


    /**
     * Set the title of the specified RootPaneContainer, if it is a Frame, Dialog, or JInternalFrame.
     * Takes no action if rpc is not one of these types or if it is null.
     *
     * @param rpc  the RootPaneContainer to examine, or null to take no action.
     * @param title  the new title to set.
     */
    public static void setTitle(RootPaneContainer rpc, String title) {
        if (rpc instanceof Frame) {
            Frame frame = (Frame)rpc;
            frame.setTitle(title);
        } else if (rpc instanceof Dialog) {
            Dialog dialog = (Dialog)rpc;
            dialog.setTitle(title);
        } else if (rpc instanceof JInternalFrame) {
            JInternalFrame jif = (JInternalFrame)rpc;
            jif.setTitle(title);
        }
    }


    /**
     * Disposes the specified RootPaneContainer, if it is a Window or a JInternalFrame.  Takes no action
     * if rpc is some other type, or null.
     *
     * @param rpc  the RootPaneContainer to dispose, or null to take no action.
     */
    public static void dispose(RootPaneContainer rpc) {
        if (rpc instanceof Window) {
            Window window = (Window)rpc;
            window.dispose();
        } else if (rpc instanceof JInternalFrame) {
            JInternalFrame jif = (JInternalFrame)rpc;
            jif.dispose();
        }
    }

    private static final ThreadLocal<Boolean> doingWithDelayedCancelDialog = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    private static final Map<Long, Boolean> threadsCanceledByCancelDialog = new ConcurrentHashMap<Long, Boolean>();

    /**
     * Check if the current thread is executing via {@link #doWithDelayedCancelDialog}.
     *
     * @return true iff. the current thread has a doWithDelayedCancelDialog call active.
     */
    public static boolean isCurrentThreadDoingWithDelayedCancelDialog() {
        return doingWithDelayedCancelDialog.get();
    }

    /**
     * Synchronously run the specified callable in a background thread, putting up a modal Cancel... dialog and returning
     * control to the user if the thread runs for longer than msBeforeDlg milliseconds.
     * <p/>
     * If the user cancels the dialog this will interrupt the background thread.  The callable may throw
     * InterruptedException to signal that this has occurred.
     * <p/>
     * This method must be called on the Swing event queue thread.
     *
     * @param callable      some work that may safely be done in a new thread.  Required.
     * @param cancelDlg     a cancel dialog to put up if the work runs too long.  This should be a modal dialog
     *                      that blocks until it is either disposed or the user dismisses it by pressing cancel.
     *                      <p/>
     *                      The dialog should be ready to display -- already packed and positioned.
     *                      This method will dispose the dialog when the callable completes.
     *                      <p/>
     *                      See <code>com.l7tech.console.panels.CancelableOperationDialog</code> for a suitable
     *                      JDialog for this purpose, if writing SSM code.
     * @param msBeforeDlg  number of milliseconds to wait (blocking the event queue) before
     *                                            putting up the cancel dialog.  If less than one, defaults to 500ms.
     * @return the result of the callable.  May be null if the callable may return null.
     * @throws InterruptedException if the task was canceled by the user, or the Swing thread was interrupted
     * @throws java.lang.reflect.InvocationTargetException if the callable terminated with any exception other than InterruptedException
     */
    public static <T> T doWithDelayedCancelDialog(final Callable<T> callable, final JDialog cancelDlg, long msBeforeDlg)
            throws InterruptedException, InvocationTargetException
    {
        if (callable == null || cancelDlg == null) throw new IllegalArgumentException();

        final Thread[] workerThread = { null };
        final boolean[] finished = { false };
        final Object cancelSentinel = new Object();
        final Object semaphore = new Object();

        SwingWorker worker = new SwingWorker() {
            public Object construct() {
                try {
                    Thread thisThread = Thread.currentThread();
                    workerThread[0] = thisThread;
                    doingWithDelayedCancelDialog.set(true);
                    threadsCanceledByCancelDialog.put(thisThread.getId(), false);
                    T t = callable.call(); // blocks until job complete or thread interrupted or throws
                    Boolean b = threadsCanceledByCancelDialog.get(thisThread.getId());
                    return (b == null ? false : b) ? cancelSentinel : t;
                } catch (InterruptedException e) {
                    return cancelSentinel;
                } catch (Throwable t) {
                    return new ThrowableHolder(t);
                } finally {
                    doingWithDelayedCancelDialog.set(false);
                    synchronized (semaphore) {
                        finished[0] = true;
                        semaphore.notifyAll();
                    }
                }
            }

            public void finished() {
                cancelDlg.dispose();
            }
        };

        worker.start();
        boolean done;
        synchronized (semaphore) {
            semaphore.wait(msBeforeDlg);  // blocks until job complete or msBeforeDialog elapsed
            done = finished[0];
        }

        boolean wasCanceled = false;
        if (!done) {
            cancelDlg.setVisible(true); // blocks until job complete or canceled

            // Cancel dialog returned.  Did job succeed or was it canceled?
            Thread wt;
            synchronized (semaphore) {
                done = finished[0];
                wt = workerThread[0];
            }
            if (!done) {
                // User hit cancel button
                if (wt != null) threadsCanceledByCancelDialog.put(wt.getId(), true);
                worker.interrupt();
                wasCanceled = true;
            }
        }

        // At this point Cancel dialog is either canceled, torn down by SwingWorker finishing, or was never displayed.
        Object result = wasCanceled ? cancelSentinel : worker.get(); // get may block until job complete

        if (result == cancelSentinel) {
            throw new InterruptedException("operation canceled by user");
        } else if (result instanceof ThrowableHolder) {
            throw new InvocationTargetException(((ThrowableHolder)result).t);
        }

        //noinspection unchecked
        return (T)result;
    }


    /**
     * Creates pop-up menus for text components.
     */
    public static interface ContextMenuFactory {
        /**
         * Create a pop-up menu for the given text component.
         *
         * @param textComponent  the component for which to create the menu.  Must not be null.
         * @return the created menu.  never null
         */
        JPopupMenu createContextMenu(JTextComponent textComponent);
    }

    /**
     * Responds to ActionEvents by sending a new ActionEvent to the specified action, with the specified
     * JTextComponent as its source.
     */
    private static class RetargetedAction extends AbstractAction {
        private final JTextComponent tc;
        private final Action wrapped;
        private final boolean mustBeEditable;

        public RetargetedAction(final JTextComponent tc, boolean mustBeEditable, Action wrapped) {
            this.tc = tc;
            this.wrapped = wrapped;
            this.mustBeEditable = mustBeEditable;
            copyValue(wrapped, Action.ACCELERATOR_KEY);
            copyValue(wrapped, Action.ACTION_COMMAND_KEY);
            copyValue(wrapped, Action.LONG_DESCRIPTION);
            copyValue(wrapped, Action.MNEMONIC_KEY);
            copyValue(wrapped, Action.NAME);
            copyValue(wrapped, Action.SHORT_DESCRIPTION);
            copyValue(wrapped, Action.SMALL_ICON);
        }

        private void copyValue(Action source, String key) {
            Object value = source.getValue(key);
            if (value != null) this.putValue(key, value);
        }

        public void actionPerformed(ActionEvent e) {
            if (mustBeEditable && !tc.isEditable())
                return;

            wrapped.actionPerformed(new ActionEvent(tc,
                                              ActionEvent.ACTION_PERFORMED,
                                              (String)wrapped.getValue(Action.NAME)));
        }
    }

    /**
     * Creates default pop-up menus for text components.
     */
    public static class DefaultContextMenuFactory implements ContextMenuFactory {
        /**
         * Create a context menu with appropriate items for the given JTextComponent.
         * The menu will always include a "Copy" option, but will include "Cut" and "Paste" only
         * if tc.isEditable() is true.
         *
         * @return A newly-created context menu, ready to pop up.
         */
        public JPopupMenu createContextMenu(final JTextComponent tc) {
            JPopupMenu contextMenu = new JPopupMenu();

            boolean sys = ClipboardActions.isSystemClipboardAvailable();

            if (sys && tc.isEditable() && shouldIncludeMenu(tc, CONTEXT_CUT)) {
                JMenuItem item = new JMenuItem(CONTEXT_CUT);
                item.addActionListener(new RetargetedAction(tc, true, ClipboardActions.getCutAction()));
                contextMenu.add(item);
            }

            if (sys && shouldIncludeMenu(tc, CONTEXT_COPY)) {
                JMenuItem item = new JMenuItem(CONTEXT_COPY);
                item.addActionListener(new RetargetedAction(tc, false, ClipboardActions.getCopyAction()));
                contextMenu.add(item);
            }

            if (sys && tc.isEditable() && shouldIncludeMenu(tc, CONTEXT_PASTE)) {
                JMenuItem item = new JMenuItem(CONTEXT_PASTE);
                item.addActionListener(new RetargetedAction(tc, true, ClipboardActions.getPasteAction()));
                contextMenu.add(item);
            }

            if (shouldIncludeMenu(tc, CONTEXT_SELECT_ALL)) {
                JMenuItem selectAllItem = new JMenuItem(CONTEXT_SELECT_ALL);
                selectAllItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        tc.selectAll();
                    }
                });
                if (contextMenu.getSubElements().length > 0)
                    contextMenu.add(new JSeparator());
                contextMenu.add(selectAllItem);
            }

            removeToolTipsFromMenuItems(contextMenu);

            return contextMenu;
        }

        /**
         * Allow subclasses to manually exclude certain menu items from appearing in a generated context menu.
         * This method always returns true.
         *
         * @param tc  the text component whose context menu we are generating.
         * @param menuText the text of the menu item we are about to include.
         * @return true if this menu item should be included, or false to exclude it.
         */
        protected boolean shouldIncludeMenu(JTextComponent tc, String menuText) {
            return true;
        }
    }

    /**
     * Set up cut/copy/paste keyboard shortcuts for the textcomponent that invoke the applet-friendly
     * actions instead of the default actions.  Shortcuts will always be added for cut, copy, and paste;
     * however, the actions for cut and paste will do nothing if the text component is not editable at
     * that time.
     *
     * @param tc  the text component whose ActionMap to adjust
     */
    public static void attachClipboardKeyboardShortcuts(final JTextComponent tc) {
        // If running as untrusted applet, our only hope is to let the default components do their default thing
        if (!ClipboardActions.isSystemClipboardAvailable())
            return;

        ClipboardActions.replaceClipboardActionMap(tc);
    }

    /**
     * Configure the specified text component with a default context menu containing Cut, Copy, Paste, and Select All.
     * @param tc the JTextComponent to configure.  Must not be null.
     */
    public static void attachDefaultContextMenu(final JTextComponent tc) {
        attachClipboardKeyboardShortcuts(tc);
        tc.addMouseListener(createContextMenuMouseListener(tc));
    }

    /**
     * Create a MouseListener that will create an edit context menu when triggered.  If the specified
     * component has the PROPERTY_CONTEXT_MENU_AUTO_SELECT_ALL client property set to "true" when
     * the listener is triggered, the component will have "select all" called on it first.  This method
     * always uses the DefaultContextMenuFactory.
     *
     * @param tc The JTextComponent to which this MouseListener will be attached
     * @return the newly created MouseListener
     */
    public static MouseListener createContextMenuMouseListener(final JTextComponent tc) {
        attachClipboardKeyboardShortcuts(tc);
        return createContextMenuMouseListener(tc, new DefaultContextMenuFactory());
    }

    /**
     * Create a MouseListener that will create an edit context menu when triggered.  The menu will be
     * created using the specified ContextMenuFactory.  See Utilities.DefaultContextMenuFactory
     * for an implementation that supports Cut/Copy/Paste/Select All. 
     *
     * @param tc      The JTextComponent to which this MouseListener will be attached
     * @param factory The ContextMenuFactory which will produce the actual context menu.
     * @return the newly created MouseListener
     */
    public static MouseListener createContextMenuMouseListener(final JTextComponent tc,
                                                               final ContextMenuFactory factory) {
        attachClipboardKeyboardShortcuts(tc);
        return new MouseAdapter() {
            public void mousePressed(final MouseEvent ev) {
                checkPopup(ev);
            }

            public void mouseReleased(final MouseEvent ev) {
                checkPopup(ev);
            }

            private void checkPopup(MouseEvent ev) {
                if (ev.isPopupTrigger()) {
                    tc.requestFocus();
                    String selectAll = (String)tc.getClientProperty(PROPERTY_CONTEXT_MENU_AUTO_SELECT_ALL);
                    if (Boolean.valueOf(selectAll).booleanValue())
                        tc.selectAll();
                    JPopupMenu menu = factory.createContextMenu(tc);
                    menu.show((Component)ev.getSource(), ev.getX(), ev.getY());
                }
            }
        };
    }

    /**
     * Configure the specified component to run the specified action when the ESCAPE key is pressed.
     * (as long as the component gets the keystroke, and not some other component)
     *
     * @param comp    the component whose input and action maps to adjust.  must not be null
     * @param action  the action to invoke.  Must not be null
     */
    public static void runActionOnEscapeKey(JComponent comp, Action action) {
        KeyStroke ek = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ek, KEY_ESCAPE);
        comp.getActionMap().put(KEY_ESCAPE, action);
    }

    /**
     * Configure the specified JTextComponent (text field or text area) to automatically do "Select All" whenever
     * it gains keyboard focus.
     *
     * @param comp the component on which selectAll() should be invoked whenever it gains focus.  Must not be null.
     */
    public static void enableSelectAllOnFocus(final JTextComponent comp) {
        comp.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                comp.selectAll();
            }

            public void focusLost(FocusEvent e) {
            }
        });
    }

    /**
     * Load the last window status (size and location) and use the status to set the current window.
     * Note: (1) do not call the method centerOnParentWindow() after this method.
     *       (2) call this method after all components have been embedded in the window.
     * @param window: the current window
     * @param properties: store the window size and location.
     * @param defaultWidth: the default width of a window.
     * @param defaultHeight: the default height of a window.
     */
    public static void restoreWindowStatus(JFrame window, Properties properties, int defaultWidth, int defaultHeight) {
        // Load the last window size if appliable.
        String widthPropName  = "last." + window.getClass().getSimpleName() + ".size.width";
        String heightPropName = "last." + window.getClass().getSimpleName() + ".size.height";
        int width  = Integer.parseInt(properties.getProperty(widthPropName, String.valueOf(defaultWidth)));
        int height = Integer.parseInt(properties.getProperty(heightPropName, String.valueOf(defaultHeight))) ;
        window.setPreferredSize(new Dimension(width, height));

        // Check all components and set their preferred size.
        Component[] components = window.getContentPane().getComponents();
        for (Component subComp: components) {
            if(subComp instanceof JComponent) {
                subComp.setPreferredSize(new Dimension(width, height));
            }
        }

        // Load the last window location if appliable.
        String xPropName = "last." + window.getClass().getSimpleName() + ".location.x";
        String yPropName = "last." + window.getClass().getSimpleName() + ".location.y";
        try {
            int x = Integer.parseInt(properties.getProperty(xPropName));
            int y = Integer.parseInt(properties.getProperty(yPropName));
            window.setLocation(x, y);
        } catch (NumberFormatException ex) {
            // If there doesn't exist x or y properties, then showing the window in the center.
            centerOnParentWindow(window);
        }
    }

    /**
     * Collect the window status such as size and location.
     * @param window: the current window.
     * @return size and location properties.
     */
    public static Properties getWindowStatus(JFrame window) {
        String widthPropName  = "last." + window.getClass().getSimpleName() + ".size.width";
        String heightPropName = "last." + window.getClass().getSimpleName() + ".size.height";
        String xPropName = "last." + window.getClass().getSimpleName() + ".location.x";
        String yPropName = "last." + window.getClass().getSimpleName() + ".location.y";

        Properties prop = new Properties();
        prop.put(widthPropName, String.valueOf(window.getWidth()));
        prop.put(heightPropName, String.valueOf(window.getHeight()));
        prop.put(xPropName, String.valueOf(window.getX()));
        prop.put(yPropName, String.valueOf(window.getY()));

        return prop;
    }

    private static final Color DISABLED_FOREGROUND_COLOR = Color.GRAY;
    private static final Color DISABLED_BACKGROUND_COLOR = new Color(232, 232, 232);

    /**
     * Enable/disable a component and its children.
     *
     * @param component the component to be enabled/disabled
     * @param enabled true to enable the component
     */
    public static void setEnabled(JComponent component, boolean enabled) {
        if(component != null) {
            // this component
            component.setEnabled(enabled);

            // children
            Component[] components = component.getComponents();
            for(int c=0; c<components.length; c++) {
                Component subComp = components[c];
                if(subComp instanceof JComponent) {
                    setEnabled((JComponent) subComp, enabled);
                }
            }
        }
    }

    /**
     * Set the font for a component and its children.
     *
     * @param component the component whose font is to be set, or null to take no action.
     * @param font  the font to set.  must not be null
     */
    public static void setFont(JComponent component, Font font) {
        if(component != null) {
            // this component
            component.setFont(font);

            // children
            Component[] components = component.getComponents();
            for(int c=0; c<components.length; c++) {
                Component subComp = components[c];
                if(subComp instanceof JComponent) {
                    setFont((JComponent) subComp, font);
                }
            }
        }
    }

    /**
     * Configure the specified component to change its foreground color to Gray whenever it is disabled.
     *
     * @param component the component whose behaviour will be altered
     */
    public static void enableGrayOnDisabled(JComponent component) {
        final Color defaultForeground = component.getForeground();
        final Color defaultBackground = component.getBackground();
        component.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() instanceof JComponent && "enabled".equals(evt.getPropertyName())) {
                    JComponent component = (JComponent)evt.getSource();
                    boolean enabled = ((Boolean)evt.getNewValue()).booleanValue();
                    component.setForeground(enabled ? defaultForeground : DISABLED_FOREGROUND_COLOR);
                    component.setBackground(enabled ? defaultBackground : DISABLED_BACKGROUND_COLOR);
                }
            }
        });
        component.setForeground(component.isEnabled() ? defaultForeground : DISABLED_FOREGROUND_COLOR);
        component.setBackground(component.isEnabled() ? defaultBackground : DISABLED_BACKGROUND_COLOR);
    }

    /**
     * Returns the bounds of the specified <code>String</code> in the
     * <code>Graphics</code> context of the specified <code>Component</code>.
     *
     * @param c    the component that is used to determine the
     *             <code>Graphics</code> context
     * @param f    - the font for which font metrics is to be obtained
     * @param str the specified <code>String</code> to render
     * @return a {@link Rectangle2D} that is the bounding box of the
     *         specified <code>String</code> in the specified
     *         component's <code>Graphics</code> context.
     */
    public static Rectangle2D getStringBounds(Component c, Font f, String str) {
        FontMetrics metrics = c.getFontMetrics(f);
        Graphics g = c.getGraphics();
        return metrics.getStringBounds(str, g);
    }

    /**
     * Remove tooltips from all menu items in the specified menu element.
     *
     * @param m the menu to scrub of tooltips along with all submenus.  Must not be null
     */
    public static void removeToolTipsFromMenuItems(MenuElement m) {
        MenuElement[] subElements = m.getSubElements();
        for (int i = 0; i < subElements.length; i++) {
            MenuElement subElement = subElements[i];
            removeToolTipsFromMenuItems(subElement);
        }

        if (m instanceof JComponent)
            ((JComponent)m).setToolTipText(null);
    }

    /**
     * Convert the given object to tooltip text.
     *
     * <p>This first calls toString() on the object.</p>
     *
     * <p>If escapeNewLines is true then any "\n" is converted to a "<br>"</p>
     *
     * <p>The resulting text is wrapped in "<html>" tags.</p>
     *
     * @param valueObject The object to convert.
     * @param escapeNewLines true to escape new lines
     * @return null if valueObject was null or the tooltip text
     */
    public static String toTooltip(Object valueObject, boolean escapeNewLines) {
        return toTooltip(new Object[]{valueObject}, escapeNewLines);
    }

    /**
     * Convert the given object array to tooltip text.
     *
     * <p>This first calls toString() on each object.</p>
     *
     * <p>If escapeNewLines is true then any "\n" is converted to a "<br>"</p>
     *
     * <p>The resulting text is wrapped in "<html>" tags.</p>
     *
     * @param valueObjects The objects to convert.
     * @param escapeNewLines true to escape new lines
     * @return null if valueObject was null or the tooltip text
     */
    public static String toTooltip(Object[] valueObjects, boolean escapeNewLines) {
        String tooltipText = null;

        if (valueObjects != null) {
            StringBuffer textBuffer = null;

            for (int o=0; o<valueObjects.length; o++) {
                Object object = valueObjects[o];
                if (object != null) {
                    if (textBuffer == null) {
                        textBuffer = new StringBuffer();
                        textBuffer.append("<html>");
                    }

                    String objectText = object.toString();
                    if (escapeNewLines) {
                        textBuffer.append(objectText.replace("\n", "<br>"));
                    }
                    else {
                        textBuffer.append(objectText);
                    }

                    if (o<valueObjects.length-1) {
                        textBuffer.append("<br>");
                    }
                }
            }

            if (textBuffer != null) {
                textBuffer.append("</html>");
                tooltipText = textBuffer.toString();
            }
        }

        return tooltipText;
    }

    /**
     * Synchronously invokes the specified runnable on the Swing thread.
     * If this thread is the Swing thread, just runs the runnable directly.
     * Any exceptions thrown by the invocation, including InterruptedException, will be rethrown wrapped
     * as RuntimeException.
     *
     * @param runnable the runnable to run on the swing thread.  Must not be null.
     * @throws RuntimeException if the runnable threw an unchecked exception.  The original exception is wrapped.
     *                          <p/>if the current thread was interrupted while waiting for the target thread.
     *         The InterruptedException is wrapped, and the interrupt status has been reasserted for this thread.
     */
    public static void invokeOnSwingThreadAndWait(Runnable runnable) throws RuntimeException {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Safely return the default toolkit's system selection, or null if there isn't one or it's unavailable.
     *
     * @return  the system selection, or null if nonexistent or unavailable
     * @see java.awt.Toolkit#getSystemSelection()
     */
    public static Clipboard getDefaultSystemSelection() {
        try {
            return Toolkit.getDefaultToolkit().getSystemSelection();
        } catch (AccessControlException e) {
            // Very restrictive security manager
            return null;
        }
    }

    /**
     * Get an ActionListener that dispatches the action to a list of listeners.
     *
     * @param component the component that must be enabled for the dispatch to occur
     * @param listeners The listeners to pass the event
     * @return The dispatching listener           
     */
    public static ActionListener getDispatchingActionListener(final Component component, final ActionListener[] listeners) {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if ( component.isEnabled() ) {
                    if (listeners != null) {
                        for (ActionListener listener : listeners) {
                            listener.actionPerformed(e);
                        }
                    }
                }
            }
        };
    }

    private static final Method methodSetIconImages;
    private static final Method methodGetIconImages;
    static {
        final Method[] getMethod = new Method[]{null};
        final Method[] setMethod = new Method[]{null};

        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    getMethod[0] = Window.class.getMethod("getIconImages", new Class[0]);
                    setMethod[0] = Window.class.getMethod("setIconImages", new Class[] { java.util.List.class });
                } catch (NoSuchMethodException e) {
                    // No can do
                } catch (AccessControlException e) {
                    // No can do
                }
                return null;
            }
        });

        methodSetIconImages = setMethod[0];
        methodGetIconImages = getMethod[0];
    }

    /**
     * Safely get the icon images for the specified window, if running under a supported JRE (Java 1.6 or higher).
     *
     * @param window  the window whose icon images to get.  Must not be null.
     * @return a List of Image instances of various sizes to be used for the Window's frame and taskbar icon.
     *         May be null or empty if not set or running with a pre-1.6 JRE.
     */
    public static java.util.List getIconImages(final Window window) {
        if (methodGetIconImages == null) return null;

        return (java.util.List)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    return methodGetIconImages.invoke(window, new Object[0]);
                } catch (IllegalAccessException e) {
                    return null;
                } catch (InvocationTargetException e) {
                    return null;
                } catch (AccessControlException e) {
                    return null;
                }
            }
        });
    }

    /**
     * Safely set the icon images for the specified window, if running under a supported JRE (Java 1.6 or higher).
     *
     * @param window  the window whose icon images to set.  Must not be null.
     * @param images  a List of Image instances.  Must not be null or empty, and must not contain nulls.
     * @return true if the image list was set; false if the operation was not supported.
     */
    public static boolean setIconImages(final Window window, final java.util.List images) {
        if (methodSetIconImages == null) return false;

        Boolean result = (Boolean)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    methodSetIconImages.invoke(window, new Object[] { images });
                    return Boolean.TRUE;
                } catch (IllegalAccessException e) {
                    return Boolean.FALSE;
                } catch (InvocationTargetException e) {
                    return Boolean.FALSE;
                } catch (AccessControlException e) {
                    return Boolean.FALSE;
                }
            }
        });
        return result != null && result.booleanValue();
    }

    /**
     * Convert a row index to a table model index.
     *
     * <p>This will do nothing on JDK 1.5, on JDK 1.6 it will delegate to the
     * new JTable method.</p>
     *
     * @param table The table with the row
     * @param row The table row index
     * @return The model row index
     */
    public static int convertRowIndexToModel( JTable table, int row ) {
        return table.convertRowIndexToModel( row );
    }

    /**
     * Set a row sorter on the given table if the JDK supports it.
     *
     * @param table The table to be sorted.
     * @param model The model for the table.
     */
    public static void setRowSorter( JTable table, TableModel model ) throws IllegalArgumentException, NullPointerException{
       setRowSorter( table, model, null, null, null);
    }

    /**
     * Set a row sorter on the given table if the JDK supports it.
     *
     * @param table The table to be sorted.
     * @param model The model for the table.
     * @param cols The columns to be sorted  (if null, order must also be null, if not null length must equal order param length)
     * @param order The ascending (true) or descending (false) sorts for the columns (if null, cols must also be null). Index matches
     * the index of cols array. Length must be the same.
     * @param comparators The comparator to use for sorting the cols in the param cols. The comparator in comparators
     * at index i matches the column at index i in cols. Null values are allowed if a column does not need a comparator.
     * Must supply an array the length of cols
     * @throws IllegalArgumentException if the length of the comparators array, if specified, and the order array
     * does not match the length of the cols array
     * @throws NullPointerException if one of the cols or order arrays are null 
     */
    public static void setRowSorter( JTable table, TableModel model, int[] cols, boolean[] order,
                                     Comparator [] comparators ) throws IllegalArgumentException, NullPointerException{
        TableRowSorter sorter = new TableRowSorter(model);
        if ( cols != null && order != null ) {
            if( cols.length != order.length){
                throw new IllegalArgumentException("Length of order array must match length of cols array.");
            }

            if(comparators != null){
                if(cols.length != comparators.length){
                    throw new IllegalArgumentException("Length of comparators array must match length of cols array.");
                }
            }

            java.util.List <RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
            for ( int i=0; i< cols.length; i++ ) {
                sortKeys.add(new RowSorter.SortKey(cols[i], order[i]?SortOrder.ASCENDING:SortOrder.DESCENDING));
            }
            sorter.setSortKeys(sortKeys);

            if( comparators != null){
                for(int i = 0; i < comparators.length; i++){
                    if(comparators[i] != null){
                        sorter.setComparator(i, comparators[i]);
                    }
                }
            }
        }

        table.setRowSorter(sorter);
        sorter.sort();    }

    /**
     * Table string converter interface for use when sorting.
     */
    public interface TableStringConverter {
        String toString(TableModel model, int row, int column);
    }

    private static class ThrowableHolder {
        private final Throwable t;
        private ThrowableHolder(Throwable t) { this.t = t; }
    }
}
