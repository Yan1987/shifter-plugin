/*
 * Copyright 2011-2017 Kay Stenschke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kstenschke.shifter.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.*;
import com.kstenschke.shifter.ShifterPreferences;
import com.kstenschke.shifter.models.*;
import com.kstenschke.shifter.models.shiftertypes.*;
import com.kstenschke.shifter.resources.StaticTexts;
import com.kstenschke.shifter.resources.forms.DialogNumericBlockOptions;
import com.kstenschke.shifter.utils.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;

class ActionsPerformer {

    private final Editor editor;
    private Document document;
    private CharSequence editorText;
    private int caretOffset;
    private SelectionModel selectionModel;
    private boolean hasSelection;

    private String filename = null;

    private int[] blockSelectionStarts;
    private int[] blockSelectionEnds;

    /**
     * Constructor
     */
    ActionsPerformer(final AnActionEvent event) {
        this.editor = event.getData(PlatformDataKeys.EDITOR);

        if (this.editor != null) {
            this.document       = this.editor.getDocument();
            this.filename       = this.getFilename();
            this.editorText     = this.document.getCharsSequence();
            this.caretOffset    = this.editor.getCaretModel().getOffset();
            this.selectionModel = this.editor.getSelectionModel();
            this.hasSelection   = this.selectionModel.hasSelection();
        }
    }

    /**
     * Find shiftable string (selection block/lines/regular, word at caret, line at caret) and replace it by its shifted value
     *
     * @param shiftUp   Shift up or down?
     * @param moreCount Current "more" count, starting w/ 1. If non-more shift: null
     */
    public void write(boolean shiftUp, @Nullable Integer moreCount) {
        if (this.editor != null) {
            if (this.hasSelection) {
                if (this.selectionModel.getBlockSelectionStarts().length > 1) {
                    // Shift block selection: do word-shifting if all items are identical
                    shiftBlockSelection(shiftUp, moreCount);
                } else {
                    // Shift regular selection: sort CSV or multi-line selection: sort lines alphabetically
                    shiftSelection(shiftUp, moreCount);
                }
            } else {
                // Shift word at caret
                int lineNumber = document.getLineNumber(caretOffset);
                int offsetLineStart = document.getLineStartOffset(lineNumber);
                int offsetLineEnd = document.getLineEndOffset(lineNumber);

                String line = editorText.subSequence(offsetLineStart, offsetLineEnd).toString();

                boolean isWordShifted = shiftWordAtCaret(shiftUp, this.filename, line, moreCount);

                // Word at caret wasn't identified/shifted, try shifting the whole line
                if (!isWordShifted) {
                    shiftLine(shiftUp, this.filename, offsetLineStart, line, moreCount);
                }
            }
        }
    }

    /**
     * @param shiftUp
     * @param filename
     * @param offsetLineStart
     * @param line
     * @param moreCount       Current "more" count, starting w/ 1. If non-more shift: null
     */
    private void shiftLine(boolean shiftUp, String filename, int offsetLineStart, String line, @Nullable Integer moreCount) {
        ShiftableLine shiftableLine = new ShiftableLine(line, editorText, caretOffset, filename);

        // Replace line by shifted one
        CharSequence shiftedLine = shiftableLine.getShifted(shiftUp, editor, moreCount);
        if (shiftedLine != null) {
            document.replaceString(offsetLineStart, offsetLineStart + line.length(), shiftedLine);
        }
    }

    /**
     * Get shifted word (and replace if possible)
     *
     * @param shiftUp
     * @param filename
     * @param line
     * @param moreCount Current "more" count, starting w/ 1. If non-more shift: null
     * @return boolean
     */
    private boolean shiftWordAtCaret(boolean shiftUp, String filename, String line, @Nullable Integer moreCount) {
        String word;
        boolean isOperator = false;

        String fileExtension = UtilsFile.extractFileExtension(filename, true);
        // @todo make hyphen-inclusion configurable in dictionary per file type
        boolean isCSS = fileExtension != null && fileExtension.endsWith("css");

        word = UtilsTextual.getOperatorAtOffset(editorText, caretOffset);
        if (word == null) {
            word = UtilsTextual.getWordAtOffset(editorText, caretOffset, isCSS);
        } else {
            isOperator = true;
        }

        boolean isWordShifted = false;
        if (word != null && !word.isEmpty()) {
            isWordShifted = !this.getShiftedWord(shiftUp, filename, word, line, null, true, isOperator, moreCount).equals(word);

            if (!isWordShifted) {
                // Shifting failed, try shifting lower-cased string
                String wordLower = word.toLowerCase();
                isWordShifted = !this.getShiftedWord(shiftUp, filename, wordLower, line, null, true, false, moreCount).equals(wordLower);
            }
        }

        return isWordShifted;
    }

    /**
     * @return String  filename of currently edited document
     */
    private String getFilename() {
        if (this.filename == null && this.document != null) {
            this.filename = UtilsEnvironment.getDocumentFilename(this.document);
        }

        return this.filename;
    }

    /**
     * @param shiftUp           shiftUp   ...or down?
     * @param filename
     * @param word
     * @param line
     * @param wordOffset        null = calculate from word at offset
     * @param replaceInDocument
     * @param isOperator
     * @param moreCount         current "more" count, starting w/ 1. If non-more shift: null
     * @return String           resulting shifted or original word if no shiftability was found
     */
    private String getShiftedWord(
            boolean shiftUp,
            String filename, String word, String line, @Nullable Integer wordOffset,
            Boolean replaceInDocument,
            boolean isOperator,
            @Nullable Integer moreCount
    ) {
        boolean wasWordShifted = false;

        if (wordOffset == null) {
            // Extract offset of word at caret
            wordOffset = isOperator
                    ? UtilsTextual.getStartOfOperatorAtOffset(this.editorText, this.caretOffset)
                    : UtilsTextual.getStartOfWordAtOffset(this.editorText, this.caretOffset);
        }

        String prefixChar  = UtilsTextual.getCharBeforeOffset(this.editorText, wordOffset);
        String postfixChar = UtilsTextual.getCharAfterOffset(this.editorText, wordOffset + word.length() - 1);

        // Identify word type and shift it accordingly
        ShiftableWord shiftableWord = new ShiftableWord(word, prefixChar, postfixChar, line, this.editorText, this.caretOffset, filename, moreCount);

        if (!isOperator && (NumericValue.isNumericValue(word) || CssUnit.isCssUnitValue(word)) && "-".equals(prefixChar)) {
            word = "-" + word;
            wordOffset--;
        }

        String newWord = shiftableWord.getShifted(shiftUp, editor);

        if (newWord != null && newWord.length() > 0 && !newWord.matches(Pattern.quote(word)) && wordOffset != null) {
            newWord = shiftableWord.postProcess(newWord, postfixChar);

            if (replaceInDocument) {
                // Replace word at caret by shifted one (if any)
                document.replaceString(wordOffset, wordOffset + word.length(), newWord);
            }
            wasWordShifted = true;
        }

        return wasWordShifted ? newWord : word;
    }

    /**
     * @param shiftUp
     * @param moreCount Current "more" count, starting w/ 1. If non-more shift: null
     */
    private void shiftBlockSelection(boolean shiftUp, @Nullable Integer moreCount) {
        blockSelectionStarts = this.selectionModel.getBlockSelectionStarts();
        blockSelectionEnds   = this.selectionModel.getBlockSelectionEnds();

        if (ShiftableBlock.areNumericValues(blockSelectionStarts, blockSelectionEnds, editorText)) {
            // Block selection of numeric values: ask whether to 1. replace by enumeration or 2. in/decrement each
            Integer firstNumber = Integer.valueOf(editorText.subSequence(blockSelectionStarts[0], blockSelectionEnds[0]).toString());
            if (null == firstNumber) {
                firstNumber = 0;
            }
            DialogNumericBlockOptions optionsDialog = new DialogNumericBlockOptions(firstNumber);
            UtilsEnvironment.setDialogVisible(editor, ShifterPreferences.ID_DIALOG_NUMERIC_BLOCK_OPTIONS, optionsDialog, StaticTexts.DIALOG_TITLE_NUMERIC_BLOCK_OPTIONS);
            if (!optionsDialog.wasCancelled()) {
                if (optionsDialog.isShiftModeEnumerate()) {
                    this.insertBlockEnumeration(optionsDialog.getFirstNumber());
                } else {
                    this.inOrDecrementNumericBlock(shiftUp);
                }
            }
        } else if (ShiftableBlock.areBlockItemsIdentical(blockSelectionStarts, blockSelectionEnds, editorText)) {
            String word = editorText.subSequence(blockSelectionStarts[0], blockSelectionEnds[0]).toString();
            String line = UtilsTextual.extractLine(this.document, this.document.getLineNumber(blockSelectionStarts[0])).trim();
            Integer wordOffset = UtilsTextual.getStartOfWordAtOffset(this.editorText, blockSelectionStarts[0]);
            String newWord = this.getShiftedWord(shiftUp, this.filename, word, line, wordOffset, false, false, moreCount);

            if (newWord != null && !newWord.equals(word)) {
                for (int i = blockSelectionEnds.length - 1; i >= 0; i--) {
                    document.replaceString(blockSelectionStarts[i], blockSelectionEnds[i], newWord);
                }
            }
        }
    }

    /**
     * Replace given block selection w/ enumeration starting w/ given value
     *
     * @param firstNumber
     */
    private void insertBlockEnumeration(String firstNumber) {
        Integer currentValue = Integer.valueOf(firstNumber);
        if (null == currentValue) {
            currentValue = 0;
        }

        List<CaretState> caretsAndSelections = editor.getCaretModel().getCaretsAndSelections();
        CaretState caretsAndSelection;
        LogicalPosition selectionStart, selectionEnd;
        int offsetSelectionStart, offsetSelectionEnd;

        for (CaretState caretsAndSelectionCurrent : caretsAndSelections) {
            caretsAndSelection = caretsAndSelectionCurrent;
            selectionStart = caretsAndSelection.getSelectionStart();
            selectionEnd = caretsAndSelection.getSelectionEnd();
            if (selectionStart != null && selectionEnd != null) {
                offsetSelectionStart = editor.logicalPositionToOffset(selectionStart);
                offsetSelectionEnd = editor.logicalPositionToOffset(selectionEnd);

                document.replaceString(offsetSelectionStart, offsetSelectionEnd, currentValue.toString());

                currentValue++;
            }
        }
    }

    /**
     * Increment or decrement each item in given numeric block selection
     *
     * @param shiftUp
     */
    private void inOrDecrementNumericBlock(boolean shiftUp) {
        int addend = shiftUp ? 1 : -1;
        Integer value;

        List<CaretState> caretsAndSelections = editor.getCaretModel().getCaretsAndSelections();
        CaretState caretsAndSelection;
        LogicalPosition selectionStart, selectionEnd;
        int offsetSelectionStart, offsetSelectionEnd;

        for (CaretState caretsAndSelectionCurrent : caretsAndSelections) {
            value = null;
            caretsAndSelection = caretsAndSelectionCurrent;
            selectionStart = caretsAndSelection.getSelectionStart();
            selectionEnd = caretsAndSelection.getSelectionEnd();
            if (selectionStart != null && selectionEnd != null) {
                offsetSelectionStart = editor.logicalPositionToOffset(selectionStart);
                offsetSelectionEnd = editor.logicalPositionToOffset(selectionEnd);

                try {
                    value = Integer.valueOf(editorText.subSequence(offsetSelectionStart, offsetSelectionEnd).toString());
                } catch (NumberFormatException e) {
                    // silently continue
                }
                if (null == value) {
                    value = 0;
                }

                document.replaceString(offsetSelectionStart, offsetSelectionEnd, String.valueOf(value + addend));
            }
        }
    }

    /**
     * @param isUp      Are we shifting up or down?
     * @param moreCount Current "more" count, starting w/ 1. If non-more shift: null
     */
    private void shiftSelection(boolean isUp, @Nullable Integer moreCount) {
        int offsetStart = selectionModel.getSelectionStart();
        int offsetEnd = selectionModel.getSelectionEnd();

        String selectedText = UtilsTextual.getSubString(editorText, offsetStart, offsetEnd);

        if (selectedText == null || selectedText.trim().isEmpty()) {
            return;
        }

        int lineNumberSelStart = document.getLineNumber(offsetStart);
        int lineNumberSelEnd = document.getLineNumber(offsetEnd);

        if (document.getLineStartOffset(lineNumberSelEnd) == offsetEnd) {
            lineNumberSelEnd--;
        }

        ShifterTypesManager shifterTypesManager = new ShifterTypesManager();

        int wordType = shifterTypesManager.getWordType(selectedText, editorText, offsetStart, filename);

        boolean isPhpVariable        = wordType == ShifterTypesManager.TYPE_PHP_VARIABLE;
        boolean isJsVarsDeclarations = !isPhpVariable && wordType == ShifterTypesManager.TYPE_JS_VARIABLES_DECLARATIONS;
        boolean isSizzleSelector     = !isPhpVariable && !isJsVarsDeclarations && wordType == ShifterTypesManager.TYPE_SIZZLE_SELECTOR;

        // @todo refactor logical branches (cleanup flow, extract sub sections into private methods)
        if (!isJsVarsDeclarations && ((lineNumberSelEnd - lineNumberSelStart) > 1 && !isPhpVariable)) {
            // Selection is multi-lined: sort lines alphabetical
            List<String> lines = UtilsTextual.extractLines(document, lineNumberSelStart, lineNumberSelEnd);
            sortLinesInDocument(isUp, lineNumberSelStart, lineNumberSelEnd, lines);
        } else {
            if (isJsVarsDeclarations) {
                document.replaceString(offsetStart, offsetEnd, JsVariablesDeclarations.getShifted(selectedText));
            } else {
                if (isSizzleSelector) {
                    document.replaceString(offsetStart, offsetEnd, SizzleSelector.getShifted(selectedText));
                } else {
                    // Selection within one line, or PHP array definition over 1 or more lines
                    if (!isPhpVariable && UtilsTextual.isCommaSeparatedList(selectedText)) {
                        String sortedList = this.sortCommaSeparatedList(selectedText, isUp);
                        document.replaceString(offsetStart, offsetEnd, sortedList);
                    } else {
                        boolean isDone = false;
                        if (!isPhpVariable && UtilsFile.isPhpFile(this.filename)) {
                            PhpConcatenation phpConcatenation = new PhpConcatenation(selectedText);
                            if (phpConcatenation.isPhpConcatenation()) {
                                isDone = true;
                                document.replaceString(offsetStart, offsetEnd, phpConcatenation.getShifted());
                            }
                        }

                        if (!isDone) {
                            if (TernaryExpression.isTernaryExpression(selectedText, "")) {
                                document.replaceString(offsetStart, offsetEnd, TernaryExpression.getShifted(selectedText));
                                isDone = true;
                            } else if (wordType == ShifterTypesManager.TYPE_TRAILING_COMMENT) {
                                int offsetStartCaretLine = document.getLineStartOffset(lineNumberSelStart);
                                int offsetEndCaretLine   = document.getLineEndOffset(lineNumberSelStart);
                                String leadingWhitespace = UtilsTextual.getLeadingWhitespace(editorText.subSequence(offsetStartCaretLine, offsetEndCaretLine).toString());
                                String caretLine         = editorText.subSequence(offsetStartCaretLine, offsetEndCaretLine).toString();

                                document.replaceString(offsetStartCaretLine, offsetEndCaretLine, TrailingComment.getShifted(caretLine, leadingWhitespace));
                                isDone = true;
                            } else if (!isPhpVariable) {
                                if (UtilsTextual.containsAnyQuotes(selectedText)) {
                                    document.replaceString(offsetStart, offsetEnd, UtilsTextual.swapQuotes(selectedText));
                                    isDone = true;
                                } else if (UtilsTextual.containsAnySlashes(selectedText)) {
                                    document.replaceString(offsetStart, offsetEnd, UtilsTextual.swapSlashes(selectedText));
                                    isDone = true;
                                } else if (LogicalOperator.isLogicalOperator(selectedText)) {
                                    document.replaceString(offsetStart, offsetEnd, LogicalOperator.getShifted(selectedText));
                                    isDone = true;
                                } else if (StringHtmlEncodable.isHtmlEncodable(selectedText)) {
                                    document.replaceString(offsetStart, offsetEnd, StringHtmlEncodable.getShifted(selectedText));
                                    isDone = true;
                                }
                            }

                            if (!isDone) {
                                // Detect and shift various types
                                String shiftedWord = shifterTypesManager.getShiftedWord(selectedText, isUp, editorText, caretOffset, moreCount, filename, editor);

                                if (!isPhpVariable) {
                                    // @todo extract this and its redundancy in ShiftableWord
                                    if (UtilsTextual.isAllUppercase(selectedText)) {
                                        shiftedWord = shiftedWord.toUpperCase();

                                    } else if (UtilsTextual.isCamelCase(selectedText) || UtilsTextual.isUcFirst(selectedText)) {
                                        // @todo    check is there a way to implement a toCamelCase conversion?
                                        shiftedWord = UtilsTextual.toUcFirst(shiftedWord);
                                    }
                                }

                                document.replaceString(offsetStart, offsetEnd, shiftedWord);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Sort lines in document alphabetically ascending / descending
     *
     * @param shiftUp
     * @param lineNumberSelStart
     * @param lineNumberSelEnd
     * @param lines
     */
    private void sortLinesInDocument(boolean shiftUp, int lineNumberSelStart, int lineNumberSelEnd, List<String> lines) {
        StringBuilder sortedText = UtilsTextual.joinLines(this.sortLines(lines, shiftUp));
        String sortedTextStr = sortedText.toString();

        boolean hasDuplicates = UtilsTextual.hasDuplicateLines(sortedTextStr);
        if (hasDuplicates) {
            int reply = JOptionPane.showConfirmDialog(null, "Duplicated lines detected. Reduce to single occurrences?", "Reduce duplicate lines?", JOptionPane.OK_CANCEL_OPTION);
            if (reply == JOptionPane.OK_OPTION) {
                sortedTextStr = UtilsTextual.reduceDuplicateLines(sortedTextStr);
            }
        }

        int offsetLineStart = document.getLineStartOffset(lineNumberSelStart);
        int offsetLineEnd = document.getLineEndOffset(lineNumberSelEnd) + document.getLineSeparatorLength(lineNumberSelEnd);

        document.replaceString(offsetLineStart, offsetLineEnd, sortedTextStr);
    }

    /**
     * @param lines
     * @param shiftUp
     * @return Given lines sorted alphabetically ascending / descending
     */
    private List<String> sortLines(List<String> lines, boolean shiftUp) {
        UtilsLinesList.DelimiterDetector delimiterDetector = new UtilsLinesList.DelimiterDetector(lines);

        if (ShifterPreferences.getSortingMode().equals(ShifterPreferences.SORTING_MODE_CASE_INSENSITIVE)) {
            Collections.sort(lines, String.CASE_INSENSITIVE_ORDER);
        } else {
            Collections.sort(lines);
        }

        if (!shiftUp) {
            Collections.reverse(lines);
        }

        if (delimiterDetector.isFoundDelimiter()) {
            // Maintain detected lines delimiter
            lines = UtilsLinesList.addDelimiter(lines, delimiterDetector.getCommonDelimiter(), delimiterDetector.isDelimitedLastLine());
        }

        return lines;
    }

    /**
     * @param selectedText
     * @param shiftUp
     * @return Given comma separated list, sorted alphabetically ascending / descending
     */
    private String sortCommaSeparatedList(String selectedText, boolean shiftUp) {
        String[] items = selectedText.split(",(\\s)*");

        if (ShifterPreferences.getSortingMode().equals(ShifterPreferences.SORTING_MODE_CASE_INSENSITIVE)) {
            Arrays.sort(items, String.CASE_INSENSITIVE_ORDER);
        } else {
            Arrays.sort(items);
        }

        if (!shiftUp) {
            Collections.reverse(Arrays.asList(items));
        }

        return UtilsArray.implode(items, ", ");
    }

}
