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
package com.kstenschke.shifter.models;

import com.intellij.openapi.editor.Editor;
import com.kstenschke.shifter.models.shiftertypes.StringHtmlEncodable;
import org.jetbrains.annotations.Nullable;

/**
 * Shiftable Line
 * Shifting strategy:
 * Either line contains one word of a clearly detectable type  => shifting will transform that word.
 * Or line context can be detected                             => resp. shifting be done.
 */
public class ShiftableLine {

    private final String line;
    private final CharSequence editorText;
    private final int caretOffset;
    private final String filename;

    /**
     * Constructor
     *
     * @param line        Text of line
     * @param editorText  Full text currently in editor
     * @param caretOffset Caret position in document
     * @param filename    Name of the edited file if any
     */
    public ShiftableLine(String line, CharSequence editorText, int caretOffset, String filename) {
        this.line        = line;
        this.editorText  = editorText;
        this.caretOffset = caretOffset;
        this.filename    = filename;
    }

    /**
     * Get shifted up/down word
     *
     * @param  isUp         Shift up or down?
     * @param  editor       Editor
     * @param  moreCount    Current counter while iterating multi-shift
     * @return String       Next upper/lower word
     */
    public String getShifted(boolean isUp, Editor editor, @Nullable final Integer moreCount) {
        String[] words = this.line.trim().split("\\s+");

        // Check all words for shiftable types - shiftable if there's not more than one
        int amountShiftableWordsInSentence = 0;
        String wordShiftedTest;
        String wordUnshifted = "";
        String wordShifted   = "";
        String prefixChar    = "";
        String postfixChar   = "";

        for (String word : words) {
            if (word.length() > 2) {
                // Check if word is a hex RGB color including the #-prefix
                if (word.startsWith("#")) {
                    prefixChar = "#";
                    word = word.substring(1);
                }

                wordShiftedTest = new ShiftableWord(word, prefixChar, postfixChar, this.line, this.editorText, this.caretOffset, this.filename, moreCount).getShifted(isUp, editor);

                if (wordShiftedTest != null && !wordShiftedTest.equals(word)) {
                    amountShiftableWordsInSentence++;
                    wordUnshifted = word;
                    wordShifted = wordShiftedTest;
                }
            }
        }

        // Actual shifting
        return amountShiftableWordsInSentence == 1
                ? this.line.replace(wordUnshifted, wordShifted)  // Shift detected word in line
                : StringHtmlEncodable.isHtmlEncodable(this.line)
                ? StringHtmlEncodable.getShifted(this.line)  // Encode or decode contained HTML special chars
                : this.line;                                 // No shiftability detected, return original line
    }
}