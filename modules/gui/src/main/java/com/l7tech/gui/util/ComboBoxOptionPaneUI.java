package com.l7tech.gui.util;

import javax.swing.plaf.basic.BasicOptionPaneUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.*;
import java.awt.event.*;

/**
 * This class extends BasicOptionPaneUI and allows {@link JOptionPane} to only use a JComboBox (no more JList) when the
 * method {@link JOptionPane#showInputDialog(java.awt.Component , Object, String, int, Icon, Object[], Object)} is called.
 * In this class, the method {@link ComboBoxOptionPaneUI#getMessage()} gets overridden.
 *
 * As known, {@link BasicOptionPaneUI} is used by UIManager to define whether a JComboBox or JList is used during the
 * method {@link JOptionPane#showInputDialog(java.awt.Component , Object, String, int, Icon, Object[], Object)} being called.
 * If the selectionValues are non null the component and there are < 20 values, it'll be a combobox, if non null and >= 20,
 * it'll be a list, otherwise it'll be a textfield.
 *
 * @author: ghuang
 */
public class ComboBoxOptionPaneUI extends BasicOptionPaneUI {
    public ComboBoxOptionPaneUI() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static ComponentUI createUI(JComponent component) {
        return new ComboBoxOptionPaneUI();
    }

    /**
     * Returns the message to display from the JOptionPane the receiver is
     * providing the look and feel for.
     */
    @Override
    protected Object getMessage() {
        inputComponent = null;
        if (optionPane != null) {
            if (optionPane.getWantsInput()) {
                /* Create a user component to capture the input. If the
               selectionValues are non null the component and no matter
               how many values there are, it'll always be a combobox
               otherwise it'll be a textfield. */
                Object message = optionPane.getMessage();
                Object[] sValues = optionPane.getSelectionValues();
                Object inputValue = optionPane.getInitialSelectionValue();
                JComponent toAdd;

                if (sValues != null) {

                    JComboBox cBox = new JComboBox();

                    cBox.setName("OptionPane.comboBox");
                    for(int counter = 0, maxCounter = sValues.length;
                        counter < maxCounter; counter++) {
                        cBox.addItem(sValues[counter]);
                    }
                    if (inputValue != null) {
                        cBox.setSelectedItem(inputValue);
                    }
                    inputComponent = cBox;
                    toAdd = cBox;
                } else {
                    MultiplexingTextField   tf = new MultiplexingTextField(20);

                    tf.setName("OptionPane.textField");
                    tf.setKeyStrokes(new KeyStroke[] {
                        KeyStroke.getKeyStroke("ENTER") } );
                    if (inputValue != null) {
                        String inputString = inputValue.toString();
                        tf.setText(inputString);
                        tf.setSelectionStart(0);
                        tf.setSelectionEnd(inputString.length());
                    }
                    tf.addActionListener(new TextFieldActionListener());
                    toAdd = inputComponent = tf;
                }

                Object[] newMessage;

                if (message == null) {
                    newMessage = new Object[1];
                    newMessage[0] = toAdd;

                } else {
                    newMessage = new Object[2];
                    newMessage[0] = message;
                    newMessage[1] = toAdd;
                }
                return newMessage;
            }
            return optionPane.getMessage();
        }
        return null;
    }

    /**
     * A JTextField that allows you to specify an array of KeyStrokes that
     * that will have their bindings processed regardless of whether or
     * not they are registered on the JTextField. This is used as we really
     * want the ActionListener to be notified so that we can push the
     * change to the JOptionPane, but we also want additional bindings
     * (those of the JRootPane) to be processed as well.
     */
    private class MultiplexingTextField extends JTextField {
        private KeyStroke[] strokes;

        MultiplexingTextField(int cols) {
            super(cols);
        }

        /**
         * Sets the KeyStrokes that will be additional processed for
         * ancestor bindings.
         */
        void setKeyStrokes(KeyStroke[] strokes) {
            this.strokes = strokes;
        }

        @Override
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            boolean processed = super.processKeyBinding(ks, e, condition, pressed);

            if (processed && condition != JComponent.WHEN_IN_FOCUSED_WINDOW) {
                for (int counter = strokes.length - 1; counter >= 0;
                     counter--) {
                    if (strokes[counter].equals(ks)) {
                        // Returning false will allow further processing
                        // of the bindings, eg our parent Containers will get a
                        // crack at them.
                        return false;
                    }
                }
            }
            return processed;
        }
    }

    private class TextFieldActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            optionPane.setInputValue(((JTextField) e.getSource()).getText());
        }
    }
}
