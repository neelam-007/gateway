/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gui.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.AccessControlException;

/**
 * Holds global clipboard actions for cut/copy/paste and manages their enable/disable state.  The global
 * actions will, when invoke, deliver "cut" "copy" or "paste" action commands to the component that most
 * recently owned the permanent focus.
 * <p/>
 * The actions exported from this class are not thread safe and must be used only on the Swing thread.
 * @noinspection unchecked,ForLoopReplaceableByForEach,UnnecessaryUnboxing,UnnecessaryBoxing
 */
public class ClipboardActions {
    private static final Logger logger = Logger.getLogger(ClipboardActions.class.getName());

    private static final Action L7_CUT_ACTION = new L7TransferAction("l7cut", KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
    private static final Action L7_COPY_ACTION = new L7TransferAction("l7copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
    private static final Action L7_PASTE_ACTION = new L7TransferAction("l7paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));


    private static final Action GLOBAL_CUT_ACTION = new ClipboardMutatingProxyAction(
            getCutAction(),
            TransferHandler.getCutAction(),
            KeyEvent.VK_T,
            KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));

    private static final Action GLOBAL_COPY_ACTION = new ClipboardMutatingProxyAction(
            getCopyAction(),
            TransferHandler.getCopyAction(),
            KeyEvent.VK_C,
            KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));

    private static final Action GLOBAL_COPY_ALL_ACTION = new ClipboardMutatingProxyAction(
            "copyAll",
            KeyEvent.VK_L,
            KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));

    private static final Action GLOBAL_PASTE_ACTION = new ProxyAction(
            getPasteAction(),
            TransferHandler.getPasteAction(),
            KeyEvent.VK_P,
            KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));


    /** Explicit hint, as a client property.  May be "true" or "false".  If not present will guess. */
    public static final String CUT_HINT = "com.l7tech.clipboard.cut.enable";

    /** Explicit hint, as a client property.  May be "true" or "false".  If not present will guess. */
    public static final String COPY_HINT = "com.l7tech.clipboard.copy.enable";

    /** Explicit hint, as a client property.  May be "true" or "false".  If not present will guess. */
    public static final String PASTE_HINT = "com.l7tech.clipboard.paste.enable";


    private static boolean focusListenerInstalled = false;
    private static WeakReference focusOwner = null;
    private static boolean noClipAccess = false;
    private static boolean checkedClipboard = false;
    private static DataFlavor[] clipboardFlavors = null;
    private static Clipboard lastSystemClipboard = null;
    private static final FlavorListener FLAVOR_LISTENER = new FlavorListener() {
        public void flavorsChanged(FlavorEvent e) {
            final Clipboard clip = (Clipboard)e.getSource();
            updateClipboardFlavors(clip);
            updateClipboardActions();
        }
    };


    /**
     * Global "cut" action, enabled if current focus owner has a transfer handler allowing cut.
     * <p/>
     * Your app may want to customize the SHORT_DESCRIPTION and LONG_DESCRIPTION properties of this action.
     *
     * @return a proxy action that runs "cut" on the currently-focused component.
     */
    public static Action getGlobalCutAction() {
        return GLOBAL_CUT_ACTION;
    }

    /**
     * Global "copy" action, enabled if current focus owner has a transfedr handler allowing copy.
     * <p/>
     * Your app may want to customize the SHORT_DESCRIPTION and LONG_DESCRIPTION properties of this action.
     *
     * @return a proxy action that runs "copy" on the currently-focused component.
     */
    public static Action getGlobalCopyAction() {
        return GLOBAL_COPY_ACTION;
    }

    /**
     * Global "copyAll" action, enabled if current focus owner has a transfer handler allowing copy and
     * has specifically added "copyAll" to its actionMap.
     * <p/>
     * Your app may want to customize the SHORT_DESCRIPTION and LONG_DESCRIPTION properties of this action.
     *
     * @return a proxy action that runs "copyAll" on the currently-focused component.
     */
    public static Action getGlobalCopyAllAction() {
        return GLOBAL_COPY_ALL_ACTION;
    }

    /**
     * Global "paste" action, enabled if current focus owner has a transfer handler that will accept paste of
     * any dataflavor currently on the clipboard.
     * <p/>
     * If the system clipboard isn't available, this action will always be enabled if the focus owner has
     * a transfer handler.
     * <p/>
     * Your app may want to customize the SHORT_DESCRIPTION and LONG_DESCRIPTION properties of this action.
     *
     * @return a proxy action that runs "copy" on the currently-focused component.
     */
    public static Action getGlobalPasteAction() {
        return GLOBAL_PASTE_ACTION;
    }

    private static final boolean initialClipboardAccess = getClipboard() != null;

    /** @return a version of TransferHandler.getCutAction() that works when running as signed applet. */
    public static Action getCutAction() {
        return initialClipboardAccess ? L7_CUT_ACTION : TransferHandler.getCutAction();
    }

    /** @return a version of TransferHandler.getCopyAction() that works when running as signed applet. */
    public static Action getCopyAction() {
        return initialClipboardAccess ? L7_COPY_ACTION : TransferHandler.getCopyAction();
    }

    /** @return a version of TransferHandler.getPasteAction() that works when running as signed applet. */
    public static Action getPasteAction() {
        return initialClipboardAccess ? L7_PASTE_ACTION : TransferHandler.getPasteAction();
    }

    /** @return true if the system clipboard is available from the security context containing this ClipboardActions class, if privileges are asserted. */
    public static boolean isSystemClipboardAvailable() {
        return initialClipboardAccess;
    }

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
                        focusOwner = new WeakReference(fo);
                        updateClipboardActions();
                    }
                }
            });

            Clipboard clip = getClipboard();
            if (clip != null) {
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
            clipboardFlavors = getFlavors(clip);
            logger.info("Updated cached clipboard flavors for " + clip + ": " + clipboardFlavors);
        } catch (IllegalStateException es) {
            // Windows clipboard is busy (or maybe just non-reentrant).  Do it later.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        clipboardFlavors = getFlavors(clip);
                        logger.info("Updated cached clipboard flavors for " + clip + ": " + clipboardFlavors);
                    } catch (IllegalStateException e) {
                        // Well, at least we tried (and retried, even)
                        clipboardFlavors = null;
                        logger.warning("Failed to update cached clipboard flavors for " + clip + ": " + clipboardFlavors + " (gave up - system clipboard was too busy)");
                    }
                }
            });
        }
    }

    private static DataFlavor[] getFlavors(final Clipboard clip) throws IllegalStateException {
        return (DataFlavor[])AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return clip == null ? null : clip.getAvailableDataFlavors();
            }
        });
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
            JComponent jc = (JComponent)focusOwner.get();
            if (jc == null)
                return;
            ActionMap am = jc.getActionMap();
            if (am == null)
                return;
            TransferHandler th = jc.getTransferHandler();
            if (th == null)
                return;

            int actions = th.getSourceActions(jc);

            Boolean copyHint = checkProp(jc, COPY_HINT);
            if (copyHint != null)
                acceptCopy = copyHint.booleanValue();
            else
                acceptCopy = hasAction(am, "l7copy", "copy") && ((actions & TransferHandler.COPY) != TransferHandler.NONE);

            acceptCopyAll = hasAction(am, "copyAll", null) && acceptCopy;

            Boolean cutHint = checkProp(jc, CUT_HINT);
            if (cutHint != null)
                acceptCut = cutHint.booleanValue();
            else
                acceptCut = hasAction(am, "l7cut", "cut") && ((actions & TransferHandler.MOVE) != TransferHandler.NONE);

            Boolean pasteHint = checkProp(jc, PASTE_HINT);
            if (pasteHint != null)
                acceptPaste = pasteHint.booleanValue();
            else
                acceptPaste = hasAction(am, "l7paste", "paste") && (noClipAccess || (clipboardFlavors != null && th.canImport(jc, clipboardFlavors)));

            logger.fine("focus owner: " + jc.getClass().getName() + "  paste action:" + (hasAction(am, "l7paste", "paste")) + "  clipboard=" + getClipboard() + "  flavors:" + clipboardFlavors);
        } finally {
            GLOBAL_CUT_ACTION.setEnabled(acceptCut);
            GLOBAL_COPY_ACTION.setEnabled(acceptCopy);
            GLOBAL_COPY_ALL_ACTION.setEnabled(acceptCopyAll);
            GLOBAL_PASTE_ACTION.setEnabled(acceptPaste);
        }
    }

    private static boolean hasAction(ActionMap am, String a, String b) {
        return am.get(a) != null || (b != null && am.get(b) != null);
    }

    private static Boolean checkProp(JComponent jc, String prop) {
        Object value = jc.getClientProperty(prop);
        if (value instanceof String) {
            String s = (String)value;
            return Boolean.valueOf("true".equalsIgnoreCase(s));
        }
        return null;
    }

    /**
     * @return the system Clipboard, if accessible; otherwise null.
     */
    public static Clipboard getClipboard() {
        if (noClipAccess) return null;
        if (checkedClipboard) return getSystemClipboard();
        checkedClipboard = true;

        if (GraphicsEnvironment.isHeadless()) {
            // (Of course, nothing else in the SSM is likely to work, either..)
            noClipAccess = true;
            logger.info("Running headless - no clipboard access");
            return null;
        }

        Clipboard sys = getSystemClipboard();
        logger.info(sys == null ? "No access to system clipboard" : "Will use system clipboard");
        return sys;
    }

    private static Clipboard getSystemClipboard() {
        return (Clipboard)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    Clipboard old = lastSystemClipboard;
                    Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                    lastSystemClipboard = clip;
                    if (clip != null && clip != old) {
                        FlavorListener[] flavs = clip.getFlavorListeners();
                        for (int i = 0; i < flavs.length; i++) {
                            FlavorListener flav = flavs[i];
                            if (flav == FLAVOR_LISTENER)
                                return clip;
                        }
                        clip.addFlavorListener(FLAVOR_LISTENER);
                    }
                    return clip;
                } catch (AccessControlException ace) {
                    noClipAccess = true;
                    return null;
                }
            }
        });
    }

    /**
     * Look up the specified actionCommand in the currently-focused component's action map and dispatch that
     * action.
     * <p/>
     * Takes no action if there is no currently-focused component or the currently-focused component doesn't
     * map the specified action command.
     *
     * @param actionCommand the action to dispatch, for example "l7cut".  Must not be null.
     * @param backupActionCommand  action to fall back to if the primary action command is unmapped, for example "cut",
     *                             or null if there's no fallback option.
     */
    private static void runActionCommandOnFocusedComponent(String actionCommand, String backupActionCommand) {
        if (focusOwner == null)
            return;
        JComponent focusComponent = (JComponent)focusOwner.get();
        if (focusComponent == null)
            return;

        final ActionMap actionMap = focusComponent.getActionMap();
        Action a = actionMap.get(actionCommand);
        if (a == null && backupActionCommand != null) {
            actionCommand = backupActionCommand;
            a = actionMap.get(actionCommand);
        }
        if (a != null) {
            a.actionPerformed(new ActionEvent(focusComponent,
                                              ActionEvent.ACTION_PERFORMED,
                                              actionCommand));
        }
    }

    /**
     * Installs Ctrl-C, Ctrl-V, Ctrl-X keyboard shortcuts into the specified component's action map that
     * invoke ClipboardActions.getCopyAction(), .getPasteAction(), and .getCutAction(), respectively.
     *
     * @param component  the component whose ActionMap to adjust
     */
    public static void replaceClipboardActionMap(JComponent component) {
        ActionMap map = component.getActionMap();
        final Action copyAction = getCopyAction();
        final Object copyName = copyAction.getValue(Action.NAME);
        map.put(copyName, copyAction);
        final Action cutAction = getCutAction();
        final Action pasteAction = getPasteAction();
        if (!noCut(component))
            map.put(cutAction.getValue(Action.NAME), cutAction);
        map.put(pasteAction.getValue(Action.NAME), pasteAction);
        if (!"copy".equals(copyName)) {
            // Make sure standard names are hooked up as well
            map.put("copy", copyAction);
            if (!noCut(component))
                map.put("cut", cutAction);
            map.put("paste", pasteAction);
        }

        if (component instanceof JTextComponent) {
            final JTextComponent tc = (JTextComponent)component;
            map.put("copyAll", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    tc.selectAll();
                    ClipboardActions.getCopyAction().actionPerformed(e);
                }
            });
        }
    }

    private static boolean noCut(JComponent component) {
        return Boolean.FALSE.equals(checkProp(component, CUT_HINT));
    }

    /**
     * Creates an action that will invoke the specified action's actionCommand against whichever component
     * currently has focus when the action is fired.
     */
    private static class ProxyAction extends AbstractAction {
        private final String actionCommand;
        private final String backupActionCommand;

        public ProxyAction(String actionCommand, String backupActionCommand, int mnemonic, KeyStroke accelerator) {
            super(actionCommand);
            this.actionCommand = actionCommand;
            this.backupActionCommand = backupActionCommand;
            if (mnemonic > 0) putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
            if (accelerator != null) putValue(Action.ACCELERATOR_KEY, accelerator);
            maybeInstallGlobalListeners();
        }

        public ProxyAction(Action actionToRun, Action backupAction, int mnemonic, KeyStroke accelerator) {
            this((String)actionToRun.getValue(Action.NAME),
                 backupAction == null ? null : (String)backupAction.getValue(Action.NAME),
                 mnemonic,
                 accelerator);
        }

        public void actionPerformed(ActionEvent e) {
            if (logger.isLoggable(Level.FINE)) logger.fine("Dispatching action command: " + actionCommand);
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    runActionCommandOnFocusedComponent(actionCommand, backupActionCommand);
                    return null;
                }
            });
            afterActionPerformed();
        }

        protected void afterActionPerformed() {
        }
    }

    private static class ClipboardMutatingProxyAction extends ProxyAction {
        public ClipboardMutatingProxyAction(String actionCommand, int mnemonic, KeyStroke accelerator) {
            super(actionCommand, null, mnemonic, accelerator);
        }

        public ClipboardMutatingProxyAction(Action actionToRun, Action backupActionToRun, int mnemonic, KeyStroke accelerator) {
            super(actionToRun, backupActionToRun, mnemonic, accelerator);
        }

        protected void afterActionPerformed() {
            updateClipboardFlavors(getClipboard());
            updateClipboardActions();
        }
    }

    /**
     * Used for the Layer 7 versions of the cut/copy/paste actions, which bypass
     */
    static class L7TransferAction extends AbstractAction {
        L7TransferAction(String name, KeyStroke accelerator) {
            super(name);
            if (accelerator != null) putValue(Action.ACCELERATOR_KEY, accelerator);
        }

        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (!(src instanceof JComponent))
                return;

            JComponent component = (JComponent) src;
            Clipboard clipboard = getClipboard();
            String name = (String)getValue(Action.NAME);
            if (name == null) return;

            if (clipboard == null) {
                logger.info("No access to system clipboard -- falling back to TransferHandler's actions");
                // Have to fallback to the transferhandler's actions
                if ("cut".equals(name) || "l7cut".equals(name)) {
                    if (!noCut(component))
                        TransferHandler.getCutAction().actionPerformed(e);
                } else if ("copy".equals(name) || "l7copy".equals(name)) {
                    TransferHandler.getCopyAction().actionPerformed(e);
                } else if ("paste".equals(name) || "l7paste".equals(name)) {
                    TransferHandler.getPasteAction().actionPerformed(e);
                }
                return;
            }

            TransferHandler transferHandler = component.getTransferHandler();
            if (transferHandler == null) return;

            try {
                if ("cut".equals(name) || "l7cut".equals(name)) {
                    if (!noCut(component))
                        transferHandler.exportToClipboard(component, clipboard, TransferHandler.MOVE);
                } else if ("copy".equals(name) || "l7copy".equals(name)) {
                    transferHandler.exportToClipboard(component, clipboard, TransferHandler.COPY);
                } else if ("paste".equals(name) || "l7paste".equals(name)) {
                    transferHandler.importData(component, clipboard.getContents(null));
                }
            } catch (IllegalStateException ise) {
                UIManager.getLookAndFeel().provideErrorFeedback(component);
            }
        }
    }
}

