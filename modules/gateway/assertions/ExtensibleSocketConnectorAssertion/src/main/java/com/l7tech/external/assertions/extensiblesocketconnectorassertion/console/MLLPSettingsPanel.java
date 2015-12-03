package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.MLLPCodecConfiguration;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import java.text.ParseException;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 02/12/11
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class MLLPSettingsPanel implements CodecSettingsPanel<MLLPCodecConfiguration> {
    private JPanel mainPanel;
    private JSpinner startByteField;
    private JSpinner endByte1Field;
    private JSpinner endByte2Field;

    public MLLPSettingsPanel() {
        initComponents();
    }

    private void initComponents() {
        startByteField.setModel(new SpinnerNumberModel(11, 0, 127, 1));
        startByteField.setEditor(new HexNumberEditor(startByteField));
        endByte1Field.setModel(new SpinnerNumberModel(28, 0, 127, 1));
        endByte1Field.setEditor(new HexNumberEditor(endByte1Field));
        endByte2Field.setModel(new SpinnerNumberModel(13, 0, 127, 1));
        endByte2Field.setEditor(new HexNumberEditor(endByte2Field));
    }

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

    @Override
    public boolean validateView() {
        return true;
    }

    @Override
    public void updateModel(MLLPCodecConfiguration model) {
        model.setStartByte(((Number) startByteField.getValue()).byteValue());
        model.setEndByte1(((Number) endByte1Field.getValue()).byteValue());
        model.setEndByte2(((Number) endByte2Field.getValue()).byteValue());
    }

    @Override
    public void updateView(MLLPCodecConfiguration model) {
        startByteField.setValue(model.getStartByte());
        endByte1Field.setValue(model.getEndByte1());
        endByte2Field.setValue(model.getEndByte2());
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;
    }
}
