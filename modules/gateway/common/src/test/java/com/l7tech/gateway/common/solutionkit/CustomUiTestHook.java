package com.l7tech.gateway.common.solutionkit;

import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.Functions;

import javax.swing.*;

/**
 * Helper class to test the Solution Kit Manager UI customization framework.
 */
public class CustomUiTestHook extends SolutionKitManagerUi {
    private Functions.UnaryVoidThrows<SolutionKitManagerContext, RuntimeException> testToRunCreateButton;

    private boolean initializeCalled = false;
    private boolean createButtonCalled = false;

    @Override
    public SolutionKitManagerUi initialize() {
        initializeCalled = true;
        return super.initialize();
    }

    @Override
    public JButton createButton(final JPanel parentPanel) {
        createButtonCalled = true;

        // unit test hook
        if (testToRunCreateButton != null) {
            testToRunCreateButton.call(getContext());
        }

        return null;
    }

    public void setTestToRunCreateButton(Functions.UnaryVoidThrows<SolutionKitManagerContext, RuntimeException> testToRunCreateButton) {
        this.testToRunCreateButton = testToRunCreateButton;
    }

    public boolean isInitializeCalled() {
        return initializeCalled;
    }

    public boolean isCreateButtonCalled() {
        return createButtonCalled;
    }
}
