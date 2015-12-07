package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.ISO8583CodecConfiguration;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583.ISO8583EncoderType;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 18/12/12
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583CodecSettingsPanel implements CodecSettingsPanel<ISO8583CodecConfiguration> {

    private JPanel mainPanel;
    private JComboBox mtiEncodingComboBox;
    private JTextField fieldProFileTextField;
    private JCheckBox secondaryBitmapCheckBox;
    private JCheckBox tertiaryBitmapCheckBox;
    private JPanel bitmapPanel;
    private JPanel mtiPanel;
    private JPanel propertiesPanel;
    private JPanel messageDelimiterPanel;
    private JTextField messageDelimiterField;

    private static final String DELIMITER_FIELD_REGEX = "^[0-9a-fA-F]+$";
    private Pattern pattern = Pattern.compile(DELIMITER_FIELD_REGEX);

    public ISO8583CodecSettingsPanel() {
        initComponents();
    }

    private void initComponents() {

        EnumSet<ISO8583EncoderType> encodingList = EnumSet.allOf(ISO8583EncoderType.class);
        mtiEncodingComboBox.setModel(new DefaultComboBoxModel(encodingList.toArray()));

        tertiaryBitmapCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    secondaryBitmapCheckBox.setSelected(true);
            }
        });
    }

    @Override
    public boolean validateView() {

        //validate the delimiter field.
        //May be empty
        //  or
        //A string contain characters 0-9, a-f, AND A-F of even length
        String messageDelimiter = messageDelimiterField.getText();

        if (messageDelimiter != null && !messageDelimiter.isEmpty()) {
            if ((messageDelimiter.length() % 2) != 0)
                return false;

            Matcher m = pattern.matcher(messageDelimiterField.getText());
            if (!m.find())
                return false;
        }

        return true;
    }

    @Override
    public void updateModel(ISO8583CodecConfiguration model) {
        model.setMtiEncoding((ISO8583EncoderType) mtiEncodingComboBox.getSelectedItem());
        model.setFieldPropertiesFileLocation(fieldProFileTextField.getText());
        model.setMessageDelimiterString(messageDelimiterField.getText());

        if (bitmapPanel.isVisible()) {
            model.setSecondaryBitmapMandatory(secondaryBitmapCheckBox.isSelected());
            model.setTertiaryBitmapMandatory(tertiaryBitmapCheckBox.isSelected());
        } else {
            model.setSecondaryBitmapMandatory(false);
            model.setTertiaryBitmapMandatory(false);
        }
    }

    @Override
    public void updateView(ISO8583CodecConfiguration model) {
        mtiEncodingComboBox.setSelectedItem(model.getMtiEncoding());
        fieldProFileTextField.setText(model.getFieldPropertiesFileLocation());
        secondaryBitmapCheckBox.setSelected(model.isSecondaryBitmapMandatory());
        tertiaryBitmapCheckBox.setSelected(model.isTertiaryBitmapMandatory());
        messageDelimiterField.setText(model.getMessageDelimiterString());

        if (model.isInbound())
            bitmapPanel.setVisible(false);
        else
            bitmapPanel.setVisible(true);
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private void createUIComponents() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
