package com.dehold.contentmanager.validation.step.utils;


/**
 * Small utility class for regex related helpers used by validators.
 * Kept intentionally simple to avoid introducing extra dependencies.
 */
public final class RegexValidationUtils {

    private RegexValidationUtils() {}

    /**
     * Normalizes a regex for display in error messages by trimming and replacing
     * excessive whitespace. This does NOT alter the matching semantics.
     *
     * @param regex original regex
     * @return normalized string representation
     */
    public static String normalizeForMessage(String regex) {
        if (regex == null) return "";
        return regex.trim().replaceAll("\\s+", " ");

    }
}

