package com.l7tech.console.panels.stepdebug;

import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.stepdebug.DebugAdmin;
import com.l7tech.gateway.common.stepdebug.DebugResult;
import com.l7tech.gateway.common.stepdebug.DebugState;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Timer;
import java.util.logging.Logger;

/**
 * The Policy Step Debug dialog.
 */
public class PolicyStepDebugDialog extends JDialog {
    protected static final Logger logger = Logger.getLogger(PolicyStepDebugDialog.class.getName());

    private static final long MAX_WAIT_TIME_MILLIS = 30000L;
    private static final long REFRESH_TIMER_MILLIS = 1L;

    private JPanel mainForm;
    private JSplitPane splitPane;
    private DebugContextVariableTreePanel contextVariableTreePanel;
    private DebugPolicyTreePanel policyTreePanel;
    private JButton startButton;
    private JButton stopButton;
    private JButton stepOverButton;
    private JButton stepIntoButton;
    private JButton stepOutButton;
    private JButton resumeButton;
    private JButton removeAllBreakpointsButton;
    private JButton closeButton;

    private final DebugAdmin debugAdmin;
    private final Goid entityGoid;
    private final Policy policy;
    private DebugResult debugResult;
    private Timer refreshTimer;
    private boolean scrollToCurrentLineOnRefresh = true;

    /**
     * Checks whether or not a breakpoint can be set for the given assertion.
     *
     * @param assertion the assertion
     * @return true if breakpoint can be set, false otherwise
     */
    static boolean isBreakpointAllowed(Assertion assertion) {
        return assertion.isEnabled() && !(assertion instanceof CommentAssertion);
    }

    /**
     * Creates <code>PolicyStepDebugDialog</code>.
     *
     * @param owner the owner
     * @param entityGoid the service or policy GOID
     * @param policy the policy to debug
     */
    public PolicyStepDebugDialog(@NotNull Frame owner, @NotNull Goid entityGoid, @NotNull Policy policy) {
        super(owner, "Service Debugger", true);

        Option<DebugAdmin> option = Registry.getDefault().getAdminInterface(DebugAdmin.class);
        if (option.isSome()) {
            this.debugAdmin = option.some();
        } else {
            throw new RuntimeException("DebugAdmin interface not found.");
        }

        this.entityGoid = entityGoid;
        this.policy = policy;
        this.initialize();
    }

    /**
     * Toggles breakpoint for the given node.
     *
     * @param node the node
     */
    void onToggleBreakpoint(@NotNull AssertionTreeNode node) {
        Assertion assertion = node.asAssertion();
        if (isBreakpointAllowed(assertion) && debugResult != null) {
            debugAdmin.toggleBreakpoint(
                debugResult.getTaskId(),
                AssertionTreeNode.getVirtualOrdinal(node));
            scrollToCurrentLineOnRefresh = false;
        }
    }

    /**
     * Remove all breakpoints.
     */
    void onRemoveAllBreakpoints() {
        debugAdmin.removeAllBreakpoints(debugResult.getTaskId());
        scrollToCurrentLineOnRefresh = false;
    }

    /**
     * Checks if breakpoint is set for the given node in the policy tree.
     *
     * @param node the node
     * @return true is breakpoint is set, false otherwise
     */
    boolean isBreakpointSet(@NotNull AssertionTreeNode node) {
        if (debugResult != null) {
            return debugResult.getBreakpoints().contains(AssertionTreeNode.getVirtualOrdinal(node));
        }
        return false;
    }

    /**
     * Checks if the given node is the current line being executed.
     *
     * @param node the node
     * @return true if the node is current line, false otherwise
     */
    boolean isCurrentLine(@NotNull AssertionTreeNode node) {
        if (debugResult != null) {
            return AssertionTreeNode.getVirtualOrdinal(node).equals(debugResult.getCurrentLine());
        }
        return false;
    }

    void removeUserContextVariable(@NotNull String name) {
        debugAdmin.removeUserContextVariable(debugResult.getTaskId(), name);
    }

    void addUserContextVariable(@NotNull String name) {
        debugAdmin.addUserContextVariable(debugResult.getTaskId(), name);
    }

    @Override
    public void dispose() {
        if (this.refreshTimer != null) {
            this.refreshTimer.cancel();
            this.refreshTimer = null;
        }

        if (this.debugResult != null) {
            this.debugAdmin.terminateDebug(debugResult.getTaskId());
            this.debugResult = null;
        }

        super.dispose();
    }

    /**
     * Initialize the dialog.
     */
    private void initialize() {
        splitPane.setResizeWeight(0.5);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onStart();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onStop();
            }
        });

        stepOverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onStepOver();
            }
        });

        stepIntoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onStepInto();
            }
        });

        stepOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onStepOut();
            }
        });

        resumeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onResume();
            }
        });

        removeAllBreakpointsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRemoveAllBreakpoints();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setContentPane(mainForm);
        this.pack();
        Utilities.centerOnParentWindow(this);

        boolean isSuccessful = policyTreePanel.initialize(policy);

        // Register shortcut keys.
        // IMPORTANT: Remove key binding for F2 key in JTree components, prior to adding new key bindings.
        // There are JTree components in policyTreePanel and contextVariableTreePanel.
        //
        Utilities.setButtonAccelerator(this, startButton, KeyEvent.VK_F1);
        Utilities.setButtonAccelerator(this, stopButton, KeyStroke.getKeyStroke(KeyEvent.VK_F1, InputEvent.SHIFT_MASK));
        Utilities.setButtonAccelerator(this, stepIntoButton, KeyEvent.VK_F2);
        Utilities.setButtonAccelerator(this, stepOverButton, KeyEvent.VK_F3);
        Utilities.setButtonAccelerator(this, stepOutButton, KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK));
        Utilities.setButtonAccelerator(this, resumeButton, KeyEvent.VK_F4);

        if (isSuccessful) {
            this.onInit();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateComponents();
                }
            });
            refreshTimer = new Timer();
            refreshTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    onRefresh();
                }}, REFRESH_TIMER_MILLIS, REFRESH_TIMER_MILLIS);
        } else {
          this.disableComponents();
        }
    }

    private void onInit() {
        try {
            this.debugResult = debugAdmin.initializeDebug(entityGoid, policy.getType());
        } catch (PermissionDeniedException e) {
            this.dispose();
            throw e;
        }
    }

    private void onStart() {
        Option<String> error = debugAdmin.startDebug(debugResult.getTaskId());
        if (error.isSome()) {
            JOptionPane.showMessageDialog(this,
                error.some(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onStop() {
        debugAdmin.stopDebug(debugResult.getTaskId());
    }

    private void onStepOver() {
        AssertionTreeNode nextNode = policyTreePanel.getNextNodeStepOver();
        if (nextNode != null) {
            debugAdmin.stepOver(debugResult.getTaskId(), AssertionTreeNode.getVirtualOrdinal(nextNode));
        } else {
            // No sibling. Resume.
            debugAdmin.resume(debugResult.getTaskId());
        }
    }

    private void onStepInto() {
        debugAdmin.stepInto(debugResult.getTaskId());
    }

    private void onStepOut() {
        AssertionTreeNode nextNode = policyTreePanel.getNextNodeStepOut();
        if (nextNode != null) {
            debugAdmin.stepOut(debugResult.getTaskId(), AssertionTreeNode.getVirtualOrdinal(nextNode));
        } else {
            // No parent sibling. Resume.
            debugAdmin.resume(debugResult.getTaskId());
        }
    }

    private void onResume() {
        debugAdmin.resume(debugResult.getTaskId());
    }

    private void onClose() {
        this.dispose();
    }

    private void onRefresh() {
        if (debugResult != null) {
            DebugResult updatedDebugResult = debugAdmin.waitForUpdates(debugResult.getTaskId(), MAX_WAIT_TIME_MILLIS);
            if (updatedDebugResult != null) {
                debugResult = updatedDebugResult;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateComponents();
                    }
                });
            }
        }
    }

    private void updateComponents() {
        if (debugResult == null) {
            return;
        }

        switch (this.debugResult.getDebugState()) {
            case STARTED:
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                stepOverButton.setEnabled(false);
                stepIntoButton.setEnabled(false);
                stepOutButton.setEnabled(false);
                resumeButton.setEnabled(false);
                break;

            case AT_BREAKPOINT:
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                stepOverButton.setEnabled(true);
                stepIntoButton.setEnabled(true);
                stepOutButton.setEnabled(true);
                resumeButton.setEnabled(true);
                break;

            case STOPPED:
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                stepOverButton.setEnabled(false);
                stepIntoButton.setEnabled(false);
                stepOutButton.setEnabled(false);
                resumeButton.setEnabled(false);
                break;

            case BREAK_AT_NEXT_LINE:
            case BREAK_AT_NEXT_BREAKPOINT:
                // Do nothing.
                //
                break;

            default:
                throw new RuntimeException("Unknown debug state.");
        }

        boolean scrollToCurrentLine =
            scrollToCurrentLineOnRefresh &&
            debugResult.getCurrentLine() != null &&
            debugResult.getDebugState().equals(DebugState.AT_BREAKPOINT);
        policyTreePanel.update(scrollToCurrentLine);
        contextVariableTreePanel.update(debugResult.getContextVariables());

        // Rest to scroll to current line.
        scrollToCurrentLineOnRefresh = true;
    }

    private void disableComponents() {
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        stepOverButton.setEnabled(false);
        stepIntoButton.setEnabled(false);
        stepOutButton.setEnabled(false);
        resumeButton.setEnabled(false);
        removeAllBreakpointsButton.setEnabled(false);
        contextVariableTreePanel.disableComponents();
    }

    private void createUIComponents() {
        policyTreePanel = new DebugPolicyTreePanel(this);
        contextVariableTreePanel = new DebugContextVariableTreePanel(this);
    }
}