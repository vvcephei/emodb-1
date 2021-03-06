package com.bazaarvoice.emodb.common.api;

import com.google.common.base.CharMatcher;

public abstract class Names {

    /** Prevent instantiation. */
    private Names() {}

    // Exclude whitespace, control chars, non-ascii, upper-case, most punctuation.
    private static final CharMatcher TABLE_NAME_ALLOWED =
            CharMatcher.inRange('a', 'z')
                    .or(CharMatcher.inRange('0', '9'))
                    .or(CharMatcher.anyOf("-.:_"))
                    .precomputed();

    /**
     * Table names must be lowercase ASCII strings. between 1 and 255 characters in length.  Whitespace, ISO control
     * characters and certain punctuation characters that aren't generally allowed in file names or in elasticsearch
     * index names are excluded (elasticsearch appears to allow: !$%&()+-.:;=@[]^_`{}~).  Table names may not begin
     * with a single underscore to allow URL space for extensions such as "/_table/...".  Table names may not look
     * like relative paths, ie. "." or "..".
     */
    public static boolean isLegalTableName(String table) {
        return table != null &&
                table.length() > 0 && table.length() <= 255 &&
                !(table.charAt(0) == '_' && !table.startsWith("__")) &&
                !(table.charAt(0) == '.' && (".".equals(table) || "..".equals(table))) &&
                TABLE_NAME_ALLOWED.matchesAllOf(table);
    }
}
