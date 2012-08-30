package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.ContextVariableUtils;
import com.l7tech.util.*;
import x0Assertion.oasisNamesTcSAML2.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validation for SAML 2.x Subject and Conditions.
 */
class Saml2SubjectAndConditionValidate {

    private static final Logger logger = Logger.getLogger(Saml2SubjectAndConditionValidate.class.getName());

    /**
     * Subject validation for 2.x
     */
    static void validateSubject(final RequireSaml requestSaml,
                                final SubjectType subject,
                                final Calendar now,
                                final Collection<String> clientAddresses,
                                final Collection<SamlAssertionValidate.Error> validationResults,
                                final Map<String, Object> serverVariables,
                                final Audit auditor) {
        if (subject == null) {
            validationResults.add(new SamlAssertionValidate.Error("Subject Required", null));
            return; // no point trying to continue validating a null subject
        }

        final String nameQualTest = requestSaml.getNameQualifier();
        final String nameQualifier = (nameQualTest == null) ? nameQualTest : ExpandVariables.process(nameQualTest, serverVariables, auditor);
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
        String[] nameFormats = requestSaml.getNameFormats();
        final String presentedNameFormat = (nameIdentifierType != null && nameIdentifierType.getFormat() != null)?
                nameIdentifierType.getFormat():
                SamlConstants.NAMEIDENTIFIER_UNSPECIFIED;

        if (nameIdentifierType != null) {
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

        if (!nameFormatMatch) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("Name Format does not match presented/required {0}/{1}", null, presentedNameFormat, Arrays.asList(nameFormats));
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }

        // map the config v1 names to v2 names
        String[] confirmations = ArrayUtils.copy(requestSaml.getSubjectConfirmations());
        for (int i = 0; i < confirmations.length; i++) {
            String confirmation = confirmations[i];
            String saml2Confirmation = (String) SamlConstants.CONF_MAP_SAML_1TO2.get(confirmation);
            if (saml2Confirmation != null) {
                confirmations[i] = saml2Confirmation;
            }
        }

        final Collection<SamlAssertionValidate.Error> subjectConfirmationValidationFailures = new ArrayList<SamlAssertionValidate.Error>();
        final SubjectConfirmationType[] subjectConfirmations = subject.getSubjectConfirmationArray();
        List<String> presentedConfirmations = new ArrayList<String>();
        boolean anyInvalid = false;
        if (subjectConfirmations != null) {
            for (SubjectConfirmationType subjectConfirmation : subjectConfirmations) {
                SubjectConfirmationDataType confirmationData = subjectConfirmation.getSubjectConfirmationData();
                boolean statementValid = true;
                if (confirmationData != null) {
                    if ( requestSaml.isSubjectConfirmationDataCheckValidity() ) {
                        Calendar notBefore = confirmationData.getNotBefore();
                        if (notBefore != null && now.before(adjustNotBefore(notBefore))) {
                            subjectConfirmationDataValidationError( subjectConfirmationValidationFailures,
                                    "Condition 'Not Before' check failed, now :{0} Not Before:{1}",
                                    now.getTime().toString(),
                                    notBefore.getTime().toString() );
                            statementValid = false;
                        }

                        Calendar notOnOrAfter = confirmationData.getNotOnOrAfter();
                        if (notOnOrAfter != null) {
                            Calendar adjustedNotOnOrAfter = adjustNotAfter(notOnOrAfter);
                            if ( (now.equals(adjustedNotOnOrAfter) || now.after(adjustedNotOnOrAfter)) ) {
                                subjectConfirmationDataValidationError( subjectConfirmationValidationFailures,
                                        "Condition 'Not On Or After' check failed, now: {0} Not On Or After: {1}",
                                        now.getTime().toString(),
                                        notOnOrAfter.getTime().toString() );
                                statementValid = false;
                            }
                        }

                        if (notBefore != null && notOnOrAfter != null && !notBefore.before(notOnOrAfter)) {
                            logger.finer("Statement condition is invalid, 'Not On Or After' " + notOnOrAfter.getTime().toString() + " MUST be later than 'Not Before' " + notBefore.getTime().toString());
                            validationResults.add(new SamlAssertionValidate.Error("Statement condition is invalid, 'Not On Or After' MUST be later than 'Not Before': {0}/{1}", null, notOnOrAfter.getTime().toString(), notBefore.getTime().toString()));
                            statementValid = false;
                        }
                    }

                    if ( requestSaml.isSubjectConfirmationDataCheckAddress() && confirmationData.getAddress() != null ) {
                        if ( clientAddresses==null || !clientAddresses.contains( confirmationData.getAddress() ) ) {
                            subjectConfirmationDataValidationError( subjectConfirmationValidationFailures,
                                    "Statement condition is invalid, 'Address' {0} does not match client address.",
                                    confirmationData.getAddress() );
                            statementValid = false;
                        }
                    }

                    final String recipientTest = requestSaml.getSubjectConfirmationDataRecipient();
                    final String recipient = (recipientTest == null)? recipientTest: ExpandVariables.process(recipientTest, serverVariables, auditor);
                    if ( recipient != null &&
                         confirmationData.getRecipient() !=null &&
                         !recipient.equals(confirmationData.getRecipient()) ) {
                        subjectConfirmationDataValidationError( subjectConfirmationValidationFailures,
                                "Statement condition is invalid, 'Recipient' {0} does not match required value {1}.",
                                confirmationData.getRecipient(),
                                recipient );
                        statementValid = false;
                    }
                }

                if (statementValid)
                    presentedConfirmations.add(subjectConfirmation.getMethod());
                else
                    anyInvalid = true;
            }
        }

        // if no confirmations have been presented, and that is what corresponds to assertion requirements
        // no confirmation check is performed
        if (presentedConfirmations.isEmpty() && requestSaml.isNoSubjectConfirmation()) {
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
            // Add the subject confirmation data failures from before that could have
            // caused the error. See bug 9085.
            validationResults.addAll( subjectConfirmationValidationFailures );

            List<String> acceptedConfirmations = new ArrayList<String>(Arrays.asList(confirmations));
            if (requestSaml.isNoSubjectConfirmation()) {
                acceptedConfirmations.add("None");
            }
            SamlAssertionValidate.Error error = new SamlAssertionValidate.Error("Subject Confirmations mismatch "+(anyInvalid?"(some confirmations were rejected) " : "")+"presented/accepted {0}/{1}", null, presentedConfirmations, acceptedConfirmations);
            validationResults.add(error);
            logger.finer(error.toString());
        }
    }

    private static void subjectConfirmationDataValidationError( final Collection<SamlAssertionValidate.Error> subjectConfirmationValidationFailures,
                                                                final String message,
                                                                final Object... messageParameters ) {
        final SamlAssertionValidate.Error error = new SamlAssertionValidate.Error( message, null, messageParameters );

        if (logger.isLoggable(Level.FINE)) {
            logger.fine( error.toString() );
        }

        subjectConfirmationValidationFailures.add( error );
    }

    /**
     * Validate the SAML v2 assertion conditions
     */
    static void validateConditions(final RequireSaml requestSaml,
                                   final ConditionsType conditionsType,
                                   final Calendar now,
                                   final Collection<SamlAssertionValidate.Error> validationResults,
                                   final Map<String, Object> serverVariables,
                                   final Audit auditor) {
        if (!requestSaml.isCheckAssertionValidity()) {
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
                    notBefore = SamlAssertionValidate.adjustNotBefore(notBefore);
                    notOnOrAfter = SamlAssertionValidate.adjustNotAfter(notOnOrAfter);
                    if (now.before(notBefore)) {
                        logger.finer("Condition 'Not Before' check failed, now :" + now.getTime().toString() + " Not Before:" + notBefore.getTime().toString());
                        validationResults.add(new SamlAssertionValidate.Error("SAML ticket does not become valid until: {0}", null, notBefore.getTime().toString()));
                    }

                    if (now.equals(notOnOrAfter) || now.after(notOnOrAfter)) {
                        logger.finer("Condition 'Not On Or After' check failed, now :" + now.getTime().toString() + " Not Before:" + notOnOrAfter.getTime().toString());
                        validationResults.add(new SamlAssertionValidate.Error("SAML ticket has expired as of: {0}", null, notOnOrAfter.getTime().toString()));
                    }
                }
            }
        }

        validateAudienceRestriction(requestSaml, conditionsType, validationResults, serverVariables, auditor);

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

    private static void validateAudienceRestriction(final RequireSaml requestSaml,
                                                    final ConditionsType conditionsType,
                                                    final Collection<SamlAssertionValidate.Error> validationResults,
                                                    final Map<String, Object> serverVariables,
                                                    final Audit auditor) {
        final String audienceResTest = requestSaml.getAudienceRestriction();
        final Option<String> option = Option.optional(requestSaml.getAudienceRestriction());
        final List<String> allAudienceRestrictions = (!option.isSome()) ?
                Collections.<String>emptyList() :
                ContextVariableUtils.getAllResolvedStrings(audienceResTest, serverVariables, auditor, TextUtils.URI_STRING_SPLIT_PATTERN, new Functions.UnaryVoid<Object>() {
            @Override
            public void call(Object unexpectedNonString) {
                //todo get an auditor
                logger.warning("Found non string value for audience restriction: " + unexpectedNonString);
            }
        });

        if (allAudienceRestrictions.isEmpty()) {
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
        if (audienceRestrictionArray.length <= 0) {
            SamlAssertionValidate.Error error = new SamlAssertionValidate.Error("Audience Restriction Check Failed (assertion does not specify audience restriction condition)", null, allAudienceRestrictions);
            logger.finer(error.toString());
            validationResults.add(error);
            return;
        }

        boolean audienceRestrictionMatch = false;
        final StringBuilder builder = new StringBuilder();
        // SAML only requires a disjunction - If we find the configured audience values in any set of audience elements, then it passes validation for this Condition

        for (AudienceRestrictionType val : audienceRestrictionArray) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Validating audience restrictions against resolved list: " + allAudienceRestrictions);
            }

            final String[] incomingAudienceValues = val.getAudienceArray();
            if (ArrayUtils.containsAny(incomingAudienceValues, allAudienceRestrictions.toArray(new String[allAudienceRestrictions.size()]))) {
                audienceRestrictionMatch = true;
                break;
            } else {
                builder.append(Arrays.asList(incomingAudienceValues));
            }
        }
        if (!audienceRestrictionMatch) {
            SamlAssertionValidate.Error error =
                    new SamlAssertionValidate.Error("Audience Restriction Check Failed received {0} expected one of {1}",
                            null, builder, allAudienceRestrictions);
            logger.finer(error.toString());
            validationResults.add(error);
        }
    }

    static Calendar adjustNotAfter(Calendar notOnOrAfter) {
        int afterOffsetMinutes = ConfigFactory.getIntProperty( ServerConfigParams.PARAM_SAML_VALIDATE_AFTER_OFFSET_MINUTES, 0 );
        if (afterOffsetMinutes != 0) {
            notOnOrAfter = (Calendar)notOnOrAfter.clone();
            notOnOrAfter.add(Calendar.MINUTE, afterOffsetMinutes);
        }
        return notOnOrAfter;
    }

    static Calendar adjustNotBefore(Calendar notBefore) {
        int beforeOffsetMinutes = ConfigFactory.getIntProperty( ServerConfigParams.PARAM_SAML_VALIDATE_BEFORE_OFFSET_MINUTES, 0 );
        if (beforeOffsetMinutes != 0) {
            notBefore = (Calendar)notBefore.clone();
            notBefore.add(Calendar.MINUTE, -beforeOffsetMinutes);
        }
        return notBefore;
    }

}
