/* $Id$ */
package com.l7tech.proxy.gui;

import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.gui.util.IconManager;
import com.l7tech.console.panels.Utilities;
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
    private SsgListPanel ssgListPanel;

    /** Get the singleton Gui. */
    public static Gui getInstance() {
        if (instance == null)
            instance = new Gui();
        return instance;
    }

    /** Try to set the Kunststoff look and feel. */
    private static void setKunststoffLnF() throws Exception {
        final Class kunststoffClass = Class.forName(KUNSTSTOFF_CLASSNAME);
        final Object kunststoffLnF = kunststoffClass.newInstance();
        final Class themeClass = Class.forName(KUNSTSTOFF_THEME_CLASSNAME);
        final Object theme = themeClass.newInstance();
        kunststoffClass.getMethod("setCurrentTheme", new Class[] {MetalTheme.class}).invoke(kunststoffLnF,
                                                                                            new Object[] {theme});
        UIManager.setLookAndFeel((LookAndFeel)kunststoffLnF);
    }

    /** Try to set the Windows look and feel. */
    private static void setWindowsLnF() throws Exception {

        UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
        for (int i = 0; i < feels.length; i++) {
            UIManager.LookAndFeelInfo feel = feels[i];
            if (feel.getName().indexOf("indow") >= 0) {
                UIManager.setLookAndFeel(feel.getClassName());
                break;
            }
        }
    }

    /**
     * Initialize the Gui.
     */
    private Gui() {
        // Try to set up enhanced look and feel
        try {
            setKunststoffLnF();      // vvv Swap these two lines to prefer Windows look and feel vvv
        } catch (Exception e) {
            try {
                setWindowsLnF();     // ^^^ Swap these two lines to prefer Windows look and feel ^^^
            } catch (Exception e1) {
                log.warn(e1);
            }
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
            messageViewer.pack();
            Utilities.centerOnScreen(messageViewer);
        }
        return messageViewer;
    }

    /** Get the main frame. */
    public JFrame getFrame() {
        if (frame == null) {
            frame = new JFrame("Client Proxy");
            frame.setIconImage(IconManager.getAppImage());
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(final WindowEvent e) {
                    closeFrame();
                }
            });

            final JMenuBar menus = makeMenus();
            frame.setJMenuBar(menus);
            frame.setContentPane(getSsgListPanel());
            frame.pack();
            Utilities.centerOnScreen(frame);
        }

        return frame;
    }

    private SsgListPanel getSsgListPanel() {
        if (ssgListPanel == null)
            ssgListPanel = new SsgListPanel();
        return ssgListPanel;
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

        fileMenu.add(new JMenuItem(getSsgListPanel().getActionNewSsg()));
        fileMenu.add(new JMenuItem(getSsgListPanel().getActionEditSsg()));
        fileMenu.add(new JMenuItem(getSsgListPanel().getActionDeleteSsg()));
        fileMenu.add(new JSeparator());

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

        if (getSsgListPanel().getNumSsgs() < 1)
            getSsgListPanel().getActionNewSsg().actionPerformed(new ActionEvent(this, 1, "NewDefault"));
    }

    /**
     * Display an error message.
     * @param msg the error message to display
     */
    public static void errorMessage(final String msg) {
        JOptionPane.showMessageDialog(getInstance().getFrame(), msg, "Unable to proceed", JOptionPane.ERROR_MESSAGE);
    }
}
