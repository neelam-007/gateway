/*
 * $Header$
 */
package com.l7tech.common.gui.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

/**
 * This class is a bag of utilites shared by panels.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Utilities {
    private static final Logger logger = Logger.getLogger(Utilities.class.getName());

    public static final String CONTEXT_CUT = "Cut";
    public static final String CONTEXT_COPY = "Copy";
    public static final String CONTEXT_PASTE = "Paste";
    public static final String CONTEXT_SELECT_ALL = "Select All";

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
     * A factory that creates a JFileChooser, working around Java bug parade #4711700.  Will retry
     * for up to one second.
     * @return a new JFileChooser instance.  Never null.
     * @throws RuntimeException if a new JFileChooser could not be created.
     */
    public static JFileChooser createJFileChooser() throws RuntimeException {
        JFileChooser fc = null;
        int tries = 40;
        while (fc == null) {
            try {
                fc = new JFileChooser();
                break;
            } catch (NullPointerException nfe) {
                // Bug parade 4711700 -- retry a few times before giving up
                if (--tries < 0)
                    throw new RuntimeException("J4711700 workaround: retry count exceeded", nfe);
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("J4711700 workaround: interrupted while waiting to retry", nfe);
                }
                logger.finest("J4711700 workaround: retrying");
            }
        }
        return fc;
    }

    /**
     * Update the input map of the JDialog's <code>JLayeredPane</code> so
     * the ESC keystroke  invoke dispose on the dialog.
     *
     * @param d the dialog
     */
    public static void setEscKeyStrokeDisposes(final JDialog d) {
        JLayeredPane layeredPane = d.getLayeredPane();
        final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, "close-it");
        layeredPane.getActionMap().put("close-it",
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
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, "close-it");
        layeredPane.getActionMap().put("close-it",
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
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, "close-it");
        layeredPane.getActionMap().put("close-it", action);
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
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, "close-it");
        layeredPane.getActionMap().put("close-it", action);
    }

    /**
     * Creates pop-up menus for text components.
     */
    public static interface ContextMenuFactory {
        /**
         * Create a pop-up menu for the given text component.
         */
        JPopupMenu createContextMenu(JTextComponent textComponent);
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

            if (tc.isEditable() && shouldIncludeMenu(tc, CONTEXT_CUT)) {
                JMenuItem cutItem = new JMenuItem(CONTEXT_CUT);
                cutItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        tc.cut();
                    }
                });
                contextMenu.add(cutItem);
            }

            if (shouldIncludeMenu(tc, CONTEXT_COPY)) {
                JMenuItem copyItem = new JMenuItem(CONTEXT_COPY);
                copyItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        tc.copy();
                    }
                });
                contextMenu.add(copyItem);
            }

            if (tc.isEditable()) {
                if (shouldIncludeMenu(tc, CONTEXT_PASTE)) {
                    JMenuItem pasteItem = new JMenuItem(CONTEXT_PASTE);
                    pasteItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            tc.paste();
                        }
                    });
                    contextMenu.add(pasteItem);
                }
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
     * Configure the specified text component with a default context menu containing Cut, Copy, Paste, and Select All.
     * @param tc the JTextComponent to configure.  Must not be null.
     */
    public static void attachDefaultContextMenu(final JTextComponent tc) {
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
     */
    public static void runActionOnEscapeKey(JComponent comp, Action action) {
        String ACTION_MAP_KEY_ESCAPE = "ESCAPE";
        InputMap inputMap = comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), ACTION_MAP_KEY_ESCAPE);
        comp.getActionMap().put(ACTION_MAP_KEY_ESCAPE, action);
    }

    /**
     * Configure the specified JTextComponent (text field or text area) to automatically do "Select All" whenever
     * it gains keyboard focus.
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
     * @param component the component to be enabled/disabled
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
}
