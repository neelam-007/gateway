package com.l7tech.console;

import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * To display log records.
 *
 * @author $Author$
 * @version $Revision$
 */
public class GatewayLogWindow extends JFrame implements LogonListener {

    //- PUBLIC

    /**
     * Create a disconnected log window.
     */
    public GatewayLogWindow() {
        this(null, null, false);
    }

    /**
     * Create a log window for the given node.
     *
     * @param nodeName the name of the node for which logs are to be displayed
     * @param nodeId the id of the node for which logs are to be displayed
     */
    public GatewayLogWindow(String nodeName, String nodeId) {
        this(nodeName, nodeId, true);
    }

    /**
     * Create a log window for the given node.
     *
     * @param nodeName the name of the node for which logs are to be displayed
     * @param nodeId the id of the node for which logs are to be displayed
     */
    private GatewayLogWindow(String nodeName, String nodeId, boolean connected) {
        super(WINDOW_TITLE + (nodeName == null ? "" : " for " + nodeName));

        this.startConnected = connected;
        this.nodeName = nodeName;
        this.nodeId = nodeId;

        ImageIcon imageIcon =
          new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png"));

        setIconImage(imageIcon.getImage());
        setBounds(0, 0, 850, 600);
        setJMenuBar(getWindowMenuBar());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);

        Utilities.setEscKeyStrokeDisposes(this);

        // saveMenuItem listener
        getSaveMenuItem().
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  saveAsEventHandler();
              }
          });


        // exitMenuItem listener
        getExitMenuItem().
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  exitMenuEventHandler();
              }
          });

        // HelpTopics listener
        getHelpTopicsMenuItem().
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  TopComponents.getInstance().showHelpTopics();
              }
          });

        pack();

        if(!startConnected) getLogPane().onDisconnect();
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(visible) {
            if(startConnected) {
                getLogPane().onConnect();
            }
        }
    }

    /**
     * Display the given logs.
     *
     * @param logFile the log records to display.
     * @throws IOException on error
     */
    public boolean displayLogs(File logFile) throws IOException {
        setTitle(WINDOW_TITLE + " (" + logFile.getName() + ")");
        gatewayLogTitle.setText(BANNER_TITLE + " (" + logFile.getName() + ")");
        return getLogPane().importView(logFile);
    }

    /**
     * Intialization when the connection to the server is established.
     */
    public void onLogon(LogonEvent e) {
        getLogPane().onConnect();
    }

    /**
     * Clean up the resources when the connection to the server went down.
     */
    public void onLogoff(LogonEvent e) {
        getLogPane().onDisconnect();
    }

    public void dispose() {
        getLogPane().getLogsRefreshTimer().stop();
        super.dispose();
    }

    //- PRIVATE

    private static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static final ResourceBundle resapplication =
            ResourceBundle.getBundle("com.l7tech.console.resources.console");
    private static final String WINDOW_TITLE = "SecureSpan Manager - Gateway Log Events";
    private static final String BANNER_TITLE = " Log Events";

    private boolean startConnected;
    private String nodeName = null;
    private String nodeId = null;
    private JLabel gatewayLogTitle = null;
    private JPanel gatewayLogPane = null;
    private JPanel mainPane = null;
    private JMenuBar windowMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu helpMenu = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem saveMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;
    private JPanel frameContentPane = null;
    private LogPanel logPane = null;

    /**
     * Save currently displayed logs records to file
     * TODO this can probably be merged with the code in GatewayAuditWindow that does the same thing
     */
    private void saveAsEventHandler() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            public void useFileChooser(JFileChooser fc) {
                doSave(fc);
            }
        });
    }

    private void doSave(final JFileChooser fc) {
        fc.setDialogTitle("Save log data as ...");
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File f) {
                return  f.isDirectory() || f.getName().toLowerCase().endsWith(".ssgl");
            }
            public String getDescription() {
                return "(*.ssgl) SecureSpan Gateway Log data file.";
            }
        };
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final String suggestedName = "SecureSpanGateway_Log_" +sdf.format(new Date()) + ".ssgl";
        fc.setSelectedFile(new File(suggestedName));
        fc.addChoosableFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        fc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(e.getActionCommand())) {
                    fc.setSelectedFile(new File(suggestedName));
                }
            }
        });
        int r = fc.showDialog(GatewayLogWindow.this, "Save");
        if(r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if(file!=null) {
                //
                // Can't check parent is writable due to JDK bug (see bug 2349 for info)
                //
                if((!file.exists() && file.getParentFile()!=null /*&& file.getParentFile().canWrite()*/) ||
                   (file.isFile() && file.canWrite())) {
                    try {
                        logPane.exportView(file);
                    }
                    catch(IOException ioe) {
                        file.delete(); // attempt to clean up
                        DialogDisplayer.showMessageDialog(GatewayLogWindow.this, null,
                                "Error writing to file:\n'" + file.getAbsolutePath() + "'.", null);
                    }
                }
                else {
                    DialogDisplayer.showMessageDialog(GatewayLogWindow.this, null,
                                "Cannot write to the file:\n'" + file.getAbsolutePath() + "'.", null);
                }
            }
        }
    }

    /**
     * Clean up the resources of the window when the user exits the window.
     */
    private void exitMenuEventHandler() {
        dispose();
    }

    /**
     * Return frameContentPane property value
     *
     * @return JPanel
     */
    private JPanel getJFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setPreferredSize(new Dimension(800, 500));
            frameContentPane.setLayout(new BorderLayout());
            getJFrameContentPane().add(getMainPane(), "Center");
        }
        return frameContentPane;
    }

    /**
     * Return clusterWindowMenuBar property value
     *
     * @return JMenuBar
     */
    private JMenuBar getWindowMenuBar() {
        if (windowMenuBar == null) {
            windowMenuBar = new JMenuBar();
            windowMenuBar.add(getFileMenu());
            windowMenuBar.add(getHelpMenu());
        }
        return windowMenuBar;
    }

    /**
     * Return fileMenu property value
     *
     * @return JMenu
     */
    private JMenu getFileMenu() {
        if (fileMenu == null) {
            fileMenu = new JMenu();
            fileMenu.setText(resapplication.getString("File"));
            fileMenu.add(getSaveMenuItem());
            fileMenu.addSeparator();
            fileMenu.add(getExitMenuItem());
            int mnemonic = fileMenu.getText().toCharArray()[0];
            fileMenu.setMnemonic(mnemonic);
        }
        return fileMenu;
    }

    /**
     * Return helpMenu property value
     *
     * @return JMenu
     */
    private JMenu getHelpMenu() {
        if (helpMenu != null) return helpMenu;

        helpMenu = new JMenu();
        helpMenu.setText(resapplication.getString("Help"));
        helpMenu.add(getHelpTopicsMenuItem());
        int mnemonic = helpMenu.getText().toCharArray()[0];
        helpMenu.setMnemonic(mnemonic);

        return helpMenu;
    }

    /**
     * Return helpTopicsMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getHelpTopicsMenuItem() {
        if (helpTopicsMenuItem == null) {
            helpTopicsMenuItem = new JMenuItem();
            helpTopicsMenuItem.setText(resapplication.getString("Help_TopicsMenuItem_text_name"));
            int mnemonic = helpTopicsMenuItem.getText().toCharArray()[0];
            helpTopicsMenuItem.setMnemonic(mnemonic);
            helpTopicsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        }
        return helpTopicsMenuItem;
    }

    /**
     * Return exitMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getExitMenuItem() {
        if (exitMenuItem == null) {
            exitMenuItem = new JMenuItem();
            exitMenuItem.setText(resapplication.getString("ExitMenuItem.name"));
            int mnemonic = 'X';
            exitMenuItem.setMnemonic(mnemonic);
            exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return exitMenuItem;
    }

    /**
     * Return saveMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getSaveMenuItem() {
        if (saveMenuItem == null) {
            saveMenuItem = new JMenuItem();
            saveMenuItem.setText(resapplication.getString("SaveAsMenuItem.name"));
            int mnemonic = 'S';
            saveMenuItem.setMnemonic(mnemonic);
            saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return saveMenuItem;
    }

    /**
     * Return mainPane property value
     *
     * @return JPanel
     */
    private JPanel getMainPane() {
        if (mainPane != null) return mainPane;

        mainPane = new JPanel();
        gatewayLogPane = new JPanel();
        gatewayLogTitle = new JLabel();

        mainPane.setLayout(new BorderLayout());

        gatewayLogTitle.setFont(new Font("Dialog", 1, 18));
        gatewayLogTitle.setText(BANNER_TITLE + (nodeName == null ? "" : " for " + nodeName));
        gatewayLogTitle.setMaximumSize(new Dimension(136, 40));
        gatewayLogTitle.setMinimumSize(new Dimension(136, 40));
        gatewayLogTitle.setPreferredSize(new Dimension(136, 40));

        gatewayLogPane.setLayout(new BorderLayout());
        gatewayLogPane.add(gatewayLogTitle);

        mainPane.add(gatewayLogPane, BorderLayout.NORTH);
        mainPane.add(getLogPane(), BorderLayout.CENTER);

        return mainPane;
    }

    /**
     * Return logPane property value
     *
     * @return LogPanel
     */
    private LogPanel getLogPane() {
        if (logPane != null) return logPane;

        logPane = new LogPanel(false, false, getNodeId());
        return logPane;
    }

    private void flushCachedLogs() {
        getLogPane().clearMsgTable();
    }

}
