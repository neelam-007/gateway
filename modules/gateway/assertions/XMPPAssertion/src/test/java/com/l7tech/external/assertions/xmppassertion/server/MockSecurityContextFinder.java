package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.xml.processor.SecurityContextFinder;

/**
 * User: rseminoff
 * Date: 25/05/12
 */
public class MockSecurityContextFinder implements SecurityContextFinder {
    @Override
    public SecurityContext getSecurityContext(String securityContextIdentifier) {
        return new SecurityContext() {
            @Override
            public byte[] getSharedSecret() {
                return new byte[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
            }

            @Override
            public SecurityToken getSecurityToken() {
                return new MockSecurityToken();
            }
        };
    }

    public class MockSecurityToken implements SecurityToken {

        @Override
        public SecurityTokenType getType() {
            return SecurityTokenType.UNKNOWN;
        }
    }

}
