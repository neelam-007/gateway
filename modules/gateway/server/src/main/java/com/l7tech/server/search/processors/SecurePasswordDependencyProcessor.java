package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.search.Dependency;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

/**
 * This is used to find secure passwords given a context variable as a reference. The secure password context variable
 * is of the form. secpass.<name>.plaintext
 *
 * @author Victor Kazakov
 */
public class SecurePasswordDependencyProcessor extends DefaultDependencyProcessor<SecurePassword> implements DependencyProcessor<SecurePassword> {

    @Inject
    private SecurePasswordManager securePasswordManager;

    @NotNull
    public List<DependencyFinder.FindResults<SecurePassword>> find(@NotNull final Object searchValue, @NotNull final Dependency.DependencyType dependencyType, @NotNull final Dependency.MethodReturnType searchValueType) throws FindException {
        switch (searchValueType) {
            case NAME: {
                final SecurePassword securePassword = securePasswordManager.findByUniqueName((String) searchValue);
                return Arrays.<DependencyFinder.FindResults<SecurePassword>>asList(DependencyFinder.FindResults.<SecurePassword>create(securePassword, new EntityHeader(Goid.DEFAULT_GOID, EntityType.SECURE_PASSWORD,(String)searchValue,null)));
            }
            case VARIABLE: {
                final Matcher matcher = ServerVariables.SINGLE_SECPASS_PATTERN.matcher((String) searchValue);
                if (!matcher.matches()) {
                    // Assume it is a literal password
                    return Collections.emptyList();
                }
                final String alias = matcher.group(1);
                assert alias != null; // enforced by regex
                assert alias.length() > 0; // enforced by regex
                final SecurePassword securePassword = securePasswordManager.findByUniqueName(alias);
                return securePassword != null ? Arrays.<DependencyFinder.FindResults<SecurePassword>>asList(DependencyFinder.FindResults.<SecurePassword>create(securePassword,null)) : Collections.<DependencyFinder.FindResults<SecurePassword>>emptyList();
            }
            default:
                return super.find(searchValue, dependencyType, searchValueType);
        }
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws CannotRetrieveDependenciesException {
        switch (searchValueType) {
            case NAME: {
                return Arrays.asList(createDependentObject(new SecurePassword((String) searchValue)));
            }
            case VARIABLE: {
                final Matcher matcher = ServerVariables.SINGLE_SECPASS_PATTERN.matcher((String) searchValue);
                if (!matcher.matches()) {
                    // Assume it is a literal password
                    return Collections.emptyList();
                }
                final String alias = matcher.group(1);
                assert alias != null; // enforced by regex
                assert alias.length() > 0; // enforced by regex
                return Arrays.asList(createDependentObject(new SecurePassword(alias)));
            }
            default:
                //if a different search method is specified then create the secure password using the DefaultDependency processor
                return super.createDependentObjects(searchValue, dependencyType, searchValueType);
        }
    }
}
