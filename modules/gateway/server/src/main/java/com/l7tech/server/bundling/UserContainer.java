package com.l7tech.server.bundling;

import com.l7tech.identity.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;

/**
 * Entity container for holding users and their associated certificates.
 */
public class UserContainer extends EntityContainer<User> {
    @Nullable
    private final X509Certificate certificate;

    /**
     * Creates a new User entity container
     *
     * @param user        The User
     * @param certificate The users certificate if it has one
     */
    public UserContainer(@NotNull final User user, @Nullable final X509Certificate certificate) {
        super(user);
        this.certificate = certificate;
    }


    @Nullable
    public X509Certificate getCertificate() {
        return certificate;
    }
}