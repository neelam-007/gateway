package com.l7tech.console.panels;

import com.l7tech.console.MainWindow;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.ActionVetoException;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.FilteredTreeModel;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.policy.PolicyToolBar;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.util.Functions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.beans.PropertyChangeListener;
import java.util.Properties;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the feature, "Multi Policy Tab Support", https://wiki.l7tech.com/mediawiki/index.php/Multi_Policy_Tab_Support
 */
@Ignore("Cannot Test UI Components. For Developer Testing Only")
public class MultiplePolicyTabsTest {
    private static ApplicationContext applicationContext;
    private static WorkSpacePanel workSpacePanel;
    private static WorkSpacePanel.TabbedPane tabbedPane;
    private static WorkSpacePanel.MouseTabListener mouseTabListener;

    @BeforeClass
    public static void setupClass() {
        applicationContext = createApplicationContext();

        final MainWindow mainWindow = new MainWindow(applicationContext.getBean("ssmApplication", SsmApplication.class));
        final JTree servicesAndPolicesTree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        final DefaultTreeModel servicesTreeModel = new FilteredTreeModel(null);
        servicesAndPolicesTree.setModel(servicesTreeModel);
        mainWindow.setPolicyToolBar(new PolicyToolBarStub());

        workSpacePanel = TopComponents.getInstance().getCurrentWorkspace();
        tabbedPane = workSpacePanel.getTabbedPane();
        mouseTabListener = tabbedPane.getMouseTabListener();

        // Set a default value 30 for the property, "Maximum Policy Tabs"
        setPropertyMaximumPolicyTabs(30);

        Registry.setDefault(new RegistryStub());
    }

    @Before
    public void setup() throws FindException, ActionVetoException {
        // For each test, clean the workspace first.
        workSpacePanel.clearWorkspace();
        assertEquals("Workspace is cleaned, so no policy tabs in the workspace pane", 0, tabbedPane.getTabCount());
    }

    /**
     * Test two different policies added into the workspace
     * Result: two tabs are in the workspace pane.
     */
    @Test
    public void testAddDifferentPolicies() throws ActionVetoException {
        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 1", 0), policyTree, false, false));

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 2", 0), policyTree, false, false));

        assertEquals("Two policies opened, so there are two policy tabs in the workspace", 2, tabbedPane.getTabCount());
    }

    /**
     * Test one policy into the workspace twice.
     * Result: only one tab is in the workspace pane.
     */
    @Test
    public void testSamePolicyAddedOnceOnly() throws ActionVetoException, FindException {
        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);

        // Add the policy at the first time
        PolicyEditorPanel.PolicyEditorSubject subject = createPolicyEditorSubject(newGoid(), "Policy", 0);
        workSpacePanel.setComponent(new PolicyEditorPanel(subject, policyTree, false, false));

        // Add the policy at the second time
        final EntityWithPolicyNode policyNode = subject.getPolicyNode();
        Goid policyGoid = policyNode.getPolicy().getGoid();
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(policyGoid, "Policy", 0), policyTree, false, false));

        assertEquals("Same policy added, so only one policy in the workspace", 1, tabbedPane.getTabCount());
    }


    /**
     * Test two policy versions of a policy added into the workspace
     * Result: two tabs are in the workspace pane.
     */
    @Test
    public void testAddDifferentPolicyVersions() throws ActionVetoException, FindException {
        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);

        // Add the first policy version
        PolicyEditorPanel.PolicyEditorSubject subject = createPolicyEditorSubject(newGoid(), "Policy", 0);
        workSpacePanel.setComponent(new PolicyEditorPanel(subject, policyTree, false, false));

        // Add the second policy version
        final EntityWithPolicyNode policyNode = subject.getPolicyNode();
        final Goid policyGoid = policyNode.getPolicy().getGoid();  // Use the Same Policy GOID
        final PolicyEditorPanel secondPanel = new PolicyEditorPanel(createPolicyEditorSubject(policyGoid, "Policy", 0), policyTree, false, false);
        secondPanel.setOverrideVersionNumber(1);                  // But different version ordinals
        workSpacePanel.setComponent(secondPanel);

        assertEquals("Two different policy versions added, so there are two tabs in the workspace", 2, tabbedPane.getTabCount());
    }

    /**
     * Test a HomePage always added in the first position of the workspace pane
     * Result: the index of HomePage is zero.
     */
    @Test
    public void testHomePageIndexAlwaysBeingZero() throws ActionVetoException {
        // Add a policy first
        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        PolicyEditorPanel.PolicyEditorSubject subject = createPolicyEditorSubject(newGoid(), "Policy", 0);
        workSpacePanel.setComponent(new PolicyEditorPanel(subject, policyTree, false, false));

        // Add a HomePage
        workSpacePanel.setComponent(new HomePagePanelStub());

        assertTrue("HomePage Index always is zero", tabbedPane.getComponentAt(0) instanceof HomePagePanel);
    }

    /**
     * Test a HomePage only added once into the workspace pane
     * Result: there is only one HomePage in the workspace pane, even if it has been added twice.
     */
    @Test
    public void testHomePageAddedOnceOnly() throws ActionVetoException {
        // Add a HomePage once
        workSpacePanel.setComponent(new HomePagePanelStub());

        // Add a second HomePage
        workSpacePanel.setComponent(new HomePagePanelStub());

        assertEquals("HomePage cann be added only once", 1, tabbedPane.getTabCount());
    }

    /**
     * Test the property, "Maximum Policy Tabs"
     * Result: if the number of tabs reaches the maximum value, any new tabs can not be added.
     */
    @Test
    public void testPropertyMaximumPolicyTabs() throws ActionVetoException {
        // Overwrite the property "Maximum Policy Tabs" to 2
        setPropertyMaximumPolicyTabs(2);
        assertEquals("Set 'Maximum Policy Tabs' to be 2", "2", TopComponents.getInstance().getPreferences().asProperties().getProperty(SsmPreferences.MAX_NUM_POLICY_TABS));

        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 1", 0), policyTree, false, false));

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 2", 0), policyTree, false, false));

        assertEquals("There are two tabs added so far.", 2, tabbedPane.getTabCount());

        // Try to add one more tab
        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 3", 0), policyTree, false, false));

        assertEquals("The number of added tabs is still 2, since the last one is not successfully added.", 2, tabbedPane.getTabCount());

        // Set the default property value back to 30
        setPropertyMaximumPolicyTabs(30);
        assertEquals("Set 'Maximum Policy Tabs' back to be 30", "30", TopComponents.getInstance().getPreferences().asProperties().getProperty(SsmPreferences.MAX_NUM_POLICY_TABS));
    }

    /**
     * Test the tab action of "Close Tab"
     * Result: the tab preset will be closed.
     */
    @Test
    public void testCloseTab() throws ActionVetoException {
        // Add a policy first
        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        PolicyEditorPanel.PolicyEditorSubject subject = createPolicyEditorSubject(newGoid(), "Policy", 0);
        final PolicyEditorPanel policyEditorPanel = new PolicyEditorPanel(subject, policyTree, false, false);
        workSpacePanel.setComponent(policyEditorPanel);

        assertEquals("One tab added", 1, tabbedPane.getTabCount());

        int presetIndex = tabbedPane.indexOfComponent(policyEditorPanel);
        mouseTabListener.doCloseTab(presetIndex);
        assertEquals("The tab is removed, so no tabs in workspace now", 0, tabbedPane.getTabCount());
    }

    /**
     * Test the tab action of "Close Others"
     * Result: all tabs except the preset tab are closed.
     */
    @Test
    public void testCloseOthers() throws ActionVetoException, FindException {
        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 1", 0), policyTree, false, false));

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 2", 0), policyTree, false, false));

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 3", 0), policyTree, false, false));

        assertEquals("Before closing others, there are three tabs in workspace.", 3, tabbedPane.getTabCount());

        int presetIndex = 1; // Match to the second policy
        mouseTabListener.doCloseOthers(presetIndex);

        assertEquals("There is one tab left after others closed", 1, tabbedPane.getTabCount());
        assertEquals("The name of the tab left is 'Policy 2'", "Policy 2", ((PolicyEditorPanel) tabbedPane.getComponentAt(0)).getPolicyNode().getPolicy().getName());
    }

    /**
     * Test the tab action of "Close All"
     * Result: all tabs are closed.
     */
    @Test
    public void testCloseAll() throws ActionVetoException {
        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 1", 0), policyTree, false, false));

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 2", 0), policyTree, false, false));

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 3", 0), policyTree, false, false));

        assertEquals("Before closing all tabs, there are three tabs in workspace.", 3, tabbedPane.getTabCount());

        mouseTabListener.doCloseAll();

        assertEquals("All tabs are closed.", 0, tabbedPane.getTabCount());
    }

    /**
     * Test the tab action of "Close Unmodified"
     * Result: all tabs without any unsaved changes are closed.
     */
    @Test
    public void testCloseUnmodified() throws ActionVetoException, FindException {
        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        final PolicyEditorPanel firstPolicyEditorPanel = new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 1", 0), policyTree, false, false);
        workSpacePanel.setComponent(firstPolicyEditorPanel);

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 2", 0), policyTree, false, false));

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 3", 0), policyTree, false, false));

        assertEquals("Currently there are three tabs in workspace now", 3, tabbedPane.getTabCount());

        // Modify the first policy tab by setting SaveButtons enabled
        firstPolicyEditorPanel.getToolBar().setSaveButtonsEnabled(true);

        // Close unmodified tabs
        mouseTabListener.doCloseUnmodified();

        assertEquals("There is only one tab left in workspace and other tabs are closed.", 1, tabbedPane.getTabCount());
        assertEquals("The first tab is only one left.", "Policy 1", ((PolicyEditorPanel) tabbedPane.getComponentAt(0)).getPolicyNode().getPolicy().getName());
    }

    /**
     * Test the tab action of "Reopen Closed Tab"
     * Result: The latest closed tab is reopened.
     */
    @Test
    public void testReopenClosedTab() throws ActionVetoException, FindException {
        PolicyTree policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 1", 0), policyTree, false, false));

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 2", 0), policyTree, false, false));

        policyTree = new PolicyTree(applicationContext);
        TopComponents.getInstance().unregisterComponent(PolicyTree.NAME);
        TopComponents.getInstance().registerComponent(PolicyTree.NAME, policyTree);
        workSpacePanel.setComponent(new PolicyEditorPanel(createPolicyEditorSubject(newGoid(), "Policy 3", 0), policyTree, false, false));

        assertEquals("Currently there are three tabs in workspace now", 3, tabbedPane.getTabCount());

        // Close the second tab (name = "Policy 2")
        mouseTabListener.doCloseTab(1);
        assertEquals("Currently there are two tabs in workspace now", 2, tabbedPane.getTabCount());

        // Close the third tab (name = "Policy 3")
        mouseTabListener.doCloseTab(1);
        assertEquals("Currently there is one tab in workspace now", 1, tabbedPane.getTabCount());
        assertEquals("The left tab is the first policy tab.", "Policy 1", ((PolicyEditorPanel) tabbedPane.getComponentAt(0)).getPolicyNode().getPolicy().getName());

        workSpacePanel.setReopenPolicyEditorPanel(new Functions.BinaryVoidThrows<PolicyEditorPanel, Long, FindException>() {
            @Override
            public void call(PolicyEditorPanel pep, Long versionOrdinal) throws FindException {
                try {
                    workSpacePanel.setComponent(pep);
                } catch (ActionVetoException e) {
                    throw new FindException(e.getMessage());
                }
            }
        });

        // Reopen the closed tab, which will be the "Policy 3" tab
        mouseTabListener.doReopenClosedTab();
        assertEquals("There are two tabs in workspace now", 2, tabbedPane.getTabCount());
        assertEquals("The reopened tab should be 'Policy 3'.", "Policy 3", ((PolicyEditorPanel) tabbedPane.getComponentAt(1)).getPolicyNode().getPolicy().getName());

        // Reopen one more tab, which will be the "Policy 2" tab
        mouseTabListener.doReopenClosedTab();
        assertEquals("There are three tabs in workspace now", 3, tabbedPane.getTabCount());
        assertEquals("The reopened tab should be 'Policy 2'.", "Policy 2", ((PolicyEditorPanel) tabbedPane.getComponentAt(2)).getPolicyNode().getPolicy().getName());
    }

    @SuppressWarnings("unchecked")
    private PolicyEditorPanel.PolicyEditorSubject createPolicyEditorSubject(final Goid policyGoid, final String policyName, final int versionOrdinal) {
        // Create a policy node by using the given Policy Goid and Policy Name
        final EntityWithPolicyNode policyNode = new EntityWithPolicyNode(new EntityHeader(newGoid(), EntityType.SERVICE, policyName, "testing multiple tabs")) {
            @Override
            public Entity getEntity() throws FindException {
                return null;
            }

            @Override
            protected String getEntityName() {
                return null;
            }

            @Override
            public void clearCachedEntities() {
            }

            @Override
            public Policy getPolicy() throws FindException {
                Policy policy = new Policy(PolicyType.PRIVATE_SERVICE,
                    policyName,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"Testing Mulitple Tabs\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>",
                    false);
                policy.setGoid(policyGoid);
                policy.setVersionOrdinal(versionOrdinal);

                return policy;
            }

            @Override
            public void updateUserObject() throws FindException {
            }

            @Override
            protected String iconResource(boolean open) {
                return null;
            }
        };

        // Then create a PolicyEditorSubject object by using the policy node just created above
        return new PolicyEditorPanel.PolicyEditorSubject() {
            private Policy getPolicy() {
                try {
                    return policyNode.getPolicy();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to load policy", e);
                }
            }

            @Override
            public EntityWithPolicyNode getPolicyNode() {
                return policyNode;
            }

            @Override
            public Assertion getRootAssertion() {
                return new AllAssertion();
            }

            @Override
            public String getName() {
                return policyNode.getName();
            }

            @Override
            public long getVersionNumber() {
                return getPolicy().getVersionOrdinal();
            }

            @Override
            public boolean isActive() {
                return getPolicy().isVersionActive();
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener policyPropertyChangeListener) {
            }

            @Override
            public void removePropertyChangeListener(PropertyChangeListener policyPropertyChangeListener) {
            }

            @Override
            public boolean hasWriteAccess() {
                return true;
            }
        };
    }

    /**
     * Generate a new random Goid
     * @return a Goid object
     */
    private Goid newGoid() {
        return new Goid(new Random().nextLong(), new Random().nextLong());
    }

    /**
     * Set a value for the property, "Maximum Policy Tabs".
     */
    private static void setPropertyMaximumPolicyTabs(final int propValue) {
        final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
        final Properties properties = preferences.asProperties();
        properties.setProperty(SsmPreferences.MAX_NUM_POLICY_TABS, Integer.toString(propValue));
        preferences.updateFromProperties(properties, true);
    }

    /**
     * Create a application context used by Console components such as SsmApplication, MainWindow, etc.
     * @return an ApplicationContext object
     */
    private static ApplicationContext createApplicationContext() {
        String[] ctxNames = new String[] {
            "com/l7tech/console/resources/beans-context.xml",
            "com/l7tech/console/resources/beans-application.xml",
        };
        return new ClassPathXmlApplicationContext(ctxNames);
    }

    /**
     *  A HomePagePanel stub class overrides reBuildToolbar method only.
     */
    private class HomePagePanelStub extends HomePagePanel {
        @Override
        public void rebuildToolbar() {}
    }

    /**
     * A PolicyToolBar stub class overrides updateActions method only.
     */
    private static class PolicyToolBarStub extends PolicyToolBar {
        @Override
        public void updateActions() {}
    }
}