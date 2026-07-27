package com.conclave.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse role mentions from conversation messages.
 */
public class MentionParser {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9\\-_]+)");

    /**
     * Extracts the first occurrence of an '@' mention from the message content.
     * (e.g., "@Lead-Writer hello" -> "Lead-Writer").
     *
     * @param content The message content to parse
     * @return An Optional containing the role name, or Optional.empty() if not found
     */
    public static Optional<String> extractMention(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }

        return Optional.empty();
    }
}
