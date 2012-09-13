package com.l7tech.client.gui;

import com.l7tech.gui.ExceptionDialog;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.HelpUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JavaVersionChecker;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.Constants;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.client.gui.dialogs.AboutBox;
import com.l7tech.client.gui.util.IconManager;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import javax.swing.plaf.metal.MetalTheme;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the Client Proxy's user interface.
 *
 * Ultimate holder of everything related to an SSB GUI.
 */
public class Gui {
    private static final Logger log = Logger.getLogger( Gui.class.getName() );
    public static final String RESOURCE_PATH = "com/l7tech/proxy/resources";
//    public static final String HELP_PATH = "com/l7tech/proxy/resources/helpset/SecureSpan_Bridge_Help_System.hs";

    private static final boolean USE_SYSTEM_TRAY = SyspropUtil.getBoolean("com.l7tech.client.gui.useSystemTray", true);

    //the property name for the current applications home directory. If not set, this is defaulted to null by code
    // that uses it
    private static final String APPLICATION_HOME_PROPERTY = "com.l7tech.applicationHome";

    public static final String APP_NAME = "SecureSpan " + Constants.APP_NAME;

    private static final String KUNSTSTOFF_CLASSNAME = "com.incors.plaf.kunststoff.KunststoffLookAndFeel";
    private static final String KUNSTSTOFF_THEME_CLASSNAME = "com.incors.plaf.kunststoff.themes.KunststoffDesktopTheme";
    private static final String SYSTRAY_ICON = "com/l7tech/proxy/resources/logosm.gif";

    private static Gui instance;
    private boolean started = false;

    private JFrame frame;
    private MessageViewer messageViewer;

    private static final String SYSTRAY_TOOLTIP = APP_NAME;
    private static final String WINDOW_TITLE = APP_NAME;
    private static final String WINDOW_TITLE_CONFIG = "Configure " + APP_NAME;
    private static final String MESSAGE_WINDOW_TITLE = "Recent Message Traffic";
    private static final String MENU_FILE = "File";
    private static final String MENU_FILE_QUIT = "Exit";
    private static final String MENU_WINDOW = "Window";
    private static final String MENU_MESSAGES = "Recent Message Traffic";
    private static final String MENU_HELP = "Help";
    private static final String MENU_HELP_ABOUT = "About";
    private static final String MENU_HELP_HELP = "Help System";
    private JCheckBoxMenuItem showMessages;
    private SsgListPanel ssgListPanel;
    private final SsgManager ssgManager;
    private final int bindPort;
    private final boolean configOnly;
    private final boolean hideMenus;
    private final String bigQuitButtonLabel;
    private TrayIcon sysTrayMenu = null;

    /**
     * Get the singleton Gui.
     */
    public static Gui getInstance() {
        if ( instance == null )
            throw new IllegalStateException( "No Gui instance has been configured yet" );
        return instance;
    }

    /**
     * Set the singleton Gui.
     */
    public static void setInstance( Gui instance ) {
        Gui.instance = instance;
    }

    public static class GuiParams {
        private final SsgManager ssgManager;
        private final int bindPort;
        private final boolean configOnly;
        private final boolean hideMenus;
        private final String bigQuitButtonLabel;

        public GuiParams(SsgManager ssgManager, int bindPort) {
            this(ssgManager, bindPort, false, false, null);
        }

        public GuiParams(SsgManager ssgManager, int bindPort, boolean configOnly, boolean hideMenus, String bigQuitButtonLabel) {
            this.ssgManager = ssgManager;
            this.bindPort = bindPort;
            this.configOnly = configOnly;
            this.hideMenus = hideMenus;
            this.bigQuitButtonLabel = bigQuitButtonLabel;
        }
    }

    /**
     * Configure a Gui instance.
     */
    public static Gui createGui(GuiParams p) {
        return new Gui(p);
    }

    private static interface LnfSetter {
        void setLnf() throws Exception;
    }

    /**
     * Try to set the Kunststoff look and feel.
     */
    private static LnfSetter kunststoffLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            final Class kunststoffClass = Class.forName( KUNSTSTOFF_CLASSNAME );
            final Object kunststoffLnF = kunststoffClass.newInstance();
            try {
                final Class themeClass = Class.forName( KUNSTSTOFF_THEME_CLASSNAME );
                final Object theme = themeClass.newInstance();
                kunststoffClass.getMethod( "setCurrentTheme", new Class[]{MetalTheme.class} ).invoke( kunststoffLnF,
                                                                                                      new Object[]{theme} );
            } catch ( Exception e ) {
                // eat it, themes not make one great
            }
            UIManager.setLookAndFeel( (LookAndFeel) kunststoffLnF );
        }
    };

    /**
     * Try to set the Windows look and feel.
     */
    private static LnfSetter windowsLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
            for ( int i = 0; i < feels.length; i++ ) {
                UIManager.LookAndFeelInfo feel = feels[i];
                if ( feel.getName().indexOf( "indows" ) >= 0 ) {
                    UIManager.setLookAndFeel( feel.getClassName() );
                    break;
                }
            }
        }
    };

    /**
     * Otherwise, system look and feel.
     */
    private static LnfSetter systemLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        }
    };

    /**
     * Initialize the Gui.
     */
    private Gui(GuiParams p) {
        this.bindPort = p.bindPort;
        this.ssgManager = p.ssgManager;
        this.configOnly = p.configOnly;
        this.hideMenus = p.hideMenus;
        this.bigQuitButtonLabel = p.bigQuitButtonLabel;

        boolean haveXpLnf = JavaVersionChecker.isJavaVersionAtLeast( new int[]{1, 4, 2} );
        LnfSetter[] order = haveXpLnf ? new LnfSetter[]{windowsLnfSetter, kunststoffLnfSetter, systemLnfSetter}
                            : new LnfSetter[]{kunststoffLnfSetter, windowsLnfSetter, systemLnfSetter};
        for ( int i = 0; i < order.length; i++ ) {
            try {
                order[i].setLnf();
            } catch ( Exception e ) {
                continue;
            }
            break;
        }

        try {
            // incors.org Kunststoff faq says we need the following line if we want to use Java Web Start:
            UIManager.getLookAndFeelDefaults().put( "ClassLoader", Gui.class.getClassLoader() );
        } catch ( Exception e ) {
            log.log(Level.WARNING, "Unable to update look-and-feel classloader", e);
        }

        initSystemTray();
    }

    /** @return true if this Gui is running in configuration-only mode, not attached to an in-process ClientProxy instance. */
    public boolean isConfigOnly() {
        return configOnly;
    }

    /** @return true if the top level menus are hidden.  */
    private boolean isHideMenus() {
        return hideMenus;
    }

    /** @return label for big quit button at bottom, or null if disabled. */
    private String getBigQuitButtonLabel() {
        return bigQuitButtonLabel;
    }

    private void initSystemTray() {
        if ( sysTrayMenu != null )
            return;

        if (isConfigOnly()) {
            log.finer("System tray disabled; running in configuration-only mode");
            return;
        }

        if ( !USE_SYSTEM_TRAY || !SystemTray.isSupported() ) {
            log.fine("System tray disabled: system tray support not available");
            return;
        }

        URL si = getClass().getClassLoader().getResource(SYSTRAY_ICON);
        if ( si == null ) {
            log.info("System tray disabled: system tray icon not found: " + SYSTRAY_ICON);
            return;
        }

        log.info("Enabling system tray");
        SystemTray systemTray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(si);

        ActionListener defaultTrayAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Gui.this.getFrame().setVisible(true);
                Gui.this.getFrame().setState( Frame.NORMAL );
                Gui.this.getFrame().toFront();
            }
        };

        PopupMenu menu = new PopupMenu();

        MenuItem showItem = new MenuItem("Show " + Constants.APP_NAME+ " window");
        showItem.addActionListener(defaultTrayAction);
        menu.add(showItem);

        menu.addSeparator();

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Gui.this.closeFrame();
            }
        });
        menu.add(exitItem);


        TrayIcon trayIcon = new TrayIcon(image, Constants.APP_NAME, menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(defaultTrayAction);

        try {
            systemTray.add(trayIcon);
            sysTrayMenu = trayIcon;
        } catch (AWTException e) {
            log.log(Level.FINE, "Unable to add system tray: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * Interface implemented by consumers who wish to be notified when the user shuts down the GUI.
     */
    public interface ShutdownListener {
        void guiShutdown();
    }

    private ShutdownListener shutdownListener;

    /**
     * Shut down the GUI.  The actual shutdown will occur asynchronously, on the Swing thread.
     */
    public void stop() {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                closeFrame();
            }
        } );
    }

    /**
     * Shut down the GUI.
     */
    private void closeFrame() {
        if ( messageViewer != null ) {
            messageViewer.dispose();
            messageViewer = null;
        }
        hideSystemTrayIcon();
        frame.dispose();
        frame = null;
        started = false;
        if ( shutdownListener != null ) {
            ShutdownListener sd = shutdownListener;
            shutdownListener = null;
            sd.guiShutdown();
        }
    }

    /**
     * Connect us to someone who wants to know when the GUI is exiting.
     */
    public void setShutdownListener( final ShutdownListener guiShutdownListener ) {
        this.shutdownListener = guiShutdownListener;
    }

    /**
     * Create the Message Viewer.
     */
    private MessageViewer getMessageViewer() {
        if ( messageViewer == null ) {
            messageViewer = new MessageViewer( MESSAGE_WINDOW_TITLE );
            messageViewer.addWindowListener( new WindowAdapter() {
                public void windowClosing( final WindowEvent e ) {
                    showMessages.setSelected( false );
                }

                public void windowStateChanged( final WindowEvent e ) {
                    showMessages.setSelected( messageViewer.isShowing() );
                }
            } );
        }
        return messageViewer;
    }

    /**
     * Get the main frame.
     */
    public JFrame getFrame() {
        if ( frame == null ) {
            frame = new JFrame( isConfigOnly() ? WINDOW_TITLE_CONFIG : WINDOW_TITLE );
            frame.setIconImage( IconManager.getAppImage() );
            frame.addWindowListener( new WindowAdapter() {

                public void windowIconified( WindowEvent e ) {
                    if ( sysTrayMenu != null ) {
                        frame.setVisible(false);
                        frame.setState(Frame.NORMAL);
                        showSystemTrayIcon();
                    }
                }

                public void windowClosing( final WindowEvent e ) {
                    if ( sysTrayMenu != null ) {
                        frame.setVisible(false);
                        frame.setState( Frame.NORMAL );
                        showSystemTrayIcon();
                    } else
                        closeFrame();
                }
            } );

            if (!isHideMenus()) {
                final JMenuBar menus = makeMenus();
                frame.setJMenuBar( menus );
            }
            if (getBigQuitButtonLabel() == null) {
                frame.setContentPane( getSsgListPanel() );
            } else {
                JPanel quitStrip = new JPanel();
                quitStrip.setLayout(new BoxLayout(quitStrip, BoxLayout.X_AXIS));
                quitStrip.add(Box.createGlue());
                JButton quitButton = new JButton(getBigQuitButtonLabel());
                quitButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        closeFrame();
                    }
                });
                Utilities.equalizeButtonSizes(new AbstractButton[] { quitButton });
                quitStrip.add(quitButton);

                JPanel p = new JPanel();
                p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
                p.add(getSsgListPanel());
                p.add(quitStrip);
                frame.setContentPane(p);

            }
            frame.pack();
            Utilities.centerOnScreen( frame );
        }

        return frame;
    }

    private SsgListPanel getSsgListPanel() {
        if ( ssgListPanel == null )
            ssgListPanel = new SsgListPanel( ssgManager, bindPort, isHideMenus(), isConfigOnly() );
        return ssgListPanel;
    }

    /**
     * Build the menu bar.
     */
    private JMenuBar makeMenus() {
        final ActionListener menuActionListener = new ActionListener() {
            public void actionPerformed( final ActionEvent e ) {
                action( e );
            }
        };

        final JMenuBar menus = new JMenuBar();
        final JMenu fileMenu = new JMenu( MENU_FILE );
        fileMenu.setMnemonic( KeyEvent.VK_F );

        fileMenu.add( new JMenuItem( getSsgListPanel().getActionNewSsg() ) );
        fileMenu.add( new JMenuItem( getSsgListPanel().getActionEditSsg() ) );
        fileMenu.add( new JMenuItem( getSsgListPanel().getActionSetDefaultSsg() ) );
        fileMenu.add( new JMenuItem( getSsgListPanel().getActionDeleteSsg() ) );
        fileMenu.add( new JMenuItem( getSsgListPanel().getChangePasswordAction() ) );
        fileMenu.add( new JSeparator() );

        final JMenuItem fileQuit = new JMenuItem( MENU_FILE_QUIT );
        fileQuit.setMnemonic( KeyEvent.VK_X );
        fileQuit.setAccelerator(KeyStroke.getKeyStroke('X', ActionEvent.ALT_MASK));
        fileQuit.addActionListener( menuActionListener );
        fileMenu.add( fileQuit );

        menus.add( fileMenu );

        if (!isConfigOnly()) {
            final JMenu windowMenu = new JMenu( MENU_WINDOW );
            windowMenu.setMnemonic( KeyEvent.VK_W );

            showMessages = new JCheckBoxMenuItem( MENU_MESSAGES, false );
            showMessages.addActionListener( menuActionListener );
            //showMessages.setMnemonic( KeyEvent.VK_M );   removed due to apparent minor Swing bug, see Bug #783
            windowMenu.add( showMessages );

            menus.add( windowMenu );
        }

        final JMenu aboutMenu = new JMenu( MENU_HELP );
        aboutMenu.setMnemonic( KeyEvent.VK_H );
        final JMenuItem aboutMenuItem = new JMenuItem( MENU_HELP_ABOUT );
        aboutMenuItem.setMnemonic( KeyEvent.VK_A );
        aboutMenuItem.addActionListener( menuActionListener );
        aboutMenu.add( aboutMenuItem );
        final JMenuItem helpMenuItem = new JMenuItem( MENU_HELP_HELP );
        helpMenuItem.setMnemonic(( KeyEvent.VK_H ));
        helpMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        helpMenuItem.addActionListener( menuActionListener );
        aboutMenu.add( helpMenuItem );

        menus.add( aboutMenu );

        Utilities.removeToolTipsFromMenuItems(menus);

        return menus;
    }

    /**
     * Respond to a menu command.
     */
    private void action( final ActionEvent e ) {
        if ( MENU_FILE_QUIT.equals( e.getActionCommand() ) ) {
            closeFrame();
        } else if ( MENU_MESSAGES.equals( e.getActionCommand() ) ) {
            if ( showMessages.isSelected() ) {
                getMessageViewer().setVisible(true);
            } else {
                getMessageViewer().setVisible(false);
            }
        } else if ( MENU_HELP_ABOUT.equals( e.getActionCommand() ) ) {
            new AboutBox().setVisible(true);
        } else if ( MENU_HELP_HELP.equals( e.getActionCommand() ) ) {
            HelpUtil.showHelpTopicsRoot( ConfigFactory.getProperty( APPLICATION_HOME_PROPERTY, new File( "." ).getAbsolutePath() ),
                    Gui.getInstance().getFrame() );
        }
    }

    /**
     * Notification that the Message Viewer window has been shown or hidden.
     */
    public void updateMessageViewerStatus() {
        showMessages.setSelected( getMessageViewer().isVisible() );
    }

    /**
     * Get the RequestInterceptor attached to the Message Viewer window.
     */
    public RequestInterceptor getRequestInterceptor() {
        return getMessageViewer().getModel();
    }

    /**
     * Start the GUI.
     */
    public void start() throws IllegalStateException {
        if ( started )
            throw new IllegalStateException( "Gui has already been started" );

        if ( sysTrayMenu != null ) {
            getFrame().setVisible(false);
            showSystemTrayIcon();
        } else
            getFrame().setVisible(true);

        if ( getSsgListPanel().getNumSsgs() < 1 )
            getSsgListPanel().getActionNewSsg().actionPerformed( new ActionEvent( this, 1, "NewDefault" ) );
        else
            getSsgListPanel().selectDefaultSsg();
    }

    private void showSystemTrayIcon() {
        if (sysTrayMenu == null)
            return;

        SystemTray.getSystemTray().remove(sysTrayMenu);
        try {
            SystemTray.getSystemTray().add(sysTrayMenu);
        } catch (AWTException e1) {
            // Ignore
        }
    }

    private void hideSystemTrayIcon() {
        if (sysTrayMenu == null)
            return;

        SystemTray.getSystemTray().remove(sysTrayMenu);
    }

    /**
     * Display an error message.
     *
     * @param msg the error message to display
     */
    public static void errorMessage( final String msg ) {
        Object[] message = new Object[]{msg};
        // wrap for readability
        JTextArea jta = new JTextArea(msg);
        jta.setEditable(false);
        jta.setBorder(null);
        jta.setOpaque(false);
        jta.setLineWrap(true);
        jta.setWrapStyleWord(true);
        jta.setSize(600, 1);
        message[0] = jta;
        JOptionPane.showMessageDialog( null, message, "Unable to Proceed", JOptionPane.ERROR_MESSAGE );
    }

    /**
     * Display a critical error message.  This version displays a dialog that states that the error is so critical
     * that the application may need to be restarted; to this end, the dialog features a "Restart" button.  If
     * the error is not so critical that the user is highly likely to need to restart the application, use
     * {@link #errorMessage(String, String, String, Throwable)} instead.
     *
     * @param title
     * @param message
     * @param t
     */
    public static void criticalErrorMessage( String title, String message, Throwable t ) {
        errorMessage(title, null, message, t);
    }

    /**
     * Display an error message based on a caught exception, but without inviting the user to restart the application.
     */
    public static void errorMessage( String title, String labelMessage, String message, Throwable t ) {
        ExceptionDialog d = ExceptionDialog.createExceptionDialog( getInstance().getFrame(), title, labelMessage, message, t, Level.SEVERE );
        d.setModal( true );
        d.pack();
        Utilities.centerOnScreen( d );
        d.setVisible(true);
    }
}
