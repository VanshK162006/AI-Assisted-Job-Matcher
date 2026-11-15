package com.project.job_finder_ai.service.AtsFetchers;

import com.project.job_finder_ai.enums.AtsType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <h1> How to find AtsFetcher class of each company? </h1>
 *
 * As already we had made all our AtsFetcher classes like GreenHouseFetcher, LeverFetcher,
 * AshbyFetcher,etc. We can easily add any company into database because it just required
 * CompanyRequest Dto data.
 *
 * But problems occur when we have to fetch new jobs of only those companies like openai from
 * their Ats store again. So we made a FetcherRegistry class which work is just to return the
 * classes by looking at the AtsType of company. If company atstype is GreenHouse, It will return
 * us GreenHouseFetcher class and then simply we can call fetch() method of GreenHouseFetcher class
 * which will work to fetch jobs about those company only.
 */
@Component
public class FetcherRegistry {

    private final Map<AtsType, JobFetcher> fetchers;

    /**
     * <h2> How does the FetcherRegistry constructor work? </h2>
     *
     * We never create GreenHouseFetcher, LeverFetcher or AshbyFetcher by ourselves here.
     * Every one of them implements the JobFetcher interface and is marked as a Spring
     * component, so when the application starts, Spring finds all of them and passes them
     * into this constructor as one List (this is why the parameter is List&lt;JobFetcher&gt;).
     *
     * A List is not useful for us, because finding the right fetcher would mean looping
     * through it every time. So inside the constructor we convert this List into a Map
     * in which:
     * <ul>
     *   <li><b>Key</b> = AtsType, which we get by calling supportedType() on each fetcher
     *       (e.g. GreenHouseFetcher tells us it supports AtsType.GREENHOUSE)</li>
     *   <li><b>Value</b> = the fetcher object itself</li>
     * </ul>
     *
     * Example: [GreenHouseFetcher, LeverFetcher, AshbyFetcher] becomes
     * {GREENHOUSE -> GreenHouseFetcher, LEVER -> LeverFetcher, ASHBY -> AshbyFetcher}.
     *
     * This Map is built only once at startup. Because of this, when we add a new ATS
     * like Workday in future, we only have to create a new fetcher class which implements
     * JobFetcher. We don't need to touch this class at all, Spring will give it to the
     * constructor automatically.
     *
     * <b>Note:</b> Each fetcher must return a different AtsType from supportedType().
     * If two fetchers return the same type, Collectors.toMap() will throw an
     * IllegalStateException while the application is starting, so the mistake is caught
     * immediately.
     *
     * @param all every JobFetcher implementation found by Spring
     */
    public FetcherRegistry(List<JobFetcher> all) {
        this.fetchers = all.stream()
            .collect(Collectors.toMap(JobFetcher::supportedType, f -> f));
    }

    /**
     * <h2> How does the getFetcher method work? </h2>
     *
     * This is the method which other classes call when they want to fetch new jobs of a
     * company. They just pass the AtsType of that company, and we return the fetcher
     * which knows how to talk to that ATS.
     *
     * Working step by step:
     * <ol>
     *   <li>Look into the fetchers Map (built in the constructor) using the given AtsType
     *       as the key.</li>
     *   <li>If a fetcher is found, return it. Then the caller can simply call its fetch()
     *       method to get the jobs of that company.</li>
     *   <li>If nothing is found, it means we have not made a fetcher for that AtsType yet,
     *       so we throw an IllegalArgumentException with a clear message instead of
     *       returning null. This way we avoid a confusing NullPointerException later.</li>
     * </ol>
     *
     * Example: for a company like openai whose AtsType is GREENHOUSE,
     * getFetcher(AtsType.GREENHOUSE) returns GreenHouseFetcher, and then we call
     * fetch() on it to get only openai's jobs.
     *
     * @param type the AtsType of the company (GREENHOUSE, LEVER, ASHBY, etc.)
     * @return the JobFetcher which supports that AtsType
     * @throws IllegalArgumentException if no fetcher is registered for the given AtsType
     */
    public JobFetcher getFetcher(AtsType type) {
        JobFetcher fetcher = fetchers.get(type);
        if (fetcher == null) {
            throw new IllegalArgumentException("No fetcher for " + type);
        }
        return fetcher;
    }
}