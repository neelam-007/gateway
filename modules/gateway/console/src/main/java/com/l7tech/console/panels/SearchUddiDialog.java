package com.l7tech.console.panels;

import com.l7tech.gui.util.*;
import com.l7tech.gui.util.SwingWorker;
import com.l7tech.uddi.WsdlInfo;
import com.l7tech.uddi.UDDINamedEntity;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.table.WsdlTable;
import com.l7tech.console.table.WsdlTableSorter;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.UDDIRegistry;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.AbstractDocument;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 *
 * Search dialog to find either WSDL's or BusinessEntities from UDDI.
 * <p> @author fpang </p>
 * @author darmstrong
 */
public class SearchUddiDialog extends JDialog {

    private JScrollPane searchResultsScrollPane;
    private JPanel mainPanel;
    private JButton searchButton;
    private JComboBox serviceNameFilterOptionComboBox;
    private JTextField serviceNameSearchPattern;
    private JButton selectButton;
    private JButton cancelButton;
    private WsdlTable wsdlTable = null;
    private EventListenerList listenerList = new EventListenerList();
    private JCheckBox caseSensitiveCheckBox;
    private JLabel retrievedRows;
    private JComboBox uddiRegistryComboBox;
    private JLabel nameOfSearchItem;
    private JLabel searchTitleLabel;

    private final SsmPreferences preferences = TopComponents.getInstance().getPreferences();

    private static final Logger logger = Logger.getLogger(SearchUddiDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.SearchUddiDialog", Locale.getDefault());
    private static final String EQUALS = "Equals";
    private static final String CONTAINS = "Contains";
    private static final String UDDI_REGISTRY = "UDDI.TYPE";
    private static final int MAX_SERVICE_NAME_LENGTH = 255;
    private Map<String, UDDIRegistry> allRegistries;
    private BusinessEntityTable businessEntityTable;

    public enum SEARCH_TYPE{WSDL_SEARCH, BUSINESS_ENTITY_SEARCH}

    private final SEARCH_TYPE searchType;

    public SearchUddiDialog(JDialog parent, SEARCH_TYPE searchType) throws FindException {
        super(parent, resources.getString("window.title"), true);
        this.searchType = searchType;
        initialize();
    }

    public SearchUddiDialog(JFrame parent, SEARCH_TYPE searchType) throws FindException {
        super(parent, resources.getString("window.title"), true);
        this.searchType = searchType;
        initialize();
    }

    public static interface TableInterface{
        void clearData();
    }

    public static boolean uddiEnabled() {
        try {
            UDDIRegistryAdmin uddiRegistrydmin = Registry.getDefault().getUDDIRegistryAdmin();
            if(!uddiRegistrydmin.findAllUDDIRegistries().isEmpty()) return true;
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Could not check if UDDI is enabled. '"+e.getMessage()+"'.");
        }

        return false;
    }

    private void initialize() throws FindException {
        if (getOwner() == null)
            Utilities.setAlwaysOnTop(this, true);

        Container p = getContentPane();
        Utilities.setEscKeyStrokeDisposes(this);
        
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        // The default-service-name-filter option is "Contains", so the valid max length of the search input
        // is MAX_SERVICE_NAME_LENGTH - 2, since there are two '%'s at the begin and the end respectively.
        ((AbstractDocument)serviceNameSearchPattern.getDocument()).setDocumentFilter(new DocumentSizeFilter(MAX_SERVICE_NAME_LENGTH - 2));
        serviceNameFilterOptionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { CONTAINS, EQUALS }));
        serviceNameFilterOptionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox cb = (JComboBox)actionEvent.getSource();
                String filterName = (String)cb.getSelectedItem();
                int validMaxLen = MAX_SERVICE_NAME_LENGTH;
                if (CONTAINS.equals(filterName)) {
                    // since there are two '%' in the search input: the first % locates at the begin and the second % locates the end.
                    validMaxLen = MAX_SERVICE_NAME_LENGTH - 2;
                    // Check if keeping the search string when swapping the two filter options
                    if (serviceNameSearchPattern.getText().length() > validMaxLen) {
                        serviceNameSearchPattern.setText("");
                    }
                }
                ((AbstractDocument)serviceNameSearchPattern.getDocument()).setDocumentFilter(new DocumentSizeFilter(validMaxLen));
            }
        });

        loadUddiRegistries();

        switch(searchType){
            case WSDL_SEARCH:
                nameOfSearchItem.setText("Service Name:");
                wsdlTable = new WsdlTable();
                searchResultsScrollPane.setViewportView(wsdlTable);
                selectButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        int row = wsdlTable.getSelectedRow();
                        if (row != -1) {
                            WsdlInfo si = (WsdlInfo) wsdlTable.getTableSorter().getData(row);
                            fireItemSelectedEvent(si);
                        }
                        dispose();
                    }
                });
                Utilities.setDoubleClickAction(wsdlTable, selectButton);
                searchTitleLabel.setText("Search UDDI Registry for WSDLs");
                break;
            case BUSINESS_ENTITY_SEARCH:
                nameOfSearchItem.setText("Business Name:");
                businessEntityTable = new BusinessEntityTable();
                searchResultsScrollPane.setViewportView(businessEntityTable);
                selectButton.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        UDDINamedEntity businessEntity = businessEntityTable.getSelectedBusinessEntity();
                        if(businessEntity != null){
                            fireItemSelectedEvent(businessEntity);                                    
                        }
                        dispose();
                    }
                });
                Utilities.setDoubleClickAction(businessEntityTable, selectButton);
                searchTitleLabel.setText("Search UDDI Registry for Business Entities");
                break;
        }

        //BusinessEntityTable
        searchResultsScrollPane.getViewport().setBackground(Color.white);

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SearchUddiDialog.this.dispose();
            }
        });

        searchButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                InputValidator.ValidationRule comboValidator = new InputValidator.ValidationRule() {
                    @Override
                    public String getValidationError() {
                        if(uddiRegistryComboBox.getSelectedIndex() == -1) return "Please select a UDDI Registry";
                        return null;
                    }
                };

                InputValidator ruleValidator = new InputValidator(SearchUddiDialog.this, "Search Business Entity");
                ruleValidator.addRule(comboValidator);
                if(!ruleValidator.validateWithDialog()) return;
                
                final String searchString;
                if(serviceNameSearchPattern.getText().length() > 0) {
                    String escapedStr = escapeString(serviceNameSearchPattern.getText());
                    if((serviceNameFilterOptionComboBox.getSelectedItem()).equals(CONTAINS)) {
                        searchString = "%" + escapedStr + "%";
                    } else {
                        searchString = escapedStr;
                    }
                } else {
                    searchString = "%";
                }

                retrievedRows.setText("Result: 0");
                final CancelableOperationDialog dlg =
                        new CancelableOperationDialog(SearchUddiDialog.this, "Searching UDDI", "Please wait, Searching UDDI...");
                final String errorMessage;
                switch(searchType){
                    case WSDL_SEARCH:
                        // clear the list
                        ((WsdlTableSorter) wsdlTable.getModel()).clearData();
                        errorMessage = "Error searching UDDI for WSDLs";
                        break;
                    case BUSINESS_ENTITY_SEARCH:
                        errorMessage = "Error seraching UDDI for BusinessEntities";
                        break;
                    default:errorMessage = "Undefined";//can't happen
                }

                final String registryName = (String) uddiRegistryComboBox.getSelectedItem();


                final SwingWorker worker = new SwingWorker() {

                    public Object construct() {
                        try {

                            ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
                            if (serviceAdmin == null) throw new RuntimeException("Service Admin reference not found");
                            String regName = (String) uddiRegistryComboBox.getSelectedItem();
                            UDDIRegistry uddiRegistry = allRegistries.get(regName);

                            switch(searchType){
                                case WSDL_SEARCH:
                                    return serviceAdmin.findWsdlUrlsFromUDDIRegistry(uddiRegistry, searchString, caseSensitiveCheckBox.isSelected());
                                case BUSINESS_ENTITY_SEARCH:
                                    return serviceAdmin.findBusinessesFromUDDIRegistry(uddiRegistry, searchString, caseSensitiveCheckBox.isSelected());
                            }

                            return null;
                        } catch (FindException e) {
                            logger.log(Level.WARNING, errorMessage, e);
                            JOptionPane.showMessageDialog(SearchUddiDialog.this, "Find Exception, " + e.getMessage(), "Search UDDI Registry", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    }

                    public void finished() {
                        dlg.dispose();
                    }
                };

                worker.start();

                DialogDisplayer.display(dlg, new Runnable() {
                    public void run() {
                        worker.interrupt();
                        Object result = worker.get();
                        if (result == null)
                            return;    // canceled
                        if (result instanceof WsdlInfo[]) {
                            // store prefs on successful search
                            preferences.putProperty(UDDI_REGISTRY, registryName);

                            boolean searchTruncated = false;
                            Vector urlList = new Vector();
                            for (int i = 0; i < ((WsdlInfo[])result).length; i++) {
                                final WsdlInfo wi = ((WsdlInfo[])result)[i];
                                if (WsdlInfo.MAXED_OUT_UDDI_RESULTS_URL.equals(wi.getWsdlUrl())) {
                                    // Flag value indicating that search results were truncated
                                    searchTruncated = true;
                                } else {
                                    // Normal result
                                    urlList.add(wi);
                                }
                            }

                            // populate the data to the table
                            ((WsdlTableSorter) wsdlTable.getModel()).setData(urlList);
                            String warning = "";
                            if (searchTruncated) {
                                warning = "  <b>(QUERY TOO BROAD - Only the first " + urlList.size() + " results are presented)</b>";
                                retrievedRows.setForeground(new Color(255, 64, 64));
                            } else {
                                retrievedRows.setForeground(new JLabel().getForeground());
                            }
                            retrievedRows.setText("<HTML>Result: " + urlList.size() + warning);

                            return;
                        } else if (result instanceof UDDINamedEntity[]) {
                            // store prefs on successful search
                            preferences.putProperty(UDDI_REGISTRY, registryName);

                            boolean searchTruncated = false;
                            List<BusinessEntityTableRow> rows = new ArrayList<BusinessEntityTableRow>();
                            for (int i = 0; i < ((UDDINamedEntity[]) result).length; i++) {
                                final UDDINamedEntity entity = ((UDDINamedEntity[]) result)[i];
                                if (WsdlInfo.MAXED_OUT_UDDI_RESULTS_URL.equals(entity.getName())) {
                                    // Flag value indicating that search results were truncated
                                    searchTruncated = true;
                                } else {
                                    // Normal result
                                    rows.add(new BusinessEntityTableRow(entity));
                                }
                            }

                            // populate the data to the table
                            businessEntityTable.setData(rows);
                            Utilities.setRowSorter(businessEntityTable, businessEntityTable.getModel());
                            String warning = "";
                            if (searchTruncated) {
                                warning = "  <b>(QUERY TOO BROAD - Only the first " + rows.size() + " results are presented)</b>";
                                retrievedRows.setForeground(new Color(255, 64, 64));
                            } else {
                                retrievedRows.setForeground(new JLabel().getForeground());
                            }
                            retrievedRows.setText("<HTML>Result: " + rows.size() + warning);
                            return;
                        }
                        retrievedRows.setText("Result: 0");
                    }
                });
            }
        });

        pack();
        Utilities.centerOnScreen(this);
    }

    private void loadUddiRegistries() {
        try {
            UDDIRegistryAdmin uddiRegistryAdmin = getUDDIRegistryAdmin();
            //todo what are the rbac requirements here? Does the user need the permission to be able to read all registries?
            //todo canUpdate is set when the user has update on UDDI_REGISTRIES

            allRegistries = new HashMap<String, UDDIRegistry>();
            Collection<UDDIRegistry> registries = uddiRegistryAdmin.findAllUDDIRegistries();
            for (UDDIRegistry uddiRegistry : registries){
                uddiRegistryComboBox.addItem(uddiRegistry.getName());
                allRegistries.put(uddiRegistry.getName(), uddiRegistry);
            }

            // load prefs
            String registryName = preferences.getString(UDDI_REGISTRY, "");
            if(!registryName.equals("")){
                uddiRegistryComboBox.setSelectedItem(registryName);
            }else{
                uddiRegistryComboBox.setSelectedIndex(-1);
            }

        } catch (FindException e) {
            showErrorMessage("Loading failed", "Unable to list all UDDI Registry: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        showErrorMessage(title, msg, e, null);
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }
    
    /** @return the UDDIRegistryAdmin interface, or null if not connected or it's unavailable for some other reason */
    private UDDIRegistryAdmin getUDDIRegistryAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getUDDIRegistryAdmin();
    }

    /**
     * notfy the listeners
     *
     * @param  item the trusted certs
     */
    private void fireItemSelectedEvent(final Object item) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for(ItemSelectedListener itemSelectedListener: listeners) {
                    itemSelectedListener.itemSelected(item);
                }
            }
        });
    }

    private final List<ItemSelectedListener> listeners = new ArrayList<ItemSelectedListener>();

    public void addSelectionListener(ItemSelectedListener selectedListener){
        listeners.add(selectedListener);        
    }

    public static interface ItemSelectedListener{
        void itemSelected(Object item);
    }

    /**
     * @param string    The string that will be escaped.
     * @return  The new escaped string which will only escape the following characters:
     *
     *<ul>
     * <li>% --> \%</li>
     * <li>_ --> \_</li>
     * <li>\ --> \\</li> 
     * </ul>
     */
    public String escapeString(String string) {
        StringBuffer result = new StringBuffer("");
        for (int i=0; i < string.length(); i++) {
            char character = string.charAt(i);
            if (character == '%') {
                result.append("\\%");
            } else if (character == '_') {
                result.append("\\_");
            } else if (character == '\\') {
                result.append("\\\\");
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private static class BusinessEntityTable extends JTable {
        private final BusinessEntityTableModel model = new BusinessEntityTableModel();

        BusinessEntityTable() {
            setModel(model);
            getTableHeader().setReorderingAllowed(false);
            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            TableColumnModel cols = getColumnModel();
            int numCols = model.getColumnCount();
            for (int i = 0; i < numCols; ++i) {
                final TableColumn col = cols.getColumn(i);
                col.setMinWidth(model.getColumnMinWidth(i));
                col.setPreferredWidth(model.getColumnPrefWidth(i));
                col.setMaxWidth(model.getColumnMaxWidth(i));
                TableCellRenderer cr = model.getCellRenderer(i, getDefaultRenderer(String.class));
                if (cr != null) col.setCellRenderer(cr);
            }
        }

        public BusinessEntityTableRow getRowAt(int row) {
            return model.getRowAt(row);
        }

        public void setData(java.util.List<BusinessEntityTableRow> rows) {
            model.setData(rows);
        }

        /** @return the current selected SsgConnector, or null */
        public UDDINamedEntity getSelectedBusinessEntity() {
            int selectedRow = getSelectedRow();
            if (selectedRow < 0)
                return null;

            int rowNum = this.getRowSorter().convertRowIndexToModel(selectedRow);
            BusinessEntityTableRow row = getRowAt(rowNum);
            if (row == null)
                return null;
            return row.getNamedEntity();
        }

    }

    private static class BusinessEntityTableModel extends AbstractTableModel {
        private abstract class Col {
            final String name;
            final int minWidth;
            final int prefWidth;
            final int maxWidth;

            protected Col(String name, int minWidth, int prefWidth, int maxWidth) {
                this.name = name;
                this.minWidth = minWidth;
                this.prefWidth = prefWidth;
                this.maxWidth = maxWidth;
            }

            abstract Object getValueForRow(BusinessEntityTableRow row);

            public TableCellRenderer getCellRenderer(final TableCellRenderer current) {
                return new TableCellRenderer() {
                    private Color defFg;
                    private Color defSelFg;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        if (defFg == null) {
                            TableCellRenderer def1 = new DefaultTableCellRenderer();
                            Component c = def1.getTableCellRendererComponent(table, value, false, false, row, column);
                            defFg = c.getForeground();
                            TableCellRenderer def2 = new DefaultTableCellRenderer();
                            Component csel = def2.getTableCellRendererComponent(table, value, true, false, row, column);
                            defSelFg = csel.getForeground();
                        }

                        Component ret = current.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                        if (isSelected) {
                            ret.setForeground(defSelFg);
                        } else {
                            ret.setForeground(defFg);
                        }

                        return ret;
                    }
                };
            }
        }

        public final Col[] columns = new Col[] {
                new Col("BusinessEntity Name", 60, 90, 999999) {
                    @Override
                    Object getValueForRow(BusinessEntityTableRow row) {
                        return row.getName();
                    }
                },

                new Col("businessKey", 60, 90, 999999) {
                    @Override
                    Object getValueForRow(BusinessEntityTableRow row) {
                        return row.getBusinessKey();
                    }
                },

        };

        private final ArrayList<BusinessEntityTableRow> rows = new ArrayList<BusinessEntityTableRow>();

        private BusinessEntityTableModel() {
        }

        public int getColumnMinWidth(int column) {
            return columns[column].minWidth;
        }

        public int getColumnPrefWidth(int column) {
            return columns[column].prefWidth;
        }

        public int getColumnMaxWidth(int column) {
            return columns[column].maxWidth;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column].name;
        }

        public TableCellRenderer getCellRenderer(int column, final TableCellRenderer current) {
            return columns[column].getCellRenderer(current);
        }

        public void setData(List<BusinessEntityTableRow> rows) {
            this.rows.clear();
            this.rows.addAll(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns[columnIndex].getValueForRow(rows.get(rowIndex));
        }

        public BusinessEntityTableRow getRowAt(int rowIndex) {
            return rows.get(rowIndex);
        }
   }

    private static class BusinessEntityTableRow {
        private final UDDINamedEntity namedEntity;

        public BusinessEntityTableRow(UDDINamedEntity namedEntity) {
            this.namedEntity = namedEntity;
        }

        public UDDINamedEntity getNamedEntity() {
            return namedEntity;
        }

        public Object getName() {
            return namedEntity.getName();
        }

        public Object getBusinessKey(){
            return namedEntity.getKey();
        }
    }

}
