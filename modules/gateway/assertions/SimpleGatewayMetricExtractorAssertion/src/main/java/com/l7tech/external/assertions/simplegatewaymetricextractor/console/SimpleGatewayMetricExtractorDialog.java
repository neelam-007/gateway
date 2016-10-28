package com.l7tech.external.assertions.simplegatewaymetricextractor.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.simplegatewaymetricextractor.SimpleGatewayMetricExtractorEntity;
import com.l7tech.external.assertions.simplegatewaymetricextractor.SimpleGatewayMetricExtractorGenericEntityAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleGatewayMetricExtractorDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField serviceNameFilterTextField;

    final private SimpleGatewayMetricExtractorEntity entity;
    private static final Logger logger = Logger.getLogger(SimpleGatewayMetricExtractorDialog.class.getName());

    SimpleGatewayMetricExtractorDialog(Frame owner) {
        super(owner, "Simple Gateway Metric Extractor Properties");

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        entity = getEntity();

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK(entity);
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        serviceNameFilterTextField.setText(entity.getServiceNameFilter());
    }

    private void onOK(SimpleGatewayMetricExtractorEntity entity) {
        //TODO deleting an entity that does not exists raises an error, need to find a way to delete without 2 network calls (one checking for entity and one to delete entity)
//        if(serviceNameFilterTextField.getText().isEmpty()) {
//            deleteEntity(entity);
//        } else {
//            entity.setServiceNameFilter(serviceNameFilterTextField.getText());
//            saveEntity(entity);
//        }
        entity.setServiceNameFilter(serviceNameFilterTextField.getText());
        saveEntity(entity);
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private static SimpleGatewayMetricExtractorGenericEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(SimpleGatewayMetricExtractorGenericEntityAdmin.class, null);
    }

    private SimpleGatewayMetricExtractorEntity getEntity() {
        try {
            final SimpleGatewayMetricExtractorEntity entity = getEntityManager().getEntity();
            if(entity != null) {
                return entity;
            }
        } catch (FindException ex) {
            error("Unable to find entity: " + ExceptionUtils.getMessage(ex));
        }
        return new SimpleGatewayMetricExtractorEntity();
    }

    private void saveEntity(SimpleGatewayMetricExtractorEntity entity) {
        try {
            getEntityManager().save(entity);
        } catch (SaveException e1) {
            error("Unable to save entity: " + ExceptionUtils.getMessage(e1));
        } catch (UpdateException e1) {
            error("Unable to update entity: " + ExceptionUtils.getMessage(e1));
        }
    }

    private void deleteEntity(final SimpleGatewayMetricExtractorEntity entity) {
        try {
            getEntityManager().delete(entity);
        } catch (FindException e1) {
            logger.log(Level.WARNING, "Unable to find entity to delete" + ExceptionUtils.getMessage(e1));
        } catch (DeleteException e1) {
            logger.log(Level.WARNING, "Unable to delete entity" + ExceptionUtils.getMessage(e1));
        }
    }

    private void error(final String s) {
        DialogDisplayer.showMessageDialog(this, s, null);
    }
}
