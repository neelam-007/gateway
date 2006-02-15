package com.l7tech.console;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.console.action.Actions;
import com.l7tech.console.action.DeleteAuditEventsAction;
import com.l7tech.console.action.DownloadAuditEventsAction;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
     * Create a log window for the given node.
     *
     * @param nodeName the name of the node for which logs are to be displayed
     * @param nodeId the id of the node for which logs are to be displayed
     */
    public GatewayLogWindow(String nodeName, String nodeId) {
        super("SecureSpan Manager - Gateway Log Events for " + nodeName);

        this.nodeName = nodeName;
        this.nodeId = nodeId;

        ImageIcon imageIcon =
          new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png"));

        setIconImage(imageIcon.getImage());
        setBounds(0, 0, 850, 600);
        setJMenuBar(getWindowMenuBar());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);

        Actions.setEscKeyStrokeDisposes(this);

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
                  TopComponents.getInstance().getMainWindow().showHelpTopics(e);
              }
          });

        pack();

        getLogPane().onConnect();
        getLogPane().refreshLogs();
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
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

    private String nodeName = null;
    private String nodeId = null;
    private JLabel gatewayLogTitle = null;
    private JPanel gatewayLogPane = null;
    private JPanel mainPane = null;
    private JMenuBar windowMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu helpMenu = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;
    private JPanel frameContentPane = null;
    private LogPanel logPane = null;


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
        gatewayLogTitle.setText(" Log Events for " + getNodeName());
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
