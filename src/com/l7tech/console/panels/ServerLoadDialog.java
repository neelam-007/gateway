package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.adminws.logging.Log;
import com.l7tech.console.action.Actions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.RemoteException;

/**
 * Dialog to display server statistics
 * @author flascell
 */
public class ServerLoadDialog extends JDialog implements ActionListener, KeyListener  {
    static Logger log = Logger.getLogger(ServerLoadDialog.class.getName());

    /**
     * Resource bundle with default locale
     */
    private ResourceBundle resources = null;

    private JLabel uptimeText = null;
    private JLabel lastminuteLoadText = null;
    private JLabel fiveminuteLoadText = null;
    private JLabel fifteenminuteLoadText = null;

    private String CMD_CANCEL = "cmd.cancel";
    private String CMD_REFRESH = "cmd.refresh";
    private String CMD_HELP = "cmd.help";

    /**
     * Creates new form Preferences
     */
    public ServerLoadDialog(Frame parent, boolean modal) {
        super(parent, modal);
        logstub = (Log) Locator.getDefault().lookup(Log.class);
        initResources();
        initComponents();
        loadValues();
        pack();
    }

    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F5) {
            e.consume();
            refresh();
            System.out.println("hit f5");
        }
        System.out.println("other key event");
        super.processKeyEvent(e);
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources =
          ResourceBundle.getBundle("com.l7tech.console.resources.ServerLoadDialog", locale);
    }


    /**
     *
     * Called from the constructor to initialize this window
     */
    private void initComponents() {

        // initialize the window
        setTitle(resources.getString("window.title"));
        Container contents = getContentPane();
        GridBagLayout grid = new GridBagLayout();
        contents.setLayout(grid);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit "close" button at top of window
                windowAction(CMD_CANCEL);
            }
        });

        uptimeText = new JLabel();

        // server URL label
        JLabel uptimeLabel = new JLabel();
        uptimeLabel.setText(resources.getString("uptimeText.label"));
        uptimeLabel.setLabelFor(uptimeText);

        GridBagConstraints constraints;

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = constraints.WEST;
        constraints.insets = new Insets(11, 12, 0, 0);
        contents.add(uptimeLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        constraints.fill = constraints.HORIZONTAL;
        constraints.insets = new Insets(11, 7, 0, 11);
        constraints.ipadx = 100;
        contents.add(uptimeText, constraints);


        lastminuteLoadText = new JLabel();

        // look and feel label
        JLabel lastminuteLoadLabel = new JLabel();
        lastminuteLoadLabel.setLabelFor(lastminuteLoadText);
        lastminuteLoadLabel.setText(resources.getString("lastMinuteLoadText.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.anchor = constraints.WEST;
        constraints.insets = new Insets(11, 12, 0, 0);
        contents.add(lastminuteLoadLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        constraints.fill = constraints.HORIZONTAL;
        constraints.insets = new Insets(11, 7, 0, 11);
        constraints.ipadx = 100;
        contents.add(lastminuteLoadText, constraints);

        fiveminuteLoadText = new JLabel();

        // look and feel label
        JLabel fiveminuteLoadLabel = new JLabel();
        fiveminuteLoadLabel.setLabelFor(fiveminuteLoadText);
        fiveminuteLoadLabel.setText(resources.getString("fiveMinuteLoadText.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.anchor = constraints.WEST;
        constraints.insets = new Insets(11, 12, 0, 0);
        contents.add(fiveminuteLoadLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        constraints.fill = constraints.HORIZONTAL;
        constraints.insets = new Insets(11, 7, 0, 11);
        constraints.ipadx = 100;
        contents.add(fiveminuteLoadText, constraints);

        fifteenminuteLoadText = new JLabel();

        // look and feel label
        JLabel fifteenminuteLoadLabel = new JLabel();
        fifteenminuteLoadLabel.setLabelFor(fifteenminuteLoadText);
        fifteenminuteLoadLabel.setText(resources.getString("fifteenMinuteLoadText.label"));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.anchor = constraints.WEST;
        constraints.insets = new Insets(11, 12, 0, 0);
        contents.add(fifteenminuteLoadLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 12;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        constraints.fill = constraints.HORIZONTAL;
        constraints.insets = new Insets(11, 7, 0, 11);
        constraints.ipadx = 100;
        contents.add(fifteenminuteLoadText, constraints);

        // Button panel at the bottom of the window
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, 0));

        // refresh button
        JButton refreshButton = new JButton();
        refreshButton.setText(resources.getString("refreshButton.label"));
        refreshButton.setActionCommand(CMD_REFRESH);

        buttonPanel.add(refreshButton);
        refreshButton.addActionListener(this);

        // space
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(this);

        // space
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        // help button
        JButton helpButton = new JButton();
        helpButton.setText(resources.getString("helpButton.label"));
        helpButton.setMnemonic(
        resources.getString("helpButton.mnemonic").charAt(0));
        helpButton.setActionCommand(CMD_HELP);
        buttonPanel.add(helpButton);
        helpButton.addActionListener(this);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 16;
        constraints.gridwidth = 7;
        constraints.anchor = constraints.EAST;
        constraints.insets = new Insets(17, 12, 11, 11);

        contents.add(buttonPanel, constraints);

        refreshButton.addKeyListener(this);
        cancelButton.addKeyListener(this);
        helpButton.addKeyListener(this);

        // equalize button sizes
        getRootPane().setDefaultButton(refreshButton);
        JButton buttons[] =
          new JButton[]{
              refreshButton, helpButton, cancelButton
          };
        Utilities.equalizeButtonSizes(buttons);

        JLabel[] jLabels =
          new JLabel[]{
              uptimeLabel,
              lastminuteLoadLabel,
              fiveminuteLoadLabel,
              fifteenminuteLoadLabel
          };

        // equalize label sizes
        Utilities.equalizeLabelSizes(jLabels);
    }

    /**
     * The user has selected an option.
     * If actionCommand is an ActionEvent, getCommandString() is called,
     * otherwise toString() is used to get the action command.
     *
     * @param actionCommand may be null
     */
    private void windowAction(Object actionCommand) {
        boolean closeWindow = false;

        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
            closeWindow = true;
        } else if (actionCommand.equals(CMD_HELP)) {
            Actions.invokeHelp(ServerLoadDialog.this);
        } else if (actionCommand.equals(CMD_REFRESH)) {
            refresh();
        }
        if (closeWindow) {
            setVisible(false);
            dispose();
        }
    }

    public void actionPerformed(ActionEvent e) {
        windowAction(e.getActionCommand());
    }

    private void refresh() {
        loadValues();
    }

    private void loadValues() {
        UptimeMetrics metrics = null;
        try {
            metrics = logstub.getUptime();
        } catch (RemoteException e) {
            log.log(Level.WARNING, "error getting server metrics", e);
            metrics = null;
        }
        if (metrics == null) {
            uptimeText.setText("unavailable");
            lastminuteLoadText.setText("unavailable");
            fiveminuteLoadText.setText("unavailable");
            fifteenminuteLoadText.setText("unavailable");
        } else {
            int days = metrics.getDays();
            int hrs = metrics.getHours();
            int minutes = metrics.getMinutes();
            String uptime = "";
            if (days > 0) uptime += Integer.toString(days) + " days ";
            if (hrs > 0) uptime += Integer.toString(hrs) + " hrs ";
            if (minutes > 0) uptime += Integer.toString(minutes) + " minutes ";
            uptimeText.setText(uptime);
            lastminuteLoadText.setText(Double.toString(metrics.getLoad1()));
            fiveminuteLoadText.setText(Double.toString(metrics.getLoad2()));
            fifteenminuteLoadText.setText(Double.toString(metrics.getLoad3()));
        }
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F5) windowAction(CMD_REFRESH);
        else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) windowAction(CMD_CANCEL);
        else if (e.getKeyCode() == KeyEvent.VK_F1) windowAction(CMD_HELP);
    }

    public void keyTyped(KeyEvent e) {}

    public void keyReleased(KeyEvent e) {}

    private Log logstub = null;
}