package com.dehold.contentmanager.content.blogpost.export;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

/**
 * Convert an ExportResponse into an XML byte array.
 * <p>
 * Produces:
 * <export>
 * <blogPosts>
 * <blogPost>...</blogPost>
 * </blogPosts>
 * <supportRequests>...</supportRequests>
 * <supportResponses>...</supportResponses>
 * </export>
 * <p>
 * Simple string-based serializer to avoid additional dependencies.
 */
public final class ExportXmlConverter {

    private ExportXmlConverter() {
    }

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static byte[] toXmlBytes(ExportResponse resp) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<export>\n");

        // BlogPosts
        sb.append("  <blogPosts>\n");
        for (BlogPost b : Optional.ofNullable(resp.getBlogPosts()).orElse(Collections.emptyList())) {
            sb.append("    <blogPost>\n");
            appendXmlElement(sb, "id", safe(b.getId() == null ? "" : b.getId().toString()));
            appendXmlElement(sb, "title", safe(b.getTitle()));
            appendXmlElement(sb, "content", safe(b.getContent()));
            appendXmlElement(sb, "createdAt", formatInstant(b.getCreatedAt()));
            appendXmlElement(sb, "updatedAt", formatInstant(b.getUpdatedAt()));
            appendXmlElement(sb, "userId", safe(b.getUserId() == null ? "" : b.getUserId().toString()));
            sb.append("    </blogPost>\n");
        }
        sb.append("  </blogPosts>\n");

        // SupportRequests
        sb.append("  <supportRequests>\n");
        for (SupportRequest r : Optional.ofNullable(resp.getSupportRequests()).orElse(Collections.emptyList())) {
            sb.append("    <supportRequest>\n");
            appendXmlElement(sb, "id", safe(r.getId() == null ? "" : r.getId().toString()));
            appendXmlElement(sb, "userId", safe(r.getUserId() == null ? "" : r.getUserId().toString()));
            appendXmlElement(sb, "text", safe(r.getText()));
            appendXmlElement(sb, "customerId", safe(r.getCustomerId() == null ? "" : r.getCustomerId().toString()));
            appendXmlElement(sb, "createdAt", formatInstant(r.getCreatedAt()));
            appendXmlElement(sb, "updatedAt", formatInstant(r.getUpdatedAt()));
            sb.append("    </supportRequest>\n");
        }
        sb.append("  </supportRequests>\n");

        // SupportResponses
        sb.append("  <supportResponses>\n");
        for (SupportResponse s : Optional.ofNullable(resp.getSupportResponses()).orElse(Collections.emptyList())) {
            sb.append("    <supportResponse>\n");
            appendXmlElement(sb, "id", safe(s.getId() == null ? "" : s.getId().toString()));
            appendXmlElement(sb, "supportRequestId", safe(s.getSupportRequest() == null ? "" : s.getSupportRequest().toString()));
            appendXmlElement(sb, "userId", safe(s.getUserId() == null ? "" : s.getUserId().toString()));
            appendXmlElement(sb, "text", safe(s.getText()));
            appendXmlElement(sb, "createdAt", formatInstant(s.getCreatedAt()));
            appendXmlElement(sb, "updatedAt", formatInstant(s.getUpdatedAt()));
            sb.append("    </supportResponse>\n");
        }
        sb.append("  </supportResponses>\n");

        sb.append("</export>\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendXmlElement(StringBuilder sb, String name, String value) {
        sb.append("      <").append(name).append(">");
        sb.append(escapeXml(value));
        sb.append("</").append(name).append(">\n");
    }

    private static String formatInstant(java.time.Instant instant) {
        if (instant == null) return "";
        LocalDateTime local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return ISO.format(local);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Basic XML escape for & < > " ' characters.
     */
    private static String escapeXml(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '\"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&apos;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}

