package com.dehold.contentmanager.content.blogpost.export;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;

/**
 * Simple CSV converter for ExportResponse. Produces a concatenated CSV with sections for each content type.
 * This is intentionally simple and avoids external CSV libraries.
 */
public final class ExportCsvConverter {

    private ExportCsvConverter() {}

    public static byte[] toCsvBytes(List<BlogPost> resp) {
        StringBuilder sb = new StringBuilder();

        // Blog posts section
        sb.append(csvHeader(List.of("id", "title", "content", "createdAt", "updatedAt", "userId")));
        for (BlogPost b : resp) {
            sb.append(csvLine(List.of(
                    safe(b.getId() == null ? "" : b.getId().toString()),
                    safe(b.getTitle()),
                    safe(b.getContent()),
                    safe(b.getCreatedAt() == null ? "" : b.getCreatedAt().toString()),
                    safe(b.getUpdatedAt() == null ? "" : b.getUpdatedAt().toString()),
                    safe(b.getUserId() == null ? "" : b.getUserId().toString())
            )));
        }
        sb.append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvHeader(List<String> cols) {
        return csvLine(cols);
    }

    private static String csvLine(List<String> cols) {
        StringJoiner sj = new StringJoiner(",", "", "\n");
        for (String c : cols) {
            sj.add(quote(c));
        }
        return sj.toString();
    }

    private static String quote(String s) {
        if (s == null) s = "";
        String escaped = s.replace("\"", "\"\""); // double quotes
        return "\"" + escaped + "\"";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

