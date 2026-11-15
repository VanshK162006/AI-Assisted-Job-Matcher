/*
It contains the logic of just matching the jobs to your filters like Location, java,
SDE,etc.
It returns either true or false with reason and based on that reason JobFilterServices
takes action
 */

package com.project.job_finder_ai.service.filteringJobs;

import com.project.job_finder_ai.config.FilterProperties;
import com.project.job_finder_ai.dto.FilterDecision;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JobFilter {

    // finds things like "5+ years", "0-2 years", "3 to 5 yrs"
    private static final Pattern YEARS = Pattern.compile(
            "(?<!\\d)(\\d{1,2})\\s*(?:\\+|(?:-|to)\\s*\\d{1,2})?\\s*(?:years?|yrs?)\\b",
            Pattern.CASE_INSENSITIVE);
    private final int maxYears;
    private final List<Rule> include;
    private final List<Rule> exclude;
    private final List<Rule> india;

    public JobFilter(FilterProperties props) {
        this.maxYears = props.getMaxYears();
        this.include = toRules(props.getIncludeTitleWords());
        this.exclude = toRules(props.getExcludeTitleWords());
        this.india = toRules(props.getIndiaLocationWords());
    }

    private static List<Rule> toRules(List<String> words) {
        return words.stream()
                .filter(w -> w != null && !w.isBlank())
                .map(w -> new Rule(w.trim(), Pattern.compile(
                        "\\b" + Pattern.quote(w.trim()) + "\\b", Pattern.CASE_INSENSITIVE)))
                .toList();
    }

    private static Optional<Rule> firstMatch(List<Rule> rules, String text) {
        if (text == null) return Optional.empty();
        return rules.stream().filter(r -> r.pattern().matcher(text).find()).findFirst();
    }

    public FilterDecision evaluate(String title, String location, String description) {

        if (title == null || title.isBlank()) {
            return FilterDecision.reject("Title is not available!");
        } else if (firstMatch(include, title).isEmpty()) {
            return FilterDecision.reject("No Jobs found based on your title!");
        }

        Optional<Rule> bad = firstMatch(exclude, title);
        if (bad.isPresent()) {
            return FilterDecision.reject("Title has excluded word: " + bad.get().word());
        }
        if (firstMatch(india, location).isEmpty()) {
            return FilterDecision.reject("Location is not in India: " + location);
        }

        Integer years = requiredYears(description);
        if (years != null && years > maxYears) {
            return FilterDecision.reject("Needs " + years + " years (limit " + maxYears + ")");
        }
        return FilterDecision.pass();

    }

    // first "N years" that is mentioned near the word "experience"; null if none
    Integer requiredYears(String text) {
        if (text == null) return null;
        Matcher m = YEARS.matcher(text);
        while (m.find()) {
            int from = Math.max(0, m.start() - 60);
            int to = Math.min(text.length(), m.end() + 60);
            if (text.substring(from, to).toLowerCase().contains("experience")) {
                return Integer.parseInt(m.group(1));
            }
        }
        return null;
    }

    private record Rule(String word, Pattern pattern) {
    }
}
