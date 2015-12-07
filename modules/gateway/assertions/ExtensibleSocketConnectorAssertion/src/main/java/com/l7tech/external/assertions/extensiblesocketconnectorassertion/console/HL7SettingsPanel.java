package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.HL7CodecConfiguration;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import java.text.ParseException;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 4/2/13
 * Time: 10:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class HL7SettingsPanel implements CodecSettingsPanel<HL7CodecConfiguration> {

    private static class HexNumberEditor extends JSpinner.NumberEditor {
        public HexNumberEditor(JSpinner spinner) {
            super(spinner);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(new HexNumberFormatterFactory());
        }
    }

    private static class HexNumberFormatterFactory extends DefaultFormatterFactory {
        public HexNumberFormatterFactory() {
            super(new HexNumberFormatter());
        }
    }

    private static class HexNumberFormatter extends JFormattedTextField.AbstractFormatter {
        @Override
        public Object stringToValue(String text) throws ParseException {
            if (text.startsWith("0x")) {
                return Integer.parseInt("0" + text.substring(2), 16);
            } else {
                return Integer.parseInt(text, 16);
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            Number number = (Number) value;
            return "0x" + Integer.toHexString(number.intValue());
        }
    }

    private JPanel mainPanel;
    private JTextField characterSetTextField;
    private JSpinner startByteSpinner;
    private JSpinner endByte1Spinner;
    private JSpinner endByte2Spinner;

    public HL7SettingsPanel() {
        initComponents();
    }

    @Override
    public boolean validateView() {
        if (characterSetTextField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel, "No character set was specified.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    @Override
    public void updateModel(HL7CodecConfiguration model) {
        model.setCharset(characterSetTextField.getText().trim());
        model.setStartByte(((Number) startByteSpinner.getValue()).byteValue());
        model.setEndByte1(((Number) endByte1Spinner.getValue()).byteValue());
        model.setEndByte2(((Number) endByte2Spinner.getValue()).byteValue());
    }

    @Override
    public void updateView(HL7CodecConfiguration model) {
        characterSetTextField.setText(model.getCharset());
        startByteSpinner.setValue(model.getStartByte());
        endByte1Spinner.setValue(model.getEndByte1());
        endByte2Spinner.setValue(model.getEndByte2());
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;
    }

    private void initComponents() {
        startByteSpinner.setModel(new SpinnerNumberModel(11, 0, 127, 1));
        startByteSpinner.setEditor(new HexNumberEditor(startByteSpinner));
        endByte1Spinner.setModel(new SpinnerNumberModel(28, 0, 127, 1));
        endByte1Spinner.setEditor(new HexNumberEditor(endByte1Spinner));
        endByte2Spinner.setModel(new SpinnerNumberModel(13, 0, 127, 1));
        endByte2Spinner.setEditor(new HexNumberEditor(endByte2Spinner));
    }
}