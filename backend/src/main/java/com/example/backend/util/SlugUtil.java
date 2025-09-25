package com.example.backend.util;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@UtilityClass
public class SlugUtil {
    private static final Pattern NON_ASCII_ALNUM = Pattern.compile("[^A-Za-z0-9]+");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    public static String slugify(String input) {
        return slugify(input, "-", 0, true);
    }

    public static String slugify(String input, int maxLen) {
        return slugify(input, "-", maxLen, true);
    }

    public static String slugify(String input, String separator, int maxLen, boolean toLower) {
        if (input == null || input.isBlank()) return "n-a";
        Objects.requireNonNull(separator, "separator");

        // Common replacements to improve readability
        input = applyCommonReplacements(input);

        // Remove diacritics
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKD);
        String noDiacritics = COMBINING_MARKS.matcher(normalized).replaceAll("");

        // Replace non-alphanumerics with separator
        String sepEscaped = Pattern.quote(separator);
        String slug = NON_ASCII_ALNUM.matcher(noDiacritics).replaceAll(separator);

        // Collapse repeated separators and trim
        slug = slug.replaceAll(sepEscaped + "{2,}", separator);
        slug = trimSeparator(slug, separator);

        if (toLower) slug = slug.toLowerCase();

        if (maxLen > 0 && slug.length() > maxLen) {
            slug = truncateAtWordBoundary(slug, maxLen, separator);
        }

        slug = trimSeparator(slug, separator);
        return slug.isEmpty() ? "n-a" : slug;
    }

    public static String uniqueSlug(String desired, Predicate<String> exists) {
        return uniqueSlug(desired, exists, 0, "-");
    }

    public static String uniqueSlug(String desired, Predicate<String> exists, int maxLen, String separator) {
        String base = slugify(desired, separator, maxLen, true);
        if (!exists.test(base)) return base;

        for (int i = 2; i < Integer.MAX_VALUE; i++) {
            String candidate = appendSuffix(base, i, maxLen, separator);
            if (!exists.test(candidate)) return candidate;
        }
        // Fallback (practice: shouldn't happen)
        return base + separator + System.currentTimeMillis();
    }

    public static String truncateAtWordBoundary(String slug, int maxLen, String separator) {
        if (maxLen <= 0 || slug.length() <= maxLen) return slug;
        String cut = slug.substring(0, maxLen);
        int lastSep = cut.lastIndexOf(separator);
        if (lastSep >= 0 && lastSep >= maxLen / 2) {
            cut = cut.substring(0, lastSep);
        }
        return trimSeparator(cut, separator);
    }

    private static String appendSuffix(String base, int index, int maxLen, String separator) {
        String suffix = separator + index;
        if (maxLen > 0 && base.length() + suffix.length() > maxLen) {
            int cut = Math.max(0, maxLen - suffix.length());
            base = truncateAtWordBoundary(base, cut, separator);
            base = trimSeparator(base, separator);
        }
        return base + suffix;
    }

    private static String trimSeparator(String s, String separator) {
        String q = Pattern.quote(separator);
        return s.replaceAll("^" + q + "+|" + q + "+$", "");
    }

    private static String applyCommonReplacements(String s) {
        Map<String, String> repl = Map.of(
                "&", " and ",
                "@", " at ",
                "%", " percent ",
                "+", " plus ",
                "#", " number "
        );
        String out = s;
        for (Map.Entry<String, String> e : repl.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }
        return out;
    }
}