package com.l7tech.common.gui.util;

import com.l7tech.common.util.SyspropUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Display a possibly-modal dialog using a sheet if possible, otherwise just natively using setVisible(true).
 * To work properly with this mechanism, dialogs must be designed to call dispose() on themselves when
 * dismissed.
 *
 * @noinspection ForLoopReplaceableByForEach,unchecked
 */
public class DialogDisplayer {
    public static final String PROPERTY_FORCE_NATIVE_DIALOG_DISPLAY = "com.l7tech.common.gui.util.forceNativeDialogDisplay";
    private static boolean forceNative = SyspropUtil.getBoolean(PROPERTY_FORCE_NATIVE_DIALOG_DISPLAY);

    /** Look-aside map to find sheet holders associated with applet container frames. */
    private static final Map defaultSheetHolderMap = new HashMap();

    /** JDialog instances that should never be displayed as a sheet. */
    private static final Map suppressSheetDisplayInstances = new WeakHashMap();

    /** Dialog classes that should never be displayed as a sheet. */
    private static final Map suppressSheetDisplayClasses = new WeakHashMap();

    /**
     * Display the specified dialog as a sheet if possible, but otherwise as a normal dialog.
     * Even if the dialog is modal, this method <b>may return immediately</b>,
     * without having yet invoked the continuation, if the dialog is being displayed as a sheet.
     *
     * @param dialog  the dialog to display.  Must not be null.
     */
    public static void display(JDialog dialog) {
        display(dialog, dialog.getParent(), null);
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
    public static void display(JOptionPane optionPane, Container parent, String title, Runnable continuation) {
        SheetHolder holder = getSheetHolderAncestor(parent);
        if (holder != null) {
            JInternalFrame jif = optionPane.createInternalFrame(parent, title);
            jif.pack();
            Utilities.centerOnParent(jif);
            if (!mustShowNative(jif, holder)) {
                jif.putClientProperty(Sheet.PROPERTY_CONTINUATION, continuation);
                holder.showSheet(jif);
                return;
            }
        }

        JDialog dlg = optionPane.createDialog(parent, title);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        displayNatively(dlg, continuation);
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

        Dimension sheetSize = dialog.getLayeredPane().getSize();
        if (sheetSize == null) {
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

    private static SheetHolder getSheetHolderAncestor(Component c) {
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
    private static void displayNatively(JDialog dialog, final Runnable continuation) {
        if (continuation != null) {
            dialog.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    continuation.run();
                }
            });
        }
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
                if (window instanceof Dialog) {
                    Dialog dialog = (Dialog)window;
                    if (dialog.isModal() && dialog.isVisible())
                        return true;
                }
            }
        }
        return false;
    }
}
