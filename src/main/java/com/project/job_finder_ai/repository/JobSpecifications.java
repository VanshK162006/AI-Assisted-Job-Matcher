package com.project.job_finder_ai.repository;

import com.project.job_finder_ai.entity.Job;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class JobSpecifications {

    public static final List<String> INDIA_WORDS = List.of(
        "india", "bengaluru", "bangalore", "hyderabad", "pune", "mumbai",
        "chennai", "gurgaon", "gurugram", "noida", "delhi", "kolkata", "ahmedabad");

    private JobSpecifications() {}

    public static Specification<Job> active() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Job> company(Long companyId) {
        return (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
    }

    public static Specification<Job> titleContainsAny(List<String> words) {
        return containsAny("title", words);
    }

    public static Specification<Job> locationContainsAny(List<String> words) {
        return containsAny("location", words);
    }

    public static Specification<Job> departmentContains(String text) {
        return containsAny("department", List.of(text));
    }

    private static Specification<Job> containsAny(String field, List<String> words) {
        return (root, query, cb) -> cb.or(
                words.stream()
            .map(w -> cb.like(cb.lower(root.get(field)), "%" + w.trim().toLowerCase() + "%"))
            .toList());
    }

    public static Specification<Job> passedFilter() {
        return (root, query, cb) -> cb.isTrue(root.get("passedFilter"));
    }
}