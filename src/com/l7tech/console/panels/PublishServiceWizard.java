package com.l7tech.console.panels;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
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

        public void setRoutingAssertion(RoutingAssertion ra) {
            routingAssertion = ra;
        }

        public RoutingAssertion getRoutingAssertion() {
            return routingAssertion;
        }

        private RoutingAssertion routingAssertion = null;
        private PublishedService service = new PublishedService();
        private CompositeAssertion assertions = new AllAssertion();
    }

    private ServiceAndAssertion saBundle = new ServiceAndAssertion();

    private WizardStepPanel[] panels =
      new WizardStepPanel[]{
          new ServicePanel(),
          new EndpointCredentialsPanel(),
          new IdentityProviderWizardPanel()
      };

    private int currentPanel = 0;
    private EventListenerList listenerList = new EventListenerList();


    /** Creates new form PublishServiceWizard */
    public PublishServiceWizard(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        stepPanel.add(panels[0], BorderLayout.CENTER);
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
        titleLabel = new JLabel();
        panelButtons = new JPanel();
        buttonBack = new JButton();
        buttonNext = new JButton();
        buttonFinish = new JButton();
        cancelButton = new JButton();
        buttonHelp = new JButton();
        stepsjPanel = new JPanel();
        stepsTitlePanel = new JPanel();
        stepsLabel = new JLabel();
        stepPanel = new JPanel();
        stepDescriptionScrollPane = new JScrollPane();
        stepDescriptionTextArea = new JTextArea();

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
        titleLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        titleLabel.setText("Publish Service wizard");
        titlePanel.add(titleLabel);

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
                    // routing assertion?
                    if (saBundle.getRoutingAssertion() != null) {
                        java.util.List ass = new ArrayList();
                        ass.addAll(saBundle.getAssertion().getChildren());
                        ass.add(saBundle.getRoutingAssertion());
                        saBundle.getAssertion().setChildren(ass);
                    }
                    saBundle.setAssertion(
                      pruneEmptyCompositeAssertions(saBundle.getAssertion()));
                    if (saBundle.getAssertion() != null) {
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

        stepsTitlePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        stepsTitlePanel.setPreferredSize(new Dimension(150, 40));
        stepsTitlePanel.setMaximumSize(new Dimension(150, 40));

        stepsTitlePanel.setBackground(new java.awt.Color(213, 222, 222));
        stepsTitlePanel.
          setBorder(new CompoundBorder(new EmptyBorder(new java.awt.Insets(5, 5, 5, 5)),
            new MatteBorder(new Insets(0, 0, 1, 0), new Color(0, 0, 0))));

        stepsLabel.setFont(new java.awt.Font("Dialog", 1, 14));
        stepsLabel.setText("Steps");
        stepsTitlePanel.add(stepsLabel);

        stepsjPanel.add(stepsTitlePanel);

        mainjPanel.add(stepsjPanel, BorderLayout.WEST);


        stepPanel.setLayout(new BorderLayout());
        stepPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        stepDescriptionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        stepDescriptionTextArea.setEditable(false);
        stepDescriptionTextArea.setLineWrap(true);
        stepDescriptionTextArea.setWrapStyleWord(true);
        stepDescriptionTextArea.setRows(5);
        stepDescriptionTextArea.setBackground(stepDescriptionScrollPane.getBackground());
        stepDescriptionScrollPane.setViewportView(stepDescriptionTextArea);

        stepPanel.add(stepDescriptionScrollPane, BorderLayout.SOUTH);

        mainjPanel.add(stepPanel, BorderLayout.CENTER);

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
                    stepLabels[i] = new JLabel("" + (i + 1) + ". " + panels[i].getStepLabel());
                    stepLabels[i].setFont(new java.awt.Font("Dialog", 1, 12));
                    stepLabels[i].setForeground(Color.WHITE);
                    stepsjPanel.add(stepLabels[i]);
                }
                stepsjPanel.add(Box.createGlue());
                updateWizardUiState();
            }
        }
    }

    private void readThenStoreSettings() {
        for (int i = 0; i < panels.length; i++) {
            panels[i].readSettings(saBundle);
        }
        for (int i = 0; i < panels.length; i++) {
            panels[i].storeSettings(saBundle);
        }
    }

    private void buttonBackActionPerformed(ActionEvent evt) {
        // Add your handling code here:
        stepPanel.remove(panels[currentPanel]);
        stepPanel.add(panels[--currentPanel], BorderLayout.CENTER);
        stepPanel.updateUI();
        updateWizardUiState();
        readThenStoreSettings();

    }

    private void buttonNextActionPerformed(ActionEvent evt) {
        stepPanel.remove(panels[currentPanel]);
        stepPanel.add(panels[++currentPanel], BorderLayout.CENTER);
        stepPanel.updateUI();
        updateWizardUiState();
        readThenStoreSettings();
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
            stepDescriptionTextArea.setText(panels[currentPanel].getDescription());
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
    private JLabel stepsLabel;
    private JScrollPane stepDescriptionScrollPane;
    private JPanel mainjPanel;
    private JPanel panelButtons;
    private JLabel titleLabel;
    private JButton buttonFinish;
    private JPanel stepsjPanel;
    private JPanel stepsTitlePanel;
    private JPanel stepPanel;
    private JButton cancelButton;
    private JButton buttonNext;
    private JTextArea stepDescriptionTextArea;
    private JButton buttonBack;
    private JButton buttonHelp;

    private JLabel[] stepLabels = new JLabel[0];


}
