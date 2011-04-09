package com.l7tech.external.assertions.httpdigest;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

/**
 * HttpDigestAssertion modular assertion to 'activate' core HttpDigest assertion which is hidden
 * by default since Chinook.
 * 
 */
public class HttpDigestAssertion extends HttpDigest {
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = (DefaultAssertionMetadata) new HttpDigest().meta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{ "accessControl"});
        return meta;
    }
}
