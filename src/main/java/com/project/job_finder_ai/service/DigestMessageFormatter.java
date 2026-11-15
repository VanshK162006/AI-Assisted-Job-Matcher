package com.project.job_finder_ai.service;

import com.project.job_finder_ai.dto.DigestMessage;
import com.project.job_finder_ai.dto.JobMatchView;
import com.project.job_finder_ai.dto.JobView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Component
public class DigestMessageFormatter {

    // Telegram allows 4096 characters; we stay well below to be safe
    private static final int SAFE_LIMIT = 3800;

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escAttr(String s) {
        return esc(s).replace("\"", "&quot;");
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    // ---------- Public API ----------

    public DigestMessage format(List<JobMatchView> matches, long totalPending) {
        return build(matches, totalPending,
                this::entry, JobMatchView::id,
                "new job match", "new job matches",
                "See /api/matches");
    }

    public DigestMessage formatJobs(List<JobView> jobs, long totalPending) {
        return build(jobs, totalPending,
                this::entry, JobView::id,
                "new job", "new jobs",
                "See /api/jobs");   // adjust to your real endpoint
    }

    // ---------- Shared digest builder ----------

    private <T> DigestMessage build(List<T> items,
                                    long totalPending,
                                    BiFunction<Integer, T, String> entryFn,
                                    Function<T, Long> idFn,
                                    String singularLabel,
                                    String pluralLabel,
                                    String moreHint) {
        StringBuilder entries = new StringBuilder();
        List<Long> included = new ArrayList<>();

        int number = 1;
        for (T item : items) {
            String entry = entryFn.apply(number, item);
            if (!included.isEmpty() && entries.length() + entry.length() > SAFE_LIMIT) {
                break;
            }
            entries.append(entry);
            included.add(idFn.apply(item));
            number++;
        }

        StringBuilder text = new StringBuilder();
        text.append("<b>🎯 ").append(included.size()).append(" ")
                .append(included.size() == 1 ? singularLabel : pluralLabel)
                .append("</b>\n\n");
        text.append(entries);

        long waiting = Math.max(0, totalPending - included.size());
        if (waiting > 0) {
            text.append("… and ").append(waiting).append(" more waiting. ").append(moreHint);
        }
        return new DigestMessage(text.toString().trim(), included);
    }

    // ---------- Entry renderers ----------

    /*
        1. <b>92</b> · Java Developer
           Razorpay · Pune, India
           Missing: Kafka
           <a href="https://...">Apply</a>
     */
    private String entry(int number, JobMatchView m) {
        StringBuilder sb = new StringBuilder();
        sb.append(number).append(". <b>").append(m.score()).append("</b> · ")
                .append(esc(cut(m.title(), 100))).append("\n");

        appendCompanyLine(sb, m.company(), m.location());

        if (m.missingSkills() != null && !m.missingSkills().isEmpty()) {
            sb.append("   Missing: ")
                    .append(esc(cut(String.join(", ", m.missingSkills()), 80))).append("\n");
        }

        appendApplyLink(sb, m.applyUrl());
        sb.append("\n");
        return sb.toString();
    }

    /*
        1. <b>Java Developer</b>
           Razorpay · Pune, India
           Department: Engineering
           <a href="https://...">Apply</a>
     */
    private String entry(int number, JobView j) {
        StringBuilder sb = new StringBuilder();
        sb.append(number).append(". <b>")
                .append(esc(cut(j.title(), 100))).append("</b>\n");

        appendCompanyLine(sb, j.company(), j.location());

        if (j.department() != null && !j.department().isBlank()) {
            sb.append("   Department: ")
                    .append(esc(cut(j.department(), 60))).append("\n");
        }

        appendApplyLink(sb, j.applyUrl());
        sb.append("\n");
        return sb.toString();
    }

    // ---------- Shared entry pieces ----------

    private void appendCompanyLine(StringBuilder sb, String company, String location) {
        sb.append("   ").append(esc(cut(company, 60)));
        if (location != null && !location.isBlank()) {
            sb.append(" · ").append(esc(cut(location, 60)));
        }
        sb.append("\n");
    }

    private void appendApplyLink(StringBuilder sb, String url) {
        if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
            sb.append("   <a href=\"").append(escAttr(url)).append("\">Apply</a>\n");
        }
    }
}