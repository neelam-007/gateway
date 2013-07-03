package com.l7tech.gateway.common.custom;

import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.ServiceInvocation;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;

import java.util.HashSet;
import java.util.Set;

/**
 * The class <code>CustomAssertionDescriptor</code> contains the runtime information that represent a custom assertion.
 * Required:
 * <ul>
 * <li> assertion name
 * <li> <code>CustomAssertion</code> class
 * <li> corresponding server side <code>ServiceInvocation</code> class
 * <li> assertion <code>Category</code>
 * </ul>
 * Optional:
 * <ul>
 * <li> UI class, must be implementation of <code>CustomAssertionUI</code>
 * <li> task action UI class, must be implementation of <code>CustomTaskActionUI</code>*
 * <li> optional description
 * <li> flag indicating whether or not the UI should be opened automatically when the assertion is added to a policy
 * <li> white list allowed packages for use by the SSM
 * <li> white list allowed resources for use by the SSM
 * <li> display name for the node in the left palette
 * <li> display name for the node in the right policy editor
 * <li> display module file name in the Assertion View Info dialog
 * </ul>
 */
public class CustomAssertionDescriptor {
    private final Class assertion;
    private final Class serverAssertion;
    private final String name;
    private final Category category;

    private Class uiClass;
    private Class taskActionUiClass;
    private String moduleFileName;
    private String description;
    private String paletteNodeName;
    private String policyNodeName;
    private boolean isUiAutoOpen;
    private String[] uiAllowedPackages = new String[0];
    private Set<String> uiAllowedResources = new HashSet<>();

    /**
     * Create the new extensibility holder instance with the assertion and server assertion class.
     *
     * @param name                 the assertion name
     * @param assertionClass       the assertion class
     * @param serverAssertionClass the server side assertion class
     * @param cat                  the assertion category
     */
    public CustomAssertionDescriptor(final String name,
                                     final Class assertionClass,
                                     final Class serverAssertionClass,
                                     final Category cat) {

        this.name = name;
        this.assertion = assertionClass;
        this.category = cat;

        if (!CustomAssertion.class.isAssignableFrom(assertionClass)) {
            throw new IllegalArgumentException("Assertion " + assertionClass);
        }

        this.serverAssertion = serverAssertionClass;

        if (!ServiceInvocation.class.isAssignableFrom(serverAssertionClass)) {
            throw new IllegalArgumentException("Server assertion " + serverAssertionClass);
        }

        if (uiClass != null && !CustomAssertionUI.class.isAssignableFrom(uiClass)) {
            throw new IllegalArgumentException("Editor assertion " + uiClass);
        }
    }

    /**
     * @return the custom assertion name
     */
    public String getName() {
        return name;
    }

    public String getModuleFileName() {
        return moduleFileName;
    }

    public String getDescription() {
        return description;
    }

    public String getPaletteNodeName() {
        return paletteNodeName;
    }

    public String getPolicyNodeName() {
        return policyNodeName;
    }

    public boolean getIsUiAutoOpen() {
        return isUiAutoOpen;
    }

    public String[] getUiAllowedPackages() {
        return uiAllowedPackages;
    }

    public Set<String> getUiAllowedResources() {
        return uiAllowedResources;
    }

    /**
     * @return the assertion class
     */
    public Class getAssertion() {
        return assertion;
    }

    /**
     * @return the server assertion class
     */
    public Class getServerAssertion() {
        return serverAssertion;
    }

    /**
     * @return the category for this custom assertion
     * @see Category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return the UI class or <b>null</b> if it has not been set
     */
    public Class getUiClass() {
        return uiClass;
    }

    /**
     * @return the task action UI class or <b>null</b> if it has not been set
     */
    public Class getTakActionUiClass() {
        return taskActionUiClass;
    }

    public String toString() {
        return "[" + "; name='" + name + "'" + "; category=" + category + "; assertion=" + safeName(assertion) + "; serverAssertion="
                + safeName(serverAssertion) + "; editorClass=" + safeName(uiClass) + "]" + super.toString();
    }

    private String safeName(Class cl) {
        return cl != null ? cl.getName() : "null";
    }

    public void setUiClass(Class uiClass) {
        this.uiClass = uiClass;
    }

    public void setTaskActionUiClass(Class taskActionUiClass) {
        this.taskActionUiClass = taskActionUiClass;
        if (taskActionUiClass != null && !CustomTaskActionUI.class.isAssignableFrom(taskActionUiClass)) {
            throw new IllegalArgumentException("Task Action UI " + taskActionUiClass);
        }
    }

    public void setModuleFileName(String moduleFileName) {
        this.moduleFileName = moduleFileName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPaletteNodeName(String paletteNodeName) {
        this.paletteNodeName = paletteNodeName;
    }

    public void setPolicyNodeName(String policyNodeName) {
        this.policyNodeName = policyNodeName;
    }

    public void setUiAutoOpen(boolean uiAutoOpen) {
        isUiAutoOpen = uiAutoOpen;
    }

    public void setUiAllowedPackages(String uiAllowedPackages) {
        if (uiAllowedPackages != null) {
            this.uiAllowedPackages = uiAllowedPackages.split(",");
            for (int ix = 0; ix < this.uiAllowedPackages.length; ix++) {
                this.uiAllowedPackages[ix] = this.uiAllowedPackages[ix].replace('.', '/').trim();
            }
        }
    }

    public void setUiAllowedResources(String uiAllowedResources) {
        if (uiAllowedResources != null) {
            String[] split = uiAllowedResources.split(",");
            for (String aSplit : split) {
                this.uiAllowedResources.add(aSplit.trim());
            }
        }
    }
}