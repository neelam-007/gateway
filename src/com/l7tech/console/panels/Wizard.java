package com.l7tech.console.panels;

import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.common.gui.util.FontUtil;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.util.EventListener;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;

/**
 * The <code>Wizard</code> that drives the wizard step panels.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class Wizard extends JDialog {
    private WizardStepPanel startPanel;
    private Wizard.Iterator wizardIterator;
    protected Object wizardInput;
    private boolean wasCanceled = false;
    private boolean enableBackButton = true;
    private boolean showDescription;

    private JPanel mainPanel;
    private JPanel stepLabelsPanel;
    private JPanel wizardStepPanel;
    private JPanel descriptionPanel;
    private JTextPane stepDescriptionTextPane;
    private JButton buttonFinish;
    private JButton cancelButton;
    private JButton buttonNext;
    private JButton buttonBack;
    private JButton buttonHelp;

    protected EventListenerList listenerList = new EventListenerList();

    /**
     * Creates new wizard
     */
    public Wizard(Frame parent, WizardStepPanel panel) {
        super(parent, true);
        initialize(panel);
    }

    public Wizard(Dialog parent, WizardStepPanel panel) {
        super(parent, true);
        initialize(panel);
    }

    /**
     * is show description enabled for the panel steps
     *
     * @return true if enabled, false otherwise
     */
    public boolean isShowDescription() {
        return showDescription;
    }

    /**
     * set hte description enabled property
     *
     * @param b the show description property
     */
    public void setShowDescription(boolean b) {
        this.showDescription = b;
    }

    /**
     * access the information that the wizard collected
     *
     * @return the wizard information object, typically user
     *         entered data
     */
    public Object getWizardInput() {
        return wizardInput;
    }

    /**
     * instruct wizard to collect the information
     */
    protected final void collect() {
        Wizard.Iterator it = new Wizard.Iterator(startPanel);
        while (it.hasNext()) {
            WizardStepPanel p = it.next();
            p.readSettings(wizardInput);
            p.storeSettings(wizardInput);
        }
    }


    /**
     * Adds the specified wizard listener to receive events from
     * this wizard.
     *
     * @param l the wizard listener
     */
    public synchronized void addWizardListener(WizardListener l) {
        listenerList.add(WizardListener.class, l);
    }

    /**
     * Removes the specified wizard listener so that it no longer
     * receives events from this wizard.
     *
     * @param l the wizard listener
     */
    public synchronized void removeWizardListener(WizardListener l) {
        listenerList.remove(WizardListener.class, l);
    }

    /**
     * @return the current wizard panel
     */
    public WizardStepPanel getSelectedWizardPanel() {
        return wizardIterator.current();
    }


    /**
     * This method is called from within the constructor to
     * layout the form.
     */
    private void layoutComponents() {
        getContentPane().setLayout(new BorderLayout());
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(new EtchedBorder());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                cancel(new ActionEvent(Wizard.this, 0, ""));
            }
        });

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));

        JLabel titleLabel = new JLabel();
        titleLabel.setBorder(
          new CompoundBorder(new MatteBorder(new Insets(0, 0, 1, 0), Color.BLACK),
                             new EmptyBorder(new Insets(5, 5, 5, 5))));

        titleLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(Box.createHorizontalStrut(780), BorderLayout.NORTH);

        mainPanel.add(titlePanel, BorderLayout.NORTH);


        stepLabelsPanel = new JPanel();
        stepLabelsPanel.setLayout(new BoxLayout(stepLabelsPanel, BoxLayout.Y_AXIS));

        Color stepsPanelColor = new Color(213, 222, 222);

        stepLabelsPanel.setBackground(stepsPanelColor);
        stepLabelsPanel.setBorder(new EtchedBorder());


        JPanel stepsTitlePanel = new JPanel();
        stepsTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        stepsTitlePanel.setPreferredSize(new Dimension(100, 40));
        stepsTitlePanel.setMaximumSize(new Dimension(150, 40));

        stepsTitlePanel.setBackground(stepsPanelColor);

        /*        stepsTitlePanel.
                  setBorder(new CompoundBorder(
                    new EmptyBorder(new Insets(5, 5, 5, 5)),
                    new MatteBorder(new Insets(0, 0, 1, 0), Color.BLACK))
                  );*/

        //steps label panel
        JLabel stepsLabel = new JLabel("Steps");
        Font font = stepsLabel.getFont();
        font = font.deriveFont(Font.BOLD, 14);
        stepsLabel.setFont(font);
        stepsTitlePanel.add(stepsLabel);
        stepLabelsPanel.add(stepsTitlePanel);
        int i = 0;
        WizardStepPanel p = startPanel;
        java.util.List<WizardLabel> stepsLabels = new ArrayList<WizardLabel>();
        while (p != null) {
            String label = "" + (++i) + ". " + p.getStepLabel();
            WizardLabel l = new WizardLabel(label, p, true);
            stepsLabels.add(l);
            stepLabelsPanel.add(l);
            addWizardListener(l);
            p = p.nextPanel();
        }

        JPanel sizedStepPanel = new JPanel();
        sizedStepPanel.setLayout(new BorderLayout());
        sizedStepPanel.add(Box.createVerticalStrut(420), BorderLayout.WEST);
        sizedStepPanel.add(stepLabelsPanel, BorderLayout.CENTER);
        mainPanel.add(sizedStepPanel, BorderLayout.WEST);

        // the wizard step panel
        wizardStepPanel = new JPanel();
        wizardStepPanel.setLayout(new BorderLayout());
        wizardStepPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));

        JScrollPane descScrollPane = new JScrollPane();
        descScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JEditorPane da = getStepDescriptionTextPane();
        descScrollPane.setPreferredSize(new Dimension(640, 100));
        descScrollPane.setViewportView(da);
        descScrollPane.setBorder(new CompoundBorder(new EmptyBorder(new java.awt.Insets(10, 0, 0, 0)), new LineBorder(Color.GRAY)));
        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BorderLayout());
        descriptionPanel.add(descScrollPane, BorderLayout.CENTER);
        descriptionPanel.add(Box.createVerticalStrut(100), BorderLayout.WEST);
        wizardStepPanel.add(descriptionPanel, BorderLayout.SOUTH);

        mainPanel.add(wizardStepPanel, BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * initialize the wizard state
     */
    private void initialize(WizardStepPanel panel) {

        setEscKeyStrokeDisposes(this);
        if (panel == null) {
            throw new IllegalArgumentException("panel == null");
        }
        this.startPanel = panel;
        wizardIterator = new Wizard.Iterator(startPanel);
        layoutComponents();

        // add stepStateListener to the startPanel
        initializePanel(startPanel);

        // add stepStateListener to each subsequent panel
        for (Iterator it = new Wizard.Iterator(startPanel); it.hasNext();) {
            WizardStepPanel p = it.next();
            initializePanel(p);
        }

        Runnable runnable = new Runnable() {
            public void run() {
                WizardStepPanel next = wizardIterator.current();
                if (next != null) {
                    selectWizardPanel(null, next);
                }
            }
        };

        // run this now to ensure that the first panel is in place before
        // any call to pack()
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            }
            catch(InterruptedException ie) {
                Thread.currentThread().interrupt();                
            }
            catch(InvocationTargetException ite) {
                throw new RuntimeException(ite.getCause());
            }
        }

        // have to run this later since input will not be available yet ..
        Runnable wizardInputInit = new Runnable() {
            public void run() {
                WizardStepPanel next = wizardIterator.current();
                if (wizardInput != null && next != null) {
                    next.readSettings(wizardInput);
                    updateWizardControls(next);
                }
            }
        };
        SwingUtilities.invokeLater(wizardInputInit);
    }

    /**
     * Initialize a panel
     */
    private void initializePanel(WizardStepPanel panel) {
        panel.addChangeListener(stepStateListener);
        panel.setOwner(this);
    }

    /**
     * Update the input map of the JDialog's <code>JLayeredPane</code> so
     * the ESC keystroke  invoke dispose on the dialog.
     *
     * NOTE: This method is duplicated from the Actions class to allow reuse
     * of the Wizard framework without requiring all the other console classes.
     *
     * @param d the dialog
     */
    protected static void setEscKeyStrokeDisposes(final JDialog d) {
        JLayeredPane layeredPane = d.getLayeredPane();
        final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, "close-it");
        layeredPane.getActionMap().put("close-it",
          new AbstractAction() {
              public void actionPerformed(ActionEvent evt) {
                  d.dispose();
              }
          });
    }

    /**
     * advance the wizard
     */
    protected void advance(ActionEvent evt) {
        WizardStepPanel current = wizardIterator.current();

        WizardStepPanel next = null;
        if (wizardInput != null) {
            current.storeSettings(wizardInput);
        }
        while (wizardIterator.hasNext()) {
            next = wizardIterator.next();
            // give the wizard object to the panel, so the panel can decide whether is skipped or not
            if (wizardInput != null) {
                next.readSettings(wizardInput);
            }
            if (!next.isSkipped()) {
                break;
            }
        }

        if (next != null) {
            selectWizardPanel(current, next);
        }
    }

    /**
     * reverse the wizard
     */
    protected void reverse(ActionEvent evt) {
        WizardStepPanel current = wizardIterator.current();
        if (wizardInput != null) {
            current.storeSettings(wizardInput);
        }

        WizardStepPanel previous = null;
        while (wizardIterator.hasPrevious()) {
            previous = wizardIterator.previous();
            // give the wizard object to the panel, so the panel can decide whether is skipped or not
            if (wizardInput != null) {
                previous.readSettings(wizardInput);
            }
            if (!previous.isSkipped()) {
                break;
            }
        }

        if (previous != null) {
            selectWizardPanel(current, previous);
        }
    }

    /**
     * cancel the wizard
     */
    protected void cancel(ActionEvent evt) {
        dispose();
        fireWizardCanceled();
    }

    /**
     * cancel the wizard
     */
    protected void finish(ActionEvent evt) {
        if (wizardInput != null) {
            wizardIterator.current().storeSettings(wizardInput);
        }
        dispose();
        fireWizardFinished();
    }

    /**
     * Notifies all listeners that the selected wizard panel has changed
     *
     * @param p the panel to which the selection has changed
     */
    private void fireSelectionChanged(WizardStepPanel p) {
        EventListener[] listeners = listenerList.getListeners(WizardListener.class);
        WizardEvent we = new WizardEvent(p, WizardEvent.SELECTION_CHANGED);
        for (int i = 0; i < listeners.length; i++) {
            ((WizardListener)listeners[i]).wizardSelectionChanged(we);
        }
    }

    public boolean wasCanceled() {
        return wasCanceled;
    }

    /**
     * Notifies all the listeners that the wizard has been canceled
     */
    private void fireWizardCanceled() {
        wasCanceled = true;
        EventListener[] listeners = listenerList.getListeners(WizardListener.class);
        WizardEvent we = new WizardEvent(this, WizardEvent.CANCELED);
        for (int i = 0; i < listeners.length; i++) {
            ((WizardListener)listeners[i]).wizardCanceled(we);
        }
    }

    /**
     * Notifies all the listeners that the wizard has been canceled
     */
    private void fireWizardFinished() {
        EventListener[] listeners = listenerList.getListeners(WizardListener.class);
        WizardEvent we = new WizardEvent(this, WizardEvent.FINISHED);
        for (int i = 0; i < listeners.length; i++) {
            ((WizardListener)listeners[i]).wizardFinished(we);
        }
    }

    /**
     * the listener that is registered with each panel
     */
    private ChangeListener stepStateListener = new ChangeListener() {
        /**
         * Invoked when the target of the listener has changed its state.
         *
         * @param e a ChangeEvent object
         */
        public void stateChanged(ChangeEvent e) {
            Object source = e.getSource();
            if (source instanceof WizardStepPanel) {
                updateWizardControls((WizardStepPanel)source);
            }
        }
    };

    /**
     * select the wizard panel
     *
     * @param current the current panel
     * @param next    the next panel
     */
    private void selectWizardPanel(WizardStepPanel current, WizardStepPanel next) {
        if (next == null) {
            throw new IllegalArgumentException("next == null");
        }
        descriptionPanel.setVisible(next.isShowDescriptionPanel());
        if (current != null) {
            wizardStepPanel.remove(current);
        }
        wizardStepPanel.add(next, BorderLayout.CENTER);
        updateWizardControls(next);
        validate();
        fireSelectionChanged(next);
    }

    /**
     * updates the wizard controls with the state from the
     * panel parameter
     * Default Wizard deals with the standard buttons. Wizards
     * that provide more controls (wizard buttons for example)
     * override this method.
     *
     * @param wp the wizard panel
     */
    protected void updateWizardControls(WizardStepPanel wp) {
        //todo: rework wizard to allow 'finish' to be set dynamically
        /*
        boolean finishEnabled = true;
        Wizard.Iterator it = new Wizard.Iterator(startPanel);
        while (it.hasNext()) {
            WizardStepPanel p = it.next();
            finishEnabled = p.canFinish() && p.canAdvance();
        }

        buttonFinish.setEnabled(finishEnabled);

        */
        buttonFinish.setEnabled(wp.canFinish());
        buttonNext.setEnabled(wp.canAdvance() && wizardIterator.hasNext());
        buttonBack.setEnabled(wizardIterator.hasPrevious() && enableBackButton);
        stepDescriptionTextPane.setText(getSelectedWizardPanel().getDescription());
        wizardStepPanel.updateUI();
    }


    /**
     * @return the description area
     */
    private JTextPane getStepDescriptionTextPane() {
        if (stepDescriptionTextPane != null)
            return stepDescriptionTextPane;

        stepDescriptionTextPane = new JTextPane();
        stepDescriptionTextPane.setEditorKit(new HTMLEditorKit());
        stepDescriptionTextPane.setEditable(false);
        stepDescriptionTextPane.setOpaque(false);
        addWizardListener(new WizardAdapter() {
            /**
             * Invoked when the wizard page has been changed.
             *
             * @param e the event describing the selection change
             */
            public void wizardSelectionChanged(WizardEvent e) {
                WizardStepPanel wp = (WizardStepPanel)e.getSource();
                stepDescriptionTextPane.setText(wp.getDescription());
                stepDescriptionTextPane.setCaretPosition(0);
            }
        });

        return stepDescriptionTextPane;
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
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    cancel(evt);
                }
            });
        }
        return cancelButton;
    }

    protected JButton getButtonFinish() {
        if (buttonFinish == null) {
            buttonFinish = new JButton();
            buttonFinish.setText("Finish");
            buttonFinish.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (wizardIterator.current().onNextButton()) {
                        finish(evt);
                    }
                }
            });
        }
        return buttonFinish;
    }


    protected JButton getButtonNext() {
        if (buttonNext == null) {
            buttonNext = new JButton();
            buttonNext.setText("Next");
            buttonNext.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (wizardIterator.current().onNextButton()) {
                        advance(evt);
                    }
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
                    reverse(evt);
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

    public boolean isEnableBackButton() {
        return enableBackButton;
    }

    public void setEnableBackButton(boolean enableBackButton) {
        this.enableBackButton = enableBackButton;
        getButtonBack().setEnabled(enableBackButton);
    }

    /**
     * the wizard iterator that allows wizard panels traversal
     * in each direction.
     */
    public static class Iterator {
        private WizardStepPanel first = null;
        private WizardStepPanel previous = null;
        private WizardStepPanel current = null;

        /**
         * construct the iteratir with the starting panel
         *
         * @param panel the start paanel
         */
        public Iterator(WizardStepPanel panel) {
            if (panel == null) {
                throw new IllegalArgumentException();
            }
            first = panel;
            current = panel;
        }

        /**
         * Returns <tt>true</tt> if this iterator has more elements when
         * traversing the list in the forward direction.
         *
         * @return <tt>true</tt> if the list iterator has more elements when
         *         traversing the list in the forward direction.
         */
        public boolean hasNext() {
            return current.nextPanel() != null;
        }

        /**
         * Returns <tt>true</tt> if this list iterator has more elements when
         * traversing the list in the reverse direction.
         *
         * @return <tt>true</tt> if the list iterator has more elements when
         *         traversing the list in the reverse direction.
         */
        public boolean hasPrevious() {
            return previous != null;
        }

        /**
         * Returns the next element in the list.  This method may be called
         * repeatedly to iterate through the list, or intermixed with calls to
         * <tt>previous</tt> to go back and forth.
         *
         * @return the next element in the list.
         * @throws java.util.NoSuchElementException
         *          if the iteration has no next element.
         */
        public WizardStepPanel next() {
            if (hasNext()) {
                previous = current;
                current = current.nextPanel();
                return current;
            }
            throw new NoSuchElementException();
        }

        /**
         * @return the current element in the list.
         */
        public WizardStepPanel current() {
            return current;
        }

        /**
         * Returns the previous element in the list.  This method may be called
         * repeatedly to iterate through the list backwards, or intermixed with
         * calls to <tt>next</tt> to go back and forth.
         *
         * @return the previous element in the list.
         * @throws java.util.NoSuchElementException
         *          if the iteration has no previous
         *          element.
         */
        public WizardStepPanel previous() {
            if (hasPrevious()) {
                current = previous;
                previous = null;
                for (WizardStepPanel p = first; p != null; p = p.nextPanel()) {
                    if (p.nextPanel() == current) {
                        previous = p;
                    }
                }
                return current;
            }
            throw new NoSuchElementException();

        }
    }

    private class WizardLabel extends JLabel implements WizardListener {
        private WizardStepPanel wizardPanel;
        boolean selected;
        Font initialFont;

        WizardLabel(String label, WizardStepPanel panel, boolean selected) {
            this.wizardPanel = panel;
            setText(label);
            initialFont = getFont();
            setSelected(selected);
        }

        /**
         * @return true if the label is selected ,false otherwise
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * @param b the new selected propert
         */
        public void setSelected(boolean b) {
            selected = b;
            setFont( selected ? FontUtil.emboldenFont(initialFont) : initialFont);
        }

        /**
         * Calculate the preferred size based on the bold font which is
         * presumably wider than the plain font.
         */
        public Dimension getPreferredSize() {
            Dimension dimension;

            boolean embolden = !selected;
            if (embolden) {
                setSelected(true);
            }

            dimension = super.getPreferredSize();

            if (embolden) {
                setSelected(false);
            }

            return dimension;
        }

        /**
         * Invoked when the wizard page has been changed.
         *
         * @param e the event describing the selection change
         */
        public void wizardSelectionChanged(WizardEvent e) {
            setSelected(e.getSource() == wizardPanel);
        }

        /**
         * Invoked when the wizard has finished.
         *
         * @param e the event describing the wizard finish
         */
        public void wizardFinished(WizardEvent e) {
        }

        /**
         * Invoked when the wizard has been cancelled.
         *
         * @param e the event describinng the wizard cancel
         */
        public void wizardCanceled(WizardEvent e) {
        }
    }
}
