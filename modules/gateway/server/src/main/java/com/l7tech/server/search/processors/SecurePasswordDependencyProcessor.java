package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

/**
 * This is used to find secure passwords given a context variable as a reference. The secure password context variable
 * is of the form. secpass.<name>.plaintext
 *
 * @author Victor Kazakov
 */
public class SecurePasswordDependencyProcessor extends GenericDependencyProcessor<SecurePassword> implements DependencyProcessor<SecurePassword> {

    @Inject
    private SecurePasswordManager securePasswordManager;

    @SuppressWarnings("unchecked")
    public List<SecurePassword> find(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        switch (searchValueType) {
            case NAME: {
                final SecurePassword securePassword = securePasswordManager.findByUniqueName((String) searchValue);
                return securePassword != null ? Arrays.asList(securePassword) : null;
            }
            case VARIABLE: {
                Matcher matcher = ServerVariables.SINGLE_SECPASS_PATTERN.matcher((String) searchValue);
                if (!matcher.matches()) {
                    // Assume it is a literal password
                    return null;
                }
                String alias = matcher.group(1);
                assert alias != null; // enforced by regex
                assert alias.length() > 0; // enforced by regex
                final SecurePassword securePassword = securePasswordManager.findByUniqueName(alias);
                return securePassword != null ? Arrays.asList(securePassword) : null;
            }
            default:
                return (List<SecurePassword>) super.find(searchValue, dependencyType, searchValueType);
        }
    }
}
