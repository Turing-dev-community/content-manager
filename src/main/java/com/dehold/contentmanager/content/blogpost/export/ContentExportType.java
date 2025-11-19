package com.dehold.contentmanager.content.blogpost.export;


/**
 * Supported export content types for the `contentType` request parameter.
 */
public enum ContentExportType {
    BLOGPOST("blogpost"),
    SUPPORT_REQUEST("supportrequest"),
    SUPPORT_RESPONSE("supportresponse");

    private final String token;

    ContentExportType(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    public static ContentExportType fromStringIgnoreCase(String s) {
        if (s == null) return null;
        String normalized = s.trim().toLowerCase();
        for (ContentExportType t : values()) {
            if (t.token.equals(normalized)) return t;
        }
        return null;
    }
}
