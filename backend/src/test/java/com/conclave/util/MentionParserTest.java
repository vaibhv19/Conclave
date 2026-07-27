package com.conclave.util;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class MentionParserTest {

    @Test
    void testExtractMention_AtStart() {
        Optional<String> result = MentionParser.extractMention("@Lead-Writer write the code.");
        assertTrue(result.isPresent());
        assertEquals("Lead-Writer", result.get());
    }

    @Test
    void testExtractMention_InMiddle() {
        Optional<String> result = MentionParser.extractMention("Hey @Lead-Writer write the code.");
        assertTrue(result.isPresent());
        assertEquals("Lead-Writer", result.get());
    }

    @Test
    void testExtractMention_AtEnd() {
        Optional<String> result = MentionParser.extractMention("Hey code writer @Lead-Writer");
        assertTrue(result.isPresent());
        assertEquals("Lead-Writer", result.get());
    }

    @Test
    void testExtractMention_MultipleMentions_ReturnsFirst() {
        Optional<String> result = MentionParser.extractMention("Hey @Lead-Writer and @Fact-Checker, resolve this.");
        assertTrue(result.isPresent());
        assertEquals("Lead-Writer", result.get());
    }

    @Test
    void testExtractMention_NoMention() {
        Optional<String> result = MentionParser.extractMention("Hey everyone, resolve this.");
        assertFalse(result.isPresent());
    }

    @Test
    void testExtractMention_NullOrEmpty() {
        assertFalse(MentionParser.extractMention(null).isPresent());
        assertFalse(MentionParser.extractMention("").isPresent());
        assertFalse(MentionParser.extractMention("   ").isPresent());
    }

    @Test
    void testExtractMention_AlphanumericAndSymbols() {
        Optional<String> result = MentionParser.extractMention("Calling @model_1-assistant_role!");
        assertTrue(result.isPresent());
        assertEquals("model_1-assistant_role", result.get());
    }
}
