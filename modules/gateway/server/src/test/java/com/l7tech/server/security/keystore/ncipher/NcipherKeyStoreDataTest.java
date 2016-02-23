package com.l7tech.server.security.keystore.ncipher;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.AbstractMapDecorator;
import org.apache.commons.collections.map.TransformedMap;
import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NcipherKeyStoreDataTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static String POWNED_SYS_PROP = "com.l7tech.test.powned";

    private String enableUnsafeSerialization;

    @Before
    public void setUp() throws Exception {
        // make sure InvokerTransformer is available in common collections
        enableUnsafeSerialization = System.getProperty("org.apache.commons.collections.enableUnsafeSerialization");
        System.setProperty("org.apache.commons.collections.enableUnsafeSerialization", "true");
        // make sure to reset our system property
        System.setProperty(POWNED_SYS_PROP, "false");
    }

    @After
    public void tearDown() throws Exception {
        folder.delete();
        // reset property
        if (enableUnsafeSerialization != null) {
            System.setProperty("org.apache.commons.collections.enableUnsafeSerialization", enableUnsafeSerialization);
        } else {
            System.clearProperty("org.apache.commons.collections.enableUnsafeSerialization");
        }
    }

    @Test
    public void testToBytes() throws Exception {
        // create 10 samples
        for (int i = 0; i < 10; ++i) {
            ////////////////////////////////////////////////////////////////////////////////
            // first test with valid payload
            ////////////////////////////////////////////////////////////////////////////////
            // make sure system property is reset
            Assert.assertThat(System.getProperty(POWNED_SYS_PROP), Matchers.equalToIgnoringCase("false"));
            // create original nCipher data
            NcipherKeyStoreData original = createOK_nCipherKeyStoreData();
            Assert.assertNotNull(original);
            // serialize original
            byte[] bytes = original.toBytes();
            Assert.assertNotNull(bytes);

            // deserialize it from bytes
            NcipherKeyStoreData serialized = NcipherKeyStoreData.createFromBytes(bytes);
            Assert.assertNotNull(serialized);
            // make sure system property remains reset after deserialization
            Assert.assertThat(System.getProperty(POWNED_SYS_PROP), Matchers.equalToIgnoringCase("false"));

            // make sure original and serialized objects are the equal
            serialized.validate();
            compare(original, serialized);
            ////////////////////////////////////////////////////////////////////////////////


            ////////////////////////////////////////////////////////////////////////////////
            // second make sure our malicious payload does work
            ////////////////////////////////////////////////////////////////////////////////
            // for that "org.apache.commons.collections.enableUnsafeSerialization" must be set to "true"
            Assert.assertThat(System.getProperty("org.apache.commons.collections.enableUnsafeSerialization"), Matchers.equalToIgnoringCase("true"));
            // make sure system property is reset
            Assert.assertThat(System.getProperty(POWNED_SYS_PROP), Matchers.equalToIgnoringCase("false"));

            original = createMalicious_nCipherKeyStoreData();
            Assert.assertNotNull(original);
            bytes = original.toBytes();
            Assert.assertNotNull(bytes);

            // another check that system property remains reset
            Assert.assertThat(System.getProperty(POWNED_SYS_PROP), Matchers.equalToIgnoringCase("false"));
            // deserialize it from bytes (using the original unsecured method)
            serialized = createFromBytes(bytes);
            Assert.assertNotNull(serialized);
            // make sure our exploit works i.e. that our system property is set to true
            Assert.assertThat(System.getProperty(POWNED_SYS_PROP), Matchers.equalToIgnoringCase("true"));

            // make sure original and serialized objects are the equal
            serialized.validate();
            compare(original, serialized);
            ////////////////////////////////////////////////////////////////////////////////

            // reset our system property
            System.setProperty(POWNED_SYS_PROP, "false");

            ////////////////////////////////////////////////////////////////////////////////
            // finally make sure our fix works and that NcipherKeyStoreData is serialization safe
            ////////////////////////////////////////////////////////////////////////////////
            // for that "org.apache.commons.collections.enableUnsafeSerialization" must be set to "true"
            Assert.assertThat(System.getProperty("org.apache.commons.collections.enableUnsafeSerialization"), Matchers.equalToIgnoringCase("true"));
            // make sure system property is reset
            Assert.assertThat(System.getProperty(POWNED_SYS_PROP), Matchers.equalToIgnoringCase("false"));

            original = createMalicious_nCipherKeyStoreData();
            Assert.assertNotNull(original);
            bytes = original.toBytes();
            Assert.assertNotNull(bytes);

            // another check that system property remains reset
            Assert.assertThat(System.getProperty(POWNED_SYS_PROP), Matchers.equalToIgnoringCase("false"));
            // deserialize it from bytes (using the original unsecured method)
            try {
                //noinspection UnusedAssignment
                serialized = NcipherKeyStoreData.createFromBytes(bytes);
                Assert.fail("NcipherKeyStoreData.createFromBytes should have failed with IOException (cause ClassNotFoundException)");
            } catch (IOException e) {
                // ok
                Assert.assertThat(e.getCause(), Matchers.instanceOf(ClassNotFoundException.class));
                serialized = null;
            }
            //noinspection ConstantConditions
            Assert.assertNull(serialized);

            // make sure our exploit was not executed
            Assert.assertThat(System.getProperty(POWNED_SYS_PROP), Matchers.equalToIgnoringCase("false"));
            ////////////////////////////////////////////////////////////////////////////////
        }
    }

    @Test
    public void testWhiteListClasses() throws Exception {
        // try few payloads using different classes

        ////////////////////////////////////////////////////////////////////////////////////////
        // try with whitelisted classes:
        // String, LinkedHashSet, HashSet, HashMap, "[B"
        // no need to test NcipherKeyStoreData, as its cover by other unit tests
        //
        // createFromBytes will still fail with IOException, but the cause will not be ClassNotFoundException (in case if the class is not permitted)
        ////////////////////////////////////////////////////////////////////////////////////////
        doTestPayload(serializeArbitraryObject("test-string"), 0);
        Set<String> set = new LinkedHashSet<>();
        set.add("test1");
        doTestPayload(serializeArbitraryObject(set), 0);
        set = new HashSet<>();
        set.add("test2");
        doTestPayload(serializeArbitraryObject(set), 0);
        Map<String, byte[]> map = new HashMap<>();
        map.put("test", genRndBytes(rnd, 0, 10));
        doTestPayload(serializeArbitraryObject(map), 0);
        doTestPayload(serializeArbitraryObject(genRndBytes(rnd, 10, 100)), 0);
        ////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////
        // try with not whitelisted classes
        //
        // createFromBytes will fail with IOException with message "Ncipher keystore data deserialized to unexpected type:"
        ////////////////////////////////////////////////////////////////////////////////////////
        doTestPayload(serializeArbitraryObject(new String[] {"test-string"}), 1);
        doTestPayload(serializeArbitraryObject(Collections.singleton("test")), 1);
        final Set<Integer> intSet = new HashSet<>();
        intSet.add(1);
        doTestPayload(serializeArbitraryObject(intSet), 1);
        doTestPayload(serializeArbitraryObject(Collections.singletonMap("test", genRndBytes(rnd, 0, 10))), 1);
        Map<Number, byte[]> numByteMap = new HashMap<>();
        numByteMap.put(1, genRndBytes(rnd, 0, 10));
        doTestPayload(serializeArbitraryObject(numByteMap), 1);
        Map<String, Number> strNumMap = new HashMap<>();
        strNumMap.put("test", 1);
        doTestPayload(serializeArbitraryObject(strNumMap), 1);
        map = new LinkedHashMap<>();
        map.put("test", genRndBytes(rnd, 0, 10));
        doTestPayload(serializeArbitraryObject(map), 1);
        map = new Hashtable<>();
        map.put("test", genRndBytes(rnd, 0, 10));
        doTestPayload(serializeArbitraryObject(map), 1);
        map = new IdentityHashMap<>();
        map.put("test", genRndBytes(rnd, 0, 10));
        doTestPayload(serializeArbitraryObject(map), 1);
        map = new TreeMap<>();
        map.put("test", genRndBytes(rnd, 0, 10));
        doTestPayload(serializeArbitraryObject(map), 1);
        map = new ConcurrentHashMap<>();
        map.put("test", genRndBytes(rnd, 0, 10));
        doTestPayload(serializeArbitraryObject(map), 1);
        doTestPayload(serializeArbitraryObject(1), 1);
        doTestPayload(serializeArbitraryObject(1L), 1);
        doTestPayload(serializeArbitraryObject(1D), 1);
        doTestPayload(serializeArbitraryObject(1F), 1);
        doTestPayload(serializeArbitraryObject((short)1), 1);
        doTestPayload(serializeArbitraryObject((byte) 1), 1);
        doTestPayload(serializeArbitraryObject('a'), 1);
        doTestPayload(serializeArbitraryObject(new Byte[0]), 1);
        doTestPayload(serializeArbitraryObject(true), 1);
        ////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////
        // try with arbitrary bytes
        ////////////////////////////////////////////////////////////////////////////////////////
        doTestPayload(genRndBytes(rnd, 10, 100), 2);
        ////////////////////////////////////////////////////////////////////////////////////////
    }

    /**
     * Calls {@link NcipherKeyStoreData#createFromBytes(byte[])} with the specified {@code bytes}.
     *
     * @param type    the type of test:<ul>
     *                  <li>0 - means {@code bytes} are from whitelisted (permitted) class</li>
     *                  <li>1 - means {@code bytes} are from a non-whitelist class </li>
     *                  <li>2 - means {@code bytes} are arbitrary bytes (i.e. don't contain serialized object)</li>
     *                </ul>
     */
    private void doTestPayload(final byte[] bytes, final int type) {
        Assert.assertNotNull(bytes);
        Assert.assertThat(bytes.length, Matchers.greaterThan(0));
        Assert.assertThat(type, Matchers.anyOf(Matchers.is(0), Matchers.is(1), Matchers.is(2)));
        try {
            NcipherKeyStoreData.createFromBytes(bytes);
        }  catch (final StreamCorruptedException e) {
            Assert.assertThat(type, Matchers.is(2));
        } catch (final IOException e) {
            switch (type) {
                case 0: // whitelisted
                    Assert.assertNull(e.getCause());
                    Assert.assertThat(e.getMessage(), Matchers.containsString("Ncipher keystore data deserialized to unexpected type:"));
                    break;
                case 1: // not whitelisted
                    Assert.assertThat(e.getCause(), Matchers.instanceOf(ClassNotFoundException.class));
                    break;
                case 2: // not serialized object
                    Assert.assertNull(e.getCause());
                    Assert.assertThat(e.getMessage(), Matchers.containsString("Ncipher keystore data deserialized to null"));
                    break;
                default:
                    Assert.fail("unexpected type: " + type);
                    break;
            }
        }
    }

    private byte[] serializeArbitraryObject(final Object obj) throws Exception {
        Assert.assertNotNull(obj);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream os = new ObjectOutputStream(baos)) {
            os.writeObject(obj);
            os.flush();
        }
        return baos.toByteArray();
    }

    static private void compare(final NcipherKeyStoreData original, final NcipherKeyStoreData test) {
        Assert.assertNotNull(original);
        Assert.assertNotNull(test);
        // make sure they are not the same instances
        Assert.assertThat(test, Matchers.not(Matchers.sameInstance(original)));
        Assert.assertThat(test.keystoreMetadata, Matchers.allOf(Matchers.not(Matchers.sameInstance(original.keystoreMetadata)), Matchers.equalTo(original.keystoreMetadata)));
        Assert.assertThat(test.deletedFilenames, Matchers.allOf(Matchers.not(Matchers.sameInstance(original.deletedFilenames)), Matchers.equalTo(original.deletedFilenames)));
        Assert.assertThat(test.fileset, Matchers.not(Matchers.sameInstance(original.fileset)));
        Assert.assertThat(test.fileset.size(), Matchers.is(original.fileset.size()));
        for (final Map.Entry<String, byte[]> entry : original.fileset.entrySet()) {
            Assert.assertThat(test.fileset, Matchers.hasKey(entry.getKey()));
            Assert.assertTrue(Arrays.equals(test.fileset.get(entry.getKey()), entry.getValue()));
        }
    }

    final Random rnd = new Random();
    private NcipherKeyStoreData createOK_nCipherKeyStoreData() throws Exception {
        final NcipherKeyStoreData nCipherData = NcipherKeyStoreData.createFromLocalDisk(RandomStringUtils.randomAlphanumeric(genRndNumber(rnd, 40, 100)), folder.newFolder());
        // add some arbitrary fileset
        for (int i = 0; i < genRndNumber(rnd, 1, 10); ++i) {
            nCipherData.fileset.put("key_jcecsp_fileset" + i, genRndBytes(rnd, 1024, 50*1024));
        }
        // add some arbitrary deletedFilenames
        for (int i = 0; i < genRndNumber(rnd, 0, 10); ++i) {
            nCipherData.addDeletedFiles(new LinkedHashSet<>(Collections.singleton("deletedFile" + i)));
        }
        return nCipherData;
    }

    private NcipherKeyStoreData createMalicious_nCipherKeyStoreData() throws Exception {
        final NcipherKeyStoreData nCipherData = NcipherKeyStoreData.createFromLocalDisk(RandomStringUtils.randomAlphanumeric(genRndNumber(rnd, 40, 100)), folder.newFolder());
        // add some arbitrary fileset
        final Map<String, byte[]> innerMap = new HashMap<>();
        for (int i = 0; i < genRndNumber(rnd, 1, 10); ++i) {
            nCipherData.fileset.put("key_jcecsp_fileset" + i, genRndBytes(rnd, 1024, 50*1024));
        }
        nCipherData.fileset = craftMaliciousPayload(innerMap);
        // add some arbitrary deletedFilenames
        for (int i = 0; i < genRndNumber(rnd, 0, 10); ++i) {
            nCipherData.addDeletedFiles(new LinkedHashSet<>(Collections.singleton("deletedFile" + i)));
        }
        return nCipherData;
    }

    public static final class MyProxyMap extends AbstractMapDecorator implements Serializable {
        private Map<Object, Object> payload;

        public MyProxyMap(Map map, Map<Object, Object> payload) {
            super(map);
            this.payload = payload;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.writeObject(this.payload);
            stream.writeObject(super.map);
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            this.payload = (Map) stream.readObject();
            super.map = (Map) stream.readObject();

            Assert.assertNotNull(map);
            Assert.assertNotNull(payload);

            for (Map.Entry<Object, Object> memberValue : payload.entrySet()) {
                // force decorator to be executed
                memberValue.setValue("some arbitrary value");
            }
        }
    }

    private Map<String, byte[]> craftMaliciousPayload(Map<String, byte[]> innerMap) throws Exception {
        // create chained transformer that would set POWNED_SYS_PROP system property to true (using InvokerTransformer and reflection)
        final Transformer[] transformers = new Transformer[]{
                new ConstantTransformer(System.class),
                new InvokerTransformer(
                        "getMethod",
                        new Class[]{
                                String.class,
                                Class[].class
                        },
                        new Object[]{
                                "setProperty",
                                new Class[]{
                                        String.class,
                                        String.class
                                }
                        }
                ),
                new InvokerTransformer(
                        "invoke",
                        new Class[]{
                                Object.class,
                                Object[].class
                        },
                        new Object[]{
                                null,
                                new Object[] {
                                        POWNED_SYS_PROP,
                                        "true"
                                }
                        }
                )
        };
        final Transformer transformerChain = new ChainedTransformer(transformers);

        // create the map for InvocationHandler
        final Map<String, String> map = new HashMap<>();
        map.put("value", "value");
        final Map outerMap = TransformedMap.decorate(map, null, transformerChain);

        // create our proxy map holding our innerMap and our InvocationHandler instance
        //noinspection unchecked
        return new MyProxyMap(innerMap, outerMap);
    }

    /**
     * This is the original NcipherKeyStoreData#createFromBytes() method (without the ClassFilter).<br/>
     * We'll use this method to test whether our payload works correctly (i.e. if our system property is set accordingly)
     */
    private static NcipherKeyStoreData createFromBytes(byte[] bytes) throws IOException {
        if (bytes == null) throw new NullPointerException("nCipher keystore bytes is null");
        if (bytes.length < 1) throw new IOException("nCipher keystore bytes is empty");
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            Object obj = ois.readObject();
            if (obj == null)
                throw new IOException("Ncipher keystore data deserialized to null");
            if (!(obj instanceof NcipherKeyStoreData))
                throw new IOException("Ncipher keystore data deserialized to unexpected type: " + obj.getClass());
            return (NcipherKeyStoreData) obj;

        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    private byte[] genRndBytes(final Random rand, final int min, final int max) {
        Assert.assertNotNull(rand);
        final byte[] bytes = new byte[genRndNumber(rand, min, max)];
        rand.nextBytes(bytes);
        return bytes;
    }

    private int genRndNumber(final Random rand, final int min, final int max) {
        Assert.assertNotNull(rand);
        Assert.assertThat(min, Matchers.lessThanOrEqualTo(max));
        if (min == max) return min;
        return min + rand.nextInt(max - min);
    }
}