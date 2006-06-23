package com.l7tech.server.policy.assertion.xmlsec;

import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

import x0Assertion.oasisNamesTcSAML2.SubjectType;
import x0Assertion.oasisNamesTcSAML2.NameIDType;
import x0Assertion.oasisNamesTcSAML2.SubjectConfirmationType;
import x0Assertion.oasisNamesTcSAML2.ConditionsType;
import x0Assertion.oasisNamesTcSAML2.AudienceRestrictionType;

import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.util.ArrayUtils;

/**
 * Validation for SAML 2.x Subject and Conditions.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
class Saml2SubjectAndConditionValidate {

    private static final Logger logger = Logger.getLogger(Saml2SubjectAndConditionValidate.class.getName());
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * Subject validation for 2.x
     */
    static void validateSubject(RequestWssSaml requestWssSaml, SubjectType subject, Collection validationResults) {
        if (subject == null) {
            validationResults.add(newError("Subject Required", null, null, null));
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
        String[] nameFormats = requestWssSaml.getNameFormats();
        String presentedNameFormat = nameIdentifierType.getFormat();
        boolean nameFormatMatch = false;
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
    static void validateConditions(RequestWssSaml requestWssSaml, ConditionsType conditionsType, Collection validationResults) {
        if (!requestWssSaml.isCheckAssertionValidity()) {
            logger.finer("No Assertion Validity requested");
        } else {
            if (conditionsType == null) {
                logger.finer("Can't validate conditions, no Conditions have been presented");
                validationResults.add(newError("Can't validate conditions, no Conditions have been presented", null, null, null));
                return;
            }
            Calendar notBefore = conditionsType.getNotBefore();
            Calendar notOnOrAfter = conditionsType.getNotOnOrAfter();
            if (notBefore == null || notOnOrAfter == null) {
                logger.finer("No Validity Period conditions have been presented, cannot validate Conditions");
                validationResults.add(newError("No Validity Period conditions have been presented, cannot validate Conditions", null, null, null));
                return;
            }

            Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
            now.clear(Calendar.MILLISECOND); //clear millis xsd:dateTime does not have it
            if (now.before(notBefore)) {
                logger.finer("Condition 'Not Before' check failed, now :" + now.toString() + " Not Before:" + notBefore.toString());
                validationResults.add(newError("Condition 'Not Before' check failed Now/ Not Before: {0}/{1}",
                                                null, new Object[]{now.getTime().toString(), notBefore.getTime().toString()}, null));
            }

            if (now.equals(notOnOrAfter) || now.after(notOnOrAfter)) {
                logger.finer("Condition 'Not On Or After' check failed, now :" + now.toString() + " Not Before:" + notOnOrAfter.toString());
                validationResults.add(newError("Condition 'Not On Or After' check failed Now/Not On Or After: {0}/{1}",
                                                null, new Object[]{now.getTime().toString(), notOnOrAfter.getTime().toString()}, null));
            }
        }

        final String audienceRestriction = requestWssSaml.getAudienceRestriction();
        if (audienceRestriction == null || "".equals(audienceRestriction)) {
            logger.finer("No audience restriction requested");
            return;
        }

        if (conditionsType == null) {
            Object error = newError("Can't validate conditions, no Conditions have been found", null, null, null);
            validationResults.add(error);
            logger.finer(error.toString());
            return;
        }

        AudienceRestrictionType[] audienceRestrictionArray = conditionsType.getAudienceRestrictionArray();
        boolean audienceRestrictionMatch = false;
        for (int i = 0; i < audienceRestrictionArray.length; i++) {
            AudienceRestrictionType audienceRestrictionType = audienceRestrictionArray[i];
            String[] audienceArray = audienceRestrictionType.getAudienceArray();
            for (int j = 0; j < audienceArray.length; j++) {
                String s = audienceArray[j];
                if (audienceRestriction.equals(s)) {
                    audienceRestrictionMatch = true;
                    break;
                }
            }
        }
        if (!audienceRestrictionMatch) {
            Object error = newError("Audience Restriction Check Failed",
                                     null, new Object[]{audienceRestriction}, null);
            logger.finer(error.toString());
            validationResults.add(error);
        }
    }

    private static SamlAssertionValidate.Error newError(String reason, Object context, Object args, Exception exception) {
        return new SamlAssertionValidate.Error(reason, context, args, exception);
    }
}
