package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Name resolver for Assertion Access Entity
 */
public class AssertionAccessNameResolver extends EntityNameResolver {
    private AssertionRegistry assertionRegistry;
    private static final String MULTIPLE_PATH = "<multiple>";

    public AssertionAccessNameResolver(AssertionRegistry assertionRegistry, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.assertionRegistry = assertionRegistry;
    }
    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.ASSERTION_ACCESS.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof AssertionAccess;
    }

    @Override
    public String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        String name = StringUtils.EMPTY;
        if (entityHeader.getName() != null) {
            final Assertion assertion = assertionRegistry.findByClassName(entityHeader.getName());
            name = getNameForAssertion(assertion, entityHeader.getName());
        }
        String path = null;
        if (includePath) {
            if (entityHeader instanceof HasFolderId) {
                path = getPath((HasFolderId) entityHeader);
            } else if (entityHeader.getType() == EntityType.ASSERTION_ACCESS) {
                final Assertion assertion = assertionRegistry.findByClassName(entityHeader.getName());
                if (assertion != null) {
                    path = getPaletteFolders(assertion);
                }
            }
        }
        return buildName(name, StringUtils.EMPTY, path, false);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        final AssertionAccess assertionAccess = (AssertionAccess) entity;
        final Assertion assertion = assertionRegistry.findByClassName(assertionAccess.getName());
        String name = getNameForAssertion(assertion, assertionAccess.getName());
        String path = null;
        if (includePath && assertion != null) {
            path = getPaletteFolders(assertion);
        }

        return buildName(name, StringUtils.EMPTY, path, false);
    }

    /**
     * Retrieve a comma-separated list of palette folders that the assertion belongs to.
     *
     * @param assertion the Assertion for which to retrieve its palette folders.
     * @return a comma-separated list of palette folders that the assertion belongs to.
     */

    public String getPaletteFolders(final Assertion assertion) {
        List<String> folderNames = new ArrayList<>();
        if (assertion instanceof CustomAssertionHolder || assertion instanceof EncapsulatedAssertion) {
            folderNames.add(MULTIPLE_PATH);
        } else {
            final Object paletteFolders = assertion.meta().get(AssertionMetadata.PALETTE_FOLDERS);
            if (paletteFolders instanceof String[]) {
                final String[] folderIds = (String[]) paletteFolders;
                for (int i = 0; i < folderIds.length; i++) {
                    final String folderId = folderIds[i];
                    String folderName = "${paletteFolder:" + folderId + "}";
                    folderNames.add(folderName);
                }
            }
        }
        return StringUtils.join(folderNames, ",");
    }
}
