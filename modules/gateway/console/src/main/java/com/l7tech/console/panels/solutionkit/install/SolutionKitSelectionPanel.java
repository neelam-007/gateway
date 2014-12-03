package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gui.SelectableTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
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

    public SolutionKitSelectionPanel() {
        super(null);
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
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        solutionKitsModel.setRows(new ArrayList<>(settings.getLoadedSolutionKits()));
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        settings.setSelectedSolutionKits(new HashSet<>(solutionKitsModel.getSelected()));
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