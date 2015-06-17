package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.licensing.ManageLicensesDialog;
import com.l7tech.console.panels.solutionkit.SolutionKitCustomization;
import com.l7tech.console.panels.solutionkit.SolutionKitUtils;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gui.SelectableTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Wizard panel which allows the user to select component(s) within a solution kit to install.
 */
public class SolutionKitSelectionPanel extends WizardStepPanel<SolutionKitsConfig>  {
    private static final Logger logger = Logger.getLogger(SolutionKitSelectionPanel.class.getName());
    private static final String STEP_LABEL = "Select Solution Kit";
    private static final String STEP_DESC = "Select solution kit(s) to install.";

    private JPanel mainPanel;
    private JButton selectAllButton;
    private JButton clearAllButton;
    private JTable solutionKitsTable;
    private JButton manageLicensesButton;
    private JPanel customizableButtonPanel;
    private JButton instanceModifierButton;

    private SelectableTableModel<SolutionKit> solutionKitsModel;

    private final SolutionKitAdmin solutionKitAdmin;
    private SolutionKitsConfig settings = null;
    private Map<SolutionKit, Mappings> testMappings = new HashMap<>();
//    private boolean disableAutoNext = false;

    public SolutionKitSelectionPanel() {
        super(null);
        solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();
        initialize();
    }

    @Override
    public String getStepLabel() {
        return STEP_LABEL;
    }

    @Override
    public String getDescription() {
        return STEP_DESC;
    }

    @Override
    public boolean canAdvance() {
        return !solutionKitsModel.getSelected().isEmpty();
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        testMappings.clear();
        solutionKitsModel.deselectAll();
        settings.setSelectedSolutionKits(Collections.<SolutionKit>emptySet());
        solutionKitsModel.setRows(new ArrayList<>(settings.getLoadedSolutionKits().keySet()));
        this.settings = settings;

        addCustomUis(customizableButtonPanel, settings);
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        settings.setSelectedSolutionKits(new HashSet<>(solutionKitsModel.getSelected()));
        settings.setTestMappings(testMappings);
    }

    @Override
    public boolean onNextButton() {
        boolean success = false;
        String errorMessage = "";

        // todo (kpak) - handle multiple kits. for now, install first one.
        //
        SolutionKit solutionKit = solutionKitsModel.getSelected().get(0);

        // invoke custom callback
        try {
            invokeCustomCallback(settings);
        } catch (SolutionKitManagerCallback.CallbackException | IOException | TooManyChildElementsException | MissingRequiredElementException e) {
            errorMessage = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
        }

        String bundle = settings.getBundleAsString(solutionKit);
        if (bundle == null) {
            DialogDisplayer.showMessageDialog(this, "Unexpected error: unable to get Solution Kit bundle.", "Install Solution Kit", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }
        try {
            Either<String, String> result = AdminGuiUtils.doAsyncAdmin(
                    solutionKitAdmin,
                    this.getOwner(),
                    "Testing Solution Kit",
                    "The gateway is testing selected solution kit(s)",
                    solutionKitAdmin.testInstall(solutionKit, bundle));

            if (result.isLeft()) {
                errorMessage = result.left();
                logger.log(Level.WARNING, errorMessage);
            } else if (result.isRight()) {
                Item item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(result.right())));
                Mappings mappings = (Mappings)item.getContent();
                testMappings.put(solutionKit, mappings);
                success = true;
            }
        } catch (InvocationTargetException | IOException e) {
            testMappings.clear();
            errorMessage = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
        } catch (InterruptedException e) {
            testMappings.clear();
            return false;
        }

        if (!success) {
            DialogDisplayer.showMessageDialog(this, errorMessage, "Install Solution Kit", JOptionPane.ERROR_MESSAGE, null);
        }

        return success;
    }

    @Override
    public void notifyActive() {
        // auto next step the wizard for solution kit with single item
        if (solutionKitsModel.getRowCount() == 1) {
            solutionKitsModel.select(0);
// todo auto next step bug: left step menu shows incorrect last step; comment out for now
//            if (!disableAutoNext && owner instanceof InstallSolutionKitWizard) {
//                ((InstallSolutionKitWizard) owner).clickButtonNext();
//            }
        }
//        disableAutoNext = true;
    }

    private void initialize() {
        Utilities.buttonToLink(selectAllButton);
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                solutionKitsModel.selectAll();
            }
        });

        Utilities.buttonToLink(clearAllButton);
        clearAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                solutionKitsModel.deselectAll();
            }
        });

        manageLicensesButton.setEnabled(true);
        manageLicensesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onManageLicenses();
            }
        });

        instanceModifierButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (solutionKitsModel.getSelected().isEmpty()) return;

                final SolutionKitInstanceModifierDialog instanceModifierDialog = new SolutionKitInstanceModifierDialog();
                instanceModifierDialog.pack();
                Utilities.centerOnParentWindow(instanceModifierDialog);
                instanceModifierDialog.setModal(true);
                DialogDisplayer.display(instanceModifierDialog);

                if (instanceModifierDialog.isOK()) {
                    final String instanceModifier = instanceModifierDialog.getInstanceModifier();
                    final SolutionKit solutionKit = solutionKitsModel.getSelected().get(0);

                    final Map<SolutionKit, Bundle> loadedSolutionKits = settings.getLoadedSolutionKits();
                    final Bundle bundle = loadedSolutionKits.get(solutionKit);
                    loadedSolutionKits.remove(solutionKit);

                    final Map<SolutionKit, SolutionKitCustomization> customizations = settings.getCustomizations();
                    final SolutionKitCustomization customization = customizations.get(solutionKit);
                    customizations.remove(solutionKit);

                    solutionKit.setInstanceModifier(instanceModifier);
                    loadedSolutionKits.put(solutionKit, bundle);
                    customizations.put(solutionKit, customization);

                    solutionKitsModel.fireTableDataChanged();
                }
            }
        });

        solutionKitsModel = TableUtil.configureSelectableTable(solutionKitsTable, true, 0,
            column("", 50, 50, 100, new Functions.Unary<Boolean, SolutionKit>() {
                @Override
                public Boolean call(SolutionKit solutionKit) {
                    return solutionKitsModel.isSelected(solutionKit);
                }
            }),
            column("Name", 50, 400, 5000, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    final String featureSet = solutionKit.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
                    if (StringUtils.isEmpty(featureSet) || ConsoleLicenseManager.getInstance().isFeatureEnabled(featureSet)) {
                        return solutionKit.getName();
                    } else {
                        return solutionKit.getName() + " (Unlicensed)";
                    }
                }
            }),
            column("Version", 50, 100, 500, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    return solutionKit.getSolutionKitVersion();
                }
            }),
            column("Instance Modifier", 50, 400, 5000, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    return solutionKit.getInstanceModifier();
                }
            }),
            column("Description", 50, 500, 5000, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    return solutionKit.getProperty(SolutionKit.SK_PROP_DESC_KEY);
                }
            })
        );

        solutionKitsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        solutionKitsModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                final boolean enabled = ! solutionKitsModel.getSelected().isEmpty();
                instanceModifierButton.setEnabled(enabled);
                for (Component component: customizableButtonPanel.getComponents()) {
                    if (component instanceof JButton) component.setEnabled(enabled);
                }

                notifyListeners();
            }
        });
        Utilities.setRowSorter(solutionKitsTable, solutionKitsModel, new int[]{0}, new boolean[]{true}, new Comparator[]{null});

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private void onManageLicenses() {
        final Frame mainWindow = TopComponents.getInstance().getTopParent();
        ManageLicensesDialog dlg = new ManageLicensesDialog(mainWindow);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        dlg.setModal(true);
        DialogDisplayer.display(dlg);
    }

    private void addCustomUis(final JPanel customizableButtonPanel, final SolutionKitsConfig settings) {
        customizableButtonPanel.removeAll();
        final Map<SolutionKit, SolutionKitCustomization> customizations = settings.getCustomizations();

        // todo handle multiple kits
        if (customizations.size() > 1) {
            throw new IllegalArgumentException("Can't handle multiple kits.");
        }

        // add custom ui
        for (SolutionKitCustomization customization : customizations.values()) {
            SolutionKitManagerUi customUi = customization.getCustomUi();
            if (customUi != null) {
                customizableButtonPanel.add(customUi.createButton(customizableButtonPanel));
            }
        }
    }

    private void invokeCustomCallback(final SolutionKitsConfig settings)
            throws SolutionKitManagerCallback.CallbackException, IOException, TooManyChildElementsException, MissingRequiredElementException {

        Document metadataDoc, bundleDoc;
        SolutionKitManagerContext skContext;

        SolutionKitCustomization customization;
        SolutionKitManagerCallback customCallback;
        SolutionKitManagerUi customUi;

        for (SolutionKit sk : settings.getCustomizations().keySet()) {
            customization = settings.getCustomizations().get(sk);
            if (customization == null) {
                continue;
            }

            // implementer provides a callback
            customCallback = customization.getCustomCallback();
            if (customCallback != null) {
                customUi = customization.getCustomUi();

                // if implementer provides a context
                skContext = customUi != null ? customUi.getContext() : null;
                if (skContext != null) {
                    // get from selected
                    metadataDoc = SolutionKitUtils.createDocument(sk);
                    bundleDoc = settings.getBundleAsDocument(sk);

                    // set to context
                    skContext.setSolutionKitMetadata(metadataDoc);
                    skContext.setMigrationBundle(bundleDoc);

                    // execute callback
                    customCallback.preMigrationBundleImport(skContext);

                    // copy back metadata from xml version
                    SolutionKitUtils.copyDocumentToSolutionKit(metadataDoc, sk);

                    // set (possible) changes made to metadata and bundle
                    // TODO fix duplicate HashMap bug where put(...) does not replace previous value
                    settings.setBundle(sk, bundleDoc);
                } else  {
                    customCallback.preMigrationBundleImport(null);
                }
            }
        }
    }

    /**
     * Called by IDEA's UI initialization when "Custom Create" is checked for any UI component.
     */
    private void createUIComponents() {
        customizableButtonPanel = new JPanel();
        customizableButtonPanel.setLayout(new BorderLayout());
    }
}