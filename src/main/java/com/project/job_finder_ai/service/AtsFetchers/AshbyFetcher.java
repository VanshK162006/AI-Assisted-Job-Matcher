package com.project.job_finder_ai.service.AtsFetchers;

import com.project.job_finder_ai.dto.Ashby.AshbyJob;
import com.project.job_finder_ai.dto.Ashby.AshbyResponse;
import com.project.job_finder_ai.dto.NormalizedJob;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.enums.AtsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

/**
 * Fetches jobs from <b>Ashby</b> (a Greenhouse competitor) and converts them
 * into the application's common {@link NormalizedJob} format.
 *
 * <p>This class implements {@link JobFetcher} and therefore overrides:
 * <ul>
 *   <li>{@link #supportedType()} – returns the ATS type this fetcher handles
 *       (Greenhouse, Ashby, Lever, etc.).</li>
 *   <li>{@link #fetch(Company)} ()} – calls Ashby's public job-board URL using
 *       {@code RestClient} and returns the normalized jobs.</li>
 * </ul>
 *
 * <h2>How {@code fetch(Company)} works</h2>
 * <ol>
 *   <li><b>Call the API</b> – request the Ashby public URL via {@code RestClient}.</li>
 *   <li><b>Map only what we need</b> – Ashby returns many fields we don't use
 *       (e.g. {@code descriptionHtml}, {@code secondaryLocation}). We define
 *       {@link AshbyJob} with only the required fields, and pass it to
 *       {@code body(...)} so the response is deserialized into that type;
 *       unknown fields are ignored.</li>
 *   <li><b>Normalize</b> – convert each {@code AshbyJob} into a
 *       {@link NormalizedJob}.</li>
 * </ol>
 *
 * <h2>Why normalize?</h2>
 * Every ATS returns a different response shape, so each has its own response
 * record ({@code GreenhouseResponse}, {@code AshbyResponse}, ...). We convert
 * all of them into one {@code NormalizedJob} that the rest of the app can use.
 *
 * <pre>{@code
 * Ashby API ──> AshbyResponse / AshbyJob ──> NormalizedJob ──> rest of the app
 * }</pre>
 *
 * @see JobFetcher
 * @see AshbyJob
 * @see NormalizedJob
 */
@Component
@RequiredArgsConstructor
public class AshbyFetcher implements JobFetcher {

    private final RestClient restClient;

    @Override
    public AtsType supportedType() {
        return AtsType.ASHBY;
    }

    @Override
    public List<NormalizedJob> fetch(Company company) {
        String url = "https://api.ashbyhq.com/posting-api/job-board/" + company.getBoardSlug();

        AshbyResponse response = restClient.get().uri(url)
            .retrieve().body(AshbyResponse.class);

        if (response == null || response.jobs() == null) {
            return List.of();
        }
        return response.jobs().stream()
            .filter(j -> j.id() != null)
            .map(this::toNormalized)
            .toList();
    }

    private NormalizedJob toNormalized(AshbyJob j) {
        String applyUrl = j.jobUrl() != null ? j.jobUrl() : j.applyUrl();
        return new NormalizedJob(j.id(), j.title(), j.location(), j.department(),
            j.employmentType(), applyUrl, j.descriptionPlain(),
            Instant.parse(j.publishedAt()));
    }
}