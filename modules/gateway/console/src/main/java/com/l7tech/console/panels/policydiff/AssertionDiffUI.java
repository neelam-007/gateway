package com.l7tech.console.panels.policydiff;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.console.panels.policydiff.PolicyDiffWindow.*;

/**
 * A class creates a pane with two tabs, "Properties" and "Raw XML" to show assertion diff results.
 * This class is used in two cases: (1) When "Show Assertion Comparison" button is clicked in the policy diff window,
 * and a two-tab pane is displayed at the bottom of the policy diff window.  (2) When user right clicks an assertion
 * and chooses "Compare Assertion", a two-tab window with diff results is shown.
 */
public class AssertionDiffUI {

    private static final Logger logger = Logger.getLogger(AssertionDiffUI.class.getName());
    private static final List<String> IGNORED_PROPERTIES = CollectionUtils.list("ordinal");
    private static final String PROPERTY_COLUMN_NAME = "Property";

    private JPanel contentPane;
    private JTable propertiesTable;
    private JTextArea leftAssertionXmlTextArea;
    private JTextArea rightAssertionXmlTextArea;
    private JTextArea leftAssertionNumberTextArea;
    private JTextArea rightAssertionNumberTextArea;
    private JLabel leftAssertionLabel;
    private JLabel rightAssertionLabel;
    private JScrollPane leftAssertionXmlScrollPane;
    private JScrollPane leftAssertionNumberScrollPane;
    private JScrollPane rightAssertionXmlScrollPane;
    private JScrollPane rightAssertionNumberScrollPane;
    private JButton prevDiffButton;
    private JButton nextDiffButton;
    private JButton closeButton;
    private JTabbedPane resultTabbedPane;
    private JFrame parent;  // Used to access PolicyDiffWindow or AssertionDiffWindow

    private List<Triple<String, String, String>> properties = new ArrayList<>();
    private List<Integer> nextDiffIndexList;          // Store the indies of all next diffs in the diff result list
    private int currElmtIdxOfNextDiffIndexList = -1;  // The index of a current element in nextDiffIndexList

    public AssertionDiffUI(@Nullable final JFrame parent, final AssertionTreeNode nodeL, final AssertionTreeNode nodeR) {
        this.parent = parent;

        final String nodeNameL = DefaultAssertionPolicyNode.getNameFromMeta(nodeL.asAssertion(), true, false);
        leftAssertionLabel.setText(getDisplayingText(nodeNameL, leftAssertionLabel, contentPane));
        leftAssertionLabel.setToolTipText(nodeNameL);

        final String nodeNameR = DefaultAssertionPolicyNode.getNameFromMeta(nodeR.asAssertion(), true, false);
        rightAssertionLabel.setText(getDisplayingText(nodeNameR, rightAssertionLabel, contentPane));
        rightAssertionLabel.setToolTipText(nodeNameR);

        contentPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                leftAssertionLabel.setText(getDisplayingText(nodeNameL, leftAssertionLabel, contentPane));
                rightAssertionLabel.setText(getDisplayingText(nodeNameR, rightAssertionLabel, contentPane));
            }
        });

        resultTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableOrDisableDiffNavigationButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (AssertionDiffUI.this.parent != null) AssertionDiffUI.this.parent.dispose();
            }
        });

        initializeTable(nodeL, nodeR);
        initializeXmlPanes(nodeL, nodeR);

        // Disable diff navigation buttons, since these buttons are used by the tab "Raw XML" and the default tab is "Properties".
        enableOrDisableDiffNavigationButtons();
    }

    public AssertionDiffUI(final JFrame parent) {
        this.parent = parent;

        resultTabbedPane.setEnabled(false);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (AssertionDiffUI.this.parent != null) AssertionDiffUI.this.parent.dispose();
            }
        });

        enableOrDisableDiffNavigationButtons();
    }

    public AssertionDiffUI(final AssertionTreeNode nodeL, final AssertionTreeNode nodeR) {
        this(null, nodeL, nodeR);
    }

    public void setParent(JFrame parent) {
        this.parent = parent;
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    /**
     * Move the "Property" column into the middle position.
     * This method should be used when a two-tab pane is shown at the bottom of the policy diff window.
     */
    public void movePropColumnToMiddle() {
        if (PROPERTY_COLUMN_NAME.equals(propertiesTable.getColumnName(0))) {
            propertiesTable.moveColumn(0, 1);
        }
    }

    /**
     * Move the "Property" column into the most left position.
     * This method should be used when a two-tab pane is embedded in the assertion diff window.
     */
    public void movePropColumnToLeft() {
        if (! PROPERTY_COLUMN_NAME.equals(propertiesTable.getColumnName(0))) {
            propertiesTable.moveColumn(0, 1);
        }
    }

    private void initializeXmlPanes(AssertionTreeNode nodeL, AssertionTreeNode nodeR) {
        prevDiffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigatePreviousOrNextDiff(true);
            }
        });

        nextDiffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigatePreviousOrNextDiff(false);
            }
        });

        leftAssertionXmlScrollPane.getHorizontalScrollBar().setModel(rightAssertionXmlScrollPane.getHorizontalScrollBar().getModel());
        leftAssertionXmlScrollPane.getVerticalScrollBar().setModel(rightAssertionXmlScrollPane.getVerticalScrollBar().getModel());
        leftAssertionNumberScrollPane.getVerticalScrollBar().setModel(rightAssertionXmlScrollPane.getVerticalScrollBar().getModel());
        rightAssertionNumberScrollPane.getVerticalScrollBar().setModel(rightAssertionXmlScrollPane.getVerticalScrollBar().getModel());

        String assertionXmlL = WspWriter.getPolicyXml(nodeL.asAssertion());
        String assertionXmlR = WspWriter.getPolicyXml(nodeR.asAssertion());

        // We don't add a progress bar here for the PolicyDiffUtils.diffXml method, since user would quickly click the
        // diff navigation buttons many times and we don't want to see many progress bars floating around there.
        try {
            XmlDiff xmlDiff = new XmlDiff(assertionXmlL, assertionXmlR);
            nextDiffIndexList = xmlDiff.setTextAreas(leftAssertionXmlTextArea, leftAssertionNumberTextArea, rightAssertionXmlTextArea, rightAssertionNumberTextArea);
        } catch (BadLocationException e) {
            DialogDisplayer.showMessageDialog(parent, "Error due to an invalid range specification used by Highlighter", "Policy Comparison Error", JOptionPane.ERROR_MESSAGE, null);
            return;
        } catch (IOException e) {
            DialogDisplayer.showMessageDialog(parent, "Cannot read the policy XML", "Policy Comparison Error", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        setFirstDiffDisplayingPosition();
    }

    private void initializeTable(AssertionTreeNode nodeL, AssertionTreeNode nodeR) {
        populateProperties(nodeL.asAssertion(), nodeR.asAssertion());

        final TableModel tableModel = getTableModel(
            DefaultAssertionPolicyNode.getNameFromMeta(nodeL.asAssertion(), true, false),
            DefaultAssertionPolicyNode.getNameFromMeta(nodeR.asAssertion(), true, false)
        );
        propertiesTable.setModel(tableModel);

        // Set the column widths
        TableColumnModel columnModel = new DefaultTableColumnModel();
        columnModel.addColumn(new TableColumn(0, 200)); // Property
        columnModel.addColumn(new TableColumn(1, 350)); // Left Assertion
        columnModel.addColumn(new TableColumn(2, 350)); // Right Assertion

        // Set up tool tips for all cells.
        DefaultTableCellRenderer tableCellRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (component instanceof JComponent) {
                    ((JComponent)component).setToolTipText((String) propertiesTable.getValueAt(row, column));
                }
                if (component instanceof JLabel) {
                    ((JLabel) component).setHorizontalAlignment(JLabel.CENTER);
                }
                return component;
            }
        };
        for(int i = 0; i < columnModel.getColumnCount(); i++){
            TableColumn tc = columnModel.getColumn(i);
            tc.setMinWidth(150);
            tc.setHeaderValue(tableModel.getColumnName(tc.getModelIndex()));
            tc.setCellRenderer(tableCellRenderer);
        }
        propertiesTable.setColumnModel(columnModel);

        // Center headers
        final JTableHeader tableHeader = propertiesTable.getTableHeader();
        final DefaultTableCellRenderer headerCellRenderer = (DefaultTableCellRenderer) tableHeader.getDefaultRenderer();
        tableHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                headerCellRenderer.setHorizontalAlignment(JLabel.CENTER);
                return headerCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        // Row Sorter
        final TableRowSorter<TableModel> connectionTableRowSorter = new TableRowSorter<>(tableModel);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        connectionTableRowSorter.setSortKeys(sortKeys);
        connectionTableRowSorter.setComparator(0, String.CASE_INSENSITIVE_ORDER);
        propertiesTable.setRowSorter(connectionTableRowSorter);
        connectionTableRowSorter.sort();
    }

    /**
     * Create a table model holding assertion diff results
     *
     * @param assertionNameL: the name of the left assertion
     * @param assertionNameR: the name of the right assertion
     *
     * @return a table model with three columns.
     */
    private TableModel getTableModel (final String assertionNameL, final String assertionNameR) {
        return new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return properties.size();
            }

            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Triple<String, String, String> rowProperties = properties.get(rowIndex);

                switch (columnIndex) {
                    case 0: return rowProperties.left;
                    case 1: return rowProperties.middle;
                    case 2: return rowProperties.right;
                    default: throw new IllegalArgumentException("No such column index: " + columnIndex);
                }
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                    case 0: return PROPERTY_COLUMN_NAME;
                    case 1: return "Left Assertion (" + assertionNameL + ")";
                    case 2: return "Right Assertion (" + assertionNameR + ")";
                    default: throw new IllegalArgumentException("No such column index: " + column);
                }
            }
        };
    }

    /**
     * Get a list of diffed assertions' properties.  The list element is a Triple object with a format,
     * <property_name, left_assertion_property_value, right_assertion_property_value>.
     *
     * @param assertionL: the left assertion
     * @param assertionR: the right assertion
     */
    private void populateProperties(Assertion assertionL, Assertion assertionR) {
        Map<String, String> assertionPropsL = getAssertionProperties(assertionL);
        Map<String, String> assertionPropsR = getAssertionProperties(assertionR);

        Set<String> assertionPropsKeySetL = assertionPropsL.keySet();
        properties = new ArrayList<>(assertionPropsKeySetL.size());

        for (String propName: assertionPropsKeySetL) {
            if (assertionPropsR.containsKey(propName)) {
                String valueL = assertionPropsL.get(propName);
                String valueR = assertionPropsR.get(propName);
                if ((valueL == null && valueR != null) || (valueL != null && !valueL.equals(valueR))) {
                    properties.add(new Triple<>(propName, valueL, valueR));
                }
            }
        }
    }

    /**
     * Get a property map for a particular assertion. The map element has a format, <property_name, property_value>.
     *
     * @param assertion: the assertion used to get properties
     *
     * @return a map with assertion properties information.
     */
    private Map<String, String> getAssertionProperties(Assertion assertion) {
        Map<String, String> propMap = null;

        final Object[] beans;
        // If the assertion is a custom assertion, we need handle two beans, the Assertion object and the CustomAssertion object.
        if (assertion instanceof CustomAssertionHolder) {
            beans = new Object[] {assertion, ((CustomAssertionHolder) assertion).getCustomAssertion()};
        } else {
            beans = new Object[] {assertion};
        }

        for (Object bean: beans) {
            try {
                BeanInfo info = Introspector.getBeanInfo(bean.getClass(), Object.class);
                PropertyDescriptor[] props = info.getPropertyDescriptors();

                if (propMap == null) {
                    propMap = new HashMap<>(props.length);
                }

                for (PropertyDescriptor prop : props) {
                    String propName = prop.getName();
                    // Ignore those properties defined in the IGNORED_PROPERTIES list
                    if (IGNORED_PROPERTIES.contains(propName)) continue;

                    Method getter = prop.getReadMethod();
                    if (getter == null) continue;

                    Object value = getter.invoke(bean);
                    if (value == null) {
                        if ("assertionComment".equals(propName)) {
                            propMap.put(propName + " (Left)", null);
                            propMap.put(propName + " (Right)", null);
                        } else {
                            propMap.put(propName, null);
                        }
                        continue;
                    }

                    Class<?> type = prop.getPropertyType();
                    // Type 1: Assertion Comment:
                    if (Assertion.Comment.class == type) {
                        saveAssertionCommentProperty((Assertion.Comment)value, propMap, propName);
                    }
                    // Type 2: Array (with Primitive or String elements):
                    else if (type.isArray()) {
                        Class componentType = type.getComponentType();
                        if (componentType.isPrimitive() || String.class== componentType) {
                            propMap.put(propName, getArrayDisplayingText((Object[])value));
                        }
                    }
                    // Type 3: Primitive or String:
                    else if (type.isPrimitive() || String.class == type) {
                        propMap.put(propName, value.toString());
                    }
                }
            } catch (Exception e) {
                String beanName = bean instanceof CustomAssertionHolder?
                    ((CustomAssertionHolder) bean).getCustomAssertion().getName() :
                    Assertion.getBaseName(bean.getClass().getName());
                logger.warning("Cannot get properties for the assertion, " + beanName);
                throw new RuntimeException(e.getMessage());
            }
        }

        return propMap;
    }

    /**
     * Generate a string listing all array elements for displaying purpose.
     *
     * @param array: its elements will be displayed as string type.
     *
     * @return a string with all array elements separated by ','.
     */
    private String getArrayDisplayingText(Object[] array) {
        StringBuilder arrayDisplayingText = new StringBuilder();
        for (Object object: array) {
            if (arrayDisplayingText.length() > 0) arrayDisplayingText.append(", ");
            arrayDisplayingText.append(object.toString());
        }
        return arrayDisplayingText.toString().trim();
    }

    /**
     * Generate two assertion comment properties (one for the left assertion comment and other one for the right assertion
     * comment) and save the two properties into the property map, propMap.
     *
     * @param comment: an AssertionComment object, which may or may not contain left and/or right comment(s).
     * @param propMap: the properties map
     * @param propName: the property name used for Assertion Comment.
     */
    private void saveAssertionCommentProperty(Assertion.Comment comment, Map<String, String> propMap, String propName) {
        if (comment == null || ! comment.hasComment()) return;

        propMap.put(propName + " (Left)", hasLeftComment(comment) ? comment.getAssertionComment(Assertion.Comment.LEFT_COMMENT) : null);
        propMap.put(propName + " (Right)", hasRightComment(comment) ? comment.getAssertionComment(Assertion.Comment.RIGHT_COMMENT) : null);
    }

    /**
     * @return true if non empty left comment exists
     */
    private boolean hasLeftComment(@Nullable Assertion.Comment comment) {
        final String leftComment = comment.getProperties().get(Assertion.Comment.LEFT_COMMENT);
        return leftComment != null && !leftComment.trim().isEmpty();
    }

    /**
     * @return  true if non empty right comment exists
     */
    private boolean hasRightComment(@Nullable Assertion.Comment comment) {
        final String rightComment = comment.getProperties().get(Assertion.Comment.RIGHT_COMMENT);
        return rightComment != null && !rightComment.trim().isEmpty();
    }

    /**
     * Find the caret position of the first XML diff and set it in the displaying area.
     */
    private void setFirstDiffDisplayingPosition() {
        final int initialCaretPosition;

        if (nextDiffIndexList.isEmpty()) {
            initialCaretPosition = 0;
        } else {
            try {
                initialCaretPosition = leftAssertionXmlTextArea.getLineStartOffset(nextDiffIndexList.get(0));
                currElmtIdxOfNextDiffIndexList = 0;
            } catch (BadLocationException e) {
                logger.warning(ExceptionUtils.getMessage(e));
                return;
            }
        }
        leftAssertionXmlTextArea.setCaretPosition(initialCaretPosition);
        leftAssertionXmlTextArea.getCaret().setVisible(true);
    }

    /**
     * Enable or disable two diff navigation buttons depending on their availability.
     */
    private void enableOrDisableDiffNavigationButtons() {
        if (resultTabbedPane.getSelectedIndex() == 0) {
            prevDiffButton.setEnabled(false);
            nextDiffButton.setEnabled(false);
        } else {
            prevDiffButton.setEnabled(currElmtIdxOfNextDiffIndexList >= 1);
            nextDiffButton.setEnabled(currElmtIdxOfNextDiffIndexList >= 0 && currElmtIdxOfNextDiffIndexList < nextDiffIndexList.size() - 1);
        }
    }

    /**
     * Find the previous or next diff and let the cursor point to it.
     * So the previous or next diff is visible in the displaying text areas.
     *
     * @param moveUp: true means Previous Diff button is clicked and false means Next Diff button is clicked.
     */
    private void navigatePreviousOrNextDiff(boolean moveUp) {
        if (moveUp) {
            if (currElmtIdxOfNextDiffIndexList < 1) return;
            else currElmtIdxOfNextDiffIndexList--;
        } else {
            if (currElmtIdxOfNextDiffIndexList == -1 || currElmtIdxOfNextDiffIndexList >= nextDiffIndexList.size() - 1) return;
            else currElmtIdxOfNextDiffIndexList++;
        }

        try {
            final int newCaretPosition = leftAssertionXmlTextArea.getLineStartOffset(nextDiffIndexList.get(currElmtIdxOfNextDiffIndexList));
            leftAssertionXmlTextArea.setCaretPosition(newCaretPosition);
            leftAssertionXmlTextArea.getCaret().setVisible(true);

            rightAssertionXmlScrollPane.getVerticalScrollBar().setValue(rightAssertionXmlTextArea.getHeight() - 2);
        } catch (BadLocationException e1) {
            logger.warning(ExceptionUtils.getMessage(e1));
            return;
        }

        enableOrDisableDiffNavigationButtons();
    }
}