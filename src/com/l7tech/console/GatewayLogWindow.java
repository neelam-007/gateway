package com.l7tech.console;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.console.action.AboutAction;
import com.l7tech.console.panels.LogPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.util.ResourceBundle;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
public class GatewayLogWindow extends JFrame {
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static
            ResourceBundle resapplication =
            java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");
   // private final ClassLoader cl = getClass().getClassLoader();

    public GatewayLogWindow(final String title) {
        super(title);
        ImageIcon imageIcon =
                new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png"));
        setIconImage(imageIcon.getImage());
        setBounds(0, 0, 850, 600);
        setJMenuBar(getClusterWindowMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);
        pack();
        //todo: need to reorganize this
        getLogPane().onConnect();
        getLogPane().refreshLogs();
    }

    private JPanel getJFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setPreferredSize(new Dimension(800, 600));
            frameContentPane.setLayout(new BorderLayout());
            //getJFrameContentPane().add(getToolBarPane(), "North");
            getJFrameContentPane().add(getMainPane(), "Center");
        }
        return frameContentPane;
    }

    private JMenuBar getClusterWindowMenuBar() {
        if (clusterWindowMenuBar == null) {
            clusterWindowMenuBar = new JMenuBar();
            clusterWindowMenuBar.add(getFileMenu());
           // clusterWindowMenuBar.add(getViewMenu());
            clusterWindowMenuBar.add(getHelpMenu());
        }
        return clusterWindowMenuBar;
    }

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

    private JMenu getHelpMenu() {
        if (helpMenu != null) return helpMenu;

        helpMenu = new JMenu();
        helpMenu.setText(resapplication.getString("Help"));
        helpMenu.add(getHelpTopicsMenuItem());
        helpMenu.add(new AboutAction());
        int mnemonic = helpMenu.getText().toCharArray()[0];
        helpMenu.setMnemonic(mnemonic);

        return helpMenu;
    }

    private JMenuItem getHelpTopicsMenuItem() {
         if (helpTopicsMenuItem == null) {
             helpTopicsMenuItem = new JMenuItem();
             helpTopicsMenuItem.setText(resapplication.getString("Help_TopicsMenuItem_text"));
             int mnemonic = helpTopicsMenuItem.getText().toCharArray()[0];
             helpTopicsMenuItem.setMnemonic(mnemonic);
             helpTopicsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
         }
         return helpTopicsMenuItem;
     }

    private JMenuItem getExitMenuItem() {
        if (exitMenuItem == null) {
            exitMenuItem = new JMenuItem();
            exitMenuItem.setText(resapplication.getString("ExitMenuItem_text"));
            int mnemonic = 'X';
            exitMenuItem.setMnemonic(mnemonic);
            exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return exitMenuItem;
    }

    private JPanel getMainPane() {
        if (mainPane != null) return mainPane;

        mainPane = new javax.swing.JPanel();


        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();


        jLabel1 = new javax.swing.JLabel();

        mainPane.setLayout(new java.awt.BorderLayout());
        mainPane.setMinimumSize(new java.awt.Dimension(800, 600));
        mainPane.setPreferredSize(new java.awt.Dimension(800, 600));

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel2.setMinimumSize(new java.awt.Dimension(600, 460));
        jPanel2.setPreferredSize(new java.awt.Dimension(800, 460));
        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());

/*
        jScrollPane1.setMaximumSize(new java.awt.Dimension(32767, 400));
        jScrollPane1.setMinimumSize(new java.awt.Dimension(22, 300));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(453, 300));
        msgTable4.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{

                    {"SSG2", "20031217 10:11:35.974", "INFO", "1103", "Policy does not allow anonymous requests."},
                     {"SSG2", "20031217 10:11:35.947", "WARNING", "1103", "No authorization header found."},
                     {"SSG2", "20031217 10:11:35.946", "INFO", "1103", "Policy does not allow anonymous requests."},

                    {"SSG1", "20031217 10:11:35.906", "WARNING", "1102", "Policy evaluation resulted in status 600 (Assertion Falsified)"},
                    {"SSG1", "20031217 10:11:35.904", "WARNING", "1102", "could not extract session id from msg. Exception: com.l7tech.common.security.xml.XMLSecurityElementNotFoundException SecurityContextToken element not present."},
                    {"SSG1", "20031217 10:11:35.900", "FINEST", "1102", "Run the server policy"},
                    {"SSG1", "20031217 10:11:35.899", "FINER", "1102", "Resolved service #5242880"},
                    {"SSG1", "20031217 10:11:35.898", "FINEST", "1102", "service resolved by com.l7tech.service.resolution.SoapActionResolver"},

                    {"SSG3", "20031217 10:10:03.504", "FINE", "1101", "Request was routed with status  0 (No Error)"},
                    {"SSG3", "20031217 10:10:03.503", "FINE", "1101", "Response document signed successfully"},
                    {"SSG3", "20031217 10:10:03.474", "FINE", "1101", "Response document was encrypted."},
                    {"SSG3", "20031217 10:10:03.470", "FINE", "1101", "Request routed successfully"},
                    {"SSG3", "20031217 10:10:03.432", "FINEST", "1101", "Request already authenticated"},
                    {"SSG3", "20031217 10:10:03.431", "FINE", "1101", "Requesting user mike does not match specified user flascell"},
                    {"SSG3", "20031217 10:10:03.430", "FINEST", "1101",  "Authenticated mike"},
                    {"SSG3", "20031217 10:10:03.428", "FINEST", "1101", "forbidCertReset for mike"},
                    {"SSG3", "20031217 10:10:03.427", "FINEST", "1101", "Authenticated user mike using a client certificate"},
                    {"SSG3", "20031217 10:10:03.426", "FINE", "1101", "Stored cert serial# is 6331694074818975970"},
                    {"SSG3", "20031217 10:10:03.424", "FINEST", "1101", "getUserCert for mike"},
                    {"SSG3", "20031217 10:10:03.423", "FINEST", "1101", "Verification OK - client cert is valid."},
                    {"SSG3", "20031217 10:10:03.422", "FINEST", "1101", "Verifying client cert against current root cert..."},
                    {"SSG3", "20031217 10:10:03.419", "FINE", "1101",  "Decrypted request successfully."},
                    {"SSG3", "20031217 10:10:03.415", "FINEST", "1101", "cert extracted from digital signature for user mike"},

                },
                new String[]{
                    "Node", "Time", "Severity", "Request Id", "Message"
                }
        ) {
            Class[] types = new Class[]{
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
        msgTable4.setPreferredSize(new java.awt.Dimension(225, 310));
        msgTable4.setShowHorizontalLines(false);
        msgTable4.setShowVerticalLines(false);
        jScrollPane1.setViewportView(msgTable4);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);*/

        jPanel1.add(getLogPane(), java.awt.BorderLayout.CENTER);


        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel1.setText("  Log Browser");
        jLabel1.setMaximumSize(new java.awt.Dimension(136, 40));
        jLabel1.setMinimumSize(new java.awt.Dimension(136, 40));
        jLabel1.setPreferredSize(new java.awt.Dimension(136, 40));
        jPanel1.add(jLabel1, java.awt.BorderLayout.NORTH);

/*
        jTabbedPane2.setMinimumSize(new java.awt.Dimension(457, 150));
        jTabbedPane2.setPreferredSize(new java.awt.Dimension(457, 150));
        jTextArea1.setText("Time      : 20031217 10:08:35.947\nNode Name : SSG2\nSeverity  : WARNING\nRequest Id: 1103\nMessage   : No authorization header found.\nClass     : com.l7tech.server.AuthenticatableHttpServlet\nMethod    : authenticateRequestBasic");
        jTabbedPane2.addTab("Details", jTextArea1);

        jPanel1.add(jTabbedPane2, java.awt.BorderLayout.SOUTH);
*/

        jPanel3.add(jPanel1, java.awt.BorderLayout.NORTH);

/*        jPanel5.setLayout(new java.awt.GridLayout());

        jPanel5.setMinimumSize(new java.awt.Dimension(194, 50));
        jPanel5.setPreferredSize(new java.awt.Dimension(400, 50));
        jSlider1.setFont(new java.awt.Font("Dialog", 0, 10));
        jSlider1.setMajorTickSpacing(40);
        jPanel5.add(getFilterSlider());

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Auto-refresh");
        jCheckBox1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jPanel5.add(jCheckBox1);

        jPanel3.add(jPanel5, java.awt.BorderLayout.SOUTH);
*/
        jPanel2.add(jPanel3, java.awt.BorderLayout.NORTH);

//        jPanel4.setMinimumSize(new java.awt.Dimension(100, 400));
 //       jPanel4.setPreferredSize(new java.awt.Dimension(100, 400));

 //       jPanel4.add(jTree1);


        mainPane.add(jPanel2, java.awt.BorderLayout.CENTER);

        return mainPane;
    }


    public LogPanel getLogPane() {
        if (logPane != null) return logPane;

        logPane = new LogPanel();
        return logPane;

    }

    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel mainPane;

    private javax.swing.JMenuBar clusterWindowMenuBar;
    private javax.swing.JMenu fileMenu;

    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem helpTopicsMenuItem;
    private JPanel frameContentPane;
    private LogPanel logPane;

}
