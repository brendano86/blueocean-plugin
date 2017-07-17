package io.jenkins.blueocean.commons;

import com.google.common.base.CharMatcher;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;

import javax.annotation.Nonnull;

/** JSON Utility */
public class JSON {

    /**
     * Sanitises string by removing any ISO control characters, tabs and line breaks
     * @param input string
     * @return sanitized string
     */
    public static String sanitizeString(@Nonnull String input) {
        return CharMatcher.JAVA_ISO_CONTROL.and(CharMatcher.anyOf("\r\n\t")).removeFrom(input);
    }

    /**
     * Safely escapeString JSON string so that any containing slashes are escaped
     * @param input string
     * @return escaped string
     */
    public static String escapeString(@Nonnull String input) {
        return JSONUtils.stripQuotes(JSONUtils.quoteCanonical(input));
    }

    private JSON() {}
}
