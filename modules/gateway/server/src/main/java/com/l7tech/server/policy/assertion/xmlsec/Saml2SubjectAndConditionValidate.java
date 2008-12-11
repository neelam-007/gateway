package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.security.saml.SamlConstants;
import com.l7tech.util.ArrayUtils;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import x0Assertion.oasisNamesTcSAML2.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validation for SAML 2.x Subject and Conditions.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
class Saml2SubjectAndConditionValidate {

    private static final Logger logger = Logger.getLogger(Saml2SubjectAndConditionValidate.class.getName());

    /**
     * Subject validation for 2.x
     */
    static void validateSubject(RequestWssSaml requestWssSaml, SubjectType subject, Calendar now, Collection validationResults) {
        if (subject == null) {
            validationResults.add(new SamlAssertionValidate.Error("Subject Required", null));
            return; // no point trying to continue validating a null subject
        }
        final String nameQualifier = requestWssSaml.getNameQualifier();
        NameIDType nameIdentifierType = subject.getNameID();
        if (nameQualifier != null && !"".equals(nameQualifier)) {
            if (nameIdentifierType != null) {
                String presentedNameQualifier = nameIdentifierType.getNameQualifier();
                if (!nameQualifier.equals(presentedNameQualifier)) {
                    SamlAssertionValidate.Error error = new SamlAssertionValidate.Error("Name Qualifiers does not match presented/required {0}/{1}", null, presentedNameQualifier, nameQualifier);
                    validationResults.add(error);
                    logger.finer(error.toString());
                    return;
                } else {
                    logger.fine("Matched Name Qualifier " + nameQualifier);
                }
            }
        }

        // name formats are the same for v1 and v2 so no mapping required
        boolean nameFormatMatch = false;
        String[] nameFormats = requestWssSaml.getNameFormats();
        String presentedNameFormat = null;
        if (nameIdentifierType != null) {
            presentedNameFormat = nameIdentifierType.getFormat();
            if (nameFormats != null) {
                for (String nameFormat : nameFormats) {
                    if (nameFormat.equals(presentedNameFormat)) {
                        nameFormatMatch = true;
                        logger.fine("Matched Name Format " + nameFormat);
                        break;
                    } else if (nameFormat.equals(SamlConstants.NAMEIDENTIFIER_UNSPECIFIED)) {
                        nameFormatMatch = true;
                        logger.fine("Matched Name Format " + nameFormat);
                        break;
                    }
                }
            }
        }
        if (presentedNameFormat == null) {
            presentedNameFormat = "";
        }
        if (!nameFormatMatch) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("Name Format does not match presented/required {0}/{1}", null, presentedNameFormat, Arrays.asList(nameFormats));
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }

        // map the config v1 names to v2 names
        String[] confirmations = ArrayUtils.copy(requestWssSaml.getSubjectConfirmations());
        for (int i = 0; i < confirmations.length; i++) {
            String confirmation = confirmations[i];
            String saml2Confirmation = (String) SamlConstants.CONF_MAP_SAML_1TO2.get(confirmation);
            if (saml2Confirmation != null) {
                confirmations[i] = saml2Confirmation;
            }
        }

        final SubjectConfirmationType[] subjectConfirmations = subject.getSubjectConfirmationArray();
        List presentedConfirmations = new ArrayList();
        if (subjectConfirmations != null) {
            for (SubjectConfirmationType subjectConfirmation : subjectConfirmations) {
                SubjectConfirmationDataType confirmationData = subjectConfirmation.getSubjectConfirmationData();
                boolean statementValid = true;
                if (confirmationData != null) {
                    Calendar notBefore = confirmationData.getNotBefore();
                    if (notBefore != null && now.before(notBefore)) {
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("Condition 'Not Before' check failed, now :" + now.toString() + " Not Before:" + notBefore.toString());
                        statementValid = false;
                    }

                    Calendar notOnOrAfter = confirmationData.getNotOnOrAfter();
                    if (notOnOrAfter != null && (now.equals(notOnOrAfter) || now.after(notOnOrAfter))) {
                        if (logger.isLoggable(Level.FINE))
                            logger.fine(String.format("Condition 'Not On Or After' check failed, now: %s Not Before: %s", now.toString(), notOnOrAfter.toString()));
                        statementValid = false;
                    }

                    if (notBefore != null && notOnOrAfter != null && !notBefore.before(notOnOrAfter)) {
                        logger.finer("Statement condition is invalid, 'Not On Or After' " + notOnOrAfter.toString() + " MUST be later than 'Not Before' " + notBefore.toString());
                        validationResults.add(new SamlAssertionValidate.Error("Statement condition is invalid, 'Not On Or After' MUST be later than 'Not Before': {0}/{1}", null, notOnOrAfter.getTime().toString(), notBefore.getTime().toString()));
                    }
                }

                if (statementValid)
                    presentedConfirmations.add(subjectConfirmation.getMethod());
            }
        }

        // if no confirmations have been presented, and that is what corresponds to assertion requirements
        // no confirmation check is performed
        if (presentedConfirmations.isEmpty() && requestWssSaml.isNoSubjectConfirmation()) {
            logger.fine("Matched Subject Confirmation 'None'");
            return;
        }

        boolean confirmationMatch = false;
        for (String confirmation : confirmations) {
            if (presentedConfirmations.contains(confirmation)) {
                confirmationMatch = true;
                logger.fine("Matched Subject Confirmation " + confirmation);
                break;
            }
        }

        if (!confirmationMatch) {
            List acceptedConfirmations = new ArrayList(Arrays.asList(confirmations));
            if (requestWssSaml.isNoSubjectConfirmation()) {
                acceptedConfirmations.add("None");
            }
            SamlAssertionValidate.Error error = new SamlAssertionValidate.Error("Subject Confirmations mismatch presented/accepted {0}/{1}", null, presentedConfirmations, acceptedConfirmations);
            validationResults.add(error);
            logger.finer(error.toString());
        }
    }

    /**
     * Validate the SAML v2 assertion conditions
     */
    static void validateConditions(RequestWssSaml requestWssSaml, ConditionsType conditionsType, Calendar now, Collection validationResults) {
        if (!requestWssSaml.isCheckAssertionValidity()) {
            logger.finer("No Assertion Validity requested");
        } else {
            if (conditionsType == null) {
                logger.finer("Can't validate conditions, no Conditions have been presented");
                validationResults.add(new SamlAssertionValidate.Error("Can't validate conditions, no Conditions have been presented", null));
            }
            else {
                Calendar notBefore = conditionsType.getNotBefore();
                Calendar notOnOrAfter = conditionsType.getNotOnOrAfter();
                if (notBefore == null || notOnOrAfter == null) {
                    logger.finer("No Validity Period conditions have been presented, cannot validate Conditions");
                    validationResults.add(new SamlAssertionValidate.Error("No Validity Period conditions have been presented, cannot validate Conditions", null));
                }
                else {
                    if (now.before(notBefore)) {
                        logger.finer("Condition 'Not Before' check failed, now :" + now.toString() + " Not Before:" + notBefore.toString());
                        validationResults.add(new SamlAssertionValidate.Error("SAML ticket does not become valid until: {0}", null, notBefore.getTime().toString()));
                    }

                    if (now.equals(notOnOrAfter) || now.after(notOnOrAfter)) {
                        logger.finer("Condition 'Not On Or After' check failed, now :" + now.toString() + " Not Before:" + notOnOrAfter.toString());
                        validationResults.add(new SamlAssertionValidate.Error("SAML ticket has expired as of: {0}", null, notOnOrAfter.getTime().toString()));
                    }
                }
            }
        }

        validateAudienceRestriction(requestWssSaml, conditionsType, validationResults);

        if (conditionsType != null) {
            if (conditionsType.getOneTimeUseArray() != null &&
                conditionsType.getOneTimeUseArray().length > 1) {
                logger.finer("Multiple OneTimeUse conditions are not permitted.");
                validationResults.add(new SamlAssertionValidate.Error("Multiple OneTimeUse conditions are not permitted.", null));
            }

            if (conditionsType.getProxyRestrictionArray() != null &&
                conditionsType.getProxyRestrictionArray().length > 1) {
                logger.finer("Multiple ProxyRestriction conditions are not permitted.");
                validationResults.add(new SamlAssertionValidate.Error("Multiple ProxyRestriction conditions are not permitted.", null));
            }
        }
    }

    private static void validateAudienceRestriction(RequestWssSaml requestWssSaml, ConditionsType conditionsType, Collection validationResults) {
        final String audienceRestriction = requestWssSaml.getAudienceRestriction();
        if (audienceRestriction == null || "".equals(audienceRestriction)) {
            logger.finer("No audience restriction requested");
            return;
        }

        if (conditionsType == null) {
            SamlAssertionValidate.Error error = new SamlAssertionValidate.Error("Can't validate conditions, no Conditions have been found", null);
            validationResults.add(error);
            logger.finer(error.toString());
            return;
        }

        final AudienceRestrictionType[] audienceRestrictionArray = conditionsType.getAudienceRestrictionArray();
        if (audienceRestrictionArray == null || audienceRestrictionArray.length <= 0) {
            SamlAssertionValidate.Error error = new SamlAssertionValidate.Error("Audience Restriction Check Failed (assertion does not specify audience)", null, audienceRestriction);
            logger.finer(error.toString());
            validationResults.add(error);
            return;
        }

        for (AudienceRestrictionType val : audienceRestrictionArray) {
            if (!ArrayUtils.contains(val.getAudienceArray(), audienceRestriction)) {
                SamlAssertionValidate.Error error = new SamlAssertionValidate.Error("Audience Restriction Check Failed", null, audienceRestriction);
                logger.finer(error.toString());
                validationResults.add(error);
            }
        }
    }

}
