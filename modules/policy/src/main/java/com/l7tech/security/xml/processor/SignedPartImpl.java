package com.l7tech.security.xml.processor;

import com.l7tech.common.mime.PartInfo;
import com.l7tech.security.token.SignedPart;
import com.l7tech.security.token.SigningSecurityToken;

/**
* Represents a signed message part that was verified by the WSS processor.
*/
class SignedPartImpl implements SignedPart {
    private final SigningSecurityToken signingToken;
    private final PartInfo partInfo;

    SignedPartImpl(SigningSecurityToken signingToken, PartInfo partInfo) {
        this.signingToken = signingToken;
        this.partInfo = partInfo;
    }

    @Override
    public SigningSecurityToken getSigningSecurityToken() {
        return signingToken;
    }

    @Override
    public PartInfo getPartInfo() {
        return partInfo;
    }
}
