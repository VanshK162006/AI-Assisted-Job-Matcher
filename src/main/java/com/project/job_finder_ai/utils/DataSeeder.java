package com.project.job_finder_ai.utils;

import com.project.job_finder_ai.entity.CandidateProfile;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.enums.AtsType;
import com.project.job_finder_ai.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final List<Seed> SEEDS = List.of(

            // ===================== TIER 1: MUMBAI (first priority) =====================
//            gh("Galaxy", "galaxydigitalservices"),                          // VERIFIED, Mumbai roles
//            gh("Blenheim Chalcot India", "blenheimchalcotindia"),           // VERIFIED, Mumbai engineering centre
//            lever("CRED", "cred"),                                          // VERIFIED, Mumbai listed, B2C
//            lever("Paytm", "paytm"),                                        // VERIFIED, Mumbai, B2C (many non-tech roles)
//            lever("Upstox", "upstox"),                                      // UNVERIFIED slug, Mumbai-based B2C broker
//            lever("Xsolla", "xsolla"),                                      // VERIFIED, Mumbai location listed
//            ashby("Juniper Square", "junipersquare"),                       // VERIFIED, Mumbai office
//            ashby("OpenAI", "openai", "https://openai.com/careers"),        // VERIFIED, Mumbai role seen, B2C
//            ashby("Sardine", "sardine")                                // VERIFIED, remote India incl. Mumbai,
//             ============ TIER 2: OTHER INDIA CITIES (Bengaluru, Pune, Hyderabad...) ============
            gh("Razorpay", "razorpaysoftwareprivatelimited", "https://razorpay.com/jobs/"),   // VERIFIED, B2C fintech
            gh("Capco", "capco", "https://www.capco.com/careers"),                            // VERIFIED, Java/Spring Boot roles
            gh("Zscaler", "zscaler", "https://www.zscaler.com/careers"),                      // VERIFIED, Mumbai sales + India engineering
            gh("Databricks", "databricks", "https://www.databricks.com/company/careers"),     // VERIFIED
            gh("GoDaddy", "godaddy", "https://careers.godaddy.com/"),                         // VERIFIED, B2C-ish
            gh("HackerRank", "hackerrank", "https://www.hackerrank.com/careers"),             // VERIFIED, SDE intern seen
            gh("Hyreo", "hyreo"),                                                             // VERIFIED, Java 0-1 year role seen
            gh("mthree Graduate Programme", "mthreerecruitingportal"),                        // VERIFIED, fresher programme (read the terms)
            gh("Postman", "postman", "https://www.postman.com/company/careers/"),             // UNVERIFIED slug
            gh("InMobi", "inmobi"),                                                           // UNVERIFIED slug
            lever("Stable Money", "stable-money1"),                                           // VERIFIED, B2C fintech, SWE I
            lever("Hevo Data", "hevodata", "https://hevodata.com/careers/"),                  // VERIFIED, SDE intern seen
            lever("Saviynt", "saviynt"),                                                      // VERIFIED, Java roles
            lever("Brillio", "brillio-2", "https://www.brillio.com/careers/"),                // VERIFIED, IT services
            lever("Acceldata", "acceldata"),                                                  // VERIFIED
            lever("Zimperium", "zimperium"),                                                  // VERIFIED, Java microservices
            lever("Uniphore", "uniphore"),                                                    // UNVERIFIED slug
            ashby("AiPrise", "aiprise"),                                                      // VERIFIED, SWE I (0-2 years)
            ashby("Ema", "ema"),                                                              // VERIFIED
            ashby("Build AI", "build-ai"),                                                    // VERIFIED, India software engineer
            ashby("Cartesia", "cartesia")                                                   // VERIFIED, India office

//            // ===================== TIER 3: REMOTE / GLOBAL =====================
//            gh("YipitData", "yipitdata"),                                                     // VERIFIED, remote India (senior roles)
//            gh("GitLab", "gitlab", "https://about.gitlab.com/jobs/"),                         // UNVERIFIED slug, remote-first
//            gh("Coinbase", "coinbase", "https://www.coinbase.com/careers"),                   // UNVERIFIED slug, B2C, remote
//            gh("Stripe", "stripe", "https://stripe.com/jobs"),                                // UNVERIFIED slug
//            gh("MongoDB", "mongodb", "https://www.mongodb.com/careers")                       // UNVERIFIED slug, last item: NO comma

            // ---------- RESERVE: remove the // to add any of these ----------
            // , lever("Jobgether", "jobgether")          // many remote India roles, but it posts for third-party partners
            // , gh("Glean", "gleanwork")
            // , gh("DigitalOcean", "digitalocean98", "https://www.digitalocean.com/careers")
            // , gh("SentinelOne", "sentinellabs")
            // , gh("Eudia", "eudia")
            // , lever("Revefi", "revefi")
            // , ashby("Granica", "granica")
            // , ashby("Altimate", "altimate")
            // , ashby("Articul8", "articul8")
    );
    private final CompanyRepository companyRepository;

    private static Seed gh(String name, String slug) {
        return new Seed(name, AtsType.GREENHOUSE, slug, null);
    }

    private static Seed gh(String name, String slug, String careersUrl) {
        return new Seed(name, AtsType.GREENHOUSE, slug, careersUrl);
    }

    private static Seed lever(String name, String slug) {
        return new Seed(name, AtsType.LEVER, slug, null);
    }

    private static Seed lever(String name, String slug, String careersUrl) {
        return new Seed(name, AtsType.LEVER, slug, careersUrl);
    }

    private static Seed ashby(String name, String slug) {
        return new Seed(name, AtsType.ASHBY, slug, null);
    }

    private static Seed ashby(String name, String slug, String careersUrl) {
        return new Seed(name, AtsType.ASHBY, slug, careersUrl);
    }

    @Override
    public void run(String... args) {
        long existing = companyRepository.count();
        if (existing > 10) {
            log.info("DataSeeder skipped: {} companies already in the database", existing);
            return;
        }
        try {
            List<Company> companies = SEEDS.stream().map(this::toCompany).toList();
            companyRepository.saveAll(companies);

            log.info("DataSeeder added {} companies", companies.size());
        } catch (Exception e) {
            log.info("Duplicate data of Company found: "+e.getMessage());
        }
    }

    private Company toCompany(Seed s) {
        Company c = new Company();
        c.setName(s.name());
        c.setAtsType(s.type());
        c.setBoardSlug(s.slug());
        c.setCareersUrl(s.careersUrl() != null ? s.careersUrl() : boardUrl(s));
        return c;
    }

    private String boardUrl(Seed s) {
        return switch (s.type) {
            case GREENHOUSE -> "https://job-boards.greenhouse.io/" + s.slug();
            case LEVER -> "https://jobs.lever.co/" + s.slug();
            case ASHBY -> "https://jobs.ashbyhq.com/" + s.slug();
        };
    }

    private record Seed(String name, AtsType type, String slug, String careersUrl) {
    }
}