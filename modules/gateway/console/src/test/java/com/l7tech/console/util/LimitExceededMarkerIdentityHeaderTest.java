package com.l7tech.console.util;

import com.l7tech.objectmodel.Goid;
import org.junit.Test;
import org.junit.Assert;
import com.l7tech.test.BugNumber;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.EntityType;

import java.util.Arrays;

/**
 *
 */
public class LimitExceededMarkerIdentityHeaderTest {

    @BugNumber(5847)
    @Test
    public void testLimitExceededMarkerIdentityHeader() {
        new LimitExceededMarkerIdentityHeader();
    }

    @Test
    public void testComparability() {
        IdentityHeader ih1 = new IdentityHeader(new Goid(0,3), "1", EntityType.USER, "admin", "", "", null);
        IdentityHeader ih2 = new IdentityHeader(new Goid(0,2), "1", EntityType.USER, "admin", "", "", null);
        IdentityHeader ih3 = new IdentityHeader(new Goid(0,1), "1", EntityType.USER, "admin", "", "", null);
        IdentityHeader ih4 = new IdentityHeader(new Goid(0,1), "2", EntityType.USER, "admin2", "", "", null);
        IdentityHeader ih5 = new IdentityHeader(new Goid(0,2), "2", EntityType.USER, "admin2", "", "", null);
        IdentityHeader ih6 = new IdentityHeader(new Goid(0,1), "0", EntityType.USER, "admin3", "", "", null);
        IdentityHeader ih7 = new IdentityHeader(new Goid(0,4), "1", EntityType.USER, "admin", "", "", null);
        IdentityHeader ih8 = new LimitExceededMarkerIdentityHeader();

        IdentityHeader[] headers1 = {
                ih1, ih2, ih3, ih4, ih5, ih6, ih7, ih8
        };

        Arrays.sort( headers1 );

        Assert.assertEquals("Incorrect sort position.", 1, Arrays.binarySearch(headers1, ih3));
        Assert.assertEquals("Incorrect sort position.", 2, Arrays.binarySearch(headers1, ih2));
        Assert.assertEquals("Incorrect sort position.", 3, Arrays.binarySearch(headers1, ih1));
        Assert.assertEquals("Incorrect sort position.", 4, Arrays.binarySearch(headers1, ih7));
        Assert.assertEquals("Incorrect sort position.", 5, Arrays.binarySearch(headers1, ih4));
        Assert.assertEquals("Incorrect sort position.", 6, Arrays.binarySearch(headers1, ih5));
        Assert.assertEquals("Incorrect sort position.", 7, Arrays.binarySearch(headers1, ih6));
        Assert.assertEquals("Incorrect sort position.", 0, Arrays.binarySearch(headers1, ih8));

        IdentityHeader[] headers2 = {
                ih8, ih7, ih6, ih5, ih4, ih3, ih2, ih1
        };

        Arrays.sort( headers2 );

        Assert.assertEquals("Incorrect sort position.", 1, Arrays.binarySearch(headers2, ih3));
        Assert.assertEquals("Incorrect sort position.", 2, Arrays.binarySearch(headers2, ih2));
        Assert.assertEquals("Incorrect sort position.", 3, Arrays.binarySearch(headers2, ih1));
        Assert.assertEquals("Incorrect sort position.", 4, Arrays.binarySearch(headers2, ih7));
        Assert.assertEquals("Incorrect sort position.", 5, Arrays.binarySearch(headers2, ih4));
        Assert.assertEquals("Incorrect sort position.", 6, Arrays.binarySearch(headers2, ih5));
        Assert.assertEquals("Incorrect sort position.", 7, Arrays.binarySearch(headers2, ih6));
        Assert.assertEquals("Incorrect sort position.", 0, Arrays.binarySearch(headers2, ih8));
    }
    
}
