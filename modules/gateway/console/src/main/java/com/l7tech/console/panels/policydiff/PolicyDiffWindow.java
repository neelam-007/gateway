package com.l7tech.console.panels.policydiff;

import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.console.tree.AssertionLineNumbersTree;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.ScrollPanesSynchronizer;
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
    private JLabel leftPolicyNameLabel;
    private JLabel rightPolicyNameLabel;
    private PolicyDiffTree leftPolicyTree;
    private PolicyDiffTree rightPolicyTree;
    private JScrollPane leftPolicyTreeScrollPane;
    private JScrollPane leftAssertionNumScrollPane;
    private JScrollPane rightPolicyTreeScrollPane;
    private JScrollPane rightAssertionNumScrollPane;
    private JButton showOrHideAssertionDiffButton;
    private JSplitPane diffSplitPane;

    private List<Integer> nextDiffRowList;  // Store the row numbers of all next diffs in the diff result list
    private int currentDiffRow = -1;        // The current element in nextDiffRowList.  It is not necessary that currentDiffRow is same as the selected row.

    private Pair<String, PolicyTreeModel> leftPolicyInfo, rightPolicyInfo;
    private Map<Integer, DiffType> diffResultMapL;
    private Map<Integer, AssertionDiffUI> assertionDiffUICache = new WeakHashMap<>(); // Store AssertionDiffUI objects embedded in the policy diff window.
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

        try {
            initializeComponents();
        } catch (Throwable t) {
            // Handle any exceptions and ask to retry.
            dispose();
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                "Policy Comparison is terminated due to error.  Please try to choose left and right policies again!",
                "Policy Comparison Error", JOptionPane.WARNING_MESSAGE, null);
            PolicyDiffContext.setLeftDiffPolicyInfo(null);
            return;
        }

        pack();
        Utilities.centerOnScreen(this);

        // Clean the left policy reference, so policy diff menu item or button will display "Choose Left" next time.
        PolicyDiffContext.setLeftDiffPolicyInfo(null);
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
     * Get a proper length of displaying text.  If the given text is too long to fit the content pane, truncate the
     * middle part of the text and replace the truncated part with "...".
     *
     * @param text: the text to be processed to get a proper length of displaying text
     * @param label: a JLabel object to hold the proper length of displaying text.
     * @param contentPane: a content pane, where the label resides
     *
     * @return a displaying text with a proper length to fit the content pane.
     */
    public static String getDisplayingText(String text, JLabel label, JPanel contentPane) {
        final int maxWidth = Math.max(contentPane.getPreferredSize().width, contentPane.getSize().width) / 2 - 10;
        final FontMetrics fontMetrics = label.getFontMetrics(label.getFont());

        int width = Utilities.computeStringWidth(fontMetrics, text);

        while (width > maxWidth && text.length() > 4) {
            if (text.length() > 13) {
                int middleIdx = text.length() / 2;
                text = text.substring(0, middleIdx - 5) + "..." + text.substring(middleIdx + 5);
            }
            width = Utilities.computeStringWidth(fontMetrics, text);
        }

        return text;
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

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                leftPolicyNameLabel.setText(getDisplayingText(leftPolicyInfo.left, leftPolicyNameLabel, contentPane));
                rightPolicyNameLabel.setText(getDisplayingText(rightPolicyInfo.left, rightPolicyNameLabel, contentPane));
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

        ScrollPanesSynchronizer synchronizer = new ScrollPanesSynchronizer(leftPolicyTreeScrollPane, rightPolicyTreeScrollPane);
        leftPolicyTreeScrollPane.getVerticalScrollBar().addAdjustmentListener(synchronizer);
        leftPolicyTreeScrollPane.getHorizontalScrollBar().addAdjustmentListener(synchronizer);
        rightPolicyTreeScrollPane.getVerticalScrollBar().addAdjustmentListener(synchronizer);
        rightPolicyTreeScrollPane.getHorizontalScrollBar().addAdjustmentListener(synchronizer);

        leftAssertionNumScrollPane.getVerticalScrollBar().setModel(rightPolicyTreeScrollPane.getVerticalScrollBar().getModel());
        rightAssertionNumScrollPane.getVerticalScrollBar().setModel(rightPolicyTreeScrollPane.getVerticalScrollBar().getModel());

        // Initialize the labels showing policy names
        leftPolicyNameLabel.setText(getDisplayingText(leftPolicyInfo.left, leftPolicyNameLabel, contentPane));
        leftPolicyNameLabel.setToolTipText(leftPolicyInfo.left);

        rightPolicyNameLabel.setText(getDisplayingText(rightPolicyInfo.left, rightPolicyNameLabel, contentPane));
        rightPolicyNameLabel.setToolTipText(rightPolicyInfo.left);

        leftPolicyTree.setParentScrollPane(leftPolicyTreeScrollPane);
        rightPolicyTree.setParentScrollPane(rightPolicyTreeScrollPane);

        leftPolicyTree.setModel(leftPolicyInfo.right);
        rightPolicyTree.setModel(rightPolicyInfo.right);

        generateDiffResults();

        // After the results obtained, initialize two assertion number trees
        AssertionLineNumbersTree leftAssertionNumberTree = new AssertionLineNumbersTree(leftPolicyTree);
        leftAssertionNumberTree.setVisible(true);
        leftAssertionNumberTree.updateOrdinalsDisplaying();
        leftAssertionNumScrollPane.getViewport().add(leftAssertionNumberTree);

        AssertionLineNumbersTree rightAssertionNumberTree = new AssertionLineNumbersTree(rightPolicyTree);
        rightAssertionNumberTree.setVisible(true);
        rightAssertionNumberTree.updateOrdinalsDisplaying();
        rightAssertionNumScrollPane.getViewport().add(rightAssertionNumberTree);

        // Initialize diff navigation buttons
        enableOrDisableDiffNavigationButtons(-1);  // -1 means we force this method to use no selection in policy trees.

        // Clean selection of two policy diff trees
        leftPolicyTree.getSelectionModel().clearSelection();
        rightPolicyTree.getSelectionModel().clearSelection();
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
            AssertionDiffUI assertionDiffUI = assertionDiffUICache.get(assertionNodeRow);
            if (assertionDiffUI == null) {
                assertionDiffUI = new AssertionDiffUI(
                    PolicyDiffWindow.this,
                    (AssertionTreeNode) leftPolicyTree.getLastSelectedPathComponent(),
                    (AssertionTreeNode) rightPolicyTree.getLastSelectedPathComponent()
                );
                // Put a new assertion panel into the cache1
                assertionDiffUICache.put(assertionNodeRow, assertionDiffUI);
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
        return diffResultMapL != null && (! diffResultMapL.isEmpty()) && MATCHED_WITH_DIFFERENCES == diffResultMapL.get(assertionNodeRow);
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
            if (selectedRow == -1 && currentDiffRow == -1) {
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

            final AssertionDiffUI assertionDiffUI = new AssertionDiffUI(nodeL, nodeR);
            assertionDiffUI.movePropColumnToLeft();

            final AssertionDiffWindow assertionDiffWindow = new AssertionDiffWindow(assertionDiffUI);
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

                assertionDiffUI.setParent(this);

                final JPanel assertionDiffContentPane = assertionDiffUI.getContentPane();
                assertionDiffContentPane.registerKeyboardAction(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dispose();
                    }
                }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

                add(assertionDiffContentPane, BorderLayout.CENTER);

                ((JComponent)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

                pack();
                Utilities.centerOnScreen(this);
            }
        }
    }
}