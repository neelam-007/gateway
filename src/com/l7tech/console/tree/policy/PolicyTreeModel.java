package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.service.PublishedService;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.event.TreeModelListener;
import java.io.IOException;


/**
 * <code>PolicyTreeModel</code> is the policy assrtions tree data model.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PolicyTreeModel extends DefaultTreeModel {
    private PublishedService service;
    /**
     * Creates a new instance of PolicyTreeModel with root set
     * to the root assertion.
     *
     * @param root
     */
    public PolicyTreeModel(Assertion root) {
        super(AssertionTreeNodeFactory.asTreeNode(root));
    }

    /**
     * Returns the service that this model represents.
     * Note that the value is optional and may be null.
     * Instance created using the <code>PublishedService</code>
     * register the value.
     *
     * @return the service or <b>null</b> if not set
     */
    public PublishedService getService() {
        return service;
    }

    /**
     * Creates a new instance of PolicyTreeModel for the Published service.
     *
     * @param service
     */
    public static PolicyTreeModel make(PublishedService service) {
        try {
            PolicyTreeModel model = new PolicyTreeModel(WspReader.parse(service.getPolicyXml()));
            model.service = service;
            return model;
        } catch (IOException e) {
            // TODO: FIXME Emil!
            throw new IllegalArgumentException("Policy was unparseable");
        }
    }
}

