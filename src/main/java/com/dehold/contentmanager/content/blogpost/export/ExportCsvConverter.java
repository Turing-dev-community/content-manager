package com.dehold.contentmanager.content.blogpost.export;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * CSV converter for the combined ExportResponse (BlogPosts, SupportRequests, SupportResponses).
 * Produces three labeled sections with header rows. Values are quoted and internal quotes doubled.
 */
public final class ExportCsvConverter {

    private ExportCsvConverter() {}

    public static byte[] toCsvBytes(ExportResponse resp) {
        StringBuilder sb = new StringBuilder();

        // BlogPosts section
        sb.append("# BlogPosts\n");
        sb.append(csvHeader(List.of("id","title","content","createdAt","updatedAt","userId")));
        for (BlogPost b : Optional.ofNullable(resp.getBlogPosts()).orElse(Collections.emptyList())) {
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

        // SupportRequests section
        sb.append("# SupportRequests\n");
        sb.append(csvHeader(List.of("id","userId","text","customerId","createdAt","updatedAt")));
        for (SupportRequest r : Optional.ofNullable(resp.getSupportRequests()).orElse(Collections.emptyList())) {
            sb.append(csvLine(List.of(
                    safe(r.getId() == null ? "" : r.getId().toString()),
                    safe(r.getUserId() == null ? "" : r.getUserId().toString()),
                    safe(r.getText()),
                    safe(r.getCustomerId() == null ? "" : r.getCustomerId().toString()),
                    safe(r.getCreatedAt() == null ? "" : r.getCreatedAt().toString()),
                    safe(r.getUpdatedAt() == null ? "" : r.getUpdatedAt().toString())
            )));
        }
        sb.append('\n');

        // SupportResponses section
        sb.append("# SupportResponses\n");
        sb.append(csvHeader(List.of("id","supportRequestId","userId","text","createdAt","updatedAt")));

        for (SupportResponse s : Optional.ofNullable(resp.getSupportResponses()).orElse(Collections.emptyList())) {
            sb.append(csvLine(List.of(
                    safe(s.getId() == null ? "" : s.getId().toString()),
                    safe(s.getSupportRequest() == null ? "" : s.getSupportRequest().toString()),
                    safe(s.getUserId() == null ? "" : s.getUserId().toString()),
                    safe(s.getText()),
                    safe(s.getCreatedAt() == null ? "" : s.getCreatedAt().toString()),
                    safe(s.getUpdatedAt() == null ? "" : s.getUpdatedAt().toString())
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
        for (String c : cols) sj.add(quote(c));
        return sj.toString();
    }

    private static String quote(String s) {
        if (s == null) s = "";
        String escaped = s.replace("\"", "\"\""); // escape double quotes by doubling
        return "\"" + escaped + "\"";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}