package com.l7tech.console.panels.policydiff;

import com.l7tech.util.GoogleDiffUtils;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import static com.l7tech.console.panels.policydiff.PolicyDiffWindow.*;
import static com.l7tech.console.panels.policydiff.PolicyDiffWindow.DiffType.IDENTICAL;

/**
 * Compare two chunks of policy XML and generate diff results.
 * Extract diff results from the results to displaying text areas.
 * Highlight the lines with differences in both displaying text areas.
 */
public class XmlDiff {
    private static Logger logger = Logger.getLogger(XmlDiff.class.getName());

    private final List<LineDiffResult> leftXmlLineDiffResults;   // The line diff result list of the left policy xml,
    private final List<LineDiffResult> rightXmlLineDiffResults;  // The line diff result list of the right policy xml,
    private final int maxLineNum;    // The max value between policyXml1's line number and policyXml2's line number

    /**
     * Compare two chunks of policy XML and generate diff results
     *
     * @param xml1: the left diffed policy xml
     * @param xml2: the right diffed policy xml
     *
     * @throws java.io.IOException thrown if an I/O error occurs on BufferedReader
     */
    public XmlDiff(final String xml1, final String xml2) throws IOException {
        if (xml1 == null || xml2 == null) {
            throw new IllegalArgumentException("String for diff cannot be null.");
        }

        leftXmlLineDiffResults = new ArrayList<>();  // Store the line diff result of xml1
        rightXmlLineDiffResults = new ArrayList<>();  // Store the line diff result of xml2
        final Vector<String> xml2LineVector = new Vector<>();  // Store each original line without trimming leading and ending whitespaces
        BufferedReader br = new BufferedReader(new StringReader(xml2));

        String line;
        while ((line = br.readLine()) != null) {
            xml2LineVector.add(line);
        }
        br.close();

        br = new BufferedReader(new StringReader(xml1));
        LinkedList<GoogleDiffUtils.Diff> onePartialMatch;
        String line1, line2; // Temporarily store each line (without leading and ending whitespace) of xml1 and xml2
        int idxMatched;      // The index of line1 matched line2
        int index1 = 0;      // The current line index of xml1
        int index2 = 0;      // The current line index of xml2

        int maxDepth = SyspropUtil.getInteger(SYSTEM_PROPERTY_MAX_SEARCH_DEPTH, DEFAULT_MAX_SEARCH_DEPTH);
        while ((line1 = br.readLine()) != null) {

            // Find one fully matched line ignoring preceding and trailing whitespace.
            idxMatched = -1;
            onePartialMatch = null;
            for (int idx = index2, depth =0; idx < xml2LineVector.size() && depth < maxDepth; idx++, depth++) {
                line2 = xml2LineVector.get(idx);
                if (line1.equals(line2)) {
                    idxMatched = idx;
                    break;
                } else if (getLineIndentation(line1) == getLineIndentation(line2)) {
                    onePartialMatch = isPolicyXmlSingleLinePartiallyMatched(line1.trim(), line2.trim());

                    if (onePartialMatch != null) {
                        idxMatched = idx;
                        break;
                    }
                }
            }

            // Case 1: Match one line from the xml2 list
            if (onePartialMatch == null && idxMatched >= index2) {
                // Add differences
                for (int i = index2; i < idxMatched; i++) {
                    leftXmlLineDiffResults.add(null);
                    rightXmlLineDiffResults.add(new LineDiffResult(i + 1, DiffType.INSERTED, xml2LineVector.get(i)));
                    index2++;
                }

                // Add matched
                leftXmlLineDiffResults.add(new LineDiffResult(index1 + 1, IDENTICAL, line1));
                rightXmlLineDiffResults.add(new LineDiffResult(index2 + 1, IDENTICAL, xml2LineVector.get(index2)));

                // Increment indexes
                index1++;
                index2++;
            }
            // Case 2: Partially match one line from the xml2 list
            else if (onePartialMatch != null) {
                // Add differences
                for (int j = index2; j < idxMatched; j++) {
                    leftXmlLineDiffResults.add(null);
                    rightXmlLineDiffResults.add(new LineDiffResult(j + 1, DiffType.INSERTED, xml2LineVector.get(j)));
                    index2++;
                }

                // Add partially matched
                leftXmlLineDiffResults.add(new LineDiffResult(index1 + 1, DiffType.MATCHED_WITH_DIFFERENCES, line1, onePartialMatch));
                rightXmlLineDiffResults.add(new LineDiffResult(index2 + 1, DiffType.MATCHED_WITH_DIFFERENCES, xml2LineVector.get(index2), onePartialMatch));

                // Increment indexes
                index1++;
                index2++;
            }
            // Case 3: Not matched
            else {
                leftXmlLineDiffResults.add(new LineDiffResult(index1 + 1, DiffType.DELETED, line1));
                rightXmlLineDiffResults.add(null);

                index1++; // At this case, do not increment idxPtr2
            }
        }
        br.close();

        // Check if xml2 has some lines left.  If so, add empty strings into xml1 result
        final int xml1ListSize = leftXmlLineDiffResults.size();
        final int xml2ListSize = xml2LineVector.size();
        if (xml1ListSize < xml2ListSize) {
            for (int i = xml1ListSize; i < xml2ListSize; i++) {
                leftXmlLineDiffResults.add(null);
                rightXmlLineDiffResults.add(new LineDiffResult(i + 1, DiffType.INSERTED, xml2LineVector.get(i)));
                index2++;
            }
        }

        if (leftXmlLineDiffResults.size() != rightXmlLineDiffResults.size()) {
            throw new RuntimeException("Two diff result list should have a same size.");
        }

        maxLineNum = Math.max(index1, index2);
    }

    /**
     * Extract diff results from the results to displaying text areas
     */
    public List<Integer> setTextAreas(final JTextArea leftXmlTextArea, final JTextArea leftLineNumTextArea,
                                      final JTextArea rightXmlTextArea, final JTextArea rightLineNumTextArea) throws BadLocationException {
        final List<Integer> nextDiffIndexList = new ArrayList<>();

        if (leftXmlLineDiffResults.size() != rightXmlLineDiffResults.size()) {
            throw new IllegalArgumentException("Two diff result lists must have a same size.");
        }
        final StringBuilder leftLineNumbersSB = new StringBuilder();
        final StringBuilder leftDiffContentSB = new StringBuilder();
        final StringBuilder rightLineNumbersSB = new StringBuilder();
        final StringBuilder rightDiffContentSB = new StringBuilder();
        final String lineNumFormatter = "%" + String.valueOf(maxLineNum).length() + "s";

        LineDiffResult leftLineDiffResult, rightLineDiffResult;
        DiffType prevDiffType;
        DiffType currDiffType = IDENTICAL;
        for (int i = 0; i < leftXmlLineDiffResults.size(); i++) {
            leftLineDiffResult = leftXmlLineDiffResults.get(i);
            rightLineDiffResult = rightXmlLineDiffResults.get(i);

            if (leftLineDiffResult != null) {
                leftLineNumbersSB.append(String.format(lineNumFormatter, leftLineDiffResult.getLineNum()));
                leftDiffContentSB.append(leftLineDiffResult.getLineContent());
            }
            leftLineNumbersSB.append("\n");
            leftDiffContentSB.append("\n");

            if (rightLineDiffResult != null) {
                rightLineNumbersSB.append(String.format(lineNumFormatter, rightLineDiffResult.getLineNum()));
                rightDiffContentSB.append(rightLineDiffResult.getLineContent());
            }
            rightLineNumbersSB.append("\n");
            rightDiffContentSB.append("\n");

            prevDiffType = currDiffType;
            currDiffType = leftLineDiffResult == null? null : leftLineDiffResult.getDiffType();

            if (currDiffType != prevDiffType && currDiffType != IDENTICAL) {
                nextDiffIndexList.add(i);
            }
        }

        leftLineNumTextArea.setText(leftLineNumbersSB.toString());
        leftXmlTextArea.setText(leftDiffContentSB.toString());

        rightLineNumTextArea.setText(rightLineNumbersSB.toString());
        rightXmlTextArea.setText(rightDiffContentSB.toString());

        highlightDiffXML(true, leftXmlTextArea, leftXmlLineDiffResults);
        highlightDiffXML(false, rightXmlTextArea, rightXmlLineDiffResults);

        return nextDiffIndexList;
    }

    /**
     * Highlight the lines with differences in both displaying text areas
     *
     * @param isLeftDiffDisplayingArea: a flag indicates if this method is called for Left Policy Displaying TextArea
     * @param diffDisplayingTextArea: the text area displays the policy xml with highlights for differences.
     * @param lineDiffResultList: a LineDiffResult-object list stores policy diff results associated with the policy xml.
     *
     * @throws javax.swing.text.BadLocationException thrown if an invalid range specification is used by Highlighter
     */
    private void highlightDiffXML(final boolean isLeftDiffDisplayingArea,
                                  final JTextArea diffDisplayingTextArea,
                                  final List<LineDiffResult> lineDiffResultList) throws BadLocationException {

        final Highlighter highlighter = diffDisplayingTextArea.getHighlighter();
        highlighter.removeAllHighlights();

        int lineIdx = 0;
        int carPosition;
        diffDisplayingTextArea.setCaretPosition(0);
        for (LineDiffResult lineDiffResult : lineDiffResultList) {
            // Get current position
            carPosition = diffDisplayingTextArea.getCaretPosition();

            // Set proper highlight painter based on diff type
            if (lineDiffResult == null) {
                highlighter.addHighlight(carPosition, carPosition, new LineHighlightPainter(COLOR_FOR_BLANK_ASSERTION, carPosition));
            } else {
                switch (lineDiffResult.getDiffType()) {
                    case DELETED:
                        highlighter.addHighlight(carPosition, carPosition, new LineHighlightPainter(COLOR_FOR_DELETION, carPosition));
                        break;
                    case INSERTED:
                        highlighter.addHighlight(carPosition, carPosition, new LineHighlightPainter(COLOR_FOR_INSERTION, carPosition));
                        break;
                    case IDENTICAL:
                        // Do nothing.  Use the default highlighter
                        break;
                    case MATCHED_WITH_DIFFERENCES:
                        // Highlight the whole line first using COLOR_FOR_MATCH_WITH_DIFFERENCES
                        highlighter.addHighlight(carPosition, carPosition, new LineHighlightPainter(COLOR_FOR_MATCH_WITH_DIFFERENCES, carPosition));

                        // Then highlight the deletion parts using COLOR_FOR_DELETION and the insertion parts using COLOR_FOR_INSERTION
                        String line = lineDiffResult.getLineContent();
                        String content;
                        int contentIdx;
                        int searchFromIdx = 0;
                        for (GoogleDiffUtils.Diff diff: lineDiffResult.getPartialMatch()) {
                            content = diff.text;
                            contentIdx = line.indexOf(content, searchFromIdx);
                            switch (diff.operation) {
                                case DELETE:
                                    if (! isLeftDiffDisplayingArea) {
                                        continue;
                                    }
                                    highlighter.addHighlight(
                                            carPosition + contentIdx,
                                            carPosition + contentIdx + content.length(),
                                            new DefaultHighlighter.DefaultHighlightPainter(COLOR_FOR_DELETION)
                                    );
                                    break;
                                case INSERT:
                                    if (isLeftDiffDisplayingArea) {
                                        continue;
                                    }
                                    highlighter.addHighlight(
                                            carPosition + contentIdx,
                                            carPosition + contentIdx + content.length(),
                                            new DefaultHighlighter.DefaultHighlightPainter(COLOR_FOR_INSERTION)
                                    );
                                    break;
                                case EQUAL:
                                    break;
                                default:
                                    throw new IllegalArgumentException("No such DIFF type: " + diff.operation.toString());
                            }
                            searchFromIdx = contentIdx + content.length();
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid line type: " + lineDiffResult.getDiffType().toString());
                }
            }

            // Update caret position
            diffDisplayingTextArea.setCaretPosition(diffDisplayingTextArea.getLineEndOffset(lineIdx));
            lineIdx++;
        }
    }

    /**
     * Get the length of the indentation of a particular XML line.
     *
     * @param line: a particular XML line
     *
     * @return the length of the XML line
     */
    private int getLineIndentation(String line) {
        if (line == null) return 0;

        String lineNoLeadingTrailingSpace = line.trim();
        return line.indexOf(lineNoLeadingTrailingSpace);
    }

    /**
     * Check if two single lines are partially matched.
     *
     * @param text1: a single line of the first policy xml
     * @param text2: a single line of the second policy xml
     *
     * @return a Diff list if there are some partial matches.  Otherwise, return null if no partial matches exist.
     */
    private LinkedList<GoogleDiffUtils.Diff> isPolicyXmlSingleLinePartiallyMatched(String text1, String text2) {
        LinkedList<GoogleDiffUtils.Diff> diffList = GoogleDiffUtils.diff_main(text1, text2);
        if (diffList.isEmpty()) return null;

        GoogleDiffUtils.Diff firstDiff = diffList.getFirst();
        if (firstDiff.operation == GoogleDiffUtils.Operation.EQUAL) {
            String equalPart = firstDiff.text.trim();
            if (equalPart.split(" ").length > 1) {
                return diffList;
            }
        }

        return null;
    }

    /**
     *  The class store XML comparison result for each XML line.
     */
    public class LineDiffResult {
        private int lineNum;                    // The original policy xml line number
        private DiffType diffType;              // It indicates if this line is deleted, inserted, or partially matched.
        private String lineContent;             // The original line content without modification such as any whitespaces trimmed
        private LinkedList<GoogleDiffUtils.Diff> partialMatch;  // It stores the partial match information.

        public LineDiffResult(int lineNum, DiffType diffType, String lineContent) {
            this.lineNum = lineNum;
            this.diffType = diffType;
            this.lineContent = lineContent;
        }

        public LineDiffResult(int lineNum, DiffType diffType, String lineContent, LinkedList<GoogleDiffUtils.Diff> partialMatch) {
            this(lineNum, diffType, lineContent);
            this.partialMatch = partialMatch;
        }

        public int getLineNum() {
            return lineNum;
        }

        public DiffType getDiffType() {
            return diffType;
        }

        public String getLineContent() {
            return lineContent;
        }

        public LinkedList<GoogleDiffUtils.Diff> getPartialMatch() {
            return partialMatch;
        }
    }

    /**
     * A customized HighlightPainter subclass with an overridden paint method to
     * highlight the area with a width from 0 to the text component's width.
     */
    private static class LineHighlightPainter implements Highlighter.HighlightPainter {
        private Color color;       // the highlight color
        private int caretPosition; // the caret position in the text component

        public LineHighlightPainter(Color color, int caretPosition) {
            this.color = color;
            this.caretPosition = caretPosition;
        }

        @Override
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            try {
                Rectangle rectangle = c.modelToView(caretPosition);
                g.setColor(color);
                g.fillRect(0, rectangle.y, c.getWidth(), rectangle.height);
            } catch (BadLocationException e) {
                logger.warning("The given position does not represent a valid location in the associated document");
            }
        }

        public void setColor(Color color) {
            this.color = color;
        }
    }
}
