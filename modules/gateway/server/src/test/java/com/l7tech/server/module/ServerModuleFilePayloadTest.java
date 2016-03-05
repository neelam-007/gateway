package com.l7tech.server.module;

import com.google.common.io.CountingInputStream;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.io.TeeInputStream;
import com.l7tech.gateway.common.module.CustomAssertionsScannerHelper;
import com.l7tech.gateway.common.module.ModularAssertionsScannerHelper;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFilePayload;
import com.l7tech.util.*;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ServerModuleFilePayloadTest {

    private static final String SIGNATURE_PROPS = "DON'T CARE";

    private static class UnsignedServerModuleFilePayload extends ServerModuleFilePayload {
        private final byte[] bytes;
        private final byte[] hash;

        public UnsignedServerModuleFilePayload(final ModuleInfo moduleInfo) {
            super(
                    Mockito.mock(PoolByteArrayOutputStream.class), new byte[0], Mockito.mock(PoolByteArrayOutputStream.class),
                    customAssertionsScannerHelper, modularAssertionsScannerHelper,
                    moduleInfo.fileName
            );

            Assert.assertNotNull(moduleInfo);
            Assert.assertNotNull(moduleInfo.fileOrBytes);
            Assert.assertTrue(moduleInfo.fileOrBytes.isLeft() || moduleInfo.fileOrBytes.isRight());
            Assert.assertNotNull(moduleInfo.assertions);
            Assert.assertThat(moduleInfo.fileLength, Matchers.greaterThanOrEqualTo(0L));
            Assert.assertThat(moduleInfo.fileName, Matchers.not(Matchers.isEmptyOrNullString()));
            Assert.assertNotNull(moduleInfo.hash);
            Assert.assertNotNull(moduleInfo.moduleType);

            // file sanity check
            if (moduleInfo.fileOrBytes.isLeft()) {
                Assert.assertTrue(moduleInfo.fileOrBytes.left().exists());
                Assert.assertTrue(moduleInfo.fileOrBytes.left().isFile());
                Assert.assertFalse(moduleInfo.fileOrBytes.left().isDirectory());
            } else {
                Assert.assertNotNull(moduleInfo.fileOrBytes.right());
            }

            if (moduleInfo.fileOrBytes.isLeft()) {
                try (
                        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        final InputStream is = new BufferedInputStream(new FileInputStream(moduleInfo.fileOrBytes.left()));
                        final TeeInputStream tis = new TeeInputStream(is, bos)
                ) {
                    IOUtils.copyStream(tis, new NullOutputStream());
                    bos.flush();
                    this.bytes = bos.toByteArray();
                } catch (IOException e) {
                    Assert.fail("Failed to read file: " + moduleInfo.fileName + ":" + System.lineSeparator() + e.getMessage());
                    throw new RuntimeException(e);
                }
            } else {
                this.bytes = moduleInfo.fileOrBytes.right();
            }

            this.hash = moduleInfo.hash;
        }

        @NotNull
        @Override
        public InputStream getDataStream() {
            return new ByteArrayInputStream(bytes);
        }

        @NotNull
        @Override
        public byte[] getDataBytes() {
            return bytes;
        }

        @Override
        public int getDataSize() {
            return bytes.length;
        }

        @NotNull
        @Override
        public byte[] getDataDigest() {
            return hash;
        }

        @NotNull
        @Override
        public String getSignaturePropertiesString() throws IOException {
            return SIGNATURE_PROPS;
        }
    }

    private static enum ModuleType{
        Custom_Dynamic,
        Custom_NonDynamic,
        Custom_FailOnLoad,
        Custom_FailOnUnload,
        Custom_BrokenDescriptor,
        Custom_MissingAssertions,
        Custom_MissingPropertiesFile,
        Custom_ArbitraryBytes,
        Custom_ArbitraryZeroBytes,
        Modular_Working,
        Modular_NoAssertions,
        Modular_MissingAssClass,
        Modular_InvalidAssClass,
        Modular_InvalidManifest,
        Modular_ArbitraryBytes,
        Modular_ArbitraryZeroBytes,
    }

    private static class ModuleInfo {
        // AssertionInfo builder
        private static class Builder {
            private final Either<File, byte[]> fileOrBytes;
            private final byte[] hash;
            private final String fileName;
            private final long fileLength;
            private final Collection<String> assertions = new HashSet<>();
            private com.l7tech.gateway.common.module.ModuleType moduleType;

            private Builder(final Either<File, Pair<byte[], String>> fileOrBytes, final byte[] hash, final Long length) {
                Assert.assertNotNull(fileOrBytes);
                Assert.assertNotNull(fileOrBytes.isLeft() ? fileOrBytes.left() : fileOrBytes.right());
                Assert.assertThat(fileOrBytes.isLeft() ? fileOrBytes.left().getName() : fileOrBytes.right().right, Matchers.not(Matchers.isEmptyOrNullString()));
                Assert.assertNotNull(hash);
                this.fileOrBytes = fileOrBytes.isLeft() ? Either.<File, byte[]>left(fileOrBytes.left()) : Either.<File, byte[]>right(fileOrBytes.right().left);
                this.hash = hash;
                this.fileName = fileOrBytes.isLeft() ? fileOrBytes.left().getName() : fileOrBytes.right().right;

                if (fileOrBytes.isLeft()) {
                    final long fileLength = fileOrBytes.left().length();
                    Assert.assertNotNull(length);
                    // test if length is between 5% of fileLength (see File#length() for more info)
                    Assert.assertThat(length, Matchers.is(Matchers.both(Matchers.greaterThan(fileLength - percentage(fileLength, 5))).and(Matchers.lessThan(fileLength + percentage(fileLength, 5)))));
                }
                this.fileLength = length;
            }

            private static long percentage(final long value, final int percentage) {
                return (long)(value*(percentage/100.0f));
            }

            Builder addAssertion(final String assertion) {
                Assert.assertThat(assertion, Matchers.not(Matchers.isEmptyOrNullString()));
                this.assertions.add(assertion);
                return this;
            }
            Builder moduleType(final com.l7tech.gateway.common.module.ModuleType moduleType) {
                this.moduleType = moduleType;
                return this;
            }

            ModuleInfo build() {
                return new ModuleInfo(fileOrBytes, fileName, fileLength, hash, moduleType, ServerModuleFilePayload.assertionsCollectionToCommaSeparatedString(assertions));
            }
        }

        private ModuleInfo(
                final Either<File, byte[]> fileOrBytes,
                final String fileName,
                final long fileLength,
                final byte[] hash,
                final com.l7tech.gateway.common.module.ModuleType moduleType,
                final String assertions
        ) {
            Assert.assertNotNull(fileOrBytes);
            Assert.assertThat(fileName, Matchers.not(Matchers.isEmptyOrNullString()));
            Assert.assertThat(fileLength, Matchers.greaterThanOrEqualTo(0L));
            Assert.assertNotNull(hash);
            Assert.assertNotNull(moduleType);
            Assert.assertNotNull(assertions);
            this.fileOrBytes = fileOrBytes;
            this.fileName = fileName;
            this.fileLength = fileLength;
            this.hash = hash;
            this.moduleType = moduleType;
            this.assertions = assertions;
        }

        final Either<File, byte[]> fileOrBytes;
        final String fileName;
        final long fileLength;
        final byte[] hash;
        final com.l7tech.gateway.common.module.ModuleType moduleType;
        final String assertions;

        public static Builder builder(final Triple<Either<File, Pair<byte[], String>>, byte[], Long> fileHashLength) {
            Assert.assertNotNull(fileHashLength);
            return new Builder(fileHashLength.left, fileHashLength.middle, fileHashLength.right);
        }
    }

    private final static Map<ModuleType, ModuleInfo> SAMPLE_MODULES = Collections.unmodifiableMap(
            CollectionUtils.<ModuleType, ModuleInfo>mapBuilder()
                    .put(
                            ModuleType.Custom_Dynamic,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/custom/dynamic/com.l7tech.DynamicCustomAssertionsTest1.jar"))
                                    .addAssertion("DynamicCustomAssertionsTest1")
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.CUSTOM_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Custom_NonDynamic,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/custom/non_dynamic/com.l7tech.NonDynamicCustomAssertionTest1.jar"))
                                    .addAssertion("NonDynamicCustomAssertionTest1")
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.CUSTOM_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Custom_FailOnLoad,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/custom/failed/onLoad/com.l7tech.DynamicOnLoadFailCustomAssertionsTest1.jar"))
                                    .addAssertion("DynamicOnLoadFailCustomAssertionsTest1")
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.CUSTOM_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Custom_FailOnUnload,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/custom/failed/onUnload/com.l7tech.DynamicOnUnloadFailCustomAssertionsTest1.jar"))
                                    .addAssertion("DynamicOnUnloadFailCustomAssertionsTest1")
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.CUSTOM_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Custom_BrokenDescriptor,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/custom/failed/descriptor/com.l7tech.BrokenDescriptorTest1.jar"))
                                    .addAssertion("BrokenDescriptorTest1Missing")
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.CUSTOM_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Custom_MissingAssertions,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/custom/failed/missing/com.l7tech.MissingAssertions1.jar"))
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.CUSTOM_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Custom_MissingPropertiesFile,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/custom/failed/missing/com.l7tech.MissingPropertiesFile1.jar"))
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.CUSTOM_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Custom_ArbitraryBytes,
                            ModuleInfo.builder(loadResource("test custom bytes".getBytes(Charsets.UTF8), "custom_arbitrary_file.jar"))
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.CUSTOM_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Custom_ArbitraryZeroBytes,
                            ModuleInfo.builder(loadResource(new byte[0], "custom_zero_bytes.jar"))
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.CUSTOM_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Modular_Working,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/modular/com.l7tech.WorkingTest1.aar"))
                                    .addAssertion("ModularTest1Assertion")
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.MODULAR_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Modular_NoAssertions,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/modular/com.l7tech.NoAssertionsTest1.aar"))
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.MODULAR_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Modular_MissingAssClass,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/modular/com.l7tech.MissingAssertionClassTest1.aar"))
                                    .addAssertion("AssertionClassIsMissing")
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.MODULAR_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Modular_InvalidAssClass,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/modular/com.l7tech.InvalidAssertionClassTest1.aar"))
                                    .addAssertion("${moduleAssertionList}")
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.MODULAR_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Modular_InvalidManifest,
                            ModuleInfo.builder(loadResource("com/l7tech/server/policy/module/modular/com.l7tech.InvalidManifestTest1.aar"))
                                    .addAssertion("BrokenDescriptorTest1Missing")
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.MODULAR_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Modular_ArbitraryBytes,
                            ModuleInfo.builder(loadResource("test custom bytes".getBytes(Charsets.UTF8), "modular_arbitrary_file.jar"))
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.MODULAR_ASSERTION)
                                    .build()
                    )
                    .put(
                            ModuleType.Modular_ArbitraryZeroBytes,
                            ModuleInfo.builder(loadResource(new byte[0], "modular_zero_bytes.jar"))
                                    .moduleType(com.l7tech.gateway.common.module.ModuleType.MODULAR_ASSERTION)
                                    .build()
                    )
                    .map()
    );

    private static CustomAssertionsScannerHelper customAssertionsScannerHelper = new CustomAssertionsScannerHelper("custom_assertions.properties");
    private static ModularAssertionsScannerHelper modularAssertionsScannerHelper = new ModularAssertionsScannerHelper("ModularAssertion-List");

    private static Triple<Either<File, Pair<byte[], String>>, byte[], Long> loadResource(final String resURL) {
        try {
            Assert.assertThat(resURL, Matchers.not(Matchers.isEmptyOrNullString()));
            final URL fileUrl = ServerModuleFilePayloadTest.class.getClassLoader().getResource(resURL);
            Assert.assertNotNull(fileUrl);
            final File file = new File(fileUrl.toURI());
            Assert.assertNotNull(file);
            Assert.assertTrue(file.exists());
            Assert.assertTrue(file.isFile());
            Assert.assertFalse(file.isDirectory());

            try (
                    final CountingInputStream cis = new CountingInputStream(new BufferedInputStream(new FileInputStream(file)));
                    final DigestInputStream dis = new DigestInputStream(cis, MessageDigest.getInstance("SHA-256"))
            ) {
                IOUtils.copyStream(dis, new NullOutputStream());
                return Triple.triple(Either.<File, Pair<byte[], String>>left(file), dis.getMessageDigest().digest(), cis.getCount());
            }
        } catch (final Exception e) {
            Assert.fail("Failed to load resource: " + resURL + ":" + System.lineSeparator() + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static Triple<Either<File, Pair<byte[], String>>, byte[], Long> loadResource(final byte[] bytes, final String fileName) {
        try (final DigestInputStream dis = new DigestInputStream(new ByteArrayInputStream(bytes), MessageDigest.getInstance("SHA-256"))) {
            IOUtils.copyStream(dis, new NullOutputStream());
            return Triple.triple(Either.<File, Pair<byte[], String>>right(Pair.pair(bytes, fileName)), dis.getMessageDigest().digest(), bytes.length*1L);
        } catch (final Exception e) {
            Assert.fail("Failed to load resource: " + fileName + ":" + System.lineSeparator() + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreate() throws Exception {
        doTestCreate(ModuleType.Custom_Dynamic);
        doTestCreate(ModuleType.Custom_NonDynamic);
        doTestCreate(ModuleType.Custom_FailOnLoad);
        doTestCreate(ModuleType.Custom_FailOnUnload);
        doTestCreate(ModuleType.Custom_BrokenDescriptor);
        doTestCreate(ModuleType.Custom_MissingAssertions);
        try {
            doTestCreate(ModuleType.Custom_MissingPropertiesFile);
            Assert.fail("Custom_MissingPropertiesFile should have failed with IOException: failed to determine module type");
        } catch (IOException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to determine module type"));
        }
        try {
            doTestCreate(ModuleType.Custom_ArbitraryBytes);
            Assert.fail("Custom_ArbitraryBytes should have failed with IOException: failed to determine module type");
        } catch (IOException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to determine module type"));
        }
        try {
            doTestCreate(ModuleType.Custom_ArbitraryZeroBytes);
            Assert.fail("Custom_ArbitraryZeroBytes should have failed with IOException: failed to determine module type");
        } catch (IOException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to determine module type"));
        }


        doTestCreate(ModuleType.Modular_Working);
        try {
            doTestCreate(ModuleType.Modular_NoAssertions);
            Assert.fail("Modular_NoAssertions should have failed with IOException: failed to determine module type");
        } catch (IOException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to determine module type"));
        }
        doTestCreate(ModuleType.Modular_MissingAssClass);
        doTestCreate(ModuleType.Modular_InvalidAssClass);
        try {
            doTestCreate(ModuleType.Modular_InvalidManifest);
            Assert.fail("Modular_InvalidManifest should have failed with IOException: failed to determine module type");
        } catch (IOException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to determine module type"));
        }
        try {
            doTestCreate(ModuleType.Modular_ArbitraryBytes);
            Assert.fail("Modular_ArbitraryBytes should have failed with IOException: failed to determine module type");
        } catch (IOException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to determine module type"));
        }
        try {
            doTestCreate(ModuleType.Modular_ArbitraryZeroBytes);
            Assert.fail("Modular_ArbitraryZeroBytes should have failed with IOException: failed to determine module type");
        } catch (IOException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString("Failed to determine module type"));
        }
    }

    private void doTestCreate(final ModuleType moduleType) throws Exception {
        Assert.assertNotNull(moduleType);

        final ModuleInfo moduleInfo = SAMPLE_MODULES.get(moduleType);
        Assert.assertNotNull(moduleInfo);

        try (final ServerModuleFilePayload payload = new UnsignedServerModuleFilePayload(moduleInfo)) {
            final ServerModuleFile smf = payload.create();
            Assert.assertNotNull(smf);
            Assert.assertThat(smf.getModuleType(), Matchers.equalTo(moduleInfo.moduleType));
            Assert.assertThat(smf.getModuleSha256(), Matchers.equalTo(HexUtils.hexDump(moduleInfo.hash)));
            Assert.assertThat(smf.getProperty(ServerModuleFile.PROP_FILE_NAME), Matchers.equalTo(moduleInfo.fileName));
            Assert.assertThat(smf.getProperty(ServerModuleFile.PROP_SIZE), Matchers.equalTo(String.valueOf(moduleInfo.fileLength)));
            Assert.assertThat(smf.getProperty(ServerModuleFile.PROP_ASSERTIONS), Matchers.equalTo(moduleInfo.assertions));

            Assert.assertNotNull(smf.getData());
            Assert.assertThat(smf.getData().getSignatureProperties(), Matchers.equalTo(SIGNATURE_PROPS));
            Assert.assertNotNull(smf.getData().getDataBytes());
            Assert.assertThat(smf.getData().getDataBytes().length*1L, Matchers.equalTo(moduleInfo.fileLength));
        }
    }
}