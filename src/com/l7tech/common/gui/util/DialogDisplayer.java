package com.l7tech.common.gui.util;

import com.l7tech.common.util.SyspropUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Display a possibly-modal dialog using a sheet if possible, otherwise just natively using setVisible(true).
 * To work properly with this mechanism, dialogs must be designed to call dispose() on themselves when
 * dismissed.
 */
public class DialogDisplayer {
    public static final String PROPERTY_FORCE_NATIVE_DIALOG_DISPLAY = "com.l7tech.common.gui.util.forceNativeDialogDisplay";
    private static boolean forceNative = SyspropUtil.getBoolean(PROPERTY_FORCE_NATIVE_DIALOG_DISPLAY);

    /** SheetHolder to use if one cannot be found. */
    private static Utilities.SheetHolder defaultSheetHolder = null;

    /**
     * Display the specified dialog as a sheet if possible, but otherwise as a normal dialog.
     *
     * @param dialog  the dialog to display.  Must not be null.
     */
    public static void display(JDialog dialog) {
        display(dialog, dialog.getParent(), null);
    }

    public static void display(JDialog dialog, Runnable continuation) {

    }

    /**
     * Display the specified dialog as a sheet if possible, but otherwise as a normal dialog.
     *
     * @param dialog  the dialog to display.  Must not be null.
     * @param parent  the parent to use instead of the dialog's parent.
     * @param continuation  code to invoke when the dialog is disposed.
     */
    public static void display(JDialog dialog, Container parent, Runnable continuation) {
        Utilities.SheetHolder holder = getSheetHolderAncestor(parent);
        display(dialog, holder, continuation);
    }

    /**
     *
     * Display the specified dialog as a sheet if possible, but otherwise as a normal dialog.
     *
     * @param dialog  the dialog to display.  Must not be null.
     * @param holder  the sheet holder to use instead of searching up the dialog's parent, or null to force native display.
     * @param continuation  code to invoke when the dialog is disposed.
     */
    public static void display(JDialog dialog, Utilities.SheetHolder holder, Runnable continuation) {
        if (holder == null && defaultSheetHolder != null) holder = defaultSheetHolder;

        if (holder != null) {
            displayInHolder(dialog, holder, continuation);
            return;
        }

        displayNatively(dialog, continuation);
    }

    private static void displayInHolder(JDialog dialog, Utilities.SheetHolder holder, Runnable continuation) {
        if (forceNative) {
            displayNatively(dialog, continuation);
            return;
        }
        holder.showSheet(new Sheet(dialog, continuation));
    }

    private static Utilities.SheetHolder getSheetHolderAncestor(Component c) {
        for (; c != null; c = c.getParent()) {
            if (c instanceof Utilities.SheetHolder)
                return (Utilities.SheetHolder)c;
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

    public static Utilities.SheetHolder getDefaultSheetHolder() {
        return defaultSheetHolder;
    }

    /**
     * Set a default SheetHolder.  If a default SheetHolder is present, dialogs will never fallback to native
     * display just becuase a SheetHolder can't be located in their parent hierarchy.
     *
     * @param defaultSheetHolder  the default sheet holder to use from now on, or null to disable this feature.
     */
    public static void setDefaultSheetHolder(Utilities.SheetHolder defaultSheetHolder) {
        DialogDisplayer.defaultSheetHolder = defaultSheetHolder;
    }
}
