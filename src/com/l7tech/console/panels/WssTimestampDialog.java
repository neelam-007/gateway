package com.l7tech.console.panels;

import java.util.ResourceBundle;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.Format;
import java.text.ParsePosition;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.NumberFormatter;
import javax.swing.text.DefaultFormatter;

import com.l7tech.policy.assertion.xmlsec.WssTimestamp;
import com.l7tech.console.action.Actions;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.util.TimeUnit;


/**
 * Property dialog for WSS Timestamp Assertion.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WssTimestampDialog extends JDialog {

    //- PUBLIC

    public WssTimestampDialog(Frame owner, boolean modal, WssTimestamp assertion) {
        super(owner, resources.getString("dialog.title"), modal);
        this.wssTimestamp = assertion;
        init();
    }

    public WssTimestampDialog(Dialog owner, boolean modal, WssTimestamp assertion) {
        super(owner, resources.getString("dialog.title"), modal);
        this.wssTimestamp = assertion;
        init();
    }

    //- PRIVATE

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.WssTimestampDialog");

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JFormattedTextField requestExpiryTextField;
    private JFormattedTextField responseExpiryTextField;
    private JComboBox requestTimeUnitComboBox;
    private JComboBox responseTimeUnitComboBox;

    private final WssTimestamp wssTimestamp;

    private void init() {
        add(mainPanel);

        DefaultComboBoxModel comboModelReq = new DefaultComboBoxModel(TimeUnit.ALL);
        DefaultComboBoxModel comboModelRes = new DefaultComboBoxModel(TimeUnit.ALL);

        requestTimeUnitComboBox.setModel(comboModelReq);
        requestTimeUnitComboBox.setSelectedItem(wssTimestamp.getRequestTimeUnit());

        responseTimeUnitComboBox.setModel(comboModelRes);
        responseTimeUnitComboBox.setSelectedItem(wssTimestamp.getResponseTimeUnit());

        JFormattedTextField.AbstractFormatterFactory formatter = new JFormattedTextField.AbstractFormatterFactory() {
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                NumberFormatter nf = new NumberFormatter(){
                    public Format getFormat() {
                        return new DecimalFormat("#.####") {
                            public Object parseObject(String source) throws ParseException {
                                ParsePosition pp = new ParsePosition(0);
                                Object parsed = super.parseObject(source, pp);
                                if (pp.getIndex() != source.length()) {
                                    throw new ParseException("Invalid characters", pp.getIndex());
                                }
                                return parsed;
                            }
                        };
                    }
                };
                nf.setValueClass(Double.class);
                return nf;
            }
        };

        RunOnChangeListener rocl = new RunOnChangeListener(new Runnable(){public void run(){update();}});

        requestExpiryTextField.setFormatterFactory(formatter);
        requestExpiryTextField.setValue(new Double((double)wssTimestamp.getRequestMaxExpiryMilliseconds() / wssTimestamp.getRequestTimeUnit().getMultiplier()));
        requestExpiryTextField.getDocument().addDocumentListener(rocl);

        responseExpiryTextField.setFormatterFactory(formatter);
        responseExpiryTextField.setValue(new Double((double)wssTimestamp.getResponseExpiryMilliseconds() / wssTimestamp.getResponseTimeUnit().getMultiplier()));
        responseExpiryTextField.getDocument().addDocumentListener(rocl);

        requestTimeUnitComboBox.addItemListener(rocl);
        requestTimeUnitComboBox.addItemListener(rocl);

        cancelButton.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent ae){setVisible(false);} });
        okButton.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent ae){setVisible(false); commit();} });
    }

    private void update() {
        if (requestExpiryTextField.isEditValid() &&
            responseExpiryTextField.isEditValid()) {
            okButton.setEnabled(true);
        }
        else {
            okButton.setEnabled(false);
        }
    }

    private void commit() {
        wssTimestamp.setRequestTimeUnit((TimeUnit)requestTimeUnitComboBox.getSelectedItem());
        wssTimestamp.setResponseTimeUnit((TimeUnit)responseTimeUnitComboBox.getSelectedItem());
        wssTimestamp.setRequestMaxExpiryMilliseconds(toMillis((Double)requestExpiryTextField.getValue(), wssTimestamp.getRequestTimeUnit()));
        wssTimestamp.setResponseExpiryMilliseconds(toMillis((Double)responseExpiryTextField.getValue(), wssTimestamp.getResponseTimeUnit()));
    }

    private int toMillis(Double value, TimeUnit unit) {
        return (int)(value.doubleValue() * unit.getMultiplier());
    }
}
