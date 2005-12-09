package com.l7tech.common.security.token;

public interface HasUsernameAndPassword extends HasUsername {
    char[] getPassword();
}
