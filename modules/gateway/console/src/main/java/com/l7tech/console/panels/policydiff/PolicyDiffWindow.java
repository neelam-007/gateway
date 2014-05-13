package com.l7tech.console.panels.policydiff;

import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.console.tree.AssertionLineNumbersTree;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static com.l7tech.console.panels.policydiff.PolicyDiffWindow.DiffType.IDENTICAL;
import static com.l7tech.console.panels.policydiff.PolicyDiffWindow.DiffType.MATCHED_WITH_DIFFERENCES;

/**
 * A window displays the results of comparing two policy trees.
 */
public class PolicyDiffWindow extends JFrame {
    // Diff Result Types
    public static enum DiffType {
        DELETED,                 // An assertion or an XML line is deleted.
        INSERTED,                // An assertion or an XML line is inserted.
        IDENTICAL,               // An assertion or an XML line is identical to other one.
        MATCHED_WITH_DIFFERENCES // An assertion or an XML line is matched to other one, but they have different properties or parts.
    }

    public static final String SYSTEM_PROPERTY_MAX_SEARCH_DEPTH = "com.l7tech.policy.diff.max.search.depth";
    public static final int DEFAULT_MAX_SEARCH_DEPTH = 5;

    public static Color COLOR_FOR_INSERTION = new Color(190, 255, 190);              // Light green color used to highlight inserted assertions or XML lines
    public static Color COLOR_FOR_DELETION = new Color(255, 190, 190);               // Light red color used to highlight deleted assertions or XML lines
    public static Color COLOR_FOR_MATCH_WITH_DIFFERENCES = new Color(204, 229, 255); // Light blue color used to highlight matched assertions or partial matched XML lines
    public static Color COLOR_FOR_BLANK_ASSERTION = new Color(192, 192, 192);        // Light grey color used to highlight blank assertions or blank lines

    private static final Logger logger = Logger.getLogger(PolicyDiffWindow.class.getName());
    private static final String SHOW_ASSERTION_DIFF_BUTTON_TXT = "Show Assertion Differences";
    private static final String HIDE_ASSERTION_DIFF_BUTTON_TXT = "Hide Assertion Differences";

    private JPanel contentPane;
    private JButton closeButton;
    private JButton prevDiffButton;
    private JButton nextDiffButton;
    private JLabel leftAssertionDiffPolicyNameLabel;
    private JLabel rightAssertionDiffPolicyNameLabel;
    private PolicyDiffTree leftPolicyTree;
    private PolicyDiffTree rightPolicyTree;
    private JScrollPane leftPolicyTreeScrollPane;
    private JScrollPane leftAssertionNumScrollPane;
    private JScrollPane rightPolicyTreeScrollPane;
    private JScrollPane rightAssertionNumScrollPane;
    private JButton showOrHideAssertionDiffButton;
    private JSplitPane diffSplitPane;
    private AssertionLineNumbersTree leftAssertionNumberTree;
    private AssertionLineNumbersTree rightAssertionNumberTree;

    private List<Integer> nextDiffRowList;  // Store the row numbers of all next diffs in the diff result list
    private int currentDiffRow = -1;        // The index of a current element in nextDiffRowList.  It is not necessary that currentDiffRow is same as the selected row.

    private Pair<String, PolicyTreeModel> leftPolicyInfo, rightPolicyInfo;
    private Map<Integer, DiffType> diffResultMapL;
    private Map<Integer, AssertionDiffUI> assertionDiffUICache1 = new WeakHashMap<>(); // Store AssertionDiffUI objects embedded in the policy diff window.
    private Map<Integer, AssertionDiffUI> assertionDiffUICache2 = new WeakHashMap<>(); // Store AssertionDiffUI objects embedded in the assertion diff window.
    private boolean isAssertionDiffPaneShown;

    /**
     * Create a window and initialize components based on left and right policies information.
     *
     * @param leftPolicyInfo: a pair of strings contains left policy's full name, policy tree model, and xml.
     * @param rightPolicyInfo: a pair of strings contains right policy's full name, policy tree model, and xml.
     */
    public PolicyDiffWindow(Pair<String, PolicyTreeModel> leftPolicyInfo, Pair<String, PolicyTreeModel> rightPolicyInfo) {
        if ((leftPolicyInfo == null || leftPolicyInfo.left == null || leftPolicyInfo.right == null) ||
            (rightPolicyInfo == null || rightPolicyInfo.left == null || rightPolicyInfo.right == null)) {
            throw new IllegalArgumentException("Compared Policy information is not sufficient.");
        }
        this.leftPolicyInfo = leftPolicyInfo;
        this.rightPolicyInfo = rightPolicyInfo;

        contentPane.setOpaque(true);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(closeButton);

        ImageIcon imageIcon = (ImageIcon) DialogDisplayer.getDefaultFrameIcon();
        setIconImage(imageIcon.getImage());
        setTitle("Policy Comparison");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        initializeComponents();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                leftPolicyTree.getSelectionModel().clearSelection();
                rightPolicyTree.getSelectionModel().clearSelection();

                PolicyDiffWindow.this.pack();
                Utilities.centerOnScreen(PolicyDiffWindow.this);
            }
        });

        // Clean the left policy reference, so policy diff menu item or button will display "Choose Left" next time.
        TopComponents.getInstance().setLeftDiffPolicyInfo(null);
    }

    /**
     * Call this method when policy tree's selection has changed.
     * @param changedPolicyTree source of the change
     * @param row selected row
     */
    void policyTreeSelectionChanged(final JTree changedPolicyTree, final int row) {
        // highlight assertion in the other tree
        if (changedPolicyTree == leftPolicyTree) {
            rightPolicyTree.setSelectionRow(row);
        } else {
            leftPolicyTree.setSelectionRow(row);
        }
    }

    /**
     * Get a proper-length label name for displaying
     *
     * @param labelName: the original label name
     * @return a displaying label name with a fixed length 100.
     */
    public static String getDisplayingLabelName(String labelName) {
        return labelName.length() > 100? (labelName.substring(0, 50) + "..." + labelName.substring(labelName.length() - 50)) : labelName;
    }

    /**
     * The IDEA GUI Generator customizes policy trees.
     */
    private void createUIComponents() {
        leftPolicyTree = new PolicyDiffTree(this);
        rightPolicyTree = new PolicyDiffTree(this);
    }

    private void initializeComponents() {
        diffSplitPane.setDividerSize(10);
        diffSplitPane.setBottomComponent(null);

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        showOrHideAssertionDiffButton.setText(SHOW_ASSERTION_DIFF_BUTTON_TXT);
        showOrHideAssertionDiffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isAssertionDiffPaneShown) {
                    isAssertionDiffPaneShown = false;
                    diffSplitPane.setBottomComponent(null);
                    closeButton.setVisible(true);
                    showOrHideAssertionDiffButton.setText(SHOW_ASSERTION_DIFF_BUTTON_TXT);
                } else {
                    isAssertionDiffPaneShown = true;
                    closeButton.setVisible(false);
                    showOrHideAssertionDiffButton.setText(HIDE_ASSERTION_DIFF_BUTTON_TXT);

                    final int row = leftPolicyTree.getMinSelectionRow();
                    showAssertionDiff(row);
                }
            }
        });

        prevDiffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findPreviousOrNextDiffRow(true);
            }
        });

        nextDiffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findPreviousOrNextDiffRow(false);
            }
        });

        leftPolicyTreeScrollPane.getHorizontalScrollBar().setModel(rightPolicyTreeScrollPane.getHorizontalScrollBar().getModel());
        leftPolicyTreeScrollPane.getVerticalScrollBar().setModel(rightPolicyTreeScrollPane.getVerticalScrollBar().getModel());
        leftAssertionNumScrollPane.getVerticalScrollBar().setModel(rightPolicyTreeScrollPane.getVerticalScrollBar().getModel());
        rightAssertionNumScrollPane.getVerticalScrollBar().setModel(rightPolicyTreeScrollPane.getVerticalScrollBar().getModel());

        leftAssertionDiffPolicyNameLabel.setText(getDisplayingLabelName(leftPolicyInfo.left));
        leftAssertionDiffPolicyNameLabel.setToolTipText(leftPolicyInfo.left);

        rightAssertionDiffPolicyNameLabel.setText(getDisplayingLabelName(rightPolicyInfo.left));
        rightAssertionDiffPolicyNameLabel.setToolTipText(rightPolicyInfo.left);

        leftPolicyTree.setParentScrollPane(leftPolicyTreeScrollPane);
        rightPolicyTree.setParentScrollPane(rightPolicyTreeScrollPane);

        leftPolicyTree.setModel(leftPolicyInfo.right);
        rightPolicyTree.setModel(rightPolicyInfo.right);

        generateDiffResults();

        enableOrDisableDiffNavigationButtons(-1);  // -1 means we force this method to use no selection in policy trees.
    }

    /**
     * Show assertion diff results in a pane with two tabs, "Properties" and "Raw XML".
     *
     * @param assertionNodeRow: the row number of two compared assertions in the policy trees.
     */
    void showAssertionDiff(int assertionNodeRow) {
        // If the assertion diff UI is not presented, then don't show assertion diff result.
        if (! isAssertionDiffPaneShown) return;

        if (assertionNodeRow == -1 || !isAssertionDiffEnabled(assertionNodeRow)) {
            // Create an empty AssertionDiffUI and display it
            diffSplitPane.setBottomComponent(new AssertionDiffUI(PolicyDiffWindow.this).getContentPane());
        } else {
            AssertionDiffUI assertionDiffUI = assertionDiffUICache1.get(assertionNodeRow);
            if (assertionDiffUI == null) {
                assertionDiffUI = new AssertionDiffUI(
                    PolicyDiffWindow.this,
                    (AssertionTreeNode) leftPolicyTree.getLastSelectedPathComponent(),
                    (AssertionTreeNode) rightPolicyTree.getLastSelectedPathComponent()
                );
                // Put a new assertion panel into the cache1
                assertionDiffUICache1.put(assertionNodeRow, assertionDiffUI);
            }

            assertionDiffUI.movePropColumnToMiddle();
            diffSplitPane.setBottomComponent(assertionDiffUI.getContentPane());
        }
    }

    /**
     * Check if assertion diff function is enabled for a particular assertion node in the policy tree
     *
     * @param assertionNodeRow: the row number of the particular assertion node.
     *
     * @return true if and only if the assertion is matched to one other assertion and both have different properties.  Otherwise, false.
     */
    boolean isAssertionDiffEnabled(int assertionNodeRow) {
        if (diffResultMapL == null || diffResultMapL.isEmpty()) return false;

        return MATCHED_WITH_DIFFERENCES == diffResultMapL.get(assertionNodeRow);
    }

    /**
     * Compare two policy trees and generate diff results.  If the comparison takes a long period of time, a progress
     * bar dialog displays and won't close until the comparison finishes.
     */
    private void generateDiffResults() {
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        final CancelableOperationDialog cancelDialog = new CancelableOperationDialog(TopComponents.getInstance().getTopParent(),
            "Policy Comparison", "Processing comparison now...", progressBar);
        cancelDialog.pack();
        cancelDialog.setModal(true);
        Utilities.centerOnScreen(cancelDialog);

        final Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() throws IOException, BadLocationException {
                // Use TreeDiff to compute diff results, which will be applied to the left and right policy trees.
                new TreeDiff(leftPolicyTree, rightPolicyTree);

                // After the diff computation done, initialize the left diff result map, which will be used a few places.
                diffResultMapL = leftPolicyTree.getDiffResultMap();

                // Generate a diff navigation list
                nextDiffRowList = getNextDiffRows();

                // Initialize two assertion number trees
                leftAssertionNumberTree = new AssertionLineNumbersTree(leftPolicyTree);
                leftAssertionNumberTree.setVisible(true);
                leftAssertionNumberTree.updateOrdinalsDisplaying();
                leftAssertionNumScrollPane.getViewport().add(leftAssertionNumberTree);

                rightAssertionNumberTree = new AssertionLineNumbersTree(rightPolicyTree);
                rightAssertionNumberTree.setVisible(true);
                rightAssertionNumberTree.updateOrdinalsDisplaying();
                rightAssertionNumScrollPane.getViewport().add(rightAssertionNumberTree);

                return null;
            }
        };

        try {
             Utilities.doWithDelayedCancelDialog(callable, cancelDialog, 500L);
        } catch (InterruptedException e) {
            logger.fine("Policy comparison was cancelled.");
        } catch (InvocationTargetException e) {
            logger.warning("Policy Comparison Error: " + ExceptionUtils.getMessage(e));
            JOptionPane.showMessageDialog(this, ExceptionUtils.getMessage(e), "Policy Comparison Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Generate a list including row numbers of next diffs.
     *
     * @return a list of row numbers for next diffs.
     */
    private List<Integer> getNextDiffRows() {
        final Set<Integer> leftResultKeys = diffResultMapL.keySet();
        final List<Integer> result = new ArrayList<>(leftResultKeys.size());

        DiffType prevType = null;
        for (int row: leftResultKeys) {
            DiffType currType = diffResultMapL.get(row);

            if (result.isEmpty()) {
                if (currType == IDENTICAL) {
                    continue;
                } else {
                    result.add(row);
                }
            } else {
                if ((currType == prevType && currType != MATCHED_WITH_DIFFERENCES) ||
                    (currType != prevType && currType == IDENTICAL)) {
                    prevType = currType;
                    continue;
                } else {
                    result.add(row);
                }
            }
            prevType = currType;
        }
        return result;
    }

    /**
     * Enable or disable two diff navigation buttons depending on prev and next diff availability.
     *
     * @param selectedRow: the current selected row is used to define a previous or next row.
     */
    void enableOrDisableDiffNavigationButtons(int selectedRow) {
        boolean prevEnabled = false; // Define the previous navigation button to be enabled
        boolean nextEnabled = false; // Define the next navigation button to be enabled

        if (! nextDiffRowList.isEmpty()) {
            // Case 1: Called from initialization
            if (selectedRow == -1 || currentDiffRow == -1) {
                nextEnabled = true;
            }
            // Case 2: Called from the method findPreviousOrNextDiffRow
            else if (selectedRow == currentDiffRow) {
                int idx = nextDiffRowList.indexOf(currentDiffRow);

                prevEnabled = idx > 0;
                nextEnabled = idx < nextDiffRowList.size() - 1;
            }
            // Case 3: Called from the selection listener of the policy diff trees.
            else {
                int idx = nextDiffRowList.indexOf(selectedRow);
                if (idx != -1) {
                    prevEnabled = idx > 0;
                    nextEnabled = idx < nextDiffRowList.size() - 1;
                } else {
                    int firstDiffRow = nextDiffRowList.get(0);
                    int lastDiffRow = nextDiffRowList.get(nextDiffRowList.size() - 1);

                    prevEnabled = selectedRow > firstDiffRow;
                    nextEnabled = selectedRow < lastDiffRow;
                }
            }
        }

        prevDiffButton.setEnabled(prevEnabled);
        nextDiffButton.setEnabled(nextEnabled);
    }

    AbstractAction getAssertionDiffMenuAction(int assertionNodeRow) {
        return new AssertionDiffMenuAction(assertionNodeRow);
    }

    /**
     * Find a previous or next diff and highlight a row with the previous or next diff.
     * After the row is highlighted, the previous or next diff will be visible in the displaying area.
     *
     * @param moveUp: true means the previous-diff navigation button is clicked and false means the next-diff navigation button is clicked.
     */
    private void findPreviousOrNextDiffRow(boolean moveUp) {
        if (nextDiffRowList.isEmpty()) return;

        int selectedRow = leftPolicyTree.getMinSelectionRow();
        if (selectedRow == -1) rightPolicyTree.getMinSelectionRow();

        if (selectedRow == currentDiffRow) { // they both may be -1.
            int idx = nextDiffRowList.indexOf(currentDiffRow);

            if (moveUp) {
                if (idx == 0) return;
                else idx--;
            } else {
                if (idx == nextDiffRowList.size() - 1) return;
                else idx++;
            }

            currentDiffRow = nextDiffRowList.get(idx);
        } else {
            int row = -1;
            if (moveUp) {
                for (int diffRow: nextDiffRowList) {
                    if (diffRow < selectedRow) {
                        row = diffRow;
                    } else {
                        break;
                    }
                }
            } else {
                for (int diffRow: nextDiffRowList) {
                    if (diffRow > selectedRow) {
                        row = diffRow;
                        break;
                    }
                }
            }
            if (row == -1) {
                currentDiffRow = nextDiffRowList.get(0);
            } else {
                currentDiffRow = row;
            }
        }

        leftPolicyTree.setSelectionRow(currentDiffRow);
        rightPolicyTree.setSelectionRow(currentDiffRow);

        // Update the navigation buttons again
        enableOrDisableDiffNavigationButtons(currentDiffRow);
    }

    /**
     * An action pops up a assertion comparison window.
     */
    private class AssertionDiffMenuAction extends AbstractAction {
        private AssertionTreeNode nodeL;
        private AssertionTreeNode nodeR;
        private int assertionNodeRow;

        private AssertionDiffMenuAction(int assertionNodeRow) {
            super("Compare Assertion");
            nodeL = (AssertionTreeNode) leftPolicyTree.getPathForRow(assertionNodeRow).getLastPathComponent();
            nodeR = (AssertionTreeNode) rightPolicyTree.getPathForRow(assertionNodeRow).getLastPathComponent();
            this.assertionNodeRow = assertionNodeRow;

            setEnabled(isAssertionDiffEnabled(assertionNodeRow));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (! isAssertionDiffEnabled(assertionNodeRow)) return;

            AssertionDiffUI assertionDiffUI = assertionDiffUICache2.get(assertionNodeRow);
            if (assertionDiffUI == null) {
                assertionDiffUI = new AssertionDiffUI(nodeL, nodeR);
                assertionDiffUICache2.put(assertionNodeRow, assertionDiffUI);
            }

            assertionDiffUI.movePropColumnToLeft();
            AssertionDiffWindow assertionDiffWindow = new AssertionDiffWindow(assertionDiffUI);
            assertionDiffUI.setParent(assertionDiffWindow);
            assertionDiffWindow.setVisible(true);
        }

        /**
         * A class creates a window to show assertion comparison results.
         */
        private class AssertionDiffWindow extends JFrame {
            private AssertionDiffWindow(AssertionDiffUI assertionDiffUI) {
                setTitle("Assertion Comparison");
                setLayout(new BorderLayout());
                setSize(920, 650);

                ImageIcon imageIcon = (ImageIcon) DialogDisplayer.getDefaultFrameIcon();
                setIconImage(imageIcon.getImage());

                setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
                addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        dispose();
                    }
                });

                add(assertionDiffUI.getContentPane(), BorderLayout.CENTER);

                ((JComponent)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        AssertionDiffWindow.this.pack();
                        Utilities.centerOnScreen(AssertionDiffWindow.this);
                    }
                });
            }
        }
    }
}