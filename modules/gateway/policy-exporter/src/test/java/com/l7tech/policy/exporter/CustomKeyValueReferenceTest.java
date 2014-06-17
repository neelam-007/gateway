package com.l7tech.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.CustomKeyStoreEntityHeader;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.TooManyChildElementsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test CustomKeyValueReference
 */
@RunWith(MockitoJUnitRunner.class)
public class CustomKeyValueReferenceTest {
    @Mock
    private ExternalReferenceFinder finder;

    @Test
    public void testEqualsAndHash() throws Exception {
        // diff key, diff prefix, diff bytes
        CustomKeyValueReference ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null));
        CustomKeyValueReference ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix2","test2".getBytes(),null));
        assertThat("diff key, diff prefix, diff bytes => not equal", ref1, not(equalTo(ref2)));
        assertThat("diff key, diff prefix, diff bytes => hash not equal as well", ref1.hashCode(), not(equalTo(ref2.hashCode())));

        // same key, diff prefix, diff bytes
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix2","test2".getBytes(),null));
        assertThat("same key, diff prefix, diff bytes => equal", ref1, equalTo(ref2));
        assertThat("same key, diff prefix, diff bytes => hash equal as well", ref1.hashCode(), equalTo(ref2.hashCode()));

        // diff key, same prefix, diff bytes
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix1","test2".getBytes(),null));
        assertThat("diff key, same prefix, diff bytes => not equal", ref1, not(equalTo(ref2)));
        assertThat("diff key, same prefix, diff bytes => hash not equal as well", ref1.hashCode(), not(equalTo(ref2.hashCode())));

        // diff key, diff prefix, same bytes
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix2","test1".getBytes(),null));
        assertThat("diff key, diff prefix, same bytes => not equal", ref1, not(equalTo(ref2)));
        assertThat("diff key, diff prefix, same bytes => hash not equal as well", ref1.hashCode(), not(equalTo(ref2.hashCode())));

        // diff key, same prefix, same bytes
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix1","test1".getBytes(),null));
        assertThat("diff key, same prefix, same bytes => not equal", ref1, not(equalTo(ref2)));
        assertThat("diff key, same prefix, same bytes => hash not equal as well", ref1.hashCode(), not(equalTo(ref2.hashCode())));

        // same key, same prefix, same bytes
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),null));
        assertThat("same key, same prefix, same bytes => equal", ref1, equalTo(ref2));
        assertThat("same key, same prefix, same bytes => hash equal as well", ref1.hashCode(), equalTo(ref2.hashCode()));


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
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix2","test2".getBytes(),TestCustomEntitySerializer2.class.getName()));
        assertThat("diff key, diff prefix, diff bytes, diff serializer => not equal", ref1, not(equalTo(ref2)));
        assertThat("diff key, diff prefix, diff bytes, diff serializer => hash not equal as well", ref1.hashCode(), not(equalTo(ref2.hashCode())));

        // same key, diff prefix, diff bytes, diff serializer
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix2","test2".getBytes(),TestCustomEntitySerializer2.class.getName()));
        assertThat("same key, diff prefix, diff bytes, diff serializer => equal", ref1, equalTo(ref2));
        assertThat("same key, diff prefix, diff bytes, diff serializer => hash equal as well", ref1.hashCode(), equalTo(ref2.hashCode()));

        // diff key, same prefix, diff bytes, diff serializer
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix1","test2".getBytes(),TestCustomEntitySerializer2.class.getName()));
        assertThat("diff key, same prefix, diff bytes, diff serializer => not equal", ref1, not(equalTo(ref2)));
        assertThat("diff key, same prefix, diff bytes, diff serializer => hash not equal as well", ref1.hashCode(), not(equalTo(ref2.hashCode())));

        // diff key, diff prefix, same bytes, diff serializer
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix2","test1".getBytes(),TestCustomEntitySerializer2.class.getName()));
        assertThat("diff key, diff prefix, same bytes, diff serializer => not equal", ref1, not(equalTo(ref2)));
        assertThat("diff key, diff prefix, same bytes, diff serializer => hash not equal as well", ref1.hashCode(), not(equalTo(ref2.hashCode())));

        // diff key, diff prefix, diff bytes, same serializer
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix2","test2".getBytes(),TestCustomEntitySerializer1.class.getName()));
        assertThat("diff key, diff prefix, diff bytes, same serializer => not equal", ref1, not(equalTo(ref2)));
        assertThat("diff key, diff prefix, diff bytes, same serializer => hash not equal as well", ref1.hashCode(), not(equalTo(ref2.hashCode())));

        // diff key, same prefix, same bytes, same serializer
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        assertThat("diff key, same prefix, same bytes, same serializer => not equal", ref1, not(equalTo(ref2)));
        assertThat("diff key, same prefix, same bytes, same serializer => hash not equal as well", ref1.hashCode(), not(equalTo(ref2.hashCode())));

        // same key, same prefix, same bytes, same serializer
        ref1 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        ref2 = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        assertThat("same key, same prefix, same bytes, same serializer => equal", ref1, equalTo(ref2));
        assertThat("same key, same prefix, same bytes, same serializer => hash equal as well", ref1.hashCode(), equalTo(ref2.hashCode()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgumentWhenHeaderHasNullBytes() throws Exception {
        class TestCustomEntitySerializer1 implements CustomEntitySerializer {
            private static final long serialVersionUID = -1624947339284400286L;
            @Override public byte[] serialize(Object entity) { return new byte[0]; }
            @Override public Object deserialize(byte[] bytes) { return null; }
        }
        new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1",null,TestCustomEntitySerializer1.class.getName()));
    }

    @Test
    public void testWhenSerializerIsNull() throws Exception {
        new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","bytes1".getBytes(),null));
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void testInvalidDocumentFormatWhenNodeNameIsWrongRefType() throws Exception {
        final String TEST_XML = "<WrongRefType RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </WrongRefType>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void testInvalidDocumentFormatWhenConfigIsMissing() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <ConfigMissing>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </ConfigMissing>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = TooManyChildElementsException.class)
    public void testInvalidDocumentFormatWithMultipleConfigNodes() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key2</Key>\n" +
                "                <KeyPrefix>prefix2</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes2".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void testInvalidDocumentFormatWhenKeyIsMissing() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void testInvalidDocumentFormatWhenKeyIsEmpty() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key/>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = TooManyChildElementsException.class)
    public void testInvalidDocumentFormatWithMultipleKeyNodes() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Key>key2</Key>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void testInvalidDocumentFormatWhenPrefixIsMissing() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void testInvalidDocumentFormatWhenPrefixIsEmpty() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix/>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = TooManyChildElementsException.class)
    public void testInvalidDocumentFormatWithMultiplePrefixNodes() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <KeyPrefix>prefix2</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void testInvalidDocumentFormatWhenBytesAreMissing() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void testInvalidDocumentFormatWhenBytesAreEmpty() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value/>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test(expected = TooManyChildElementsException.class)
    public void testInvalidDocumentFormatWithMultipleBytesNodes() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes2".getBytes()) + "</Base64Value>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test
    public void testSerializerNodeIsMissing() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test
    public void testSerializerNodeIsEmpty() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer/>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test
    public void testKeyValueStoreNameNodeIsMissing() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test
    public void testKeyValueStoreNameNodeIsEmpty() throws Exception {
        final String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>Serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName/>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";

        CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
    }

    @Test
    public void testParseFromElement() throws Exception {
        String TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer>serializer1</Serializer>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";
        CustomKeyValueReference reference = CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
        assertNull(reference.getRefId());
        assertThat(reference.getRefType(), equalTo("com.l7tech.console.policy.exporter.CustomKeyValueReference"));
        assertNotNull(reference.getSyntheticRefId());
        assertThat(reference.getEntityKey(), equalTo("key1"));
        assertThat(reference.getEntityKeyPrefix(), equalTo("prefix1"));
        assertThat(reference.getEntityBase64Value(), equalTo(HexUtils.encodeBase64("bytes1".getBytes())));
        assertThat(reference.getEntitySerializer(), equalTo("serializer1"));


        TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";
        reference = CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
        assertNull(reference.getRefId());
        assertThat(reference.getRefType(), equalTo("com.l7tech.console.policy.exporter.CustomKeyValueReference"));
        assertNotNull(reference.getSyntheticRefId());
        assertThat(reference.getEntityKey(), equalTo("key1"));
        assertThat(reference.getEntityKeyPrefix(), equalTo("prefix1"));
        assertThat(reference.getEntityBase64Value(), equalTo(HexUtils.encodeBase64("bytes1".getBytes())));
        assertNull(reference.getEntitySerializer());


        TEST_XML = "<CustomKeyValueReference RefType=\"com.l7tech.console.policy.exporter.CustomKeyValueReference\">\n" +
                "            <Serializer/>\n" +
                "            <Config>\n" +
                "                <KeyValueStoreName>internalTransactional</KeyValueStoreName>\n" +
                "                <Key>key1</Key>\n" +
                "                <KeyPrefix>prefix1</KeyPrefix>\n" +
                "                <Base64Value>" + HexUtils.encodeBase64("bytes1".getBytes()) + "</Base64Value>\n" +
                "            </Config>\n" +
                "        </CustomKeyValueReference>";
        reference = CustomKeyValueReference.parseFromElement(
                finder,
                XmlUtil.parse(TEST_XML).getDocumentElement()
        );
        assertNull(reference.getRefId());
        assertThat(reference.getRefType(), equalTo("com.l7tech.console.policy.exporter.CustomKeyValueReference"));
        assertNotNull(reference.getSyntheticRefId());
        assertThat(reference.getEntityKey(), equalTo("key1"));
        assertThat(reference.getEntityKeyPrefix(), equalTo("prefix1"));
        assertThat(reference.getEntityBase64Value(), equalTo(HexUtils.encodeBase64("bytes1".getBytes())));
        assertNull(reference.getEntitySerializer());
    }

    @Test
    public void testSerializeToRefElement() throws Exception {
        class TestCustomEntitySerializer1 implements CustomEntitySerializer {
            private static final long serialVersionUID = 2745145665531993665L;
            @Override public byte[] serialize(Object entity) { return new byte[0]; }
            @Override public Object deserialize(byte[] bytes) { return null; }
        }

        CustomKeyValueReference reference = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key1","prefix1","test1".getBytes(),TestCustomEntitySerializer1.class.getName()));
        Document doc = XmlUtil.createEmptyDocument("References", null, "exp");
        reference.serializeToRefElement(doc.getDocumentElement());
        verifyReference(doc.getDocumentElement(),"key1","prefix1",HexUtils.encodeBase64("test1".getBytes()),TestCustomEntitySerializer1.class.getName());

        reference = new CustomKeyValueReference(finder, new CustomKeyStoreEntityHeader("key2","prefix2","test2".getBytes(),null));
        doc = XmlUtil.createEmptyDocument("References", null, "exp");
        reference.serializeToRefElement(doc.getDocumentElement());
        verifyReference(doc.getDocumentElement(),"key2","prefix2",HexUtils.encodeBase64("test2".getBytes()),null);

    }

    private void verifyReference(
            @NotNull final Element documentElement,
            @NotNull final String expectedKeyValue,
            @NotNull final String expectedPrefixValue,
            @NotNull final String expectedBase64BytesValue,
            @Nullable final String expectedSerializerValue
    ) throws Exception {
        assertThat(documentElement.getChildNodes().getLength(), equalTo(1));
        final Element element = (Element)documentElement.getFirstChild();
        assertNotNull(element);
        assertThat(CustomKeyValueReference.REFERENCE_ROOT_NODE, equalTo(element.getNodeName()));

        final Element configNode = DomUtils.findExactlyOneChildElementByName(element, CustomKeyValueReference.REFERENCE_CONFIG_NODE);
        assertNotNull(configNode);

        assertThat(
                CustomKeyValueReference.getExactlyOneParamFromEl( // optional
                        element,
                        CustomKeyValueReference.REFERENCE_SERIALIZER_NODE
                ),
                equalTo(expectedSerializerValue)
        );
        assertThat(
                CustomKeyValueReference.getExactlyOneRequiredParamFromEl( // mandatory
                        configNode,
                        CustomKeyValueReference.REFERENCE_CONFIG_KEY_VALUE_STORE_NAME_NODE
                ),
                equalTo(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME)
        );
        assertThat(
                CustomKeyValueReference.getExactlyOneRequiredParamFromEl( // mandatory
                        configNode,
                        CustomKeyValueReference.REFERENCE_CONFIG_KEY_NODE
                ),
                equalTo(expectedKeyValue)
        );
        assertThat(
                CustomKeyValueReference.getExactlyOneRequiredParamFromEl( // mandatory
                        configNode,
                        CustomKeyValueReference.REFERENCE_CONFIG_PREFIX_NODE
                ),
                equalTo(expectedPrefixValue)
        );
        assertThat(
                CustomKeyValueReference.getExactlyOneRequiredParamFromEl( // mandatory
                        configNode,
                        CustomKeyValueReference.REFERENCE_CONFIG_VALUE_NODE
                ),
                equalTo(expectedBase64BytesValue)
        );
    }

    @Ignore("Not sure whether it is a valid test since it requires user intervention.")
    @Test
    public void testLocalizeAssertion() throws Exception {
        // TODO: Not sure whether it is a valid test since it requires user intervention.
    }
}
