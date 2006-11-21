package com.l7tech.common.gui.util;

import com.l7tech.common.util.SyspropUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Adapts a JDialog into an internal frame that can be displayed a sheet over top of an existing window.
 */
public class Sheet extends JInternalFrame {
    protected static final Logger logger = Logger.getLogger(Sheet.class.getName());

    private Runnable continuation;
    private SheetBlocker blocker;
    private boolean needsBlocker;
    public static final String PROPERTY_TRANSLUCENT_SHEETS = "com.l7tech.common.gui.util.translucentSheets";
    public static boolean translucentSheets = SyspropUtil.getBoolean(PROPERTY_TRANSLUCENT_SHEETS);
    public static final Object PROPERTY_SHEETLAYER = "com.l7tech.common.gui.util.Utilities.sheetLayer";

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
     * Synonym for setVisible(false).
     * Provided for aesthetics because setVisible(true) should almost never be called manually when showing
     * a sheet.
     */
    public void dismiss() {
        setVisible(false);
    }

    public SheetBlocker getBlocker() {
        return blocker;
    }

    public void setBlocker(SheetBlocker blocker) {
        this.blocker = blocker;
    }

    public boolean isNeedsBlocker() {
        return needsBlocker;
    }

    /**
     * Display the specified dialog as a sheet attached to the specified RootPaneContainer (which must
     * also be a JComponenet).
     *
     * @param rpc   the RootPaneContainer to hold the sheet.  Must be non-null.
     * @param sheet    the sheet to display.
     * @throws ClassCastException if holder isn't a JComponent.
     */
    public static void showSheet(final RootPaneContainer rpc, final Sheet sheet) {
        final JLayeredPane layers = rpc.getLayeredPane();

        Integer layer = (Integer)layers.getClientProperty(PROPERTY_SHEETLAYER);
        if (layer == null) layer = new Integer(JLayeredPane.PALETTE_LAYER.intValue() + 1);
        final Integer oldLayer = layer;

        final SheetBlocker blocker;
        if (sheet.isNeedsBlocker()) {
            blocker = new SheetBlocker(true);
            sheet.setBlocker(blocker);
            layers.add(blocker);
            layer = new Integer(layer.intValue() + 1);
            layers.putClientProperty(PROPERTY_SHEETLAYER, layer);
            layers.setLayer(blocker, layer.intValue(), 0);
            blocker.setLocation(0, 0);
            blocker.setSize(layers.getWidth(), layers.getHeight());
            blocker.addPropertyChangeListener("visible", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("false".equals(evt.getNewValue())) {
                        layers.remove(blocker);
                        layers.putClientProperty(PROPERTY_SHEETLAYER, oldLayer);
                    }
                }
            });

            ComponentListener resizeListener = new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    blocker.setSize(layers.getParent().getWidth(), layers.getParent().getHeight());
                    blocker.invalidate();
                    blocker.validate();
                }
            };
            layers.getParent().addComponentListener(resizeListener);

        } else blocker = null;

        layers.add(sheet);
        layers.setLayer(sheet, layer.intValue(), 0);
        sheet.pack();
        Utilities.centerOnParent(sheet);

        if (blocker != null) blocker.setVisible(true);
        sheet.setVisible(true);
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
                logger.info("Showing sheet blocker");
                if (blockEvents) {
                    addMouseListener(nullMouseListener);
                    addKeyListener(nullKeyListener);
                }
            } else {
                logger.info("Hiding sheet blocker");
                if (blockEvents) {
                    removeMouseListener(nullMouseListener);
                    removeKeyListener(nullKeyListener);
                }
            }
        }
    }
}
