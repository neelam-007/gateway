/* $Id$ */
package com.l7tech.proxy.gui;

import com.l7tech.console.panels.Utilities;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.gui.util.IconManager;
import com.l7tech.proxy.util.JavaVersionChecker;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.metal.MetalTheme;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Iterator;

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
    private static final String KUNSTSTOFF_THEME_CLASSNAME = "com.incors.plaf.kunststoff.themes.KunststoffDesktopTheme";

    private static Gui instance;
    private boolean started = false;

    private JFrame frame;
    private MessageViewer messageViewer;

    private static final String WINDOW_TITLE = "Layer7 Client Proxy";
    private static final String MESSAGE_WINDOW_TITLE = "Message Window";
    private static final String MENU_FILE = "File";
    private static final String MENU_FILE_QUIT = "Quit";
    private static final String MENU_OPTIONS = "Options";
    private static final String MENU_OPTIONS_IMPORT_CERT = "Import SSG Certificate";
    private static final String MENU_WINDOW = "Window";
    private static final String MENU_MESSAGES = "Message Window";
    private JCheckBoxMenuItem showMessages;
    private SsgListPanel ssgListPanel;
    private JFileChooser jFileChooser = null;
    private SsgManager ssgManager = null;

    /** Get the singleton Gui. */
   public static Gui getInstance() {
        if (instance == null)
            throw new IllegalStateException("No Gui instance has been configured yet");
        return instance;
    }

    /** Set the singleton Gui. */
    public static void setInstance(Gui instance) {
        Gui.instance = instance;
    }

    /** Configure a Gui instance. */
    public static Gui createGui(SsgManager ssgManager) {
        return new Gui(ssgManager);
    }

    private static interface LnfSetter {
        void setLnf() throws Exception;
    }

    /** Try to set the Kunststoff look and feel. */
    private static LnfSetter kunststoffLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            final Class kunststoffClass = Class.forName(KUNSTSTOFF_CLASSNAME);
            final Object kunststoffLnF = kunststoffClass.newInstance();
            final Class themeClass = Class.forName(KUNSTSTOFF_THEME_CLASSNAME);
            final Object theme = themeClass.newInstance();
            kunststoffClass.getMethod("setCurrentTheme", new Class[] {MetalTheme.class}).invoke(kunststoffLnF,
                                                                                                new Object[] {theme});
            UIManager.setLookAndFeel((LookAndFeel)kunststoffLnF);
        }
    };

    /** Try to set the Windows look and feel. */
    private static LnfSetter windowsLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
            for (int i = 0; i < feels.length; i++) {
                UIManager.LookAndFeelInfo feel = feels[i];
                if (feel.getName().indexOf("indows") >= 0) {
                    UIManager.setLookAndFeel(feel.getClassName());
                    break;
                }
            }
        }
    };

    /**
     * Initialize the Gui.
     */
    private Gui(SsgManager ssgManager) {
        this.ssgManager = ssgManager;

        boolean haveXpLnf = JavaVersionChecker.isJavaVersionAtLeast(new int[] {1, 4, 2});
        LnfSetter[] order = haveXpLnf ? new LnfSetter[] { windowsLnfSetter, kunststoffLnfSetter }
                                      : new LnfSetter[] { kunststoffLnfSetter, windowsLnfSetter };
        for (int i = 0; i < order.length; i++) {
            try {
                order[i].setLnf();
            } catch (Exception e) {
                continue;
            }
            break;
        }

        try {
            // incors.org Kunststoff faq says we need the following line if we want to use Java Web Start:
            UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());
        } catch (Exception e) {
            log.warn(e);
        }
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
            messageViewer = new MessageViewer(MESSAGE_WINDOW_TITLE);
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
            frame = new JFrame(WINDOW_TITLE);
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
            ssgListPanel = new SsgListPanel(ssgManager);
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

        final JMenu optionsMenu = new JMenu(MENU_OPTIONS);
        final JMenuItem importCert = new JMenuItem(MENU_OPTIONS_IMPORT_CERT);
        importCert.addActionListener(getImportCertActionListener());
        optionsMenu.add(importCert);

        menus.add(optionsMenu);

        final JMenu windowMenu = new JMenu(MENU_WINDOW);

        showMessages = new JCheckBoxMenuItem(MENU_MESSAGES, false);
        showMessages.addActionListener(menuActionListener);
        windowMenu.add(showMessages);

        menus.add(windowMenu);
        return menus;
    }

    /** Import Certificate */
    private Action getImportCertActionListener() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                Ssg ssg = getSsgListPanel().getSelectedSsg();
                if (ssg == null) {
                    JOptionPane.showMessageDialog(getFrame(),
                                                  "Please select the SSG for which you are importing the certificate.",
                                                  "No SSG is selected", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JFileChooser fc = getFileChooser();
                FileFilter filter = new FileFilter() {
                    public boolean accept(File f) {
                        if (f.isDirectory())
                            return true;
                        String name = f.getName();
                        int dot = name.lastIndexOf('.');
                        if (dot < 0)
                            return false;
                        String ext = name.substring(dot);
                        return ext.equalsIgnoreCase(".cer");
                    }

                    public String getDescription() {
                        return "Certificate files (*.cer)";
                    }
                };
                fc.setFileFilter(filter);

                if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(getFrame())) {
                    try {
                        importSsgCertificate(ssg, fc.getSelectedFile());
                    } catch (IOException e) {
                        log.error(e);
                        JOptionPane.showMessageDialog(getFrame(),
                                                      "The system was unable to read the specified file.",
                                                      "Unable to read file",
                                                      JOptionPane.ERROR_MESSAGE);
                    } catch (GeneralSecurityException e) {
                        log.error(e);
                        JOptionPane.showMessageDialog(getFrame(),
                                                      "The system was unable to import the specified certificate: \n" +
                                                      e.getMessage(),
                                                      "Unable to import certificate",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
    }

    private JFileChooser getFileChooser() {
        if (jFileChooser == null)
            jFileChooser = new JFileChooser();
        return jFileChooser;
    }

    /**
     * Import an SSG certificate into our trust store.
     * @param selectedFile the SSG certificate file.  Must be in *.cer binary format, whatever that is.
     *                     Probably PKCS#7.
     */
    private void importSsgCertificate(Ssg ssg, File selectedFile)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException
    {
        FileInputStream certfis = new FileInputStream(selectedFile);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection c = cf.generateCertificates(certfis);
        Iterator i = c.iterator();
        while (i.hasNext()) {
            Certificate cert = (Certificate)i.next();
            ClientProxy.importCertificate(ssg, cert);
        }

        JOptionPane.showMessageDialog(getFrame(), "Certificate import was successful. \n" +
                                                  "You'll need to restart the Client Proxy for it to take effect.",
                                      "Certificate import successful", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Respond to a menu command. */
    private void action(final ActionEvent e) {
        if (MENU_FILE_QUIT.equals(e.getActionCommand())) {
            closeFrame();
        } else if (MENU_MESSAGES.equals(e.getActionCommand())) {
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
    public void errorMessage(final String msg) {
        JOptionPane.showMessageDialog(getInstance().getFrame(), msg, "Unable to proceed", JOptionPane.ERROR_MESSAGE);
    }
}
