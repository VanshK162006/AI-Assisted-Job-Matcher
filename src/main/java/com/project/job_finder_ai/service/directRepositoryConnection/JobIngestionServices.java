/*
Compare Normalized Jobs to the Jobs saved in the database only of that company and
whichever extra jobs we found in normalized list save them to database.
 */

package com.project.job_finder_ai.service.directRepositoryConnection;

import com.project.job_finder_ai.dto.NormalizedJob;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * <h1> How do we find which jobs are new, which are still open and which are closed? </h1>
 *
 * As you know our Ats fetchers like LeverFetcher, GreenHouseFetcher, AshbyFetcher, etc.
 * fetch the jobs from the URL of their Ats in their own dto (like GreenhouseJobs, AshbyJobs,
 * etc.) and then we convert them into our business understandable format, which is the
 * NormalizedJob record. So at the end the fetcher gives us a List of NormalizedJob.
 *
 * <h2> What is the problem? </h2>
 * Every time we fetch, the company gives us ALL its jobs which are open right now, not only
 * the new ones. For example if openai has 40 open jobs, we get the same 40 jobs again and again
 * on every fetch. If we directly save them, then our database will have the same job many times.
 * And one more thing: when a company removes a job from its career page, we get no message about
 * that. The job just stops coming in the list. So we have to find that by ourselves.
 *
 * <h2> What does JobIngestionServices do? </h2>
 * It compares the fresh list (NormalizedJob) with the jobs which are already saved in our
 * database (Job) for that company. To know whether two jobs are the same, we use the
 * <b>externalId</b>. It is the id which the company's Ats has given to that job, so it is
 * unique for a job inside one company.
 *
 * After comparing, every job falls in exactly one of these three cases:
 * <ul>
 *   <li><b>Case 1 - Job is in the fresh list AND in our database:</b> it is an old job which is
 *       still open. We do not save it again, we only update its lastSeenAt to now and keep it
 *       active.</li>
 *   <li><b>Case 2 - Job is in the fresh list but NOT in our database:</b> it is a new job. We save
 *       it in the database with firstSeenAt = now.</li>
 *   <li><b>Case 3 - Job is in our database (active) but NOT in the fresh list:</b> the company has
 *       closed it. We do not delete it, we just mark it active = false, so it will not show
 *       in the search any more.</li>
 * </ul>
 *
 * <h2> Simple example </h2>
 * Suppose in our database openai has these active jobs: 101 (Backend Engineer),
 * 102 (Data Scientist), 103 (Designer). Today the fetcher gives us the jobs 102, 103 and
 * 104 (ML Engineer). Then:
 * <pre>
 *   101 -> not in fresh list     -> Case 3 -> marked closed (active = false)
 *   102 -> in both               -> Case 1 -> lastSeenAt updated
 *   103 -> in both               -> Case 1 -> lastSeenAt updated
 *   104 -> only in fresh list    -> Case 2 -> new job, saved in database
 * </pre>
 * The new jobs list which we return is only [104].
 *
 * <h2> Why we do not delete the closed jobs? </h2>
 * (1) We keep the history of what the company had opened earlier. (2) If the company opens
 * the same job again (same externalId), we simply make it active again instead of creating a
 * duplicate. (3) The firstSeenAt of a job stays correct for its whole life.
 *
 * <h2> What do the three fields mean? </h2>
 * <ul>
 *   <li><b>firstSeenAt</b> = the time when we saw this job for the first time (set once, never changes)</li>
 *   <li><b>lastSeenAt</b> = the time when we saw this job in the latest fetch (changes on every fetch)</li>
 *   <li><b>active</b> = true means the company still shows this job, false means it is closed</li>
 * </ul>
 *
 * <h2> Annotations used on this class </h2>
 * <ul>
 *   <li><b>@Service</b> tells Spring that this is a service class, so Spring creates its object by
 *       itself and gives it to whoever needs it.</li>
 *   <li><b>@RequiredArgsConstructor</b> is from Lombok. It writes the constructor
 *       JobIngestionServices(JobRepository jobRepository) for us, and Spring uses it to give us
 *       the JobRepository object.</li>
 * </ul>
 *
 * <b>Important:</b> This class changes the old jobs (lastSeenAt and active) but never calls
 * save() on them. It depends on JPA's automatic change detection, which works only inside a
 * transaction. So the method which calls latestJobs() must be @Transactional, or else add
 * @Transactional on latestJobs() itself. Otherwise those updates may not reach the database.
 */
@Service
@RequiredArgsConstructor
public class JobIngestionServices {

    /**
     * The object which talks to the database for the Job table. We use it to find an old job
     * by company and externalId, to save a new job, and to get all the active jobs of a company.
     */
    private final JobRepository jobRepository;

    public List<Job> latestJobs(Company company, List<NormalizedJob> normalizedJobsList) {
        // One common time for the whole run, so every job touched now gets the same time.
        Instant now = Instant.now();
        // Here we collect only the new jobs, this is what we return at the end.
        List<Job> newJobs = new ArrayList<>();
        // Remembers the externalId of every job which came in the fresh list (needed for Step 3).
        Set<String> seenIds = new HashSet<>();

        // Step 1: go through every job from the fresh list and decide: old job or new job?
        for (NormalizedJob n : normalizedJobsList) {
            seenIds.add(n.externalId());
            Optional<Job> existing =
                    jobRepository.findByCompanyAndExternalId(company, n.externalId());

            if (existing.isPresent()) {
                // Case 1: old job and company still shows it. Just refresh the time, keep it active.
                Job job = existing.get();
                job.setLastSeenAt(now);
                job.setActive(true);
            } else {
                // Case 2: brand new job. Copy all the data and save it for the first time.
                Job job = new Job();
                job.setCompany(company);
                job.setExternalId(n.externalId());
                job.setTitle(n.title());
                job.setLocation(n.location());
                job.setDepartment(n.department());
                job.setEmploymentType(n.employmentType());
                job.setApplyUrl(n.applyUrl());
                job.setDescriptionText(n.descriptionText());
                job.setPostedAt(n.postedAt());
                job.setFirstSeenAt(now);
                job.setLastSeenAt(now);
                job.setActive(true);
                newJobs.add(jobRepository.save(job));
            }
        }

        // Step 2: Case 3. Any active job of this company which was not in the fresh list is closed.
        for (Job old : jobRepository.findByCompanyAndActiveTrue(company)) {
            if (!seenIds.contains(old.getExternalId())) {
                old.setActive(false);
            }
        }
        return newJobs;
    }
}