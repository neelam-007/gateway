package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;

/**
 * The <code>Wizard</code> that drives the wizard step panels.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class Wizard extends JDialog {
    private EventListenerList listenerList = new EventListenerList();
    private WizardStepPanel startPanel;
    private WizardStepPanel currentPanel;

    protected Object wizardInput;

    /** Creates new wizard */
    public Wizard(Frame parent, WizardStepPanel panel) {
        super(parent, false);
        this.startPanel = panel;
        currentPanel = startPanel;
        initComponents();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        mainPanel = new JPanel();
        titlePanel = new JPanel();

        stepsPanel = new JPanel();
        stepsTitlePanel = new JPanel();
        stepPanel = new JPanel();
        stepDescriptionScrollPane = new JScrollPane();
        stepDescriptionTextArea = new JTextArea();

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

        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(new EtchedBorder());
        titlePanel.setLayout(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));

        JLabel titleLabel = new JLabel();
        titleLabel.setBorder(
          new CompoundBorder(new MatteBorder(new Insets(0, 0, 1, 0), new Color(0, 0, 0)),
            new EmptyBorder(new Insets(5, 5, 5, 5))));

        titleLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        titleLabel.setFont(new Font("Dialog", 1, 14));
        titlePanel.add(titleLabel);

        mainPanel.add(titlePanel, BorderLayout.NORTH);

        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        stepsPanel.setLayout(new BoxLayout(stepsPanel, BoxLayout.Y_AXIS));
        stepsPanel.setBackground(new Color(213, 222, 222));
        stepsPanel.setBorder(new EtchedBorder());

        stepsTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        stepsTitlePanel.setPreferredSize(new Dimension(150, 40));
        stepsTitlePanel.setMaximumSize(new Dimension(150, 40));

        stepsTitlePanel.setBackground(new Color(213, 222, 222));
        stepsTitlePanel.
          setBorder(new CompoundBorder(new EmptyBorder(new Insets(5, 5, 5, 5)),
            new MatteBorder(new Insets(0, 0, 1, 0), new Color(0, 0, 0))));

        JLabel stepsLabel = new JLabel();
        stepsLabel.setFont(new Font("Dialog", 1, 14));
        stepsLabel.setText("Steps");
        stepsTitlePanel.add(stepsLabel);
        stepsPanel.add(stepsTitlePanel);
        mainPanel.add(stepsPanel, BorderLayout.WEST);

        stepPanel.setLayout(new BorderLayout());
        stepPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        stepPanel.add(currentPanel, BorderLayout.CENTER);


        stepDescriptionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        stepDescriptionTextArea.setEditable(false);
        stepDescriptionTextArea.setLineWrap(true);
        stepDescriptionTextArea.setWrapStyleWord(true);
        stepDescriptionTextArea.setRows(5);
        stepDescriptionTextArea.setBackground(stepDescriptionScrollPane.getBackground());
        stepDescriptionScrollPane.setViewportView(stepDescriptionTextArea);

        stepPanel.add(stepDescriptionScrollPane, BorderLayout.SOUTH);
        mainPanel.add(stepPanel, BorderLayout.CENTER);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        pack();
        //setSize(new Dimension(800, 500));
    }

    private void formHierarchyChanged(HierarchyEvent evt) {
        int eID = evt.getID();
        long flags = evt.getChangeFlags();

        if (eID == HierarchyEvent.HIERARCHY_CHANGED &&
          ((flags & HierarchyEvent.DISPLAYABILITY_CHANGED) == HierarchyEvent.DISPLAYABILITY_CHANGED)) {
            WizardStepPanel p = currentPanel;
            int i = 0;
            while (p != null) {
                JLabel l = new JLabel("" + (i + 1) + ". " + p.getStepLabel());
                l.setFont(new java.awt.Font("Dialog", 1, 12));
                l.setForeground(Color.WHITE);
                stepsPanel.add(l);
                p = p.nextPanel();
            }
        }
    }

    protected void buttonBackActionPerformed(ActionEvent evt) {
    }

    protected void buttonNextActionPerformed(ActionEvent evt) {
        currentPanel.storeSettings(wizardInput);
        if (currentPanel.hasNextPanel()) {
            stepPanel.remove(currentPanel);
            currentPanel = currentPanel.nextPanel();
            currentPanel.readSettings(wizardInput);
            stepPanel.add(currentPanel, BorderLayout.CENTER);
            stepPanel.updateUI();
        }

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

    protected JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EtchedBorder());
        buttonPanel.add(getButtonBack());
        buttonPanel.add(getButtonNext());
        buttonPanel.add(getButtonFinish());
        buttonPanel.add(getButtonCancel());
        buttonPanel.add(getButtonHelp());
        return buttonPanel;
    }

    protected JButton getButtonCancel() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.setText("Cancel");
        }
        return cancelButton;
    }

    protected JButton getButtonFinish() {
        if (buttonFinish == null) {
            buttonFinish = new JButton();
            buttonFinish.setText("Finish");
        }

        return buttonFinish;
    }

    protected JButton getButtonNext() {
        if (buttonNext == null) {
            buttonNext = new JButton();
            buttonNext.setText("Next");
            buttonNext.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    buttonNextActionPerformed(evt);
                }
            });
        }
        return buttonNext;
    }

    protected JButton getButtonBack() {
        if (buttonBack == null) {
            buttonBack = new JButton();
            buttonBack.setText("Back");
            buttonBack.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    buttonBackActionPerformed(evt);
                }
            });
        }
        return buttonBack;
    }

    protected JButton getButtonHelp() {
        if (buttonHelp == null) {
            buttonHelp = new JButton();
            buttonHelp.setText("Help");
        }
        return buttonHelp;
    }

    private JPanel titlePanel;
    private JScrollPane stepDescriptionScrollPane;
    private JPanel mainPanel;
    private JTextArea stepDescriptionTextArea;

    private JPanel stepsTitlePanel;
    private JPanel stepPanel;
    private JPanel stepsPanel;

    private JButton buttonFinish;
    private JButton cancelButton;
    private JButton buttonNext;
    private JButton buttonBack;
    private JButton buttonHelp;
}
