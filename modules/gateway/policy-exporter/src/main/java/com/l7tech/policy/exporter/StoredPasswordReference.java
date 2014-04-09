package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.entity.EntitiesResolver;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The secure password reference element.
 *
 * @author Victor Kazakov
 */
public class StoredPasswordReference extends ExternalReference  {
    private static final Logger logger = Logger.getLogger(StoredPasswordReference.class.getName());

    private Goid id;
    private String name;
    private SecurePassword.SecurePasswordType type;
    private String description;
    private LocalizeAction localizeType;
    private Goid localSecurePasswordId;

    private static final String ELMT_NAME_REF = "StoredPasswordReference";
    private static final String ELMT_NAME_PASS_NAME = "Name";
    private static final String ELMT_NAME_TYPE = "Type";
    private static final String ELMT_NAME_PASS_ID = "Id";
    private static final String ELMT_NAME_PASS_DESC = "Description";

    private StoredPasswordReference( final ExternalReferenceFinder finder ) {
        super( finder );
    }

    public StoredPasswordReference( final ExternalReferenceFinder finder, SecurePasswordEntityHeader securePasswordEntityHeader ) {
        super( finder );
        final SecurePassword password;
        try {
            password = finder.findSecurePasswordById(securePasswordEntityHeader.getGoid());
            init(password);
        } catch (FindException e){
            logger.log(Level.SEVERE, "error retrieving secure password information. ", e);
        }
    }

    private void init( SecurePassword securePassword ) {
        if (securePassword != null) {
            id = securePassword.getGoid();
            name = securePassword.getName();
            type = securePassword.getType();
            description = securePassword.getDescription();
        }
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    public static StoredPasswordReference parseFromElement( final ExternalReferenceFinder context, final Element elmt) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!elmt.getNodeName().equals(ELMT_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + ELMT_NAME_REF);
        }

        StoredPasswordReference output = new StoredPasswordReference( context );
        output.id = Goid.parseGoid(getParamFromEl(elmt, ELMT_NAME_PASS_ID));
        output.name = getParamFromEl(elmt, ELMT_NAME_PASS_NAME);
        output.type = SecurePassword.SecurePasswordType.valueOf(getParamFromEl(elmt, ELMT_NAME_TYPE));
        output.description = getParamFromEl(elmt, ELMT_NAME_PASS_DESC);

        return output;
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        final Document doc = referencesParentElement.getOwnerDocument();
        Element referenceElement = doc.createElement(ELMT_NAME_REF);
        setTypeAttribute(referenceElement);
        referencesParentElement.appendChild(referenceElement);

        addParameterElement( ELMT_NAME_PASS_ID, id.toString(), referenceElement );
        addParameterElement( ELMT_NAME_PASS_NAME, name, referenceElement );
        addParameterElement( ELMT_NAME_TYPE, type.name(), referenceElement );
        addParameterElement( ELMT_NAME_PASS_DESC, description, referenceElement );
    }

    private void addParameterElement( final String name, final String value, final Element parent ) {
        if ( value != null ) {
            final Element parameterElement = parent.getOwnerDocument().createElement(name);
            final Text txt = DomUtils.createTextNode(parent, value);
            parameterElement.appendChild(txt);
            parent.appendChild(parameterElement);
        }
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        try {
            SecurePassword password = getFinder().findSecurePasswordById(id);
            return
                    password != null &&
                            password.getName().equalsIgnoreCase(name) &&
                            (password.getType().equals(type));
        } catch (FindException e) {
            logger.warning("Cannot find a Stored Password, " + name);
            return false;
        }
    }

    /**
     * Ensure only one password entity is exported
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StoredPasswordReference)) return false;

        final StoredPasswordReference ref = (StoredPasswordReference)obj;
        final Goid goid = getId();
        final String name = getName();

        if (goid != null) {
            return (goid.equals(ref.getId()));
        } else if (name != null) {
            return ref.getId() == null && name.equals(ref.getName());
        } else {
            return ref.getId() == null && ref.getName() == null;
        }
    }

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    protected boolean localizeAssertion(@Nullable Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE){
            final EntitiesResolver entitiesResolver =
                    EntitiesResolver.builder()
                            .keyValueStore(getFinder().getCustomKeyValueStore())
                            .build();
            for(EntityHeader entityHeader : entitiesResolver.getEntitiesUsed(assertionToLocalize)) {
                if ( entityHeader.getType().equals(EntityType.SECURE_PASSWORD) && entityHeader.equalsId(id) ) {
                    if(localizeType == LocalizeAction.REPLACE) {
                        if ( !localSecurePasswordId.equals(id)) {
                            EntityHeader newEntityHeader = new EntityHeader(localSecurePasswordId, EntityType.SECURE_PASSWORD, null, null);
                            entitiesResolver.replaceEntity(assertionToLocalize, entityHeader, newEntityHeader);
                            logger.info("The server stored password goid of the imported assertion has been changed " +
                                    "from " + id + " to " + localSecurePasswordId);
                            break;
                        }
                    } else if(localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public String getRefId() {
        String idString = null;

        if ( !Goid.isDefault(id)) {
            idString = id.toString();
        }

        return idString;
    }

    @Override
    public boolean setLocalizeReplace(Goid identifier) {
        localizeType = LocalizeAction.REPLACE;
        localSecurePasswordId = identifier;
        return true;
    }

    public Goid getId(){
        return id;
    }

    public String getName() {
        return name;
    }

    public SecurePassword.SecurePasswordType getType() {
        return type;
    }

    public void setId(Goid id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(SecurePassword.SecurePasswordType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
