/*
It simply contains logic of searching the jobs based on the filter you applied.
It has direct call with repository.
 */

package com.project.job_finder_ai.service;

import com.project.job_finder_ai.dto.JobView;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.repository.JobRepository;
import com.project.job_finder_ai.repository.JobSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h1> How do we search jobs from our database? </h1>
 * <p>
 * Our fetcher classes like GreenHouseFetcher, LeverFetcher, AshbyFetcher, etc. bring the
 * jobs from the Ats store of a company and those jobs are saved in our database. But
 * saving them is only half of the work. The user also wants to see them, for example
 * "show me latest backend jobs of openai in India".
 * <p>
 * So we made a JobServices class which work is just to read the saved jobs from database
 * and give them back to the user in the form of a list of JobView. The user can send any
 * filter (company, keywords, location, department, india only) and all of them are
 * optional. We only apply those filters which the user has really sent.
 * <p>
 * To do this without writing a separate repository method for every combination of filters,
 * we are using Specification. It is explained in simple language in the SPECIFICATION GUIDE
 * comment above this class, read that first if you want to add any new filter.
 *
 * <h2> Annotations used on this class </h2>
 * <ul>
 *   <li><b>@Service</b> tells Spring that this is a service class, so Spring creates its object
 *       by itself and gives it to whoever needs it (for example a controller).</li>
 *   <li><b>@RequiredArgsConstructor</b> is from Lombok. It writes the constructor for us for
 *       all the final fields. Here it makes JobServices(JobRepository jobRepository), and
 *       Spring uses that constructor to give us the JobRepository object. So we don't have
 *       to write the constructor and use @Autowired ourselves.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class JobServices {

    /**
     * This is the object which talks to the database for the Job table. We only use its
     * findAll(spec, page) method here, which takes our Specification (what to filter)
     * and Pageable (how many and in which order) and gives back the matching jobs.
     */
    private final JobRepository jobRepository;

    /**
     * <h2> How does the search method work? </h2>
     * <p>
     * This is the method which other classes (like a controller) call when the user wants
     * to see the jobs. The user can send many filters but none of them is compulsory. The
     * method starts with a basic filter of "only active jobs" and then adds one filter at
     * a time, only if that filter has a real value. At the end all the filters work together
     * with AND, so a job is returned only if it matches every filter which the user sent.
     * <p>
     * Working step by step:
     * <ol>
     *   <li><b>Start:</b> create the base Specification with JobSpecifications.active() so that
     *       closed jobs are never shown.</li>
     *   <li><b>Company filter:</b> if companyId is not null, add the filter for that company only.
     *       (e.g. only openai jobs)</li>
     *   <li><b>Keywords filter:</b> if the keywords list is not null and not empty, add the filter
     *       that the job title must contain ANY ONE of the keywords. (e.g. "backend" or
     *       "java")</li>
     *   <li><b>Department filter:</b> if department has some text (not null and not blank), add the
     *       filter that the department must contain that text. (e.g. "Engineering")</li>
     *   <li><b>Location filter:</b> here two things are possible, and indiaOnly gets the first
     *       preference:
     *       <ul>
     *         <li>If indiaOnly is true, we match the location with all the India words
     *             (JobSpecifications.INDIA_WORDS) and we ignore the location parameter
     *             completely.</li>
     *         <li>Otherwise, if location has some text, we match the location with that
     *             text only. (e.g. "London")</li>
     *       </ul>
     *   </li>
     *   <li><b>Page setting:</b> we make a PageRequest for the first page (page 0) only. The size
     *       is the given limit but it can never be more than 200 (Math.min), so nobody can
     *       ask for 10,000 jobs at once and slow down our database. The jobs are sorted by
     *       firstSeenAt in descending order, which means newest jobs come first.</li>
     *   <li><b>Fetch and convert:</b> we call jobRepository.findAll(spec, page), take the list of Job
     *       entities from it, and convert each one into a small JobView object (id, title,
     *       company name, location, department, apply url and first seen time). We return
     *       JobView and not the Job entity so that only the needed data goes out to the
     *       user.</li>
     * </ol>
     * <p>
     * Example: search(5, List.of("backend", "java"), null, null, true, 50) means "give me
     * the 50 latest active jobs of company 5, whose title has backend or java, and whose
     * location is in India". The department is null so it is skipped. The location is null
     * but indiaOnly is true so India words are used.
     * <p>
     * Example: search(null, null, null, null, false, 20) means no filters at all, so we
     * get the 20 latest active jobs of all the companies.
     *
     * <b>Note:</b> limit must be at least 1. PageRequest does not accept 0 or a negative size and
     * it will throw an IllegalArgumentException.
     *
     * @param companyId  id of the company (null means all companies)
     * @param keywords   words to look for in the job title, any one of them can match
     *                   (null or empty means no title filter)
     * @param location   text to look for in the job location, like "London" (null or blank
     *                   means no location filter). It is ignored when indiaOnly is true
     * @param department text to look for in the department, like "Engineering" (null or blank
     *                   means no department filter)
     * @param indiaOnly  true means show only the jobs located in India
     * @param limit      how many jobs the user wants at maximum (it is cut to 200 if more)
     * @return list of JobView, newest first, which matches all the given filters
     */
    public List<JobView> search(Long companyId, List<String> keywords, String location,
                                String department, boolean indiaOnly,
                                Boolean passedFilter, int limit) {

        Specification<Job> spec = JobSpecifications.active();

        if (companyId != null) {
            spec = spec.and(JobSpecifications.company(companyId));
        }
        if (keywords != null && !keywords.isEmpty()) {
            spec = spec.and(JobSpecifications.titleContainsAny(keywords));
        }
        if (department != null && !department.isBlank()) {
            spec = spec.and(JobSpecifications.departmentContains(department));
        }
        if (indiaOnly) {
            spec = spec.and(JobSpecifications.locationContainsAny(JobSpecifications.INDIA_WORDS));
        } else if (location != null && !location.isBlank()) {
            spec = spec.and(JobSpecifications.locationContainsAny(List.of(location)));
        }
        if (Boolean.TRUE.equals(passedFilter)) {
            spec = spec.and(JobSpecifications.passedFilter());
        }

        Pageable page = PageRequest.of(0, Math.min(limit, 200),
                Sort.by(Sort.Direction.DESC, "firstSeenAt"));

        return jobRepository.findAll(spec, page).getContent().stream()
                .map(j -> new JobView(j.getId(), j.getTitle(), j.getCompany().getName(),
                        j.getLocation(), j.getDepartment(), j.getApplyUrl(),
                        j.getFirstSeenAt(), j.isPassedFilter(),j.getNotifiedAt()))
                .toList();
    }
}