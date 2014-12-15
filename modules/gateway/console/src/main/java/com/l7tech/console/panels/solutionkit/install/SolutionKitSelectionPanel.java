package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gui.SelectableTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

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
 *
 */
public class SolutionKitSelectionPanel extends WizardStepPanel<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(SolutionKitSelectionPanel.class.getName());
    private static final String STEP_LABEL = "Select Solution Kit";
    private static final String STEP_DESC = "Select solution kit(s) to install.";

    private JPanel mainPanel;
    private JButton selectAllButton;
    private JButton clearAllButton;
    private JTable solutionKitsTable;

    private SelectableTableModel<SolutionKit> solutionKitsModel;

    private final SolutionKitAdmin solutionKitAdmin;
    private SolutionKitsConfig settings = null;
    private Map<SolutionKit, Mappings> testMappings = new HashMap<>();

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
        solutionKitsModel.setRows(new ArrayList<>(settings.getLoadedSolutionKits()));
        this.settings = settings;
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
        String bundle = settings.getBundleAsString(solutionKit);
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

        solutionKitsModel = TableUtil.configureSelectableTable(solutionKitsTable, true, 0,
            column("", 30, 30, 99999, new Functions.Unary<Boolean, SolutionKit>() {
                @Override
                public Boolean call(SolutionKit solutionKit) {
                    return solutionKitsModel.isSelected(solutionKit);
                }
            }),
            column("Name", 30, 100, 99999, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    return solutionKit.getName();
                }
            }),
            column("Version", 30, 100, 99999, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    return solutionKit.getSolutionKitVersion();
                }
            }),
            column("Description", 30, 100, 99999, new Functions.Unary<String, SolutionKit>() {
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
                notifyListeners();
            }
        });
        // todo (kpak) - row sorter
        //Utilities.setRowSorter(componentsTable, componentsModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});

        setLayout(new BorderLayout());
        add(mainPanel);
    }
}