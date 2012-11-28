package com.l7tech.external.assertions.createroutingstrategy.console;

import com.l7tech.common.io.failover.ConfigurableFailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyEditor;
import com.l7tech.common.io.failover.Service;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.createroutingstrategy.CreateRoutingStrategyAssertion;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ConstructorInvocation;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class CreateRoutingStrategyAssertionDialog extends AssertionPropertiesOkCancelSupport<CreateRoutingStrategyAssertion> {

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.createroutingstrategy.console.resources.CreateRoutingStrategyResources");
    private static final String ROUTE = "main.label.route";
    private static final String PROPERTIES = "main.label.properties";
    private static final String DIALOG_TITLE_NEW_SERVICE_PROPERTIES = "New Route";
    private static final String DIALOG_TITLE_EDIT_SERVICE_PROPERTIES = "Edit Route";
    private static final String DIALOG_TITLE_CLONE_SERVICE_PROPERTIES = "Clone Route";
    private static final String DIALOG_TITLE_EDIT_FAILOVER_PROPERTIES = "Edit Failover Properties";
    private static final int MAX_DISPLAYABLE_MESSAGE_LENGTH = 80;

    private ClusterStatusAdmin clusterStatusAdmin;
    private JPanel contentPane;
    private JButton removeService;
    private JButton addService;
    private JButton editService;
    private SimpleTableModel<Service> propertiesTableModel;

    private JComboBox strategy;
    private JPanel prefixPanel;
    private JButton configureStrategyButton;
    private JTable propertyTable;
    private JButton cloneButton;
    private JButton upButton;
    private JButton downButton;
    private CreateRoutingStrategyAssertion assertion;
    private TargetVariablePanel strategyField;
    private Map<String, String> strategyProperties = new HashMap<String, String>();
    private boolean settingData = false;

    public CreateRoutingStrategyAssertionDialog(final Frame parent, final CreateRoutingStrategyAssertion assertion) {
        super(CreateRoutingStrategyAssertion.class, parent, assertion, true);
        initComponents();
        intializeServiceListSection(parent);
        initAdminConnection();

        strategy.setModel(new DefaultComboBoxModel(clusterStatusAdmin.getAllFailoverStrategies()));
        strategyField = new TargetVariablePanel();
        prefixPanel.setLayout(new BorderLayout());
        prefixPanel.add(strategyField, BorderLayout.CENTER);
        this.assertion = assertion;
        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableDisableComponents();
            }
        });
        enableOrDisableConfigureStrategyButton();
        strategyField.addChangeListener(changeListener);
        strategyField.setAssertion(assertion, getPreviousAssertion());
        strategy.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (!settingData) {
                    strategyProperties.clear();
                }
                enableOrDisableConfigureStrategyButton();
            }
        });

        configureStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ConfigurableFailoverStrategy s = (ConfigurableFailoverStrategy) strategy.getSelectedItem();
                Dialog dialog = null;
                if (s.getEditorClass() == null) {
                    dialog = new RoutingStrategyConfigurationDialog(parent, DIALOG_TITLE_EDIT_FAILOVER_PROPERTIES, strategyProperties);
                } else {
                    try {
                        final Constructor<FailoverStrategyEditor> constructor =
                                ConstructorInvocation.findMatchingConstructor(assertion.getClass().getClassLoader(),
                                        s.getEditorClass(),
                                        FailoverStrategyEditor.class,
                                        new Class[]{Frame.class, Map.class});
                        dialog = constructor.newInstance(parent, strategyProperties);

                    } catch (ClassNotFoundException e1) {
                        throw new RuntimeException(e1);
                    } catch (ConstructorInvocation.WrongSuperclassException e1) {
                        throw new RuntimeException(e1);
                    } catch (ConstructorInvocation.AbstractClassException e1) {
                        throw new RuntimeException(e1);
                    } catch (ConstructorInvocation.NoMatchingPublicConstructorException e1) {
                        throw new RuntimeException(e1);
                    } catch (InvocationTargetException e1) {
                        throw new RuntimeException(e1);
                    } catch (InstantiationException e1) {
                        throw new RuntimeException(e1);
                    } catch (IllegalAccessException e1) {
                        throw new RuntimeException(e1);
                    }
                }
                dialog.pack();
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
            }
        });

    }

    /**
     * Initialize the object references of the remote services
     */
    private void initAdminConnection() {
        clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
    }


    private void enableDisableComponents() {
        boolean enable = propertiesTableModel.getRowCount() > 0;
        getOkButton().setEnabled(strategyField.isEntryValid() && enable);

        boolean selected = propertyTable.getSelectedRowCount() == 1;
        boolean selectedMany = propertyTable.getSelectedRowCount() > 1;
        editService.setEnabled(enable & selected);
        cloneButton.setEnabled(enable & selected);
        removeService.setEnabled(enable & (selected | selectedMany));
    }

    private void enableOrDisableConfigureStrategyButton() {
        FailoverStrategy fs = (FailoverStrategy) strategy.getSelectedItem();
        if (fs instanceof ConfigurableFailoverStrategy) {
            configureStrategyButton.setEnabled(true);
        } else {
            configureStrategyButton.setEnabled(false);
        }
    }

    private void intializeServiceListSection(final Window parent) {

        upButton.setEnabled(false);
        downButton.setEnabled(false);

        propertiesTableModel = TableUtil.configureTable(
                propertyTable,
                TableUtil.column(resources.getString(ROUTE), 50, 100, 100000, property("name"), String.class),
                TableUtil.column(resources.getString(PROPERTIES), 50, 100, 100000, property("properties"), String.class)
        );
        propertyTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        propertyTable.getTableHeader().setReorderingAllowed(false);
        propertyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = propertyTable.getSelectedRow();
                boolean selectedMany = propertyTable.getSelectedRowCount() > 1;
                upButton.setEnabled(index > 0 && !selectedMany);
                downButton.setEnabled(index >= 0 && index < propertiesTableModel.getRowCount() - 1 && !selectedMany);
                enableDisableComponents();
            }
        });

        propertiesTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                enableDisableComponents();
            }
        });

        addService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                editRoute(null, null, DIALOG_TITLE_NEW_SERVICE_PROPERTIES);
            }
        });
        editService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = propertyTable.getSelectedRow();
                if (viewRow > -1) {
                    editRoute(propertiesTableModel.getRowObject(propertyTable.convertRowIndexToModel(viewRow)), null, DIALOG_TITLE_EDIT_SERVICE_PROPERTIES);
                }
            }
        });
        Utilities.setDoubleClickAction(propertyTable, editService);
        removeService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int[] viewRows = propertyTable.getSelectedRows();
                if (viewRows.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < viewRows.length; i++) {
                        //limit the length to max displayable so it won't go over
                        if(sb.length() >= MAX_DISPLAYABLE_MESSAGE_LENGTH) {
                            sb.append(" ...");
                            break;
                        }
                        sb.append(propertiesTableModel.getRowObject(viewRows[i]).getName());
                        if (i != viewRows.length - 1) {
                            sb.append(",");
                        }
                    }
                    Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
                    int result = JOptionPane.showOptionDialog(
                            parent, MessageFormat.format(resources.getString("confirmation.remove.route"), sb),
                            resources.getString("confirmation.title.remove.route"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                    if (result == 0) {
                        for (int i = 0; i < viewRows.length; i++) {
                            propertiesTableModel.removeRowAt(propertyTable.getSelectedRow());
                        }
                    }
                }
                enableDisableComponents();
            }
        });
        upButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = propertyTable.getSelectedRow();
                if (viewRow > 0) {
                    int prevIndex = viewRow - 1;
                    propertiesTableModel.swapRows(prevIndex, viewRow);
                    propertyTable.changeSelection(prevIndex, 0, false, false);
                }
                enableDisableComponents();
            }
        });
        downButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = propertyTable.getSelectedRow();
                if (viewRow > -1 && viewRow < propertiesTableModel.getRowCount() - 1) {
                    int nextIndex = viewRow + 1;
                    propertiesTableModel.swapRows(viewRow, nextIndex);
                    propertyTable.changeSelection(nextIndex, 0, false, false);
                }
                enableDisableComponents();
            }
        });
        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = propertyTable.getSelectedRow();
                if (viewRow > -1) {
                    //clone service object
                    final Service   copyFrom = (Service)propertiesTableModel.getRowObject(propertyTable.convertRowIndexToModel(viewRow)).clone();
                    editRoute(null, copyFrom
                            , DIALOG_TITLE_CLONE_SERVICE_PROPERTIES);
                }
                enableDisableComponents();
            }
        });
    }

    private void editRoute(final Service service, final Service copyFrom, final String title) {
        final CreateRoutingStrategyRoutingPropertiesDialog dlg =  new CreateRoutingStrategyRoutingPropertiesDialog((Frame) getParent(), title, assertion, service==null?copyFrom:service);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    final Service property = dlg.getService();
                    if (service == null) {
                        //add new Route
                        int selectedRow = propertyTable.getSelectedRow();
                        if (selectedRow != -1 && copyFrom == null) {
                            propertiesTableModel.addRow(selectedRow, new Service(property.getName(), property.getProperties()));
                        } else {
                            propertiesTableModel.addRow(new Service(property.getName(), property.getProperties()));
                        }
                        propertyTable.getSelectionModel().clearSelection();

                    } else {
                        int i = propertiesTableModel.getRowIndex(service);
                        propertiesTableModel.setRowObject(i, property);
                    }
                }
                enableDisableComponents();
            }
        });
        enableDisableComponents();
    }

    private static Functions.Unary<String, Service> property(final String propName) {
        return Functions.propertyTransform(Service.class, propName);
    }

    @Override
    public void setData(CreateRoutingStrategyAssertion assertion) {
        settingData = true;
        for (Service icd : assertion.getRoutes()) {
            propertiesTableModel.addRow(icd);
        }
        for (int i = 0; i < strategy.getModel().getSize(); i++) {
            final FailoverStrategy fs = (FailoverStrategy) strategy.getModel().getElementAt(i);
            if (fs.getName().equals(assertion.getStrategyName())) {
                strategy.setSelectedItem(fs);
                enableOrDisableConfigureStrategyButton();
                break;
            }
        }
        strategyField.setVariable(assertion.getStrategy());
        strategyField.setAssertion(assertion, getPreviousAssertion());

        strategyProperties = (Map<String, String>) ((HashMap) assertion.getStrategyProperties()).clone();
        settingData = false;
    }

    @Override
    public CreateRoutingStrategyAssertion getData(CreateRoutingStrategyAssertion assertion) throws ValidationException {

        final FailoverStrategy selectedFailoverStrategy = (FailoverStrategy) strategy.getSelectedItem();
        assertion.setStrategyName(selectedFailoverStrategy.getName());
        assertion.setStrategyDescription(selectedFailoverStrategy.getDescription());
        assertion.getRoutes().clear();
        for (int i = 0; i < propertiesTableModel.getRowCount(); ++i) {
            assertion.getRoutes().add((Service) propertiesTableModel.getRows().get(i));
        }
        if (strategyField.getVariable().trim().isEmpty()) {
            assertion.setStrategy(CreateRoutingStrategyAssertion.DEFAULT_STRATEGY);
        } else {
            assertion.setStrategy(strategyField.getVariable().trim());
        }
        assertion.getStrategyProperties().clear();

        for (Map.Entry<String, String> entry : strategyProperties.entrySet()) {
            assertion.getStrategyProperties().put(entry.getKey(), entry.getValue());
        }

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

}
