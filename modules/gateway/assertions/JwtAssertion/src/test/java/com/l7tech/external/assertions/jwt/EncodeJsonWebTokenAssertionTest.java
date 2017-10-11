package com.l7tech.external.assertions.jwt;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class EncodeJsonWebTokenAssertionTest {
    private EncodeJsonWebTokenAssertion assertion;
    private Goid keyGoid, newKeyGoid;
    private String keyAlias,newKeyAlias;

    private SsgKeyHeader oldHeader;
    private SsgKeyHeader newHeader;

    @Before
    public void setup() throws Exception {
        assertion = new EncodeJsonWebTokenAssertion();
        keyGoid = new Goid(0L, 1L);
        newKeyGoid = new Goid(3L, 4L);

        keyAlias = new String("testAlias");
        newKeyAlias = new String("newTestAlias");

    }

    @Test
    @BugId("DE301894")
    public void getEntitiesUsedNull() {
        assertEquals(0, assertion.getEntitiesUsed().length);
    }

    @Test
    @BugId("DE301894")
    public void getEntitiesUsedNotNull() {
        assertion.setKeyGoid(keyGoid);
        assertion.setKeyAlias(keyAlias);
        assertEquals(1, assertion.getEntitiesUsed().length);
    }

    @Test
    @BugId("DE301894")
    public void replaceEntitySsgKeyHeader() {
        oldHeader=new SsgKeyHeader("oldTestId",keyGoid,keyAlias,"testName");
        newHeader=new SsgKeyHeader("newTestId",newKeyGoid,newKeyAlias,"newTestName");

        assertion.setKeyGoid(keyGoid);
        assertion.setKeyAlias(keyAlias);

        assertion.replaceEntity(oldHeader,newHeader);
        assertEquals(newHeader.getKeystoreId(),assertion.getKeyGoid());
        assertEquals(newHeader.getAlias(),assertion.getKeyAlias());
    }

}
