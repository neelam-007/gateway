package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.LengthPrefixedCodecConfiguration;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/11/12
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class LengthPrefixedCodecSettingsPanel implements CodecSettingsPanel<LengthPrefixedCodecConfiguration> {
    private JPanel mainPanel;
    private JSpinner lengthBytesField;

    public LengthPrefixedCodecSettingsPanel() {
        initComponents();
    }

    private void initComponents() {
        lengthBytesField.setModel(new SpinnerNumberModel(4, 1, 4, 1));
    }

    public boolean validateView() {
        return true;
    }

    public void updateModel(LengthPrefixedCodecConfiguration model) {
        model.setLengthBytes(((Number) lengthBytesField.getValue()).byteValue());
    }

    public void updateView(LengthPrefixedCodecConfiguration model) {
        lengthBytesField.setValue(model.getLengthBytes());
    }

    public JPanel getPanel() {
        return mainPanel;
    }
}
