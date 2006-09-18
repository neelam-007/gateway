package com.l7tech.common.security.token;

public interface HasUsernameAndPassword extends HasUsername {
    /** @return the password, or null if there wasn't one. */
    char[] getPassword();
}
