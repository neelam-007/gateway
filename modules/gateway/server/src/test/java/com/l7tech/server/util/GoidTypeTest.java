package com.l7tech.server.util;

import com.l7tech.objectmodel.Goid;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

/**
 * This was created: 7/8/13 as 10:50 AM
 *
 * @author Victor Kazakov
 */
public class GoidTypeTest {

    private GoidType goidType;
    private final static String ColumnName = "goid";
    private final static Goid goid = new Goid(new Random().nextLong(), new Random().nextLong());

    @Before
    public void before() {
        goidType = new GoidType();
    }

    @Test
    public void testNullSafeGet() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getBytes(Matchers.eq(ColumnName))).thenReturn(goid.getBytes());
        Object returnedGoid = goidType.nullSafeGet(resultSet, new String[]{ColumnName}, null);

        Assert.assertEquals(goid, returnedGoid);
    }

    @Test
    public void testNullSafeSet() throws SQLException {
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        int index = 1;
        goidType.nullSafeSet(preparedStatement, goid, index);

        Mockito.verify(preparedStatement).setBytes(Matchers.eq(index), Matchers.argThat(new BaseMatcher<byte[]>() {
            @Override
            public boolean matches(Object o) {
                if (!(o instanceof byte[]))
                    return false;
                return Arrays.equals(goid.getBytes(), (byte[]) o);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(Arrays.toString(goid.getBytes()));
            }
        }));
    }

    @Test
    public void testDeepCopy() {
        Object returnedGoid = goidType.deepCopy(goid);
        Assert.assertEquals(goid, returnedGoid);
        Assert.assertFalse(goid == returnedGoid);
    }
}
