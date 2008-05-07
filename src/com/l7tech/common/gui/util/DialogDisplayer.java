package com.l7tech.common.gui.util;

import com.l7tech.common.util.SyspropUtil;
import com.l7tech.common.gui.ErrorMessageDialog;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Display a possibly-modal dialog using a sheet if possible, otherwise just natively using setVisible(true).
 * To work properly with this mechanism, dialogs must be designed to call dispose() on themselves when
 * dismissed.
 *
 * @noinspection ForLoopReplaceableByForEach,unchecked,UnnecessaryBoxing,MismatchedQueryAndUpdateOfCollection,UnnecessaryUnboxing
 */
public class DialogDisplayer {
    protected static final Logger logger = Logger.getLogger(DialogDisplayer.class.getName());

    public static final String PROPERTY_FORCE_NATIVE_DIALOG_DISPLAY = "com.l7tech.common.gui.util.forceNativeDialogDisplay";
    private static boolean forceNative = SyspropUtil.getBoolean(PROPERTY_FORCE_NATIVE_DIALOG_DISPLAY);

    public static final String PROPERTY_TRANSLUCENT_SHEETS = "com.l7tech.common.gui.util.DialogDisplayer.translucent";
    public static boolean translucentSheets = SyspropUtil.getBoolean(PROPERTY_TRANSLUCENT_SHEETS);

    /** Look-aside map to find sheet holders associated with applet container frames. */
    private static final Map defaultSheetHolderMap = new HashMap();

    /** JDialog instances that should never be displayed as a sheet. */
    private static final Map suppressSheetDisplayInstances = new WeakHashMap();

    /** Dialog classes that should never be displayed as a sheet. */
    private static final Map suppressSheetDisplayClasses = new WeakHashMap();

    // Client properties stored on layered pane that is hosting sheets
    public static final String PROPERTY_SHEETSTACK = "com.l7tech.common.gui.util.DialogDisplayer.sheetStack";

    /** Default icon to use for dialogs. */
    private static Icon defaultFrameIcon = null;

    /** Default images to use for dialogs. */
    private static List defaultWindowImages = null;

    /**
     * Display the specified dialog as a sheet if possible, but otherwise as a normal dialog.
     * Even if the dialog is modal, this method <b>may return immediately</b>
     * if the dialog is being displayed as a sheet.
     * <p/>
     * <b>Note</b>: any use of this single-argument version of display(), to display a modal dialog,
     * when it isn't the very last statement in a method that returns void back to the Swing event pump,
     * is <b>almost certainly a bug</b>.
     *
     * @param dialogWARNINGReadTheJavadoc  the dialog to display.  Must not be null.
     */
    public static void display(JDialog dialogWARNINGReadTheJavadoc) {
        display(dialogWARNINGReadTheJavadoc, dialogWARNINGReadTheJavadoc.getParent(), null);
    }

    /**
     * Display the specified dialog as a sheet if possible, but otherwise as a normal dialog.
     * Even if the dialog is modal, this method <b>may return immediately</b>,
     * without having yet invoked the continuation, if the dialog is being displayed as a sheet.
     *
     * @param dialog  the dialog to display.  Must not be null.
     * @param continuation  code to invoke when the dialog is disposed.
     */
    public static void display(JDialog dialog, Runnable continuation) {
        display(dialog, dialog.getParent(), continuation);
    }

    /**
     * Display the specified option pane as a sheet if possible, but otherwise as a normal dialog.
     * Even though JOptionPane is normally modal, this method <b>may return immediately</b>,
     * without having yet invoked the continuation, if the JOptionPane is being displayed as a sheet.
     *
     * @param optionPane   a configured JOptionPane instance to display.  Must not be null.
     * @param parent       the parent object for the dialog or sheet.  Must not be null.
     * @param title        the title for the dialog or sheet.  Must not be null.
     * @param continuation the continuation to invoke when the option pane is hidden.  Must not be null.
     */
    public static void display(final JOptionPane optionPane, Component parent, String title, Runnable continuation) {
        SheetHolder holder = getSheetHolderAncestor(parent);
        if (holder != null) {
            JInternalFrame jif = optionPane.createInternalFrame(holder.getLayeredPane(), title);
            jif.pack();
            if (!mustShowNative(jif, holder)) {
                Utilities.centerOnParent(jif);
                Icon icon = findFrameIcon(holder);
                if (icon != null) jif.setFrameIcon(icon);
                jif.putClientProperty(Sheet.PROPERTY_CONTINUATION, continuation);
                holder.showSheet(jif);
                return;
            }
        }

        final JDialog dlg = optionPane.createDialog(parent, title);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        displayNatively(dlg, null);
        if (continuation != null) continuation.run();
    }

    /**
     * Display the specified dialog as a sheet if possible, but otherwise as a normal dialog.
     * Even if the dialog is modal, this method <b>may return immediately</b>,
     * without having yet invoked the continuation, if the dialog is being displayed as a sheet.
     *
     * @param dialog  the dialog to display.  Must not be null.
     * @param parent  the parent to use instead of the dialog's parent.
     * @param continuation  code to invoke when the dialog is disposed.
     */
    public static void display(JDialog dialog, Container parent, Runnable continuation) {
        SheetHolder holder = getSheetHolderAncestor(parent);
        display(dialog, holder, continuation);
    }

    /**
     * Display the specified dialog as a sheet if possible, but otherwise as a normal dialog.
     * Even if the dialog is modal, this method <b>may return immediately</b>,
     * without having yet invoked the continuation, if the dialog is being displayed as a sheet.
     *
     * @param dialog  the dialog to display.  Must not be null.
     * @param holder  the sheet holder to use instead of searching up the dialog's parent, or null to force native display.
     * @param continuation  code to invoke when the dialog is disposed.
     */
    public static void display(JDialog dialog, SheetHolder holder, Runnable continuation) {
        if (holder != null) {
            if (mustShowNative(dialog, holder)) {
                displayNatively(dialog, continuation);
                return;
            }
            holder.showSheet(new Sheet(dialog, continuation));
            return;
        }

        displayNatively(dialog, continuation);
    }

    private static boolean mustShowNative(RootPaneContainer dialog, SheetHolder holder) {
        return forceNative ||
               isSuppressSheetDisplay(dialog) ||
               isModalDialogShowing() ||
               !willFit(dialog, holder);
    }

    /**
     * @return true if the specified dialog will fit into the specified sheet holder.
     * @param dialog  the dialog that should be checked to see if a sheet based on it would fit in the holder.
     * @param holder  the SheetHolder that should be checked to see if the dialog would fit.
     */
    private static boolean willFit(RootPaneContainer dialog, SheetHolder holder) {
        Dimension holderSize = holder.getLayeredPane().getSize();
        if (holderSize == null) return false;

        Dimension sheetSize;
        sheetSize = dialog instanceof JDialog ? ((JDialog)dialog).getSize() : dialog.getLayeredPane().getSize();
        if (sheetSize == null || sheetSize.getWidth() < 1 || sheetSize.getHeight() < 1) {
            if (dialog instanceof JDialog) {
                JDialog jd = (JDialog)dialog;
                if (jd.isMinimumSizeSet())
                    sheetSize = jd.getMinimumSize();
            }
        }

        return sheetSize != null &&
               sheetSize.getWidth() <= holderSize.getWidth() &&
               sheetSize.getHeight() <= holderSize.getHeight();
    }

    /**
     * Check if the specified dialog is flagged to disable display as a sheet.
     *
     * @param dialog  the dialog to check.  Must not be null.
     * @return true if this dialog instance or dialog subclass has been registered as "should never be displayed
     *         as a sheet"
     */
    public static boolean isSuppressSheetDisplay(RootPaneContainer dialog) {
        return suppressSheetDisplayInstances.containsKey(dialog) ||
               suppressSheetDisplayClasses.containsKey(dialog.getClass());
    }

    /**
     * Flag the specified dialog instance so that it will never be displayed as a sheet.
     *
     * @param dialog  the dialog to flag.  Must not be null.
     */
    public static void suppressSheetDisplay(JDialog dialog) {
        suppressSheetDisplayInstances.put(dialog, new Object());
    }

    /**
     * Flag the specified dialog class so that instances of it will never be displayed as a sheet.
     *
     * @param dialogClass  the JDialog subclass to flag.  Must not be null.
     */
    public static void suppressSheetDisplay(Class dialogClass) {
        suppressSheetDisplayClasses.put(dialogClass, new Object());
    }

    public static SheetHolder getSheetHolderAncestor(Component c) {
        for (; c != null; c = c.getParent()) {
            if (c instanceof SheetHolder)
                return (SheetHolder)c;
            if (c instanceof Frame || c instanceof Dialog) {
                SheetHolder sh = getSheetHolderOwnerAncestor((Window)c);
                if (sh != null)
                    return sh;
            }
        }
        return null;
    }

    private static SheetHolder getSheetHolderOwnerAncestor(Window c) {
        for (; c != null; c = c.getOwner()) {
            if (c instanceof SheetHolder)
                return (SheetHolder)c;
            Object sh = defaultSheetHolderMap.get(c);
            if (sh instanceof SheetHolder)
                return (SheetHolder)sh;
        }
        return null;
    }

    /**
     * Just display the dialog normally.
     * @param dialog  the dialog to display.  Must not be null.
     * @param continuation  code to invoke when the dialog is disposed.
     */
    private static void displayNatively(JDialog dialog, Runnable continuation) {
        if (continuation != null) {
            final Runnable[] continuationHolder = new Runnable[]{continuation};
            final Window window = dialog;
            final WindowAdapter windowAdapter = new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    window.removeWindowListener(this);
                    Runnable runnable = continuationHolder[0];
                    continuationHolder[0] = null;
                    if (runnable != null)
                        runnable.run();
                }
            };

            dialog.addWindowListener(windowAdapter);
        }
        List images = Utilities.getIconImages(dialog);
        if ((images == null || images.isEmpty()) && defaultWindowImages != null && !defaultWindowImages.isEmpty())
            Utilities.setIconImages(dialog, defaultWindowImages);        
        dialog.setVisible(true);
    }

    /**
     * @return true if all dialogs are currently being forced to display natively.  This is a global setting.
     */
    public static boolean isForceNative() {
        return forceNative;
    }

    /**
     * @param forceNative  true if all dialogs should be forced to display natively from now on.  This is a global setting.
     */
    public static void setForceNative(boolean forceNative) {
        DialogDisplayer.forceNative = forceNative;
    }


    /**
     * Register a sheet holder to use if the specified RootPaneContainer is encountered while searching
     * up the tree for a SheetHolder parent or owner.
     *
     * @param parentFrame  the parent frame for which to specify a SheetHolder.  Must not be null.
     * @param sheetHolder  the SheetHolder to use to display sheets that have parentFrame
     *                     as their closest parent or owner parent.  Must not be null.
     */
    public static void putDefaultSheetHolder(Frame parentFrame, SheetHolder sheetHolder) {
        defaultSheetHolderMap.put(parentFrame, sheetHolder);
    }


    /**
     * Test if a native modal dialog is currently being shown.  If so, no new modal sheets should be displayed.
     *
     * @return true if at least one modal dialog is current visible.
     */
    public static boolean isModalDialogShowing() {
        Frame[] frames = Frame.getFrames();
        for (int i = 0; i < frames.length; i++) {
            Frame frame = frames[i];
            Window[] windows = frame.getOwnedWindows();
            for (int j = 0; j < windows.length; j++) {
                Window window = windows[j];
                if (isModal(window))
                    return true;
            }
        }
        return false;
    }

    private static boolean isModal(Window window) {
        // Is this window a modal dialog?
        if (window instanceof Dialog) {
            Dialog dialog = (Dialog)window;
            if (dialog.isModal() && dialog.isVisible())
                return true;
        }

        // Is one of this window's descendants a modal dialog?
        Window[] kids = window.getOwnedWindows();
        for (int i = 0; i < kids.length; i++) {
            Window kid = kids[i];
            if (isModal(kid))
                return true;
        }

        // Nope, we're clear
        return false;
    }

    /**
     * Display a message dialog as a sheet if possible; otherwise as an ordinary dialog.
     *
     * @param parent    parent component.  required
     * @param mess      message to display.  required
     * @param callback  callback to invoke when dialog is dismissed.  optional
     */
    public static void showMessageDialog(Component parent, Object mess, Runnable callback) {
        showMessageDialog(parent, mess, "", JOptionPane.INFORMATION_MESSAGE, callback);
    }

    /**
     * Display a message dialog as a sheet if possible; otherwise as an ordinary dialog.
     * <p/>
     * <b>Note</b>: If the dialog is displayed as a sheet this method may <em>return immediately</em>, before
     * the dialog is dismissed.  Calling this method with a null callback,
     * when it isn't the very last statement in a method that returns void back to the Swing event pump,
     * is <b>almost certainly a bug</b>.
     *
     * @see JOptionPane#showMessageDialog(Component, Object) JOptionPane#showMessageDialog
     * @param parent    parent component.  required
     * @param mess      message to display.  required
     * @param title     title for the dialog.  required
     * @param messType  message type.  required
     * @param callback  callback to invoke when dialog is dismissed.  optional
     */
    public static void showMessageDialog(Component parent, Object mess, String title, int messType, Runnable callback) {
        showMessageDialog(parent, mess, title, messType, null,
                          callback == null ? null : callback);
    }

    /**
     * Display a message dialog as a sheet if possible; otherwise as an ordinary dialog.
     * <p/>
     * <b>Note</b>: If the dialog is displayed as a sheet this method may <em>return immediately</em>, before
     * the dialog is dismissed.  Calling this method with a null callback,
     * when it isn't the very last statement in a method that returns void back to the Swing event pump,
     * is <b>almost certainly a bug</b>.
     *
     * @see JOptionPane#showMessageDialog(Component, Object) JOptionPane#showMessageDialog
     * @param parent    parent component.  required
     * @param mess      message to display.  required
     * @param title     title for the dialog.  required
     * @param messType  message type.  required
     * @param callback  callback to invoke when dialog is dismissed.  optional
     * @param icon      icon to display.  optional
     */
    public static void showMessageDialog(Component parent, Object mess, String title, int messType, Icon icon, final Runnable callback) {
        showOptionDialog(parent, mess, title, JOptionPane.DEFAULT_OPTION, messType, icon, null, null,
                         callback == null ? null : new OptionListener() {
                             public void reportResult(int result) {
                                 callback.run();
                             }
                         });
    }

    /**
     * First decide if a message is displayed by a message dialog or an error message dialog.  Then display it.
     *
     * @param component: component in the parent window (must not be null)
     * @param title: a title message (may be null)
     * @param message: a message to be displayed in the dialog (must not be null)
     * @param throwable: an exception (may be null)
     */
    public static void showMessageDialog(final Component component,
                                         final String title,
                                         final String message,
                                         final Throwable throwable) {
        Window parent;
        if (component instanceof Window) {
            parent = (Window) component;
        } else {
            parent = SwingUtilities.getWindowAncestor(component);
        }
        
        if (throwable == null) { // Use a JOptionPane to display a message:
            String dialogTitle = ErrorMessageDialog.resources.getString("ssm.message.title");
            if (title != null) {
                dialogTitle = title;
            }
            showMessageDialog(parent, message, dialogTitle, JOptionPane.WARNING_MESSAGE, null);
        } else {                 // Otherwise, use an error dialog to display an error message:
            // Create an error dialog
            ErrorMessageDialog emd;
            if (parent instanceof Frame) {
                emd = new ErrorMessageDialog((Frame)parent, message, throwable);
            } else {
                emd = new ErrorMessageDialog((Dialog)parent, message, throwable);
            }
            
            // Check if there is an extra job to be done after showing the error dialog
            display(emd);
        }
    }

    /**
     * Get the Icon that will be used for the frame icon for dialogs displayed as sheets if no other icon
     * can be found.
     *
     * @return the icon that will be used, or null if the system default (coffee cup) will be be used
     */
    public static Icon getDefaultFrameIcon() {
        return defaultFrameIcon;
    }

    /**
     * Set the Icon that will be used for the frame icon for dialogs displayed as sheets if no other icon
     * can be found.
     *
     * @param defaultFrameIcon  the icon to use, or null to fall back to the system default (usually some kind
     *                          of coffee cup)
     */
    public static void setDefaultFrameIcon(Icon defaultFrameIcon) {
        DialogDisplayer.defaultFrameIcon = defaultFrameIcon;
    }

    /**
     * Get the list of Images that will be used for the Window images for dialogs displayed natively if no
     * Images are already set (and the Java version is at least 1.6).
     *
     * @return the image list.  May be empty or null.
     */
    public static List getDefaultWindowImages() {
        return defaultWindowImages;
    }

    /**
     * Set the list of Images that will be used for the Window images for dialogs displayed natively if no
     * Images are already set (and the Java version is at least 1.6).
     *
     * @param images  the image list.  May be empty or null.
     */
    public static void setDefaultWindowImages(List images) {
        DialogDisplayer.defaultWindowImages = images;
    }

    static JFrame getJFrameOwnerAnscestor(Window dialog) {
        for (; dialog != null; dialog = dialog.getOwner())
            if (dialog instanceof JFrame)
                return (JFrame)dialog;
        return null;
    }

    static Icon findFrameIcon(RootPaneContainer rpc) {
        if (rpc instanceof Window) {
            // Check for already-configured Window images (Java 1.6 or higher)
            List windowImages = Utilities.getIconImages((Window)rpc);
            if (windowImages != null && windowImages.size() > 0) {
                // We have no way to know what size would be best, so just use the first one
                return new ImageIcon((Image)windowImages.iterator().next());
            }

            // Try to inherit JFrame IconImage from an owner JFrame
            JFrame ownerFrame = getJFrameOwnerAnscestor((Window)rpc);
            if (ownerFrame != null) {
                Image image = ownerFrame.getIconImage();
                if (image != null)
                    return new ImageIcon(image);
            }
        }

        if (rpc instanceof JInternalFrame) {
            JInternalFrame jf = (JInternalFrame)rpc;
            Icon icon = jf.getFrameIcon();
            if (icon != null)
                return icon;
        }

        // Use the application's default frame icon, if any
        return getDefaultFrameIcon();
    }

    /**
     * Interface implemented by callers of showOptionDialog.
     */
    public interface OptionListener {
        /**
         * Report that an option dialog was dismissed with the specified result per {@link JOptionPane}.
         *
         * @param option  the result of displaying the dialog.  May be JOptionPane.CLOSED_OPTION, or the index
         *                of the option that was selected.
         */
        void reportResult(int option);
    }

    /**
     * Interface implemented by callers of showInputDialog.
     */
    public interface InputListener {
        /**
         * Report that an input dialog was dismissed with the specified result per {@link JOptionPane}.
         *
         * @param option the result of displaying the dialog.  May be null.
         */
        void reportResult(Object option);
    }

    /**
     * Display a confirmation dialog as a sheet if possible, otherwise normally.
     * <p/>
     * <b>Note</b>: If the dialog is displayed as a sheet this method may <em>return immediately</em>, before
     * the dialog is dismissed.  Calling this method with a null callback,
     * when it isn't the very last statement in a method that returns void back to the Swing event pump,
     * is <b>almost certainly a bug</b>.
     *
     * @param parent    parent component.  required
     * @param mess      message to display.  required
     * @param title     title for the dialog.  required
     * @param opType    operation type per JOptionPane
     * @param result    callback to invoke with the result when dialog is dismissed.  optional
     */
    public static void showConfirmDialog(Component parent, Object mess, String title, int opType, OptionListener result) {
        showConfirmDialog(parent, mess, title, opType, JOptionPane.QUESTION_MESSAGE, result);
    }

    /**
     * Display a confirmation dialog as a sheet if possible, otherwise normally.
     * <p/>
     * <b>Note</b>: If the dialog is displayed as a sheet this method may <em>return immediately</em>, before
     * the dialog is dismissed.  Calling this method with a null callback,
     * when it isn't the very last statement in a method that returns void back to the Swing event pump,
     * is <b>almost certainly a bug</b>.
     *
     * @param parent    parent component.  required
     * @param mess      message to display.  required
     * @param title     title for the dialog.  required
     * @param opType    operation type per JOptionPane
     * @param messType  message type per JOptionPane.  required
     * @param result    callback to invoke with the result when dialog is dismissed.  optional
     */
    public static void showConfirmDialog(Component parent, Object mess, String title, int opType, int messType,
                                        OptionListener result)
    {
        showConfirmDialog(parent, mess, title, opType, messType, null, result);
    }

    /**
     * Display a confirmation dialog as a sheet if possible, otherwise normally.
     * <p/>
     * <b>Note</b>: If the dialog is displayed as a sheet this method may <em>return immediately</em>, before
     * the dialog is dismissed.  Calling this method with a null callback,
     * when it isn't the very last statement in a method that returns void back to the Swing event pump,
     * is <b>almost certainly a bug</b>.
     *
     * @param parent    parent component.  required
     * @param mess      message to display.  required
     * @param title     title for the dialog.  required
     * @param opType    operation type per JOptionPane
     * @param messType  message type per JOptionPane.  required
     * @param icon      icon per JOptionPane. optional
     * @param result    callback to invoke with the result when dialog is dismissed.  optional
     */
    public static void showConfirmDialog(Component parent, Object mess, String title, int opType, int messType,
                                        Icon icon, OptionListener result)
    {
        showOptionDialog(parent, mess, title, opType, messType, icon, null, null, result);
    }

    /**
     * Display a message dialog as a sheet if possible; otherwise as an ordinary dialog.
     * <p/>
     * <b>Note</b>: If the dialog is displayed as a sheet this method may <em>return immediately</em>, before
     * the dialog is dismissed.  Calling this method with a null callback,
     * when it isn't the very last statement in a method that returns void back to the Swing event pump,
     * is <b>almost certainly a bug</b>.
     *
     * @see JOptionPane#showOptionDialog
     * @param parent    parent component.  required
     * @param mess      message to display.  required
     * @param title     title for the dialog.  required
     * @param opType    operation type per JOptionPane
     * @param messType  message type per JOptionPane.  required
     * @param icon          icon per JOptionPane. optional
     * @param options       options array.  required.  may not contain null entries.
     * @param initialValue  initial value to use.  Should compare equals with one of the options.
     * @param result    callback to invoke with the result when dialog is dismissed.  optional
     */
    public static void showOptionDialog(Component parent,
                                       Object mess,
                                       String title,
                                       int opType,
                                       int messType,
                                       Icon icon,
                                       final Object[] options,
                                       Object initialValue,
                                       final OptionListener result)
    {
        final JOptionPane pane = new JOptionPane(mess, messType, opType, icon, options, initialValue);
        pane.setInitialValue(initialValue);

        display(pane, parent, title, result == null ? null : new Runnable() {
            public void run() {
                result.reportResult(getValue());
            }

            private int getValue() {
                Object val = pane.getValue();

                if (val == null)
                    return JOptionPane.CLOSED_OPTION;

                if (options == null)
                    return val instanceof Integer ? ((Integer)val).intValue() : JOptionPane.CLOSED_OPTION;

                for (int i = 0; i < options.length; i++) {
                    if (options[i].equals(val))
                        return i;
                }

                return JOptionPane.CLOSED_OPTION;
            }
        });
    }

    /**
     * Show an input dialog as a sheet if possible; otherwise as an ordinary dialog.
     *
     * @see JOptionPane#showInputDialog(java.awt.Component, Object, String, int, javax.swing.Icon, Object[], Object)
     * @param parent  dialog parent
     * @param mess    mesage to display
     * @param title   dialog title
     * @param messType type of message (ERROR_MESSAGE, INFORMATION_MESSAGE, etc)
     * @param icon     Icon to use, or null
     * @param values array of possible selection values
     * @param initialValue the initial selection value
     * @param result callback which will be invoked later with the user's input when the dialog is closed 
     */
    public static void showInputDialog(Component parent, Object mess, String title, int messType, Icon icon, 
                                         Object[] values, Object initialValue, final InputListener result)
    {
        final JOptionPane pane = new JOptionPane(mess, messType, JOptionPane.OK_CANCEL_OPTION, icon, null, null);
        pane.setWantsInput(true);
        pane.setSelectionValues(values);
        pane.setInitialSelectionValue(initialValue);
        pane.selectInitialValue();

        display(pane, parent, title, new Runnable() {
            public void run() {
                Object value = pane.getInputValue();
                result.reportResult(value == JOptionPane.UNINITIALIZED_VALUE ? null : value);
            }
        });
    }

    /**
     * Check if the specified object represents the truth.
     *
     * @return true iff. o is Boolean.TRUE or the string "true".
     * @param o  object to check for truthhood
     */
    private static boolean struth(Object o) {
        return o instanceof Boolean ? ((Boolean)o).booleanValue() : o instanceof String && Boolean.valueOf((String)o).booleanValue();
    }

    private static class SheetState {
        private final int layer;  // layer of sheet under this one, or default layer
        private final Component focusOwner;
        private final JInternalFrame sheet;

        public SheetState(int layer, Component focusOwner, JInternalFrame sheet) {
            this.layer = layer;
            this.focusOwner = focusOwner;
            this.sheet = sheet;
        }
    }

    private static class SheetStack {
        private final LinkedList stack = new LinkedList();

        /** @return the sheet state on top of the stack, or null if the stack is empty. */
        private SheetState peek() {
            if (stack.isEmpty()) return null;
            Object top = stack.getLast();
            if (top instanceof SheetState)
                return (SheetState)top;
            return null;
        }

        /**
         * Creates a new sheet state, makes it the top of the stack, and returns it.
         *
         * @param sheet    the sheet that is about to be displayed
         * @return         the new SheetState object
         */
        private SheetState push(JInternalFrame sheet) {
            SheetState prevTop = peek();
            int layer = prevTop == null ? JLayeredPane.PALETTE_LAYER.intValue() + 1 : prevTop.layer + 1;
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
            SheetState state = new SheetState(layer, focusOwner, sheet);
            stack.addLast(state);
            return state;
        }

        /**
         * Removes the specified sheet state.
         *
         * @param sheet  sheet to remove.  Must not be null.  Must already have been hidden and removed from its host layered pane.
         * @return the sheet state for the sheet that was removed, or null if the specified sheet was not found on this stack.
         */
        private SheetState pop(JInternalFrame sheet) {
            for (Iterator i = stack.iterator(); i.hasNext();) {
                final SheetState sheetState = (SheetState)i.next();
                if (sheetState.sheet == sheet) {
                    i.remove();
                    if (sheetState.focusOwner != null) {
                        sheetState.focusOwner.requestFocus();
                        sheetState.focusOwner.requestFocusInWindow();
                    }
                    return sheetState;
                }
            }
            return null;
        }
    }

    private static SheetStack getOrCreateSheetStack(JLayeredPane host) {
        Object ps = host.getClientProperty(PROPERTY_SHEETSTACK);
        if (ps instanceof SheetStack)
            return (SheetStack)ps;
        SheetStack stack = new SheetStack();
        host.putClientProperty(PROPERTY_SHEETSTACK, stack);
        return stack;
    }

    /**
     * Display the specified dialog as a sheet attached to the specified RootPaneContainer (which must
     * also be a JComponenet).
     *
     * @param rpc   the RootPaneContainer to hold the sheet.  Must be non-null.
     * @param sheet    the sheet to display.
     */
    public static void showSheet(final RootPaneContainer rpc, final JInternalFrame sheet) {
        final JLayeredPane layers = rpc.getLayeredPane();
        SheetStack sheetStack = getOrCreateSheetStack(layers);
        final SheetState sheetState = sheetStack.push(sheet);
        int layer = sheetState.layer;

        final SheetBlocker[] blocker = new SheetBlocker[1];

        if ("optionDialog".equals(sheet.getClientProperty("JInternalFrame.frameType")) ||
            struth(sheet.getClientProperty(Sheet.PROPERTY_MODAL)))
        {
            blocker[0] = new SheetBlocker(true);
            layers.add(blocker[0]);
            layer++;
            layers.setLayer(blocker[0], layer, 0);
            blocker[0].setLocation(0, 0);
            blocker[0].setSize(layers.getWidth(), layers.getHeight());
        } else blocker[0] = null;

        Object cobj = sheet.getClientProperty(Sheet.PROPERTY_CONTINUATION);
        final Runnable[] continuation = new Runnable[1];
        continuation[0] = cobj instanceof Runnable ? (Runnable)cobj : null;

        layers.add(sheet);
        layers.setLayer(sheet, layer, 0);
        sheet.pack();
        Utilities.centerOnParent(sheet);

        final ComponentListener resizeListener = new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int pw = layers.getParent().getWidth();
                int ph = layers.getParent().getHeight();

                Point sp = sheet.getLocation();
                if (sp.x > pw || sp.y > ph) Utilities.centerOnParent(sheet);

                SheetBlocker sheetBlocker = blocker[0];
                if (sheetBlocker != null) {
                    sheetBlocker.setSize(pw, ph);
                    sheetBlocker.invalidate();
                    sheetBlocker.validate();
                }
            }
        };
        layers.getParent().addComponentListener(resizeListener);

        sheet.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosed(InternalFrameEvent e) {
                SheetBlocker sheetBlocker = blocker[0];
                blocker[0] = null;
                if (sheetBlocker != null) {
                    sheetBlocker.setVisible(false);
                    layers.remove(sheetBlocker);
                    layers.remove(sheet);
                    layers.getParent().removeComponentListener(resizeListener);
                    sheet.removeInternalFrameListener(this);
                    getOrCreateSheetStack(layers).pop(sheet);
                }
                Runnable runnit = continuation[0];
                continuation[0] = null;
                if (runnit != null) {
                    runnit.run();
                }
            }
        });

        if (blocker[0] != null) blocker[0].setVisible(true);
        sheet.setVisible(true);
        sheet.requestFocusInWindow();
    }

    public static class SheetBlocker extends JPanel {
        private MouseListener nullMouseListener = new MouseAdapter(){};
        private KeyListener nullKeyListener = new KeyAdapter(){};
        private boolean blockEvents = false;

        public SheetBlocker(boolean blockEvents) {
            super.setVisible(false);
            setOpaque(false);
            this.blockEvents = blockEvents;
            setFocusable(false);
            setEnabled(false);
        }

        protected void paintComponent(Graphics g) {
            if (!translucentSheets) return;
            Graphics2D gg = (Graphics2D)g;
            Composite oldComp = gg.getComposite();
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            gg.setComposite(oldComp);
        }

        public void setVisible(boolean vis) {
            boolean wasVis = isVisible();
            super.setVisible(vis);
            if (wasVis == vis)
                return;
            if (vis) {
                logger.fine("Showing sheet blocker");
                if (blockEvents) {
                    addMouseListener(nullMouseListener);
                    addKeyListener(nullKeyListener);
                }
            } else {
                logger.fine("Hiding sheet blocker");
                if (blockEvents) {
                    removeMouseListener(nullMouseListener);
                    removeKeyListener(nullKeyListener);
                }
            }
        }
    }
}
