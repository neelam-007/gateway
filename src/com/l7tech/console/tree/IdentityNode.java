package com.l7tech.console.tree;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import java.awt.*;
import java.util.ArrayList;


/**
 * The class represents a node element in the TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class IdentityNode extends AbstractLeafPaletteNode {

    public IdentityNode() {
        super("Grant Access to Users/Groups", "com/l7tech/console/resources/user16.png");
    }

    /**
     * Return assertions representation of the node. This returns
     * the array of selected users and groups
     *
     * @return the assertion corresponding to this node or null
     */
    public Assertion[] asAssertions() {
        Frame f = TopComponents.getInstance().getMainWindow();
        Options options = new Options();
        options.disableOpenProperties();
        options.disposeOnSelect();
        FindIdentitiesDialog fd = new FindIdentitiesDialog(f, true, options);
        fd.pack();
        Utilities.centerOnScreen(fd);
        FindIdentitiesDialog.FindResult result = fd.showDialog();
        java.util.List assertions = new ArrayList();
        IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
        try {
            for (int i = 0; i < result.entityHeaders.length; i++) {
                EntityHeader header = result.entityHeaders[i];
                if (header.getType() == EntityType.USER) {
                    User u = admin.findUserByID(result.providerConfigOid, header.getStrId());
                    if ( u == null ) throw new RuntimeException("Couldn't find user " + header.getStrId() );
                    assertions.add(new SpecificUser(u.getProviderId(), u.getLogin(), u.getUniqueIdentifier(), u.getName()));
                } else if (header.getType() == EntityType.GROUP) {
                    Group g = admin.findGroupByID(result.providerConfigOid, header.getStrId());
                    if ( g == null ) throw new RuntimeException("Couldn't find group " + header.getStrId() );
                    assertions.add(new MemberOfGroup(g.getProviderId(), g.getName(), g.getUniqueIdentifier()));
                }
            }
            return (Assertion[])assertions.toArray(new Assertion[]{});
        } catch (Exception e) {
            throw new RuntimeException("Couldn't retrieve user or group", e);
        }
    }

    protected boolean isEnabledByLicense() {
        return Registry.getDefault().getLicenseManager().isAuthenticationEnabled();
    }
}