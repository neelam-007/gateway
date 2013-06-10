package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.regex.Matcher;

/**
 * This is used to find secure passwords given a context variable as a reference. The secure password context variable is of the form. secpass.<name>.plaintext
 *
 * @author Victor Kazakov
 */
public class SecurePasswordDependencyProcessor extends GenericDependencyProcessor<SecurePassword> implements DependencyProcessor<SecurePassword> {

    @Inject
    private SecurePasswordManager securePasswordManager;

    public SecurePassword find(@NotNull Object searchValue, com.l7tech.search.Dependency dependency) throws FindException {
        switch (dependency.methodReturnType()) {
            case NAME:
                return securePasswordManager.findByUniqueName((String) searchValue);
            case Variable: {
                Matcher matcher = ServerVariables.SINGLE_SECPASS_PATTERN.matcher((String)searchValue);
                if (!matcher.matches()) {
                    // Assume it is a literal password
                    return null;
                }
                String alias = matcher.group(1);
                assert alias != null; // enforced by regex
                assert alias.length() > 0; // enforced by regex
                return securePasswordManager.findByUniqueName(alias);
            }
            default:
                return (SecurePassword) super.find(searchValue, dependency);
        }
    }
}
