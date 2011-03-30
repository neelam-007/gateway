package com.l7tech.identity;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: wlui
 */
public class IdentityProviderPasswordPolicyTest extends TestCase {

    @Test
    public void testSave(){
        IdentityProviderPasswordPolicy policy = new IdentityProviderPasswordPolicy();
        policy.setProperty(IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH,5);
        
    }
}
