package com.l7tech.server.wsdm.util;

import com.l7tech.server.service.ServiceCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * This was created: 8/27/13 as 11:11 AM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class EsmUtilsTest {

    String serviceIDQueryStringUrl = "http://autotest.l7tech.com:8080/wsdm/esmsubscriptions?serviceoid=%s";
    String serviceIDPathUrl = "http://autotest.l7tech.com:8080/service/%s";
    String serviceUriQueryStringUrl = "http://autotest.l7tech.com:8080/wsdm/esmsubscriptions?serviceuri=%s";

    @Mock
    ServiceCache serviceCache;

    @Before
    public void before(){

    }

    @Test
    public void oidQueryStringTest(){
        String oid = "7543214";
        String parsedOid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, oid));

        Assert.assertEquals(oid, parsedOid);

        oid = "12345678901234567890";
        parsedOid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, oid));

        Assert.assertEquals(oid, parsedOid);
    }

    @Test
    public void oidQueryStringWithOtherPropTest(){
        String oid = "7543214";
        String extra = "&test=bla";
        String parsedOid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, oid+extra));

        Assert.assertEquals(oid, parsedOid);
    }

    @Test
    public void badIdQueryStringTest(){
        String oid = "dfs75432sdf14q";
        String parsedOid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, oid));

        Assert.assertNull(parsedOid);

        oid = "123456789012345678901";
        parsedOid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, oid));

        Assert.assertNull(parsedOid);
    }

    @Test
    public void goidQueryStringTest(){
        String goid = "74830aee477a695ab3e197ed257319ae";
        String parsedGoid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, goid));

        Assert.assertEquals(goid, parsedGoid);

        goid = "f4830aee477a695ab3e197ed257319ae";
        parsedGoid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, goid));

        Assert.assertEquals(goid, parsedGoid);

        goid = "12345678901234567890123456789012";
        parsedGoid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, goid));

        Assert.assertEquals(goid, parsedGoid);
    }

    @Test
    public void badGoidQueryStringTest(){
        String goid = "24830aee477a695ab3e197ed257319aef";
        String parsedGoid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, goid));

        Assert.assertNull(parsedGoid);
    }

    @Test
    public void badGoidQueryStringTest2(){
        String goid = "a4830aee477a695ab3e197ed257319ag";
        String parsedGoid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, goid));

        Assert.assertNull(parsedGoid);
    }

    @Test
    public void badGoidQueryStringTest3(){
        String goid = "a4830aee477a695ab3e197ed257319a";
        String parsedGoid = EsmUtils.getServiceOidFromQueryString(String.format(serviceIDQueryStringUrl, goid));

        Assert.assertNull(parsedGoid);
    }

    @Test
    public void oidPathTest(){
        String oid = "7543214";
        String parsedOid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, oid));

        Assert.assertEquals(oid, parsedOid);

        oid = "12345678901234567890";
        parsedOid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, oid));

        Assert.assertEquals(oid, parsedOid);
    }

    @Test
    public void oidPathWithOtherUrl(){
        String oid = "7543214";
        String extra = "/test/bla";
        String parsedOid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, oid+extra));

        Assert.assertEquals(oid, parsedOid);
    }

    @Test
    public void oidPathWithOtherPropTest(){
        String oid = "7543214";
        String extra = "?test=bla";
        String parsedOid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, oid+extra));

        Assert.assertEquals(oid, parsedOid);
    }

    @Test
    public void badIdPathTest(){
        String oid = "dfs75432sdf14q";
        String parsedOid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, oid));

        Assert.assertNull(parsedOid);

        oid = "123456789012345678901";
        parsedOid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, oid));

        Assert.assertNull(parsedOid);
    }

    @Test
    public void goidPathTest(){
        String goid = "74830aee477a695ab3e197ed257319ae";
        String parsedGoid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, goid));

        Assert.assertEquals(goid, parsedGoid);

        goid = "f4830aee477a695ab3e197ed257319ae";
        parsedGoid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, goid));

        Assert.assertEquals(goid, parsedGoid);

        goid = "12345678901234567890123456789012";
        parsedGoid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, goid));

        Assert.assertEquals(goid, parsedGoid);
    }

    @Test
    public void badGoidPathTest(){
        String goid = "24830aee477a695ab3e197ed257319aef";
        String parsedGoid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, goid));

        Assert.assertNull(parsedGoid);
    }

    @Test
    public void badGoidPathTest2(){
        String goid = "a4830aee477a695ab3e197ed257319ag";
        String parsedGoid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, goid));

        Assert.assertNull(parsedGoid);
    }

    @Test
    public void badGoidPathTest3(){
        String goid = "a4830aee477a695ab3e197ed257319a";
        String parsedGoid = EsmUtils.getServiceOidFromServiceUrl(String.format(serviceIDPathUrl, goid));

        Assert.assertNull(parsedGoid);
    }
}
