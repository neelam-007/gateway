package com.l7tech.console.panels;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;

/**
 * The <code>JDialog</code> wizard that drives the publish service
 * use case.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PublishServiceWizard extends JDialog {
    /** the bag of service and assertions that his wizard collects */
    static class ServiceAndAssertion {
        /** @return the service */
        public PublishedService getService() {
            return service;
        }

        public CompositeAssertion getAssertion() {
            return assertions;
        }

        public void setAssertion(CompositeAssertion assertion) {
            this.assertions = assertion;
        }

        public void addAssertion(Assertion a) {
            java.util.List list = new ArrayList(assertions.getChildren());
            list.add(a);
            assertions.setChildren(list);
        }

        private PublishedService service = new PublishedService();
        private CompositeAssertion assertions = new AllAssertion();
    }

    private ServiceAndAssertion saBundle = new ServiceAndAssertion();

    private WizardStepPanel[] panels =
      new WizardStepPanel[]{
          new ServicePanel(),
          new EndpointCredentialsPanel(),
          new IdentityProviderPanel()
      };

    private int currentPanel = 0;
    private EventListenerList listenerList = new EventListenerList();


    /** Creates new form PublishServiceWizard */
    public PublishServiceWizard(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        stepjPanel.add(panels[0], BorderLayout.CENTER);
        for (int i = 0; i < panels.length; i++) {
            panels[i].addChangeListener(changeListener);
        }
    }

    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
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
        stepsTitlejPanel = new javax.swing.JPanel();
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
        buttonFinish.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    for (int i = 0; i < panels.length; i++) {
                        panels[i].readSettings(saBundle);
                    }
                    saBundle.setAssertion(
                      pruneEmptyCompositeAssertions(saBundle.getAssertion()));
                    if (saBundle.getAssertion() !=null) {
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        WspWriter.writePolicy(saBundle.getAssertion(), bo);
                        saBundle.getService().setPolicyXml(bo.toString());
                    } else {
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        WspWriter.writePolicy(new TrueAssertion(), bo); // means no policy
                    }

                    long oid =
                      Registry.getDefault().getServiceManager().save(saBundle.getService());
                    EntityHeader header = new EntityHeader();
                    header.setType(EntityType.SERVICE);
                    header.setName(saBundle.service.getName());
                    header.setOid(oid);
                    PublishServiceWizard.this.notify(header);
                } catch (SaveException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                      "Unable to save the service '" + saBundle.service.getName() + "'\n",
                      "Error",
                      JOptionPane.ERROR_MESSAGE);
                }
                setVisible(false);
                dispose();
            }
        });
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

        stepsTitlejPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        stepsTitlejPanel.setPreferredSize(new Dimension(150, 40));
        stepsTitlejPanel.setMaximumSize(new Dimension(150, 40));

        stepsTitlejPanel.setBackground(new java.awt.Color(213, 222, 222));
        stepsTitlejPanel.
          setBorder(new CompoundBorder(new EmptyBorder(new java.awt.Insets(5, 5, 5, 5)),
            new MatteBorder(new Insets(0, 0, 1, 0), new Color(0, 0, 0))));

        stepsjLabel.setFont(new java.awt.Font("Dialog", 1, 14));
        stepsjLabel.setText("Steps");
        stepsTitlejPanel.add(stepsjLabel);

        stepsjPanel.add(stepsTitlejPanel);

        mainjPanel.add(stepsjPanel, BorderLayout.WEST);


        stepjPanel.setLayout(new BorderLayout());
        stepjPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        stepDescriptionjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        stepDescriptionjTextArea.setEditable(false);
        stepDescriptionjTextArea.setLineWrap(true);
        stepDescriptionjTextArea.setWrapStyleWord(true);
        stepDescriptionjTextArea.setRows(5);
        stepDescriptionjTextArea.setBackground(stepDescriptionjScrollPane.getBackground());
        stepDescriptionjScrollPane.setViewportView(stepDescriptionjTextArea);

        stepjPanel.add(stepDescriptionjScrollPane, BorderLayout.SOUTH);

        mainjPanel.add(stepjPanel, BorderLayout.CENTER);

        getContentPane().add(mainjPanel, BorderLayout.CENTER);

        pack();
        setSize(new Dimension(800, 500));
        Utilities.centerOnScreen(this);
    }

    /**
     * Prune empty composite assertions, and return the updated
     * asseriton tree.
     * If the root composite has no children return null.
     *
     * @param oom the input composite assertion
     * @return trhe composite assertion with pruned children
     * or null
     */
    private CompositeAssertion
      pruneEmptyCompositeAssertions(CompositeAssertion oom) {
        if (oom.getChildren().isEmpty()) return null;
        Iterator i = oom.preorderIterator();
        for (; i.hasNext();) {
            Assertion a = (Assertion)i.next();
            if (a instanceof CompositeAssertion) {
                CompositeAssertion ca = (CompositeAssertion)a;
                if (ca.getChildren().size() == 0) {
                    i.remove();
                }
            }
        }
        return oom;
    }

    /**
     * notfy the listeners
     * @param header
     */
    private void notify(EntityHeader header) {
        EntityEvent event = new EntityEvent(header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener)listeners[i]).entityAdded(event);
        }
    }

    private void formHierarchyChanged(HierarchyEvent evt) {
        int eID = evt.getID();
        long flags = evt.getChangeFlags();

        if (eID == evt.HIERARCHY_CHANGED &&
          ((flags & evt.DISPLAYABILITY_CHANGED) == evt.DISPLAYABILITY_CHANGED)) {
            if (PublishServiceWizard.this.isDisplayable()) {
                stepLabels = new JLabel[panels.length];

                for (int i = 0; i < panels.length; i++) {
                    stepLabels[i] = new JLabel(""+(i+1)+". "+panels[i].getStepLabel());
                    stepLabels[i].setFont(new java.awt.Font("Dialog", 1, 12));
                    stepLabels[i].setForeground(Color.WHITE);
                    stepsjPanel.add(stepLabels[i]);
                }
                stepsjPanel.add(Box.createGlue());
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
        buttonFinish.setEnabled(currentPanel > 0 && panels[currentPanel].isValid());
        buttonBack.setEnabled(currentPanel > 0);
        buttonNext.setEnabled(currentPanel < panels.length - 1 && panels[currentPanel].isValid());
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
                stepLabels[i].setForeground(Color.BLACK);
            } else {
                stepLabels[i].setForeground(Color.WHITE);
            }
        }
    }

    /**
     * the change listener collects state changes from 'step' panels
     */
    private final ChangeListener changeListener = new ChangeListener() {
        /**
         * Invoked when the target of the listener has changed its state.
         *
         * @param e  a ChangeEvent object
         */
        public void stateChanged(ChangeEvent e) {
            updateWizardUiState();
        }

    };
    private JPanel titlePanel;
    private JLabel stepsjLabel;
    private JScrollPane stepDescriptionjScrollPane;
    private JPanel mainjPanel;
    private JPanel panelButtons;
    private JLabel titlejLabel;
    private JButton buttonFinish;
    private JPanel stepsjPanel;
    private JPanel stepsTitlejPanel;
    private JPanel stepjPanel;
    private JButton cancelButton;
    private JSeparator jSeparator1;
    private JButton buttonNext;
    private JTextArea stepDescriptionjTextArea;
    private JButton buttonBack;
    private JButton buttonHelp;

    private JLabel[] stepLabels = new JLabel[0];


}
