package com.l7tech.policy.compatibility;

import com.l7tech.policy.assertion.ext.CustomAssertion;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;

/**
 * Helper class providing means to serialize and de-serialize test custom assertion, containing specified api module class as field.<br/>
 * Create separate test for all future serializable classes in the api module, by extending this class.
 *
 * @param <T>    targeted api module class to test.
 */
public class BaseBackwardsCompatibility<T> {

    /**
     * Sample custom assertion with targeted api module class as a field.
     * @param <T>    targeted api module class.
     */
    protected static class TestAssertion<T> implements CustomAssertion {
        private static final long serialVersionUID = -7790199956765731870L;
        private final T customData;
        public T getCustomData() { return customData; }
        public String getName() {
            return customData.toString() + getClass().getName();
        }
        private TestAssertion(final T customData) {
            this.customData = customData;
        }
    }

    /**
     * Create a instance of test custom assertion ({@link TestAssertion}) with the specified field.
     */
    protected TestAssertion<T> createAssertion(final T customData) {
        return new TestAssertion<T>(customData);
    }

    /**
     * De-serialize the test custom assertion ({@link TestAssertion}) contained in the specified serialization base64 encoded serialisation stream.
     *
     * @param base64Stream    base64 encoded serialisation stream, containing test custom assertion.
     * @return the instance of the test custom assertion ({@link TestAssertion}) contained within the serialization stream.  Never <code>null</code>.
     * @throws IOException if an error occurs while reading the stream.
     * @throws ClassNotFoundException if the test custom assertion class cannot be found in the specified stream.
     */
    protected TestAssertion<T> base64ToObject(final String base64Stream) throws IOException, ClassNotFoundException {
        final BASE64Decoder decoder = new BASE64Decoder();
        final byte[] decodedBytes = decoder.decodeBuffer(base64Stream);

        final ByteArrayInputStream bis = new ByteArrayInputStream(decodedBytes);
        final ObjectInputStream ois = new ObjectInputStream(bis);
        try {
            //noinspection unchecked
            return (TestAssertion<T>)ois.readObject();
        } finally {
            ois.close();
            bis.close();
        }
    }

    /**
     * Create base64 encoded serialisation stream off of specified test custom assertion ({@link TestAssertion}).
     *
     * @param obj    input test custom assertion ({@link TestAssertion}) object
     * @return a <code>String</code> containing the base64 encoded serialisation stream.
     * @throws IOException if an error occurs while writing the stream.
     */
    protected String objectToBase64(final TestAssertion<T> obj) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        try {
            oos.writeObject(obj);
            final BASE64Encoder encoder = new BASE64Encoder();
            return encoder.encode(bos.toByteArray()).replace("\n", "").replace("\r", "");
        } finally {
            oos.close();
            bos.close();
        }
    }
}
