package com.l7tech.external.assertions.websocket.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntityAdmin;
import com.l7tech.external.assertions.websocket.WebSocketMessageInjectionAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebSocketMessageInjectionDialog extends AssertionPropertiesEditorSupport<WebSocketMessageInjectionAssertion> {
    protected static final Logger logger = Logger.getLogger(WebSocketMessageInjectionDialog.class.getName());

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox serviceComboBox;
    private JTextField clientIdTextField;
    private JRadioButton inboundRadioButton;
    private JRadioButton outboundRadioButton;
    private JRadioButton textRadioButton;
    private JRadioButton binaryRadioButton;
    private JTextField messageBodyTextField;
    private JCheckBox broadcastCheckBox;
    private JTextField subprotcolTextField;
    private JCheckBox deliveryFailureCheckBox;

    public WebSocketMessageInjectionDialog(Window owner) {
        super(owner, "Send WebSocket Message Configuration", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        inboundRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outboundRadioButton.setSelected(!inboundRadioButton.isSelected());
            }
        });

        outboundRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inboundRadioButton.setSelected(!outboundRadioButton.isSelected());
            }
        });

        textRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                binaryRadioButton.setSelected(!textRadioButton.isSelected());
            }
        });

        binaryRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textRadioButton.setSelected(!binaryRadioButton.isSelected());
            }
        });

        broadcastCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientIdTextField.setEnabled(!broadcastCheckBox.isSelected());
                deliveryFailureCheckBox.setEnabled(!broadcastCheckBox.isSelected());
            }
        });

        messageBodyTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                if (VariableMetadata.validateName(messageBodyTextField.getText()) != null) {
                    buttonOK.setEnabled(false);
                } else {
                    buttonOK.setEnabled(true);
                }
            }
        }));

        subprotcolTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                if (VariableMetadata.validateName(subprotcolTextField.getText()) != null) {
                    buttonOK.setEnabled(false);
                } else {
                    buttonOK.setEnabled(true);
                }
            }
        }));


        serviceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WebSocketConnectionEntity connection = (WebSocketConnectionEntity)((JComboBox)e.getSource()).getSelectedItem();
                outboundRadioButton.setEnabled(isOutboundEnabled(connection.getGoid()));
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        populateServiceComboBox();
    }

    private void populateServiceComboBox() {
        try {
            Collection<WebSocketConnectionEntity> connections = getEntityManager().findAll();
            java.util.List<WebSocketConnectionEntity> entityList = new ArrayList<WebSocketConnectionEntity>();
            for ( WebSocketConnectionEntity entity : connections) {
                if (entity.isEnabled()) {
                     entityList.add(entity);
                }
            }
            serviceComboBox.setModel(new DefaultComboBoxModel(entityList.toArray()));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find connection definitions");
        }
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    @Override
    public boolean isConfirmed() {
        return true;
    }

    // Updated for 8.0.  OIDs have been deprecated in favor of GOIDs.
    private boolean isOutboundEnabled(Goid goid) {
        try {
            WebSocketConnectionEntity connection = getEntityManager().findByPrimaryKey(goid);
            if ( connection == null || connection.getOutboundUrl() == null || "".equals(connection.getOutboundUrl())) {
                return false;
            }

        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find connection definitions");
        }

        //enable by default
        return true;
    }

    @Override
    public void setData(WebSocketMessageInjectionAssertion assertion) {
        inboundRadioButton.setSelected(assertion.isInbound());
        outboundRadioButton.setSelected(!assertion.isInbound());
        textRadioButton.setSelected(assertion.isTextMessage());
        binaryRadioButton.setSelected(!assertion.isTextMessage());
        broadcastCheckBox.setSelected(assertion.isBroadcast());
        clientIdTextField.setText(assertion.getClientIds());
        messageBodyTextField.setText(assertion.getMessage());
        subprotcolTextField.setText(assertion.getSubprotocol());
        deliveryFailureCheckBox.setSelected(assertion.isDeliveryFailure());

        serviceComboBox.setSelectedIndex(0);
        for(int i = 1;i < serviceComboBox.getItemCount();i++) {
            WebSocketConnectionEntity connection = (WebSocketConnectionEntity)serviceComboBox.getItemAt(i);
            if(connection.getGoid() == assertion.getServiceOid()) {
                serviceComboBox.setSelectedIndex(i);
                break;
            }
        }
        outboundRadioButton.setEnabled(isOutboundEnabled(assertion.getServiceOid()));
        clientIdTextField.setEnabled(!broadcastCheckBox.isSelected());
        deliveryFailureCheckBox.setEnabled(!broadcastCheckBox.isSelected());



    }

    @Override
    public WebSocketMessageInjectionAssertion getData(WebSocketMessageInjectionAssertion assertion) {
        assertion.setInbound(inboundRadioButton.isSelected());
        assertion.setTextMessage(textRadioButton.isSelected());
        assertion.setServiceOid(((WebSocketConnectionEntity) serviceComboBox.getSelectedItem()).getGoid());
        assertion.setBroadcast(broadcastCheckBox.isSelected());
        assertion.setClientIds(clientIdTextField.getText());
        assertion.setMessage(messageBodyTextField.getText());
        assertion.setSubprotocol(subprotcolTextField.getText());
        if (broadcastCheckBox.isSelected()) {
           assertion.setDeliveryFailure(false);
        } else {
            assertion.setDeliveryFailure(deliveryFailureCheckBox.isSelected());
        }

        return assertion;

    }

    private static WebSocketConnectionEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(WebSocketConnectionEntityAdmin.class, null);
    }
}
