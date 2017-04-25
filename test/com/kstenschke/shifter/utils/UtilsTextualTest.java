package com.kstenschke.shifter.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UtilsTextualTest {

    @Test
    public void testIsAllUppercase() throws Exception {
        assertTrue( UtilsTextual.isAllUppercase("ALL UPPERCASE SENTENCE.") );

        assertFalse( UtilsTextual.isAllUppercase("all lowercase sentence.") );
        assertFalse( UtilsTextual.isAllUppercase("aCamelCasedWord") );
        assertFalse( UtilsTextual.isAllUppercase("A MIXED case Sentence.") );
        assertFalse( UtilsTextual.isAllUppercase("some,words,separated,by,commas,all,words,are,lower-cased") );
    }

    @Test
    public void testIsCommaSeparatedList() throws Exception {
        assertTrue( UtilsTextual.isCommaSeparatedList("some,words,separated,by,commas,all,words,are,lower-cased") );
        assertTrue( UtilsTextual.isCommaSeparatedList("SOME,WORDS,SEPARATED,BY,COMMAS,ALL,WORDS,ARE,UPPERCASED") );
        assertTrue( UtilsTextual.isCommaSeparatedList("some,WORDS,separated,BY,commas,THE,words,ARE,mixed,CASED") );

        assertFalse( UtilsTextual.isCommaSeparatedList("ALL UPPERCASE SENTENCE.") );
        assertFalse( UtilsTextual.isCommaSeparatedList("all lowercase sentence.") );
        assertFalse( UtilsTextual.isCommaSeparatedList("aCamelCasedWord") );
    }

    @Test
    public void testContainsAnySlashes() throws Exception {
        assertTrue( UtilsTextual.containsAnySlashes("http://www.xxx.ch/") );
        assertTrue( UtilsTextual.containsAnySlashes("A single quote is written \\ ' and a backslash \\") );

        assertFalse( UtilsTextual.containsAnySlashes("some,words,separated,by,commas,all,words,are,lower-cased") );
        assertFalse( UtilsTextual.containsAnySlashes("all lowercase sentence") );
        assertFalse( UtilsTextual.containsAnySlashes("ALL UPPERCASE SENTENCE.") );
    }

    @Test
    public void testContainsAnyQuotes() throws Exception {
        assertTrue( UtilsTextual.containsAnyQuotes("\'This sentence is single-quoted\'") );
        assertTrue( UtilsTextual.containsAnyQuotes("\"This sentence is double-quoted\"") );

        assertFalse(UtilsTextual.containsAnyQuotes("all lowercase sentence"));
        assertFalse(UtilsTextual.containsAnyQuotes("aCamelCasedWord"));
        assertFalse(UtilsTextual.containsAnyQuotes("A MIXED case Sentence."));
    }

    @Test
    public void testSwapSlashes() throws Exception {
        assertEquals("http://www.domain.com/", UtilsTextual.swapSlashes("http:\\\\www.domain.com\\"));
        assertEquals("http:\\\\www.domain.com\\", UtilsTextual.swapSlashes("http://www.domain.com/"));
    }

    @Test
    public void testSwapQuotes() throws Exception {
        assertEquals("i say \"bam\"", UtilsTextual.swapQuotes("i say \'bam\'"));
        assertEquals("you say \'hey\'", UtilsTextual.swapQuotes("you say \"hey\""));
        assertEquals("\'hey\"BAM!\"\'", UtilsTextual.swapQuotes("\"hey\'BAM!\'\""));
    }

    @Test
    public void testToUcFirst() throws Exception {
        assertEquals("Bam bam hey", UtilsTextual.toUcFirst("bam bam hey"));
        assertEquals("Bam bam hey", UtilsTextual.toUcFirst("BAM BAM HEY"));
    }

    @Test
    public void testIsUcFirst() throws Exception {
        assertTrue(UtilsTextual.isUcFirst("Bam bam hey"));
        assertTrue(UtilsTextual.isUcFirst(""));

        assertFalse(UtilsTextual.isUcFirst("bam bam hey"));
        assertFalse(UtilsTextual.isUcFirst("BAM BAM HEY"));
    }

    @Test
    public void testGetOperatorAtOffset() throws Exception {
        assertEquals("+", UtilsTextual.getOperatorAtOffset("1 + 2", 2) );
        assertEquals("-", UtilsTextual.getOperatorAtOffset("2 - 1", 2) );
        assertEquals("*", UtilsTextual.getOperatorAtOffset("2 * 1", 2) );
        assertEquals("/", UtilsTextual.getOperatorAtOffset("2 / 1", 2) );
        assertEquals("%", UtilsTextual.getOperatorAtOffset("2 % 1", 2) );
        assertEquals("<", UtilsTextual.getOperatorAtOffset("2 < 1", 2) );
        assertEquals(">", UtilsTextual.getOperatorAtOffset("2 > 1", 2) );

        assertNull("+", UtilsTextual.getOperatorAtOffset("1 + 2", 0) );
        assertNull("-", UtilsTextual.getOperatorAtOffset("2 - 1", 0) );
        assertNull("*", UtilsTextual.getOperatorAtOffset("2 * 1", 0) );
        assertNull("/", UtilsTextual.getOperatorAtOffset("2 / 1", 0) );
        assertNull("%", UtilsTextual.getOperatorAtOffset("2 % 1", 0) );
        assertNull("<", UtilsTextual.getOperatorAtOffset("2 < 1", 0) );
        assertNull(">", UtilsTextual.getOperatorAtOffset("2 > 1", 0) );

        assertNull("+", UtilsTextual.getOperatorAtOffset("1  + 2", 2));
        assertNull("-", UtilsTextual.getOperatorAtOffset("2  - 1", 2));
        assertNull("*", UtilsTextual.getOperatorAtOffset("2  * 1", 2));
        assertNull("/", UtilsTextual.getOperatorAtOffset("2  / 1", 2));
        assertNull("%", UtilsTextual.getOperatorAtOffset("2  % 1", 2));
        assertNull("<", UtilsTextual.getOperatorAtOffset("2  < 1", 2));
        assertNull(">", UtilsTextual.getOperatorAtOffset("2  > 1", 2));
    }

    @Test
    public void testGetStartOfOperatorAtOffset() throws Exception {

    }

    @Test
    public void testGetWordAtOffset() throws Exception {
        assertEquals("bam", UtilsTextual.getWordAtOffset("bam lam, ding, dong, what a bam.", 0));
        assertEquals("lam", UtilsTextual.getWordAtOffset("bam lam, ding, dong, what a bam.", 5));
        assertEquals("ding", UtilsTextual.getWordAtOffset("bam lam, ding, dong, what a bam.", 10));

        assertEquals(null, UtilsTextual.getWordAtOffset("bam lam, ding, dong, what a bam.", -1));
    }

    @Test
    public void testGetSubString() throws Exception {

    }

    @Test
    public void testGetCharBeforeOffset() throws Exception {

    }

    @Test
    public void testGetCharAfterOffset() throws Exception {

    }

    @Test
    public void testGetStartOfWordAtOffset() throws Exception {

    }

    @Test
    public void testExtractLines() throws Exception {

    }

    @Test
    public void testExtractLine() throws Exception {

    }

    @Test
    public void testExtractLineAroundOffset() throws Exception {
        assertEquals("bam bam,", UtilsTextual.extractLineAroundOffset("bam bam,\ney,\nwhat a bam.", 3));
        assertEquals("ey,", UtilsTextual.extractLineAroundOffset("bam bam,\ney,\nwhat a bam.", 10));
        assertEquals("what a bam.", UtilsTextual.extractLineAroundOffset("bam bam,\ney,\nwhat a bam.", 15));
    }

    @Test
    public void testJoinLines() throws Exception {
        List<String> lines = new ArrayList<String>();
        lines.add("what");
        lines.add("a");
        lines.add("bam");

        assertEquals("whatabam", UtilsTextual.joinLines(lines).toString());
    }

    @Test
    public void testRemoveLineBreaks() throws Exception {
        assertEquals("bam bam,ey,what a bam.", UtilsTextual.removeLineBreaks("bam bam,\ney,\nwhat a bam."));
        assertEquals("bam bam,ey,what a bam.", UtilsTextual.removeLineBreaks("bam bam,\rey,\rwhat a bam."));
        assertEquals("bam bam,ey,what a bam.", UtilsTextual.removeLineBreaks("bam bam,\ney,\rwhat a bam."));
        assertEquals("bam bam,ey,what a bam.", UtilsTextual.removeLineBreaks("bam bam,\n\rey,\n\rwhat a bam."));
    }

    @Test
    public void testReplaceLast() throws Exception {
        assertEquals("bam bam, ey, what a bam.", UtilsTextual.replaceLast("bam bam, ey, what a bam bam.", " bam", ""));
        assertEquals("bam bam, ey hey", UtilsTextual.replaceLast("bam bam, ey hey", "x", "y"));
    }

    @Test
    public void testFormatAmountDigits() throws Exception {
        assertEquals(5, UtilsTextual.formatAmountDigits("7", 5).length());
        assertEquals(5, UtilsTextual.formatAmountDigits("55", 5).length());
        assertEquals(5, UtilsTextual.formatAmountDigits("321", 5).length());

        assertEquals(2, UtilsTextual.formatAmountDigits("55", 1).length());
        assertEquals(3, UtilsTextual.formatAmountDigits("321", 1).length());
    }
}