/* $Id$ */
package com.l7tech.proxy.gui;

import com.l7tech.proxy.RequestInterceptor;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.plaf.metal.MetalTheme;
import java.awt.event.*;

/**
 * Encapsulates the Client Proxy's user interface.
 * User: mike
 * Date: May 22, 2003
 * Time: 1:47:04 PM
 */
public class Gui {
    private static final Category log = Category.getInstance(Gui.class.getName());
    public static final String RESOURCE_PATH = "com/l7tech/proxy/resources";
    public static final String HELP_PATH = "com/l7tech/proxy/resources/helpset/proxy.hs";

    private static final String KUNSTSTOFF_CLASSNAME = "com.incors.plaf.kunststoff.KunststoffLookAndFeel";
    private static final String KUNSTSTOFF_THEME_CLASSNAME = "com.incors.plaf.kunststoff.KunststoffTheme";

    private static Gui instance;
    private boolean started = false;

    private JFrame frame;
    private MessageViewer messageViewer;

    private static final String MENU_FILE = "File";
    private static final String MENU_FILE_QUIT = "Quit";
    private static final String MENU_SHOW = "Show";
    private static final String MENU_SHOW_MESSAGES = "Show Messages";
    private JCheckBoxMenuItem showMessages;

    /** Get the singleton Gui. */
    public static Gui getInstance() {
        if (instance == null)
            instance = new Gui();
        return instance;
    }

    /**
     * Initialize the Gui.
     */
    private Gui() {
        // Try to set up enhanced look and feel
        try {
            final Class kunststoffClass = Class.forName(KUNSTSTOFF_CLASSNAME);
            final Object kunststoffLnF = kunststoffClass.newInstance();
            final Class themeClass = Class.forName(KUNSTSTOFF_THEME_CLASSNAME);
            final Object theme = themeClass.newInstance();
            kunststoffClass.getMethod("setCurrentTheme", new Class[] {MetalTheme.class}).invoke(kunststoffLnF,
                                                                                                new Object[] {theme});
            UIManager.setLookAndFeel((LookAndFeel)kunststoffLnF);
        } catch (Exception e) {
            log.warn(e);
            // fall back to default look and feel
        }

        // incors.org Kunststoff faq says we need the following line if we want to use Java Web Start:
        UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());
    }

    /**
     * Interface implemented by consumers who wish to be notified when the user shuts down the GUI.
     */
    public static interface ShutdownListener {
        public void guiShutdown();
    }

    private ShutdownListener ShutdownListener;

    /** Shut down the GUI. */
    private void closeFrame() {
        if (messageViewer != null) {
            messageViewer.dispose();
            messageViewer = null;
        }
        frame.dispose();
        frame = null;
        started = false;
        if (ShutdownListener != null)
            ShutdownListener.guiShutdown();
    }

    /**
     * Connect us to someone who wants to know when the GUI is exiting.
     * @param guiShutdownListener
     */
    public void setShutdownListener(final ShutdownListener guiShutdownListener) {
        this.ShutdownListener = guiShutdownListener;
    }

    /** Create the Message Viewer. */
    private MessageViewer getMessageViewer() {
        if (messageViewer == null) {
            messageViewer = new MessageViewer("Message Window");
            messageViewer.addWindowListener(new WindowAdapter() {
                public void windowClosing(final WindowEvent e) {
                    showMessages.setSelected(false);
                }

                public void windowStateChanged(final WindowEvent e) {
                    showMessages.setSelected(messageViewer.isShowing());
                }
            });
        }
        return messageViewer;
    }

    /** Get the main frame. */
    public JFrame getFrame() {
        if (frame == null) {
            frame = new JFrame("Client Proxy");
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(final WindowEvent e) {
                    closeFrame();
                }
            });

            final JMenuBar menus = makeMenus();
            frame.setJMenuBar(menus);
            frame.setContentPane(new SsgListPanel());
            frame.pack();
        }

        return frame;
    }

    /** Build the menu bar. */
    private JMenuBar makeMenus() {
        final ActionListener menuActionListener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                action(e);
            }
        };

        final JMenuBar menus = new JMenuBar();
        final JMenu fileMenu = new JMenu(MENU_FILE);

        final JMenuItem fileQuit = new JMenuItem(MENU_FILE_QUIT);
        fileQuit.addActionListener(menuActionListener);
        fileMenu.add(fileQuit);

        menus.add(fileMenu);
        final JMenu showMenu = new JMenu(MENU_SHOW);

        showMessages = new JCheckBoxMenuItem(MENU_SHOW_MESSAGES, false);
        showMessages.addActionListener(menuActionListener);
        showMenu.add(showMessages);

        menus.add(showMenu);
        return menus;
    }

    /** Respond to a menu command. */
    private void action(final ActionEvent e) {
        if (MENU_FILE_QUIT.equals(e.getActionCommand())) {
            closeFrame();
        } else if (MENU_SHOW_MESSAGES.equals(e.getActionCommand())) {
            if (showMessages.isSelected()) {
                getMessageViewer().show();
            } else {
                getMessageViewer().hide();
            }
        }
    }

    /** Notification that the Message Viewer window has been shown or hidden. */
    public void updateMessageViewerStatus() {
        showMessages.setSelected(getMessageViewer().isVisible());
    }

    /** Get the RequestInterceptor attached to the Message Viewer window. */
    public RequestInterceptor getRequestInterceptor() {
        return getMessageViewer().getMessageViewerModel();
    }

    /** Start the GUI. */
    public void start() throws IllegalStateException {
        if (started)
            throw new IllegalStateException("Gui has already been started");

        getFrame().show();
    }

    /**
     * Display an error message.
     * @param msg the error message to display
     */
    public static void errorMessage(final String msg) {
        JOptionPane.showMessageDialog(getInstance().getFrame(), msg, "Unable to proceed", JOptionPane.ERROR_MESSAGE);
    }
}
