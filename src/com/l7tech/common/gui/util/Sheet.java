package com.l7tech.common.gui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

/**
 * Adapts a JDialog into an internal frame that can be displayed a sheet over top of an existing window.
 */
public class Sheet extends JInternalFrame {
    protected static final Logger logger = Logger.getLogger(Sheet.class.getName());

    private Runnable continuation;
    private Utilities.SheetBlocker blocker;
    private boolean needsBlocker;

    /**
     * Convert the specified JDialog into an attachable sheet.  If the dialog is modal, the sheet will disable
     * all mouse and keyboard events on the layers behind it; however, this method will always return
     * immediately in any case.  To transform a modal dialog into a sheetable one, caller must change
     * dialog.setVisible(true) into targetSheetHolder.showSheet(sheet).  If there was any code that
     * came after setVisible(true), use the version of the Sheet contructor that accepts a continuation
     * to invoke when the dialog is eventually dismissed.
     *
     * @param dialog dialog to adapt into a Sheet.  Must not be null.
     */
    public Sheet(JDialog dialog) {
        this(dialog, null);
    }

    /**
     * Convert the specified JDialog into an attachable sheet.  If the dialog is modal, the sheet will disable
     * all mouse and keyboard events on the layers behind it; however, this method will always return
     * immediately in any case.  To transform a modal dialog into a sheetable one, caller must change
     * dialog.setVisible(true) into targetSheetHolder.showSheet(sheet), and move everything
     * that formerly came after dialog.setVisible(true) into a continuation and pass it here.
     *
     * @param dialog dialog to adapt into a Sheet.  Must not be null.
     * @param continuation  the code to invoke after the sheet is hidden, or null
     *                      to take no action.
     */
    public Sheet(JDialog dialog, Runnable continuation)
      throws HeadlessException {
        this.continuation = continuation;
        layoutComponents(dialog);
    }

    public void setVisible(boolean vis) {
        boolean wasVis = isVisible();
        super.setVisible(vis);
        if (wasVis == vis)
            return;
        if (vis) {
            logger.finer("Showing blocker sheet");
        } else {
            logger.finer("Hiding blocker sheet");
            if (blocker != null) blocker.setVisible(false);
            if (continuation != null) continuation.run();
        }
    }

    private void layoutComponents(final JDialog dialog) {
        setTitle(dialog.getTitle());
        JButton defaultButton = dialog.getRootPane().getDefaultButton();
        setContentPane(dialog.getContentPane());
        dialog.setContentPane(new JPanel());
        setResizable(dialog.isResizable());
        if (defaultButton != null) getRootPane().setDefaultButton(defaultButton);

        needsBlocker = dialog.isModal();
        dialog.setModal(false);

        final WindowAdapter ourWindowListener = new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                dismiss();
            }
        };
        dialog.addWindowListener(ourWindowListener);

        // Simulate Window Opened event in case anyone needs to know
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                WindowListener[] windowLists = dialog.getWindowListeners();
                for (int i = 0; i < windowLists.length; i++) {
                    WindowListener wl = windowLists[i];
                    WindowEvent we = new WindowEvent(dialog, WindowEvent.WINDOW_OPENED, 0, 0);
                    if (wl != ourWindowListener) wl.windowOpened(we);
                }
            }
        });
    }

    /**
     * Synonym for setVisible(false).
     * Provided for aesthetics because setVisible(true) should almost never be called manually when showing
     * a sheet.
     */
    public void dismiss() {
        setVisible(false);
    }

    public Utilities.SheetBlocker getBlocker() {
        return blocker;
    }

    public void setBlocker(Utilities.SheetBlocker blocker) {
        this.blocker = blocker;
    }

    public boolean isNeedsBlocker() {
        return needsBlocker;
    }
}
