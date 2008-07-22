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
            validationResults.add(newError("Subject Required", null, null, null));
            return; // no point trying to continue validating a null subject
        }
        final String nameQualifier = requestWssSaml.getNameQualifier();
        NameIDType nameIdentifierType = subject.getNameID();
        if (nameQualifier != null && !"".equals(nameQualifier)) {
            if (nameIdentifierType != null) {
                String presentedNameQualifier = nameIdentifierType.getNameQualifier();
                if (!nameQualifier.equals(presentedNameQualifier)) {
                    Object error = newError("Name Qualifiers does not match presented/required {0}/{1}",
                                             null,
                                             new Object[]{presentedNameQualifier, nameQualifier}, null);
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
                for (int i = 0; i < nameFormats.length; i++) {
                    String nameFormat = nameFormats[i];
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
            Object result = newError("Name Format does not match presented/required {0}/{1}",
                                     null,
                                     new Object[]{presentedNameFormat, Arrays.asList(nameFormats)}, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }

        // map the config v1 names to v2 names
        String[] confirmations = (String[]) ArrayUtils.copy(requestWssSaml.getSubjectConfirmations());
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
            for (int i = 0; i < subjectConfirmations.length; i++) {
                SubjectConfirmationType subjectConfirmation = subjectConfirmations[i];

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
                    if (notOnOrAfter != null && now.equals(notOnOrAfter) || now.after(notOnOrAfter)) {
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("Condition 'Not On Or After' check failed, now :" + now.toString() + " Not Before:" + notOnOrAfter.toString());
                        statementValid = false;
                    }

                    if (notBefore != null && notOnOrAfter != null && !notBefore.before(notOnOrAfter)) {
                        logger.finer("Statement condition is invalid, 'Not On Or After' " + notOnOrAfter.toString() + " MUST be later than 'Not Before' " + notBefore.toString());
                        validationResults.add(newError("Statement condition is invalid, 'Not On Or After' MUST be later than 'Not Before': {0}/{1}",
                                                        null, new Object[]{notOnOrAfter.getTime().toString(), notBefore.getTime().toString()}, null));
                    }
                }

                if (statementValid)
                    presentedConfirmations.add(subjectConfirmation.getMethod());
            }
        }
        if (presentedConfirmations == null)
            presentedConfirmations = Collections.EMPTY_LIST;

        // if no confirmations have been presented, and that is what corresponds to assertion requirements
        // no confirmation check is performed
        if (presentedConfirmations.isEmpty() && requestWssSaml.isNoSubjectConfirmation()) {
            logger.fine("Matched Subject Confirmation 'None'");
            return;
        }

        boolean confirmationMatch = false;
        for (int i = 0; i < confirmations.length; i++) {
            String confirmation = confirmations[i];
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
            Object error = newError("Subject Confirmations mismatch presented/accepted {0}/{1}",
                                     null,
                                     new Object[]{presentedConfirmations, acceptedConfirmations}, null);
            validationResults.add(error);
            logger.finer(error.toString());
            return;
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
                validationResults.add(newError("Can't validate conditions, no Conditions have been presented", null, null, null));
            }
            else {
                Calendar notBefore = conditionsType.getNotBefore();
                Calendar notOnOrAfter = conditionsType.getNotOnOrAfter();
                if (notBefore == null || notOnOrAfter == null) {
                    logger.finer("No Validity Period conditions have been presented, cannot validate Conditions");
                    validationResults.add(newError("No Validity Period conditions have been presented, cannot validate Conditions", null, null, null));
                }
                else {
                    if (now.before(notBefore)) {
                        logger.finer("Condition 'Not Before' check failed, now :" + now.toString() + " Not Before:" + notBefore.toString());
                        validationResults.add(newError("SAML ticket does not become valid until: {0}",
                                                        null, new Object[]{notBefore.getTime().toString()}, null));
                    }

                    if (now.equals(notOnOrAfter) || now.after(notOnOrAfter)) {
                        logger.finer("Condition 'Not On Or After' check failed, now :" + now.toString() + " Not Before:" + notOnOrAfter.toString());
                        validationResults.add(newError("SAML ticket has expired as of: {0}",
                                                        null, new Object[]{notOnOrAfter.getTime().toString()}, null));
                    }
                }
            }
        }

        final String audienceRestriction = requestWssSaml.getAudienceRestriction();
        if (audienceRestriction == null || "".equals(audienceRestriction)) {
            logger.finer("No audience restriction requested");
        }
        else {
            if (conditionsType == null) {
                Object error = newError("Can't validate conditions, no Conditions have been found", null, null, null);
                validationResults.add(error);
                logger.finer(error.toString());
            }
            else {
                AudienceRestrictionType[] audienceRestrictionArray = conditionsType.getAudienceRestrictionArray();
                if (audienceRestrictionArray != null && audienceRestrictionArray.length > 0) {
                    for (int i = 0; i < audienceRestrictionArray.length; i++) {
                        // Check each condition in turn
                        boolean audienceRestrictionMatch = false;
                        AudienceRestrictionType audienceRestrictionType = audienceRestrictionArray[i];
                        String[] audienceArray = audienceRestrictionType.getAudienceArray();

                        for (int j = 0; j < audienceArray.length; j++) {
                            String s = audienceArray[j];
                            if (audienceRestriction.equals(s)) {
                                audienceRestrictionMatch = true;
                                break;
                            }
                        }

                        if (!audienceRestrictionMatch) {
                            Object error = newError("Audience Restriction Check Failed",
                                                     null, new Object[]{audienceRestriction}, null);
                            logger.finer(error.toString());
                            validationResults.add(error);
                        }
                    }
                }
                else {
                    Object error = newError("Audience Restriction Check Failed (assertion does not specify audience)",
                                             null, new Object[]{audienceRestriction}, null);
                    logger.finer(error.toString());
                    validationResults.add(error);
                }
            }
        }

        if (conditionsType != null) {
            if (conditionsType.getOneTimeUseArray() != null &&
                conditionsType.getOneTimeUseArray().length > 1) {
                logger.finer("Multiple OneTimeUse conditions are not permitted.");
                validationResults.add(newError("Multiple OneTimeUse conditions are not permitted.",
                                                null, null, null));
            }

            if (conditionsType.getProxyRestrictionArray() != null &&
                conditionsType.getProxyRestrictionArray().length > 1) {
                logger.finer("Multiple ProxyRestriction conditions are not permitted.");
                validationResults.add(newError("Multiple ProxyRestriction conditions are not permitted.",
                                                null, null, null));
            }
        }
    }

    private static SamlAssertionValidate.Error newError(String reason, Object context, Object args, Exception exception) {
        return new SamlAssertionValidate.Error(reason, context, args, exception);
    }
}
