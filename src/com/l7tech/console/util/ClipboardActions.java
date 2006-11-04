/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Holds global clipboard actions for cut/copy/paste and manages their enable/disable state.  The global
 * actions will, when invoke, deliver "cut" "copy" or "paste" action commands to the component that most
 * recently owned the permanent focus.
 * <p/>
 * The actions exported from this class are not thread safe and must be used only on the Swing thread.
 */
public class ClipboardActions {
    private static final Logger logger = Logger.getLogger(ClipboardActions.class.getName());

    /**
     * Global "cut" action, enabled if current focus owner has a transfer handler allowing cut.
     * <p/>
     * Your app may want to customize the SHORT_DESCRIPTION and LONG_DESCRIPTION properties of this action.
     */
    public static final Action CUT_ACTION = new ClipboardMutatingProxyAction(TransferHandler.getCutAction(),
                                                            KeyEvent.VK_T,
                                                            KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));

    /**
     * Global "copy" action, enabled if current focus owner has a transfedr handler allowing copy.
     * <p/>
     * Your app may want to customize the SHORT_DESCRIPTION and LONG_DESCRIPTION properties of this action.
     */
    public static final Action COPY_ACTION = new ClipboardMutatingProxyAction(TransferHandler.getCopyAction(),
                                                            KeyEvent.VK_C,
                                                            KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));

    /**
     * Global "copyAll" action, enabled if current focus owner has a transfer handler allowing copy and
     * has specifically added "copyAll" to its actionMap.
     * <p/>
     * Your app may want to customize the SHORT_DESCRIPTION and LONG_DESCRIPTION properties of this action.
     */
    public static final Action COPY_ALL_ACTION = new ClipboardMutatingProxyAction("copyAll",
                                                            KeyEvent.VK_L,
                                                            KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));

    /**
     * Global "paste" action, enabled if current focus owner has a transfer handler that will accept paste of
     * any dataflavor currently on the clipboard.
     * <p/>
     * If the system clipboard isn't available, this action will always be enabled if the focus owner has
     * a transfer handler.
     * <p/>
     * Your app may want to customize the SHORT_DESCRIPTION and LONG_DESCRIPTION properties of this action.
     */
    public static final Action PASTE_ACTION = new ProxyAction(TransferHandler.getPasteAction(),
                                                            KeyEvent.VK_P,
                                                            KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));

    private static boolean focusListenerInstalled = false;
    private static WeakReference<JComponent> focusOwner = null;
    private static boolean noClipAccess = false;
    private static boolean checkedClipboard = false;
    private static DataFlavor[] clipboardFlavors = null;
    private static final String cutName = (String)CUT_ACTION.getValue(Action.NAME);
    private static final String copyName = (String)COPY_ACTION.getValue(Action.NAME);
    private static final String copyAllName = (String)COPY_ALL_ACTION.getValue(Action.NAME);
    private static final String pasteName = (String)PASTE_ACTION.getValue(Action.NAME);

    /**
     * Set up our static focus and clipboard listeners, if we haven't already done so.
     */
    private static void maybeInstallGlobalListeners() {
        if (!focusListenerInstalled) {
            KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            kfm.addPropertyChangeListener("permanentFocusOwner", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    Object fo = evt.getNewValue();
                    if (fo instanceof JComponent) {
                        focusOwner = new WeakReference<JComponent>((JComponent)fo);
                        updateClipboardActions();
                    }
                }
            });

            Clipboard clip = getClipboard();
            if (clip != null) {
                clip.addFlavorListener(new FlavorListener() {
                    public void flavorsChanged(FlavorEvent e) {
                        final Clipboard clip = (Clipboard)e.getSource();
                        updateClipboardFlavors(clip);
                        updateClipboardActions();
                    }
                });
                updateClipboardFlavors(clip);
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Clipboard clip = getClipboard();
                    if (clip != null) updateClipboardFlavors(clip);
                    updateClipboardActions();
                }
            });

            focusListenerInstalled = true;
        }
    }

    private static void updateClipboardFlavors(final Clipboard clip) {
        try {
            clipboardFlavors = clip == null ? null : clip.getAvailableDataFlavors();
            logger.info("Updated cached clipboard flavors for " + clip + ": " + clipboardFlavors);
        } catch (IllegalStateException es) {
            // Windows clipboard is busy (or maybe just non-reentrant).  Do it later.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        clipboardFlavors = clip == null ? null : clip.getAvailableDataFlavors();
                        logger.info("Updated cached clipboard flavors for " + clip + ": " + clipboardFlavors);
                    } catch (IllegalStateException e) {
                        // Well, at least we tried (and retried, even)
                        clipboardFlavors = null;
                        logger.info("Updated cached clipboard flavors for " + clip + ": " + clipboardFlavors + " (gave up - system clipboard was too busy)");
                    }
                }
            });
        }
    }

    /**
     * Update the clipboard actions enable state in response to a change in either the currently focused component or
     * the contents of the clipboard.
     */
    private static void updateClipboardActions() {
        boolean acceptCut = false;
        boolean acceptCopy = false;
        boolean acceptCopyAll = false;
        boolean acceptPaste = false;

        try {
            if (focusOwner == null)
                return;
            JComponent jc = focusOwner.get();
            if (jc == null)
                return;
            ActionMap am = jc.getActionMap();
            if (am == null)
                return;
            TransferHandler th = jc.getTransferHandler();
            if (th == null)
                return;

            int actions = th.getSourceActions(jc);
            acceptCopy = am.get(copyName) != null && ((actions & TransferHandler.COPY) != TransferHandler.NONE);
            acceptCopyAll = am.get(copyAllName) != null && acceptCopy;
            acceptCut = am.get(cutName) != null && ((actions & TransferHandler.MOVE) != TransferHandler.NONE);
            acceptPaste = am.get(pasteName) != null && (noClipAccess || (clipboardFlavors != null && th.canImport(jc, clipboardFlavors)));
            logger.fine("focus owner: " + jc.getClass().getName() + "  paste action:" + (am.get(pasteName) != null) + "  clipboard=" + getClipboard() + "  flavors:" + clipboardFlavors);
        } finally {
            CUT_ACTION.setEnabled(acceptCut);
            COPY_ACTION.setEnabled(acceptCopy);
            COPY_ALL_ACTION.setEnabled(acceptCopyAll);
            PASTE_ACTION.setEnabled(acceptPaste);
        }
    }

    /**
     * @return the system Clipboard, if accessible; otherwise null.
     */
    private static Clipboard getClipboard() {
        if (noClipAccess) return null;
        if (checkedClipboard) return Toolkit.getDefaultToolkit().getSystemClipboard();
        checkedClipboard = true;

        if (GraphicsEnvironment.isHeadless()) {
            // (Of course, nothing else in the SSM is likely to work, either..)
            noClipAccess = true;
            return null;
        }
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkSystemClipboardAccess();
            } catch (SecurityException e) {
                noClipAccess = true;
                return null;
            }
        }
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    /**
     * Look up the specified actionCommand in the currently-focused component's action map and dispatch that
     * action.
     * <p/>
     * Takes no action if there is no currently-focused component or the currently-focused component doesn't
     * map the specified action command.
     *
     * @param actionCommand the action to dispatch, for example "cut".  Must not be null.
     */
    private static void runActionCommandOnFocusedComponent(String actionCommand) {
        if (focusOwner == null)
            return;
        JComponent focusComponent = focusOwner.get();
        if (focusComponent == null)
            return;

        Action a = focusComponent.getActionMap().get(actionCommand);
        if (a != null) {
            a.actionPerformed(new ActionEvent(focusComponent,
                                              ActionEvent.ACTION_PERFORMED,
                                              null));
        }
    }

    /**
     * Creates an action that will invoke the specified action's actionCommand against whichever component
     * currently has focus when the action is fired.
     */
    private static class ProxyAction extends AbstractAction {
        private final String actionCommand;

        public ProxyAction(String actionCommand, int mnemonic, KeyStroke accelerator) {
            super(actionCommand);
            this.actionCommand = actionCommand;
            if (mnemonic > 0) putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
            if (accelerator != null) putValue(Action.ACCELERATOR_KEY, accelerator);
            maybeInstallGlobalListeners();
        }

        public ProxyAction(Action actionToRun, int mnemonic, KeyStroke accelerator) {
            this((String)actionToRun.getValue(Action.NAME), mnemonic, accelerator);
        }

        public void actionPerformed(ActionEvent e) {
            if (logger.isLoggable(Level.FINE)) logger.fine("Dispatching action command: " + actionCommand);
            runActionCommandOnFocusedComponent(actionCommand);
            afterActionPerformed();
        }

        protected void afterActionPerformed() {
        }
    }

    private static class ClipboardMutatingProxyAction extends ProxyAction {
        public ClipboardMutatingProxyAction(String actionCommand, int mnemonic, KeyStroke accelerator) {
            super(actionCommand, mnemonic, accelerator);
        }

        public ClipboardMutatingProxyAction(Action actionToRun, int mnemonic, KeyStroke accelerator) {
            super(actionToRun, mnemonic, accelerator);
        }

        protected void afterActionPerformed() {
            updateClipboardFlavors(getClipboard());
            updateClipboardActions();
        }
    }
}

