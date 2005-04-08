/*
 * $Id$
 */
package com.l7tech.proxy.gui;

import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.gui.dialogs.AboutBox;
import com.l7tech.proxy.gui.util.IconManager;
import com.l7tech.proxy.util.JavaVersionChecker;
import snoozesoft.systray4j.*;

import javax.help.DefaultHelpBroker;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.*;
import javax.swing.plaf.metal.MetalTheme;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the Client Proxy's user interface. User: mike Date: May 22, 2003 Time: 1:47:04 PM
 */
public class Gui {
    private static final Logger log = Logger.getLogger( Gui.class.getName() );
    public static final String RESOURCE_PATH = "com/l7tech/proxy/resources";
    public static final String HELP_PATH = "com/l7tech/proxy/resources/helpset/SecureSpan_Bridge_Help_System.hs";
    public static final String APP_NAME = "SecureSpan Bridge";

    private static final String KUNSTSTOFF_CLASSNAME = "com.incors.plaf.kunststoff.KunststoffLookAndFeel";
    private static final String KUNSTSTOFF_THEME_CLASSNAME = "com.incors.plaf.kunststoff.themes.KunststoffDesktopTheme";
    private static final String SYSTRAY_ICON = "com/l7tech/proxy/resources/logosm";

    private static Gui instance;
    private boolean started = false;

    private JFrame frame;
    private MessageViewer messageViewer;

    private static final String SYSTRAY_TOOLTIP = "SecureSpan Bridge";
    private static final String WINDOW_TITLE = APP_NAME;
    private static final String MESSAGE_WINDOW_TITLE = "Message Window";
    private static final String MENU_FILE = "File";
    private static final String MENU_FILE_QUIT = "Exit";
    private static final String MENU_WINDOW = "Window";
    private static final String MENU_MESSAGES = "Message Window";
    private static final String MENU_HELP = "Help";
    private static final String MENU_HELP_ABOUT = "About";
    private static final String MENU_HELP_HELP = "Help System";
    private JCheckBoxMenuItem showMessages;
    private SsgListPanel ssgListPanel;
    private SsgManager ssgManager = null;
    private ClientProxy clientProxy;
    private SysTrayMenu sysTrayMenu = null;

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

    /**
     * Configure a Gui instance.
     */
    public static Gui createGui( ClientProxy clientProxy, SsgManager ssgManager ) {
        return new Gui( clientProxy, ssgManager );
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
    private Gui( ClientProxy clientProxy, SsgManager ssgManager ) {
        this.clientProxy = clientProxy;
        this.ssgManager = ssgManager;

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
            UIManager.getLookAndFeelDefaults().put( "ClassLoader", getClass().getClassLoader() );
        } catch ( Exception e ) {
            log.log(Level.WARNING, "Unable to update look-and-feel classloader", e);
        }

        initSystemTray();
    }

    private void initSystemTray() {
        if ( !SysTrayMenu.isAvailable() || sysTrayMenu != null )
            return;

        URL si = getClass().getClassLoader().getResource( SYSTRAY_ICON + SysTrayMenuIcon.getExtension() );
        if ( si == null )
            return;

        SysTrayMenuIcon systrayMenuIcon = new SysTrayMenuIcon( si );

        systrayMenuIcon.setActionCommand( "show" );
        SysTrayMenuListener systrayListener = new SysTrayMenuListener() {
            public void iconLeftClicked( SysTrayMenuEvent e ) {
                doShow();
            }

            public void iconLeftDoubleClicked( SysTrayMenuEvent e ) {
                doShow();
            }

            public void menuItemSelected( SysTrayMenuEvent e ) {
                log.info( "System tray menu item selected.  Command=" + e.getActionCommand() );
                if ( "show".equals( e.getActionCommand() ) ) {
                    doShow();
                } else if ( "exit".equals( e.getActionCommand() ) ) {
                    Gui.this.closeFrame();
                }
            }

            private void doShow() {
                Gui.this.getFrame().show();
                Gui.this.getFrame().setState( Frame.NORMAL );
                Gui.this.getFrame().toFront();
            }
        };
        systrayMenuIcon.addSysTrayMenuListener( systrayListener );

        SysTrayMenuItem smExit = new SysTrayMenuItem( "Exit", "exit" );
        smExit.addSysTrayMenuListener( systrayListener );
        SysTrayMenuItem smShow = new SysTrayMenuItem( "Show Bridge window", "show" );
        smShow.addSysTrayMenuListener( systrayListener );

        sysTrayMenu = new SysTrayMenu( systrayMenuIcon, SYSTRAY_TOOLTIP );
        sysTrayMenu.addItem( smExit );
        sysTrayMenu.addSeparator();
        sysTrayMenu.addItem( smShow );
        sysTrayMenu.showIcon();
    }

    /**
     * Interface implemented by consumers who wish to be notified when the user shuts down the GUI.
     */
    public interface ShutdownListener {
        void guiShutdown();
    }

    private ShutdownListener ShutdownListener;

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
        if ( sysTrayMenu != null )
            sysTrayMenu.hideIcon();
        frame.dispose();
        frame = null;
        started = false;
        if ( ShutdownListener != null )
            ShutdownListener.guiShutdown();
    }

    /**
     * Connect us to someone who wants to know when the GUI is exiting.
     */
    public void setShutdownListener( final ShutdownListener guiShutdownListener ) {
        this.ShutdownListener = guiShutdownListener;
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
            frame = new JFrame( WINDOW_TITLE );
            frame.setIconImage( IconManager.getAppImage() );
            frame.addWindowListener( new WindowAdapter() {

                public void windowIconified( WindowEvent e ) {
                    if ( sysTrayMenu != null ) {
                        frame.hide();
                        frame.setState( Frame.NORMAL );
                        sysTrayMenu.showIcon();
                    }
                }

                public void windowClosing( final WindowEvent e ) {
                    if ( sysTrayMenu != null ) {
                        frame.hide();
                        frame.setState( Frame.NORMAL );
                        sysTrayMenu.showIcon();
                    } else
                        closeFrame();
                }
            } );

            final JMenuBar menus = makeMenus();
            frame.setJMenuBar( menus );
            frame.setContentPane( getSsgListPanel() );
            frame.pack();
            Utilities.centerOnScreen( frame );
        }

        return frame;
    }

    private SsgListPanel getSsgListPanel() {
        if ( ssgListPanel == null )
            ssgListPanel = new SsgListPanel( clientProxy, ssgManager );
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
        fileMenu.add( new JMenuItem( getSsgListPanel().getActionEmptyCookieCache() ) );
        fileMenu.add( new JSeparator() );

        final JMenuItem fileQuit = new JMenuItem( MENU_FILE_QUIT );
        fileQuit.setMnemonic( KeyEvent.VK_X );
        fileQuit.setAccelerator(KeyStroke.getKeyStroke('X', ActionEvent.ALT_MASK));
        fileQuit.addActionListener( menuActionListener );
        fileMenu.add( fileQuit );

        menus.add( fileMenu );

        final JMenu windowMenu = new JMenu( MENU_WINDOW );
        windowMenu.setMnemonic( KeyEvent.VK_W );

        showMessages = new JCheckBoxMenuItem( MENU_MESSAGES, false );
        showMessages.addActionListener( menuActionListener );
        //showMessages.setMnemonic( KeyEvent.VK_M );   removed due to apparent minor Swing bug, see Bug #783
        windowMenu.add( showMessages );

        menus.add( windowMenu );

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
                getMessageViewer().show();
            } else {
                getMessageViewer().hide();
            }
        } else if ( MENU_HELP_ABOUT.equals( e.getActionCommand() ) ) {
            new AboutBox().show();
        } else if ( MENU_HELP_HELP.equals( e.getActionCommand() ) ) {
            showHelpTopics(e);
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
        return getMessageViewer().getMessageViewerModel();
    }

    /**
     * Start the GUI.
     */
    public void start() throws IllegalStateException {
        if ( started )
            throw new IllegalStateException( "Gui has already been started" );

        if ( sysTrayMenu != null ) {
            getFrame().hide();
            sysTrayMenu.showIcon();
        } else
            getFrame().show();

        if ( getSsgListPanel().getNumSsgs() < 1 )
            getSsgListPanel().getActionNewSsg().actionPerformed( new ActionEvent( this, 1, "NewDefault" ) );
        else
            getSsgListPanel().selectDefaultSsg();
    }

    /**
     * Display an error message.
     *
     * @param msg the error message to display
     */
    public static void errorMessage( final String msg ) {
        JOptionPane.showMessageDialog( getInstance().getFrame(), msg, "Unable to Proceed", JOptionPane.ERROR_MESSAGE );
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
        ExceptionDialog d = new ExceptionDialog( getInstance().getFrame(), title, labelMessage, message, t, Level.SEVERE );
        d.setModal( true );
        d.pack();
        Utilities.centerOnScreen( d );
        d.show();
    }

    /**
     * The "Help Topics".
     * This procedure adds the JavaHelp to PMC application.
     */
    public void showHelpTopics(ActionEvent e) {
        HelpSet hs = null;
        URL url = null;
        HelpBroker javaHelpBroker = null;
        String helpsetName = "SSB Help";

        try {
            // Find the helpSet URL file.
            ClassLoader cl = getClass().getClassLoader();
            url = cl.getResource(HELP_PATH);
            hs = new HelpSet(cl, url);
            javaHelpBroker = hs.createHelpBroker();
            Object source = e.getSource();

            if (source instanceof Window) {
                ((DefaultHelpBroker)javaHelpBroker).setActivationWindow((Window)source);
            }
            javaHelpBroker.setDisplayed(true);

        } catch (MissingResourceException ex) {
            //Make sure the URL exists.
            if (url == null) {
                JOptionPane.showMessageDialog(frame,
                  "Help URL is missing",
                  "Bad HelpSet Path ",
                  JOptionPane.WARNING_MESSAGE);
            }
        } catch (HelpSetException hex) {
            JOptionPane.showMessageDialog(frame,
              helpsetName + " is not available",
              "Warning",
              JOptionPane.WARNING_MESSAGE);
            log.log(Level.SEVERE, helpsetName + " file was not found. " + hex.toString());
        }
    }
}
