package com.l7tech.external.assertions.cache.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.external.assertions.cache.CacheStorageAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.util.ValidationUtils;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;

import static com.l7tech.external.assertions.cache.CacheStorageAssertion.kMAX_ENTRY_AGE_SECONDS;
import static com.l7tech.util.ValidationUtils.isValidLong;

/**
 * Server side implementation of the CacheStorageAssertion.
 *
 * @see com.l7tech.external.assertions.cache.CacheStorageAssertion
 */
public class ServerCacheStorageAssertion extends AbstractMessageTargetableServerAssertion<CacheStorageAssertion> {

    private final String[] variablesUsed;
    private final SsgCacheManager cacheManager;
    private final static String zeroString = Integer.toString(0);

    public ServerCacheStorageAssertion(CacheStorageAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.cacheManager = SsgCacheManager.getInstance(beanFactory);

        validateSingleVariableProperty(assertion, assertion.getMaxEntryAgeSeconds(), "entry age");
        validateSingleVariableProperty(assertion, assertion.getMaxEntrySizeBytes(), "entry size");
        validateSingleVariableProperty(assertion, assertion.getMaxEntries(), "entries");
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        InputStream messageBody = null;
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

        try {
            final int cacheMaxEntries;
            final String processedCacheMaxEntries = ExpandVariables.process(assertion.getMaxEntries(), vars, getAudit());
            if (ValidationUtils.isValidInteger(processedCacheMaxEntries, false, 0, CacheStorageAssertion.kMAX_ENTRIES)) {
                cacheMaxEntries = Integer.parseInt(processedCacheMaxEntries);
            } else {
                final String format = MessageFormat.format("Resolved maximum entries value is invalid ''{0}''. Value must be between ''{1}'' and ''{2}'' inclusive.", processedCacheMaxEntries, zeroString, Integer.toString(CacheStorageAssertion.kMAX_ENTRIES));
                logAndAudit(AssertionMessages.CACHE_STORAGE_INVALID_VALUE, format);
                return AssertionStatus.FAILED;
            }

            final String maxEntryAgeSeconds = ExpandVariables.process(assertion.getMaxEntryAgeSeconds(), vars, getAudit());
            if (!isValidLong(maxEntryAgeSeconds, false, 0, kMAX_ENTRY_AGE_SECONDS)) {
                final String format = MessageFormat.format("Resolved maximum entry age value is invalid ''{0}''. Value must be seconds between ''{1}'' and ''{2}'' inclusive.", maxEntryAgeSeconds, zeroString, String.valueOf(kMAX_ENTRY_AGE_SECONDS));
                logAndAudit(AssertionMessages.CACHE_STORAGE_INVALID_VALUE, format);
                return AssertionStatus.FAILED;
            }

            final long cacheMaxEntryAgeMillis = Long.valueOf(maxEntryAgeSeconds) * 1000L;

            final long cacheMaxEntrySizeBytes;
            final String processedCacheMaxEntrySizeBytes = ExpandVariables.process(assertion.getMaxEntrySizeBytes(), vars, getAudit());
            if (isValidLong(processedCacheMaxEntrySizeBytes, false, 0, CacheStorageAssertion.kMAX_ENTRY_SIZE)) {
                cacheMaxEntrySizeBytes = Long.parseLong(processedCacheMaxEntrySizeBytes);
            } else {
                final String format = MessageFormat.format("Resolved maximum entry size value is invalid ''{0}''. Value must be between ''{1}'' and ''{2}'' inclusive.", processedCacheMaxEntrySizeBytes, zeroString, Long.toString(CacheStorageAssertion.kMAX_ENTRY_SIZE));
                logAndAudit(AssertionMessages.CACHE_STORAGE_INVALID_VALUE, format);
                return AssertionStatus.FAILED;
            }

            final Message messageToCache = context.getTargetMessage(assertion, true);
            final ContentTypeHeader contentType = messageToCache.getMimeKnob().getOuterContentType();
            final String cacheId = ExpandVariables.process(assertion.getCacheId(), vars, getAudit());
            final String cacheEntryKey = ExpandVariables.process(assertion.getCacheEntryKey(), vars, getAudit());
            messageBody = messageToCache.getMimeKnob().getEntireMessageBodyAsInputStream();

            final SsgCache.Config cacheConfig = new SsgCache.Config(cacheId)
                    .maxEntries(cacheMaxEntries)
                    .maxAgeMillis(cacheMaxEntryAgeMillis)
                    .maxSizeBytes(cacheMaxEntrySizeBytes);
            final SsgCache cache = cacheManager.getCache(cacheConfig);
            if (assertion.isStoreSoapFaults() || ! isSoapFault(messageToCache)) {
                cache.store(cacheEntryKey, messageBody, contentType.getFullValue());
                logAndAudit(AssertionMessages.CACHE_STORAGE_STORED, cacheEntryKey);
            }
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, e.getVariable());
        } catch (Exception e) {
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] {"Unable to store cached value: " + ExceptionUtils.getMessage(e)}, e);
        } finally {
            if (messageBody != null) messageBody.close();
        }
        return AssertionStatus.NONE;
    }

    // - PRIVATE

    private boolean isSoapFault(Message messageToCache) {
        try {
            return messageToCache.isSoap() && messageToCache.getSoapKnob().isFault();
        } catch (Exception e) {
            return false;
        }
    }

    private void validateSingleVariableProperty(CacheStorageAssertion assertion, String expression, String propertyName) throws PolicyAssertionException{
        final String[] refs = Syntax.getReferencedNames(expression);
        if (refs.length > 0) {
            if (!Syntax.isOnlyASingleVariableReferenced(expression)) {
                throw new PolicyAssertionException(assertion, "Invalid value for maximum " + propertyName + ". Only a single variable may be referenced: '" + expression + "'");
            }
        }
    }

}