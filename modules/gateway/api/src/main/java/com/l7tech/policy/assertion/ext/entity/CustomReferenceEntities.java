package com.l7tech.policy.assertion.ext.entity;

/**
 * To properly resolve all external entities during migration to an external Gateway, the Custom Assertion class
 * (and/or any other class depending on external entities) must implement this interface.<br/>
 * Currently external entity could be a secure-password or key-value-store, however additional entities could be
 * introduced in the future. For a list of supported entities refer to {@link CustomEntityType} enumeration.
 * <p/>
 * Use {@link CustomReferenceEntitiesSupport} to identify referenced entities by their attribute name.
 * When an entity depends on another entity, then also implement {@link CustomEntitySerializer} interface for it's type.
 * Otherwise the Gateway will not be able to identify the dependent entity and will not be able to migrate it.
 * <p/>
 * When replacing existing reference to entities (i.e. local fields) with {@code CustomReferenceEntitiesSupport}
 * be mindful of backwards compatibility (i.e. previous versions of serializable entities).
 * A Custom Assertion Developer might need to implement {@code readObject(ObjectInputStream)} to provide backward compatibility.<br/>
 * For more information on serialization compatibility, see Oracle's guidelines
 * <a href="http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6519">link</a>
 * <p/>
 * Example code:
 * <blockquote><pre>
 * {@code
 * ....
 * private CustomReferenceEntitiesSupport entitiesSupport = new CustomReferenceEntitiesSupport();
 * // variable from assertion's previous version
 * private String connectionPasswordId;
 * ....
 * // modify connectionPasswordId getters and setters to use the new referenceEntitiesSupport
 * public String getConnectionPasswordId() {
 *    return entitiesSupport.getReference("connectionPasswordId");
 * }
 * public void setConnectionPasswordId(String connectionPasswordId) {
 *    entitiesSupport.setReference("connectionPasswordId", connectionPasswordId ... );
 * }
 * ...
 * private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
 *    // will read connectionPasswordId from the stream
 *    in.defaultReadObject();
 *    ....
 *    if (entitiesSupport == null) {
 *       entitiesSupport = new CustomReferenceEntitiesSupport();
 *       setConnectionPasswordId(connectionPasswordId);
 *    }
 * }
 * }
 * </pre></blockquote>
 *
 * @see CustomEntityType
 * @see CustomReferenceEntitiesSupport
 */
public interface CustomReferenceEntities {
    /**
     * Access referenced entities support class.
     * <p/>
     * <i>Important</i>: Declare {@link CustomReferenceEntitiesSupport} as a local singleton field, so that the filed will be
     * serialized along with the class.<br/>
     * Never create new {@link CustomReferenceEntitiesSupport} instances in the method implementation.
     */
    CustomReferenceEntitiesSupport getReferenceEntitiesSupport();
}
