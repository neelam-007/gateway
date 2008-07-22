package com.l7tech.gui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

/**
 * Adapts a JDialog into an internal frame that can be displayed a sheet over top of an existing window.
 * 
 * @noinspection UnnecessaryUnboxing,UnnecessaryBoxing,ForLoopReplaceableByForEach
 */
public class Sheet extends JInternalFrame {
    protected static final Logger logger = Logger.getLogger(Sheet.class.getName());

    public static final Object PROPERTY_MODAL = "com.l7tech.common.gui.util.Sheet.modal";
    public static final Object PROPERTY_CONTINUATION = "com.l7tech.common.gui.util.Sheet.continuation";

    private final JDialog dialog;
    private final WindowAdapter ourWindowListener = new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.fine("Dialog shown as sheet has been closed");
                dispose();
            }
        };

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
     * <p/>
     * All dialogs displayed as sheets should use dispose() to close themselves.  Conversely, any dialog
     * displayed as a sheet that sets its visibility to false will be disposed automatically.
     *
     * @param dialog dialog to adapt into a Sheet.  Must not be null.
     * @param continuation  the code to invoke after the sheet is hidden, or null
     *                      to take no action.
     * @throws java.awt.HeadlessException if no GUI available
     */
    public Sheet(JDialog dialog, Runnable continuation)
      throws HeadlessException {
        putClientProperty(PROPERTY_CONTINUATION, continuation);
        this.dialog = dialog;
        layoutComponents(this.dialog);
    }

    public void dispose() {
        if (dialog != null && ourWindowListener != null) dialog.removeWindowListener(ourWindowListener);
        super.dispose();
    }

    private void layoutComponents(final JDialog dialog) {
        // Mine the info out of the dialog before we strip its content
        setTitle(dialog.getTitle());
        JButton defaultButton = dialog.getRootPane().getDefaultButton();
        setSize(dialog.getSize());
        if (dialog.isPreferredSizeSet())
            setPreferredSize(dialog.getPreferredSize());
        else
            setPreferredSize(dialog.getSize());
        if (dialog.isMinimumSizeSet())
            setMinimumSize(dialog.getMinimumSize());
        if (dialog.isMaximumSizeSet())
            setMaximumSize(dialog.getMaximumSize());
        setResizable(dialog.isResizable());
        if (defaultButton != null) getRootPane().setDefaultButton(defaultButton);

        Icon frameIcon = DialogDisplayer.findFrameIcon(dialog);
        if (frameIcon != null)
            setFrameIcon(frameIcon);

        copyEscKeyAction(dialog);

        // Steal the content
        setContentPane(dialog.getContentPane());
        dialog.setContentPane(new JPanel());

        putClientProperty(PROPERTY_MODAL, Boolean.valueOf(dialog.isModal()));
        setClosable(true);
        getRootPane().setWindowDecorationStyle(dialog.getRootPane().getWindowDecorationStyle());
        dialog.setModal(false);

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

    private void copyEscKeyAction(JDialog d) {
        Action escAction = d.getLayeredPane().getActionMap().get(Utilities.KEY_ESCAPE);
        if (escAction != null) {
            final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            JLayeredPane ourlp = getLayeredPane();
            ourlp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke, Utilities.KEY_ESCAPE);
            ourlp.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, Utilities.KEY_ESCAPE);
            ourlp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, Utilities.KEY_ESCAPE);
            ourlp.getActionMap().put(Utilities.KEY_ESCAPE, escAction);
        }
    }
}
