package com.l7tech.common.gui.util;

import com.l7tech.common.util.SyspropUtil;

import javax.swing.*;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
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

    public static final String PROPERTY_TRANSLUCENT_SHEETS = "com.l7tech.common.gui.util.Sheet.translucent";
    public static boolean translucentSheets = SyspropUtil.getBoolean(PROPERTY_TRANSLUCENT_SHEETS);
    public static final Object PROPERTY_SHEETLAYER = "com.l7tech.common.gui.util.Sheet.layer";
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

        copyEscKeyAction(dialog);

        // Steal the content
        setContentPane(dialog.getContentPane());
        dialog.setContentPane(new JPanel());

        putClientProperty(PROPERTY_MODAL, Boolean.valueOf(dialog.isModal()));
        setClosable(!dialog.isModal());
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

    /**
     * Check if the specified object represents the truth.
     *
     * @return true iff. o is Boolean.TRUE or the string "true".
     * @param o  object to check for truthhood
     */
    private static boolean struth(Object o) {
        return o instanceof Boolean ? ((Boolean)o).booleanValue() : o instanceof String && Boolean.valueOf((String)o).booleanValue();
    }

    /**
     * Display the specified dialog as a sheet attached to the specified RootPaneContainer (which must
     * also be a JComponenet).
     *
     * @param rpc   the RootPaneContainer to hold the sheet.  Must be non-null.
     * @param sheet    the sheet to display.
     * @throws ClassCastException if holder isn't a JComponent.
     */
    public static void showSheet(final RootPaneContainer rpc, final JInternalFrame sheet) {
        final JLayeredPane layers = rpc.getLayeredPane();

        Integer layer = (Integer)layers.getClientProperty(PROPERTY_SHEETLAYER);
        if (layer == null) layer = new Integer(JLayeredPane.PALETTE_LAYER.intValue() + 1);
        final Integer oldLayer = layer;

        final SheetBlocker blocker;

        if ("optionDialog".equals(sheet.getClientProperty("JInternalFrame.frameType")) ||
            struth(sheet.getClientProperty(Sheet.PROPERTY_MODAL)))
        {
            blocker = new SheetBlocker(true);
            layers.add(blocker);
            layer = new Integer(layer.intValue() + 1);
            layers.putClientProperty(PROPERTY_SHEETLAYER, layer);
            layers.setLayer(blocker, layer.intValue(), 0);
            blocker.setLocation(0, 0);
            blocker.setSize(layers.getWidth(), layers.getHeight());
        } else blocker = null;

        Object continuationObj = sheet.getClientProperty(PROPERTY_CONTINUATION);
        final Runnable continuation;
        if (continuationObj instanceof Runnable) {
            continuation = (Runnable)continuationObj;
        } else continuation = null;

        layers.add(sheet);
        layers.setLayer(sheet, layer.intValue(), 0);
        sheet.pack();
        Utilities.centerOnParent(sheet);

        ComponentListener resizeListener = new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int pw = layers.getParent().getWidth();
                int ph = layers.getParent().getHeight();

                Point sp = sheet.getLocation();
                if (sp.x > pw || sp.y > ph) Utilities.centerOnParent(sheet);

                if (blocker != null) {
                    blocker.setSize(pw, ph);
                    blocker.invalidate();
                    blocker.validate();
                }
            }
        };
        layers.getParent().addComponentListener(resizeListener);

        sheet.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosed(InternalFrameEvent e) {
                if (blocker != null) {
                    blocker.setVisible(false);
                    layers.remove(blocker);
                    layers.putClientProperty(PROPERTY_SHEETLAYER, oldLayer);
                }
                if (continuation != null) continuation.run();
            }
        });

        if (blocker != null) blocker.setVisible(true);
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
