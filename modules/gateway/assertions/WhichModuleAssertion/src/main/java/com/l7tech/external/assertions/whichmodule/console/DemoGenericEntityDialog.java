package com.l7tech.external.assertions.whichmodule.console;

import com.l7tech.console.panels.ServiceComboBox;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.whichmodule.DemoGenericEntity;
import com.l7tech.external.assertions.whichmodule.DemoGenericEntityAdmin;
import com.l7tech.gui.NumberField;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;

public class DemoGenericEntityDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox playsTromboneCheckBox;
    private JTextField nameField;
    private JTextField ageField;
    private JCheckBox enabledCheckBox;
    private ServiceComboBox serviceComboBox;
    private JCheckBox enableServiceSelection;
    private JCheckBox enableDemoGenericEntitySelection;
    private JComboBox<String> demoGenericEntityComboBox;

    final DemoGenericEntity entity;
    boolean confirmed = false;
    private boolean readOnly = false;

    public DemoGenericEntityDialog(Window owner, DemoGenericEntity entity) {
        super(owner);
        this.entity = entity;
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

        enableServiceSelection.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                serviceComboBox.setEnabled(enableServiceSelection.isSelected() && !readOnly);
            }
        });

        enableDemoGenericEntitySelection.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                demoGenericEntityComboBox.setEnabled(enableDemoGenericEntitySelection.isSelected() && !readOnly);
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

        ageField.setDocument(new NumberField(3));

        nameField.setText(entity.getName());
        ageField.setText(String.valueOf(entity.getAge()));
        playsTromboneCheckBox.setSelected(entity.isPlaysTrombone());
        enableServiceSelection.setSelected(entity.getServiceId() != null);
        serviceComboBox.populateAndSelect(entity.getServiceId() != null, entity.getServiceId());
        serviceComboBox.setEnabled(entity.getServiceId() != null);
        enableDemoGenericEntitySelection.setSelected(entity.getDemoGenericEntityId() != null);
        populateDemoGenericEntityComboBox(entity.getDemoGenericEntityId());
        demoGenericEntityComboBox.setEnabled(entity.getDemoGenericEntityId() != null);
        enabledCheckBox.setSelected(entity.isEnabled());
    }

    /**
     * This populated the demo generic entity combo box with all available demo generic entities.
     *
     * @param demoGenericEntityId The demo generic entity to set as selected.
     */
    private void populateDemoGenericEntityComboBox(@Nullable final Goid demoGenericEntityId) {
        final DefaultComboBoxModel<String> demoGenericEntityComboBoxModel = new DefaultComboBoxModel<>();
        try {
            final Collection<DemoGenericEntity> allDemoEntities = getEntityManager().findAll();
            demoGenericEntityComboBoxModel.addElement("<Select Entity>");
            for (final DemoGenericEntity demoGenericEntity : allDemoEntities) {
                demoGenericEntityComboBoxModel.addElement(demoGenericEntity.getName() + ":" + demoGenericEntity.getId());
            }
        } catch (FindException e) {
            demoGenericEntityComboBoxModel.addElement("<Error Loading Demo Generic Entities>");
        }
        demoGenericEntityComboBox.setModel(demoGenericEntityComboBoxModel);
        if (demoGenericEntityId != null) {
            for (int i = 1; i < demoGenericEntityComboBox.getItemCount(); i++) {
                if (Goid.equals(demoGenericEntityId, getGoid(demoGenericEntityComboBox.getItemAt(i)))) {
                    demoGenericEntityComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Gets the demo generic entity goid from a string reference. The reference is expected to be in the format
     * 'name:goid'
     *
     * @param demoGenericEntityReference The string goid reference in the form 'name:goid'
     * @return The goid in the string.
     */
    @NotNull
    private Goid getGoid(@NotNull final String demoGenericEntityReference) {
        return Goid.parseGoid(demoGenericEntityReference.subSequence(demoGenericEntityReference.lastIndexOf(':') + 1, demoGenericEntityReference.length()).toString());
    }

    /**
     * Returns the demo generic entity admin
     *
     * @return The demo generic entity admin
     */
    private static DemoGenericEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(DemoGenericEntityAdmin.class, null);
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        nameField.setEnabled(!readOnly);
        ageField.setEnabled(!readOnly);
        playsTromboneCheckBox.setEnabled(!readOnly);
        enableServiceSelection.setEnabled(!readOnly);
        serviceComboBox.setEnabled(enableServiceSelection.isSelected() && !readOnly);
        enableDemoGenericEntitySelection.setEnabled(!readOnly);
        demoGenericEntityComboBox.setEnabled(enableDemoGenericEntitySelection.isSelected() && !readOnly);
        enabledCheckBox.setEnabled(!readOnly);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public DemoGenericEntity getEntity() {
        return entity;
    }

    private void onOK() {
        entity.setName(nameField.getText());
        entity.setAge(Integer.valueOf(ageField.getText()));
        entity.setPlaysTrombone(playsTromboneCheckBox.isSelected());
        entity.setServiceId((enableServiceSelection.isSelected() && serviceComboBox.getSelectedPublishedService() != null) ? serviceComboBox.getSelectedPublishedService().getGoid() : null);
        entity.setDemoGenericEntityId(enableDemoGenericEntitySelection.isSelected() ? getSelectedDemoGenericEntity() : null);
        entity.setEnabled(enabledCheckBox.isSelected());
        confirmed = true;
        dispose();
    }

    /**
     * Returns the selected demo generic entity goid or null if non is selected.
     *
     * @return The selected demo generic entity goid or null if non is selected
     */
    @Nullable
    private Goid getSelectedDemoGenericEntity() {
        return demoGenericEntityComboBox.getSelectedIndex() > 0 ? getGoid((String) demoGenericEntityComboBox.getSelectedItem()) : null;
    }

    private void onCancel() {
        dispose();
    }
}
