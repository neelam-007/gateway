package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * The <code>JDialog</code> wizard that drives the publish service
 * use case.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0

 */
public class PublishServiceWizard extends JDialog {
    private WizardStepPanel[] panels =
            new WizardStepPanel[]{
                new ServicePanel(),
                new EndpointCredentialsPanel(),
               new IdentityProviderPanel()
            };

    private int currentPanel = 0;

    /** Creates new form PublishServiceWizard */
    public PublishServiceWizard(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        stepjPanel.add(panels[0], BorderLayout.CENTER);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        mainjPanel = new JPanel();
        titlePanel = new JPanel();
        titlejLabel = new JLabel();
        panelButtons = new JPanel();
        buttonBack = new JButton();
        buttonNext = new JButton();
        buttonFinish = new JButton();
        cancelButton = new JButton();
        buttonHelp = new JButton();
        stepsjPanel = new JPanel();
        stepsjLabel = new JLabel();
        jSeparator1 = new JSeparator();
        stepjPanel = new JPanel();
        stepDescriptionjScrollPane = new JScrollPane();
        stepDescriptionjTextArea = new JTextArea();

        setTitle("Publish service wizard");
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent evt) {
                formHierarchyChanged(evt);
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                closeDialog(evt);
            }
        });

        mainjPanel.setLayout(new BorderLayout());

        mainjPanel.setBorder(new EtchedBorder());
        titlePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        titlePanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        titlejLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        titlejLabel.setText("Publish Service wizard");
        titlePanel.add(titlejLabel);

        mainjPanel.add(titlePanel, BorderLayout.NORTH);

        panelButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));

        panelButtons.setBorder(new EtchedBorder());
        buttonBack.setText("Back");
        buttonBack.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                buttonBackActionPerformed(evt);
            }
        });

        panelButtons.add(buttonBack);

        buttonNext.setText("Next");
        buttonNext.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                buttonNextActionPerformed(evt);
            }
        });

        panelButtons.add(buttonNext);

        buttonFinish.setText("Finish");
        panelButtons.add(buttonFinish);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        panelButtons.add(cancelButton);

        buttonHelp.setText("Help");
        panelButtons.add(buttonHelp);

        mainjPanel.add(panelButtons, BorderLayout.SOUTH);

        stepsjPanel.setLayout(new BoxLayout(stepsjPanel, BoxLayout.Y_AXIS));

        stepsjPanel.setBackground(new Color(213, 222, 222));
        stepsjPanel.setBorder(new EtchedBorder());
        stepsjPanel.setMinimumSize(new Dimension(180, 10));
        stepsjPanel.setPreferredSize(new Dimension(180, 10));
        stepsjLabel.setText("Steps");
        stepsjPanel.add(stepsjLabel);
        stepsjPanel.add(jSeparator1);
        mainjPanel.add(stepsjPanel, BorderLayout.WEST);

        stepjPanel.setLayout(new BorderLayout());

        stepjPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        stepDescriptionjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        stepDescriptionjTextArea.setEditable(false);
        stepDescriptionjTextArea.setLineWrap(true);
        stepDescriptionjTextArea.setWrapStyleWord(true);
        stepDescriptionjTextArea.setRows(5);
        stepDescriptionjScrollPane.setViewportView(stepDescriptionjTextArea);

        stepjPanel.add(stepDescriptionjScrollPane, BorderLayout.SOUTH);

        mainjPanel.add(stepjPanel, BorderLayout.CENTER);

        getContentPane().add(mainjPanel, BorderLayout.CENTER);

        pack();
        setSize(new Dimension(800, 500));
        Utilities.centerOnScreen(this);
    }

    private void formHierarchyChanged(HierarchyEvent evt) {
        int eID = evt.getID();
        long flags = evt.getChangeFlags();

        if (eID == evt.HIERARCHY_CHANGED &&
                ((flags & evt.DISPLAYABILITY_CHANGED) == evt.DISPLAYABILITY_CHANGED)) {
            if (PublishServiceWizard.this.isDisplayable()) {
                stepLabels = new JLabel[panels.length];

                for (int i = 0; i < panels.length; i++) {
                    stepLabels[i] = new JLabel(panels[i].getStepLabel());
                    stepsjPanel.add(stepLabels[i]);
                }
                stepsjPanel.add(new JPanel());
                updateWizardUiState();
            }
        }

    }

    private void buttonBackActionPerformed(ActionEvent evt) {
        // Add your handling code here:
        stepjPanel.remove(panels[currentPanel]);
        stepjPanel.add(panels[--currentPanel], BorderLayout.CENTER);
        stepjPanel.updateUI();
        updateWizardUiState();

    }

    private void buttonNextActionPerformed(ActionEvent evt) {
        // Add your handling code here:
        stepjPanel.remove(panels[currentPanel]);
        stepjPanel.add(panels[++currentPanel], BorderLayout.CENTER);
        stepjPanel.updateUI();
        updateWizardUiState();
    }

    private void buttonCancelActionPerformed(ActionEvent evt) {
        setVisible(false);
        dispose();
    }


    /** Closes the dialog */
    private void closeDialog(WindowEvent evt) {
        setVisible(false);
        dispose();
    }

    private void updateButtonsNavigate() {
        buttonFinish.setEnabled(currentPanel > 0);
        buttonBack.setEnabled(currentPanel > 0);
        buttonNext.setEnabled(currentPanel < panels.length - 1);
        if (panels.length > 0)
            stepDescriptionjTextArea.setText(panels[currentPanel].getDescription());
    }

    private void updateWizardUiState() {
        updateButtonsNavigate();
        updateWizardStepLabels();
    }

    private void updateWizardStepLabels() {
        for (int i = 0; i < stepLabels.length; i++) {
            if (i == currentPanel) {
                stepLabels[i].setForeground(Color.WHITE);
            } else {
                stepLabels[i].setForeground(labelFgColor);
            }
        }
    }

    private JPanel titlePanel;
    private JLabel stepsjLabel;
    private JScrollPane stepDescriptionjScrollPane;
    private JPanel mainjPanel;
    private JPanel panelButtons;
    private JLabel titlejLabel;
    private JButton buttonFinish;
    private JPanel stepsjPanel;
    private JPanel stepjPanel;
    private JButton cancelButton;
    private JSeparator jSeparator1;
    private JButton buttonNext;
    private JTextArea stepDescriptionjTextArea;
    private JButton buttonBack;
    private JButton buttonHelp;

    private JLabel[] stepLabels = new JLabel[0];
    // grab default foreground color for label
    private Color labelFgColor = new JLabel().getForeground();

}
