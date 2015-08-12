package com.l7tech.server.module;

import com.l7tech.gateway.common.module.ModuleDigest;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.policy.module.ModulesScannerTestBase;
import com.l7tech.server.security.signer.SignatureTestUtils;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.util.Either;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;

import java.io.*;
import java.util.*;

/**
 * Base class for {@code ServerModuleFile} unit tests.<br/>
 * Provides common ways of creating {@code ServerModuleFile}'s.
 *
 */
@SuppressWarnings("UnusedDeclaration")
@Ignore
public abstract class ServerModuleFileTestBase extends ModulesScannerTestBase {

    // used to generate random GOID
    protected static final Random rnd = new Random();
    protected static final int typeLength = ModuleType.values().length;
    protected static final long GOID_HI_START = Long.MAX_VALUE - 1;

    // our signer utils object
    protected static SignatureVerifier SIGNATURE_VERIFIER;
    protected static final String[] SIGNER_CERT_DNS = {
            "cn=signer.team1.apim.ca.com",
            "cn=signer.team2.apim.ca.com",
            "cn=signer.team3.apim.ca.com",
            "cn=signer.team4.apim.ca.com"
    };

    protected static void beforeClass() throws Exception {
        SignatureTestUtils.beforeClass();
        SIGNATURE_VERIFIER = SignatureTestUtils.createSignatureVerifier(SIGNER_CERT_DNS);
        Assert.assertThat("modules signer is created", SIGNATURE_VERIFIER, Matchers.notNullValue());
    }

    protected static void afterClass() throws Exception {
        SignatureTestUtils.afterClass();
    }

    /**
     * Utility method for signing the specified content {@code File} and getting the signature in one step.
     */
    protected static String signAndGetSignature(final File content, final String signerCertDn) throws Exception {
        Assert.assertThat(content, Matchers.notNullValue());
        Assert.assertThat(signerCertDn, Matchers.not(Matchers.isEmptyOrNullString()));
        return SignatureTestUtils.signAndGetSignature(SIGNATURE_VERIFIER, content, signerCertDn);
    }

    /**
     * Utility method for signing the specified content {@code InputStream} and getting the signature in one step.
     */
    protected static String signAndGetSignature(final InputStream content, final String signerCertDn) throws Exception {
        Assert.assertThat(content, Matchers.notNullValue());
        Assert.assertThat(signerCertDn, Matchers.not(Matchers.isEmptyOrNullString()));
        return SignatureTestUtils.signAndGetSignature(SIGNATURE_VERIFIER, content, signerCertDn);
    }

    /**
     * Utility method for signing the specified content byte array and getting the signature in one step.
     */
    protected static String signAndGetSignature(final byte[] content, final String signerCertDn) throws Exception {
        Assert.assertThat(content, Matchers.notNullValue());
        Assert.assertThat(signerCertDn, Matchers.not(Matchers.isEmptyOrNullString()));
        return SignatureTestUtils.signAndGetSignature(SIGNATURE_VERIFIER, content, signerCertDn);
    }

    /**
     * Utility class for storing the module bytes as a File.
     */
    @SuppressWarnings("serial")
    protected class MyServerModuleFile extends ServerModuleFile {
        private Either<File, byte[]> content;
        private String signature;
        public void setModuleContent(final Either<File, byte[]> content, final String signature) {
            Assert.assertNotNull(content);
            Assert.assertTrue(content.isLeft() || content.isRight());
            this.content = content;
            this.signature = signature;
        }
        public Pair<InputStream, String> getModuleContentStreamWithSignature() throws IOException {
            if (content != null) {
                Assert.assertTrue(content.isLeft() || content.isRight());
                if (content.isLeft()) {
                    return Pair.<InputStream, String>pair(new BufferedInputStream(new FileInputStream(content.left())), signature);
                } else {
                    return Pair.<InputStream, String>pair(new ByteArrayInputStream(content.right()), signature);
                }
            }
            return null;
        }
    }

    /**
     * {@link MyServerModuleFile} builder class.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected class ServerModuleFileBuilder {
        private Goid goid;
        private String name;
        private Integer version;
        private ModuleType moduleType;
        private Either<File, byte[]> moduleContent;
        private String checkSum;
        private String signature;

        /**
         * Pre-attached {@link MyServerModuleFile}.
         * The builder will append new properties or override existing ones.
         */
        private MyServerModuleFile moduleFile;

        /**
         * Default constructor
         */
        public ServerModuleFileBuilder() {
            this(new MyServerModuleFile());
        }

        /**
         * Initialize the builder with preexisting module file.
         * This way the builder will append new properties or override existing ones.
         *
         * @param moduleFile    the {@link MyServerModuleFile} to attach to.
         */
        public ServerModuleFileBuilder(final MyServerModuleFile moduleFile) {
            this.moduleFile = moduleFile;
        }

        private final Collection<Triple<String, ModuleState, String>> states = new ArrayList<>();
        private final Map<String, String> properties = new HashMap<>();

        public ServerModuleFileBuilder goid(final Goid goid) {
            this.goid = goid;
            return this;
        }

        public ServerModuleFileBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public ServerModuleFileBuilder version(final Integer version) {
            this.version = version;
            return this;
        }

        public ServerModuleFileBuilder moduleType(final ModuleType moduleType) {
            this.moduleType = moduleType;
            return this;
        }

        public ServerModuleFileBuilder content(final File file) {
            if (file != null) {
                this.moduleContent = Either.left(file);
            }
            return this;
        }

        public ServerModuleFileBuilder content(final byte[] bytes) {
            if (bytes != null) {
                this.moduleContent = Either.right(bytes);
            }
            return this;
        }

        public ServerModuleFileBuilder signature(final String signature) {
            this.signature = signature;
            return this;
        }

        public ServerModuleFileBuilder checkSum(final String checkSum) {
            this.checkSum = checkSum;
            return this;
        }

        public ServerModuleFileBuilder addState(final String node, final ModuleState state) {
            if (state == null) // do not add if null
                return this;
            return addState(node, state, null);
        }

        public ServerModuleFileBuilder addStateError(final String node, final String error) {
            if (error == null) // do not add if null
                return this;
            return addState(node, null, error);

        }

        private ServerModuleFileBuilder addState(final String node, final ModuleState state, final String error) {
            this.states.add(Triple.triple(node, state, error));
            return this;
        }

        public ServerModuleFileBuilder addProperty(final String name, final String value) {
            this.properties.put(name, value);
            return this;
        }

        public ServerModuleFileBuilder clearProperties() {
            this.properties.clear();
            return this;
        }

        public MyServerModuleFile build() {
            Assert.assertNotNull(moduleFile);
            if (goid != null) {
                moduleFile.setGoid(goid);
            }
            if (name != null) {
                moduleFile.setName(name);
            }
            if (version != null) {
                moduleFile.setVersion(version);
            }
            if (moduleType != null) {
                moduleFile.setModuleType(moduleType);
            }
            if (moduleContent != null) {
                try {
                    moduleFile.setModuleContent(moduleContent, signature);
                    moduleFile.setModuleSha256(ModuleDigest.hexEncodedDigest(moduleFile.getModuleContentStreamWithSignature().left));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (checkSum != null) {
                moduleFile.setModuleSha256(checkSum); // override check-sum
            }
            for (final Map.Entry<String, String> property : properties.entrySet()) {
                if (MyServerModuleFile.PROP_SIZE.equals(property.getKey())) {
                    moduleFile.setProperty(MyServerModuleFile.PROP_SIZE, property.getValue());
                } else if (MyServerModuleFile.PROP_ASSERTIONS.equals(property.getKey())) {
                    moduleFile.setProperty(MyServerModuleFile.PROP_ASSERTIONS, property.getValue());
                } else if (MyServerModuleFile.PROP_FILE_NAME.equals(property.getKey())) {
                    moduleFile.setProperty(MyServerModuleFile.PROP_FILE_NAME, property.getValue());
                } else {
                    Assert.fail("Unsupported property: " + property.getKey());
                }
            }
            for (final Triple<String, ModuleState, String> state : states) {
                if (StringUtils.isNotBlank(state.left)) {
                    if (state.middle != null) {
                        moduleFile.setStateForNode(state.left, state.middle);
                    } else {
                        moduleFile.setStateErrorMessageForNode(state.left, state.right);
                    }
                }
            }

            return moduleFile;
        }
    }

    /**
     * Convenient method for creating a test sample of {@link MyServerModuleFile} without any states, having the following attributes:
     * <ul>
     *     <li>goid: {@code Goid(GOID_HI_START, ordinal)}</li>
     *     <li>name: {@code module_[ordinal]}</li>
     *     <li>version: {@code 0}</li>
     *     <li>specified {@code moduleType} or a random type if {@code null}</li>
     *     <li>bytes: {@code test data _[ordinal]}</li>
     *     <li>file-name: {@code module_[ordinal].[jar or aar, depending whether the type is modular or custom assertions]}</li>
     *     <li>size: {@code length of the bytes array}</li>
     *     <li>assertions: {@code assertion_[ordinal]}</li>
     * </ul>
     * @param ordinal       the ordinal of this test sample
     * @param moduleType    the module type either {@link ModuleType#MODULAR_ASSERTION} or {@link ModuleType#CUSTOM_ASSERTION}
     * @param moduleBytes   the module file containing the bytes, instead of loading the bytes in memory. Optional and can be {@code null}.
     */
    protected MyServerModuleFile create_test_module_without_states(final long ordinal, ModuleType moduleType, final File moduleBytes, final String signature) throws Exception {
        Assert.assertTrue(moduleBytes == null || moduleBytes.exists());
        moduleType = moduleType != null ? moduleType : ModuleType.values()[rnd.nextInt(typeLength)];
        return new ServerModuleFileBuilder()
                .goid(new Goid(GOID_HI_START, ordinal))
                .name("module_" + ordinal)
                .version(0)
                .moduleType(moduleType)
                .content(moduleBytes)
                .signature(signature)
                .addProperty(ServerModuleFile.PROP_FILE_NAME, (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? "CustomAssertion" : "ModularAssertion") + "FileName" + ordinal + (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? ".jar" : ".aar"))
                .addProperty(ServerModuleFile.PROP_ASSERTIONS, (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? "Custom" : "Modular") + "Assertion_" + ordinal)
                .addProperty(ServerModuleFile.PROP_SIZE, String.valueOf(moduleBytes != null ? moduleBytes.length() : 0))
                .build();
    }

    /**
     * @see #create_test_module_without_states(long, com.l7tech.gateway.common.module.ModuleType, java.io.File, java.lang.String)
     */
    protected MyServerModuleFile create_test_module_without_states(final long ordinal, ModuleType moduleType, final byte[] moduleBytes, final String signature) throws Exception {
        moduleType = moduleType != null ? moduleType : ModuleType.values()[rnd.nextInt(typeLength)];
        return new ServerModuleFileBuilder()
                .goid(new Goid(GOID_HI_START, ordinal))
                .name("module_" + ordinal)
                .version(0)
                .moduleType(moduleType)
                .content(moduleBytes)
                .signature(signature)
                .addProperty(ServerModuleFile.PROP_FILE_NAME, (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? "CustomAssertion" : "ModularAssertion") + "FileName" + ordinal + (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? ".jar" : ".aar"))
                .addProperty(ServerModuleFile.PROP_ASSERTIONS, (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? "Custom" : "Modular") + "Assertion_" + ordinal)
                .addProperty(ServerModuleFile.PROP_SIZE, String.valueOf(moduleBytes != null ? moduleBytes.length : 0))
                .build();
    }

    /**
     * Same as {@link #create_test_module_without_states(long, com.l7tech.gateway.common.module.ModuleType, java.io.File, String)}
     * but automatically sign the module content.
     */
    protected MyServerModuleFile create_and_sign_test_module_without_states(final long ordinal, ModuleType moduleType, final File moduleBytes, final String signatureDn) throws Exception {
        return create_test_module_without_states(ordinal, moduleType, moduleBytes, signAndGetSignature(moduleBytes, signatureDn));
    }

    /**
     * Same as {@link #create_test_module_without_states(long, com.l7tech.gateway.common.module.ModuleType, byte[], String)}
     * but automatically sign the module content.
     */
    protected MyServerModuleFile create_and_sign_test_module_without_states(final long ordinal, ModuleType moduleType, final byte[] moduleBytes, final String signatureDn) throws Exception {
        return create_test_module_without_states(ordinal, moduleType, moduleBytes, signAndGetSignature(moduleBytes, signatureDn));
    }

    /**
     * Creates unsigned {@code ServerModuleFile}.<br/>
     * Same as {@link #create_test_module_without_states(long, com.l7tech.gateway.common.module.ModuleType, java.io.File, String)}
     * but pass {@code null} for signature.
     */
    protected MyServerModuleFile create_unsigned_test_module_without_states(final long ordinal, ModuleType moduleType, final File moduleBytes) throws Exception {
        return create_test_module_without_states(ordinal, moduleType, moduleBytes, null);
    }

    /**
     * Creates unsigned {@code ServerModuleFile}.<br/>
     * Same as {@link #create_test_module_without_states(long, com.l7tech.gateway.common.module.ModuleType, byte[], String)}
     * but pass {@code null} for signature.
     */
    protected MyServerModuleFile create_unsigned_test_module_without_states(final long ordinal, ModuleType moduleType, final byte[] moduleBytes) throws Exception {
        return create_test_module_without_states(ordinal, moduleType, moduleBytes, null);
    }
}
