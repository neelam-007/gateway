package com.l7tech.objectmodel;

import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test CustomKeyStoreEntityHeader
 */
public class CustomKeyStoreEntityHeaderTest {
    @Test
    public void testEqualsAndHash() throws Exception {
        // diff key, diff prefix
        CustomKeyStoreEntityHeader header1 = new CustomKeyStoreEntityHeader("key1","prefix1",null,null);
        CustomKeyStoreEntityHeader header2 = new CustomKeyStoreEntityHeader("key2","prefix2",null,null);
        assertThat("diff key, diff prefix => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, diff prefix => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // same key, diff prefix
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1",null,null);
        header2 = new CustomKeyStoreEntityHeader("key1","prefix2",null,null);
        assertThat("same key, diff prefix => equal", header1, equalTo(header2));
        assertThat("same key, diff prefix => hash equal as well", header1.hashCode(), equalTo(header2.hashCode()));

        // diff key, same prefix
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1",null,null);
        header2 = new CustomKeyStoreEntityHeader("key2","prefix1",null,null);
        assertThat("diff key, same prefix => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, same prefix => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // same key, same prefix
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1",null,null);
        header2 = new CustomKeyStoreEntityHeader("key1","prefix1",null,null);
        assertThat("same key, same prefix => equal", header1, equalTo(header2));
        assertThat("same key, same prefix => hash equal as well", header1.hashCode(), equalTo(header2.hashCode()));


        // diff key, diff prefix, diff bytes
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null);
        header2 = new CustomKeyStoreEntityHeader("key2","prefix2","test2".getBytes(),null);
        assertThat("diff key, diff prefix, diff bytes => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, diff prefix, diff bytes => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // same key, diff prefix, diff bytes
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null);
        header2 = new CustomKeyStoreEntityHeader("key1","prefix2","test2".getBytes(),null);
        assertThat("same key, diff prefix, diff bytes => equal", header1, equalTo(header2));
        assertThat("same key, diff prefix, diff bytes => hash equal as well", header1.hashCode(), equalTo(header2.hashCode()));

        // diff key, same prefix, diff bytes
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null);
        header2 = new CustomKeyStoreEntityHeader("key2","prefix1","test2".getBytes(),null);
        assertThat("diff key, same prefix, diff bytes => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, same prefix, diff bytes => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // diff key, diff prefix, same bytes
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null);
        header2 = new CustomKeyStoreEntityHeader("key2","prefix2","test1".getBytes(),null);
        assertThat("diff key, diff prefix, same bytes => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, diff prefix, same bytes => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // diff key, same prefix, same bytes
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null);
        header2 = new CustomKeyStoreEntityHeader("key2","prefix1","test1".getBytes(),null);
        assertThat("diff key, same prefix, same bytes => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, same prefix, same bytes => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // same key, same prefix, same bytes
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null);
        header2 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null);
        assertThat("same key, same prefix, same bytes => equal", header1, equalTo(header2));
        assertThat("same key, same prefix, same bytes => hash equal as well", header1.hashCode(), equalTo(header2.hashCode()));


        class TestCustomEntitySerializer1 implements CustomEntitySerializer {
            private static final long serialVersionUID = -1624947339284400286L;
            @Override public byte[] serialize(Object entity) { return new byte[0]; }
            @Override public Object deserialize(byte[] bytes) { return null; }
        }
        class TestCustomEntitySerializer2 implements CustomEntitySerializer {
            private static final long serialVersionUID = 3781259747332050743L;
            @Override public byte[] serialize(Object entity) { return new byte[0]; }
            @Override public Object deserialize(byte[] bytes) { return null; }
        }

        // diff key, diff prefix, diff bytes, diff serializer
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName());
        header2 = new CustomKeyStoreEntityHeader("key2","prefix2","test2".getBytes(),TestCustomEntitySerializer2.class.getName());
        assertThat("diff key, diff prefix, diff bytes, diff serializer => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, diff prefix, diff bytes, diff serializer => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // same key, diff prefix, diff bytes, diff serializer
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName());
        header2 = new CustomKeyStoreEntityHeader("key1","prefix2","test2".getBytes(),TestCustomEntitySerializer2.class.getName());
        assertThat("same key, diff prefix, diff bytes, diff serializer => equal", header1, equalTo(header2));
        assertThat("same key, diff prefix, diff bytes, diff serializer => hash equal as well", header1.hashCode(), equalTo(header2.hashCode()));

        // diff key, same prefix, diff bytes, diff serializer
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName());
        header2 = new CustomKeyStoreEntityHeader("key2","prefix1","test2".getBytes(),TestCustomEntitySerializer2.class.getName());
        assertThat("diff key, same prefix, diff bytes, diff serializer => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, same prefix, diff bytes, diff serializer => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // diff key, diff prefix, same bytes, diff serializer
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName());
        header2 = new CustomKeyStoreEntityHeader("key2","prefix2","test1".getBytes(),TestCustomEntitySerializer2.class.getName());
        assertThat("diff key, diff prefix, same bytes, diff serializer => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, diff prefix, same bytes, diff serializer => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // diff key, diff prefix, diff bytes, same serializer
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName());
        header2 = new CustomKeyStoreEntityHeader("key2","prefix2","test2".getBytes(),TestCustomEntitySerializer1.class.getName());
        assertThat("diff key, diff prefix, diff bytes, same serializer => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, diff prefix, diff bytes, same serializer => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // diff key, same prefix, same bytes, same serializer
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName());
        header2 = new CustomKeyStoreEntityHeader("key2","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName());
        assertThat("diff key, same prefix, same bytes, same serializer => not equal", header1, not(equalTo(header2)));
        assertThat("diff key, same prefix, same bytes, same serializer => hash not equal as well", header1.hashCode(), not(equalTo(header2.hashCode())));

        // same key, same prefix, same bytes, same serializer
        header1 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName());
        header2 = new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName());
        assertThat("same key, same prefix, same bytes, same serializer => equal", header1, equalTo(header2));
        assertThat("same key, same prefix, same bytes, same serializer => hash equal as well", header1.hashCode(), equalTo(header2.hashCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgumentWhenKeyIsNull() throws Exception {
        class TestCustomEntitySerializer1 implements CustomEntitySerializer {
            private static final long serialVersionUID = -2921165125484140054L;
            @Override public byte[] serialize(Object entity) { return new byte[0]; }
            @Override public Object deserialize(byte[] bytes) { return null; }
        }
        //noinspection ConstantConditions
        new CustomKeyStoreEntityHeader(null,"prefix1","bytes1".getBytes(),TestCustomEntitySerializer1.class.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgumentWhenPrefixIsNull() throws Exception {
        class TestCustomEntitySerializer1 implements CustomEntitySerializer {
            private static final long serialVersionUID = -2921165125484140054L;
            @Override public byte[] serialize(Object entity) { return new byte[0]; }
            @Override public Object deserialize(byte[] bytes) { return null; }
        }
        //noinspection ConstantConditions
        new CustomKeyStoreEntityHeader("key1",null,"bytes1".getBytes(),TestCustomEntitySerializer1.class.getName());
    }

    @Test
    public void testWhenBytesAreNull() throws Exception {
        class TestCustomEntitySerializer1 implements CustomEntitySerializer {
            private static final long serialVersionUID = -2921165125484140054L;
            @Override public byte[] serialize(Object entity) { return new byte[0]; }
            @Override public Object deserialize(byte[] bytes) { return null; }
        }
        new CustomKeyStoreEntityHeader("key1","prefix1",null,TestCustomEntitySerializer1.class.getName());
    }

    @Test
    public void testWhenSerializerIsNull() throws Exception {
        new CustomKeyStoreEntityHeader("key1","prefix1","bytes1".getBytes(),null);
    }
}
