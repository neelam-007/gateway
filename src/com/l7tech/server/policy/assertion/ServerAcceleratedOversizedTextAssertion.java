/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariMessageContextImpl;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.xml.xpath.XpathResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.OversizedTextAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.rax.token.XmlToken;
import com.tarari.xml.rax.token.XmlTokenList;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server side implementation of the OversizedTextAssertion convenience assertion.
 * This is a special version optimized to run very quickly on a request that has already
 * been examined by Tarari, and works by scanning the token buffer in one pass (two if nesting depth
 * is being checked).
 */
public class ServerAcceleratedOversizedTextAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerAcceleratedOversizedTextAssertion.class.getName());
    private final Auditor auditor;
    private final ServerAssertion softwareFallback;
    private final OversizedTextAssertion ota;
    private final CompiledXpath nestingLimitChecker;

    public ServerAcceleratedOversizedTextAssertion(OversizedTextAssertion data, ApplicationContext springContext) throws ServerPolicyException {
        auditor = new Auditor(this, springContext, ServerAcceleratedOversizedTextAssertion.logger);
        softwareFallback = new ServerOversizedTextAssertion(data, springContext);
        this.ota = data;
        CompiledXpath nestingLimitChecker = null;
        if (ota.isLimitNestingDepth()) {
            try {
                nestingLimitChecker = new XpathExpression(ota.makeNestingXpath()).compile();
            } catch (InvalidXpathException e) {
                // Can't happen, but just in case, make one that always succeeds so checkRequest() always fails
                nestingLimitChecker = CompiledXpath.ALWAYS_TRUE;
            }
        }
        this.nestingLimitChecker = nestingLimitChecker;
    }

    private static final int TEXT = 0;
    private static final int ATTR = 1;
    private static final int ELEM = 2;
    private static final int OTHR = 3;

    private static class ChunkState {
        private int[] longest = new int[] {0, 0, 0, 0};
        private int cur = -1;
        private int curType = -1;

        /**
         * Record the specified token, which must be of the specified chunk type.  This may cause the current
         * chunk to be ended and a new chunk to be started.
         */
        private void handleToken(int chunkType, XmlTokenList.Iterator it) {
            if (cur >= 0) {
                if (curType != chunkType) {
                    if (cur > longest[curType])
                        longest[curType] = cur;
                    /* FALLTHROUGH and record start of new chunk of chunkType stuff */
                } else {
                    // Continue recording the current chunk of chunkType stuff
                    cur += it.getTokenLength();
                    return;
                }
            }

            // Start recording a new chunk of chunkType stuff
            cur = it.getTokenLength();
            curType = chunkType;
        }

        /** End any chunk that is being recorded. */
        private void endChunk() {
            if (cur >= 0) {
                if (cur > longest[curType])
                    longest[curType] = cur;
            }
            cur = -1;
            curType = -1;
        }

        private int getLongestText() { return longest[TEXT]; }
        private int getLongestAttr() { return longest[ATTR]; }
        private int getLongestElem() { return longest[ELEM]; }
        private int getLongestOther(){ return longest[OTHR]; }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws PolicyAssertionException, IOException
    {
        if (ServerRegex.isPostRouting(context)) {
            auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_ALREADY_ROUTED);
            return AssertionStatus.FAILED;
        }


        Message mess = context.getRequest();
        try {
            // Force Tarari evaluation to have occurred
            mess.isSoap();

            // Are we to check nesting depth?  If so, do that first
            if (nestingLimitChecker != null) {
                ElementCursor cursor = mess.getXmlKnob().getElementCursor();
                XpathResult xr = cursor.getXpathResult(nestingLimitChecker);
                if (xr != null && xr.matches()) {
                    // It matched, so nesting depth is too long -- fail
                    auditor.logAndAudit(AssertionMessages.XML_NESTING_DEPTH_EXCEEDED);
                    return AssertionStatus.BAD_REQUEST;
                }
            }

            TarariKnob tknob = (TarariKnob) mess.getKnob(TarariKnob.class);
            if (tknob == null) {
                auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
                return fallbackToSoftwareOnly(context);
            }

            TarariMessageContext tmc = tknob.getContext();
            TarariMessageContextImpl tmContext = (TarariMessageContextImpl)tmc;
            if (tmContext == null) {
                auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
                return fallbackToSoftwareOnly(context);
            }

            ChunkState chunkState = findLongestChunks(tmContext.getRaxDocument());

            int longestText = chunkState.getLongestText();
            int longestAttrValue = chunkState.getLongestAttr();
            int longestOther = Math.max(chunkState.getLongestOther(), chunkState.getLongestElem());

            if (longestAttrValue > ota.getMaxAttrChars() || longestText > ota.getMaxTextChars() ||
                    (longestOther > ota.getMaxAttrChars()*2 && longestOther > ota.getMaxTextChars()*2))
            {
                auditor.logAndAudit(AssertionMessages.OVERSIZEDTEXT_REQUEST_REJECTED);
                return AssertionStatus.BAD_REQUEST;
            }

            return AssertionStatus.NONE;

        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.XPATH_REQUEST_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] {"The required attachment " + e.getWhatWasMissing() + "was not found in the request"}, e);
            return AssertionStatus.BAD_REQUEST;
        }
    }

    private ChunkState findLongestChunks(RaxDocument doc) {
        XmlTokenList tokenList = doc.getTokenList();
        XmlTokenList.Iterator tokenIterator = tokenList.getIterator();
        ChunkState s = new ChunkState();

        while (tokenIterator.next()) {
            switch (tokenIterator.getTokenId()) {
                case XmlToken.CDATA_SECTION:
                case XmlToken.CHARACTER_DATA:
                case XmlToken.CHARACTER_DATA_CR:
                case XmlToken.CHARACTER_DATA_WS:
                case XmlToken.CHARACTER_DATA_WSCR:
                case XmlToken.COMMENT:
                case XmlToken.ENTITY_REFERENCE:
                    s.handleToken(TEXT, tokenIterator);
                    break;

                case XmlToken.START_TAG_ATTR_VALUE:
                case XmlToken.START_TAG_ATTR_VALUE_CR:
                case XmlToken.START_TAG_ATTR_VALUE_END:
                case XmlToken.START_TAG_NS_DECL_VALUE:
                case XmlToken.START_TAG_NS_DECL_VALUE_CR:
                case XmlToken.START_TAG_NS_DECL_VALUE_END:
                    s.handleToken(ATTR, tokenIterator);
                    break;

                case XmlToken.START_TAG_ELEMENT_PREFIX:
                case XmlToken.START_TAG_ELEMENT_LOCAL_PART:
                case XmlToken.START_TAG_DEFAULT_NS_DECL:
                case XmlToken.START_TAG_ATTR_PREFIX:
                case XmlToken.START_TAG_ATTR_LOCAL_PART:
                case XmlToken.END_TAG:
                case XmlToken.EMPTY_ELEMENT_TAG_END:
                    s.handleToken(ELEM, tokenIterator);
                    break;

                case XmlToken.START_TAG_END:
                    s.handleToken(ELEM, tokenIterator);
                    s.endChunk();
                    break;

                default:
                    s.handleToken(OTHR, tokenIterator);
                    break;
            }
        }
        s.endChunk();
        return s;
    }

    private AssertionStatus fallbackToSoftwareOnly(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        return softwareFallback.checkRequest(context);
    }
}
