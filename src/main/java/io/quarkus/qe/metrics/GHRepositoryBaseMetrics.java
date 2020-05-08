package io.quarkus.qe.metrics;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.metrics.ExtendedMetadataBuilder;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Metrics based on details available in GHRepository object
 */
@ApplicationScoped
public class GHRepositoryBaseMetrics {

    private static final Logger log = Logger.getLogger(GHRepositoryBaseMetrics.class);
    private String ghToken;

    public void initiateGH(String ghToken) {
        this.ghToken = ghToken;
    }

    public boolean isGHInitiated() {
        return GHUtils.isTokenValid(ghToken);
    }

    public void ghBaseMetrics(MetricRegistry registry, String repositoryName, Tag... tags) {
        registry.register(
                new ExtendedMetadataBuilder()
                        .withName("gh_repo_stars")
                        .withType(MetricType.GAUGE)
                        .withDescription("Total number of Stars for given repository")
                        .skipsScopeInOpenMetricsExportCompletely(true)
                        .prependsScopeToOpenMetricsName(false)
                        .build(),
                (Gauge<Number>) () -> getGHRepository(repositoryName).getStargazersCount(),
                tags);

        registry.register(
                new ExtendedMetadataBuilder()
                        .withName("gh_repo_open_issues_and_prs")
                        .withType(MetricType.GAUGE)
                        .withDescription("Total number of open issues and PRs for given repository")
                        .skipsScopeInOpenMetricsExportCompletely(true)
                        .prependsScopeToOpenMetricsName(false)
                        .build(),
                (Gauge<Number>) () -> getGHRepository(repositoryName).getOpenIssueCount(),
                tags);

        registry.register(
                new ExtendedMetadataBuilder()
                        .withName("gh_repo_forks")
                        .withType(MetricType.GAUGE)
                        .withDescription("Total number of forks for given repository")
                        .skipsScopeInOpenMetricsExportCompletely(true)
                        .prependsScopeToOpenMetricsName(false)
                        .build(),
                (Gauge<Number>) () -> getGHRepository(repositoryName).getForksCount(),
                tags);

        registry.register(
                new ExtendedMetadataBuilder()
                        .withName("gh_repo_subscribers")
                        .withType(MetricType.GAUGE)
                        .withDescription("Total number of watchers/subscribers for given repository")
                        .skipsScopeInOpenMetricsExportCompletely(true)
                        .prependsScopeToOpenMetricsName(false)
                        .build(),
                (Gauge<Number>) () -> getGHRepository(repositoryName).getSubscribersCount(),
                tags);

        registry.register(
                new ExtendedMetadataBuilder()
                        .withName("gh_repo_size")
                        .withType(MetricType.GAUGE)
                        .withDescription("Size in kB for given repository")
                        .skipsScopeInOpenMetricsExportCompletely(true)
                        .prependsScopeToOpenMetricsName(false)
                        .build(),
                (Gauge<Number>) () -> getGHRepository(repositoryName).getSize(),
                tags);
    }

    public void ghRateLimitMetrics(MetricRegistry registry) {
        registry.register(
                new ExtendedMetadataBuilder()
                        .withName("gh_rate_remaining")
                        .withType(MetricType.GAUGE)
                        .withDescription("Number of API queries remaining in the current window")
                        .skipsScopeInOpenMetricsExportCompletely(true)
                        .prependsScopeToOpenMetricsName(false)
                        .build(),
                (Gauge<Number>) () -> getRemainingRateLimit()
        );
    }

    class GHRepository {
        private final JsonObject rootJSON;

        public GHRepository(JsonObject rootJSON) {
            this.rootJSON = rootJSON;
        }
        public Number getStargazersCount() {
            return rootJSON.getInt("stargazers_count");
        }
        public Number getOpenIssueCount() {
            return rootJSON.getInt("open_issues_count");
        }
        public Number getForksCount() {
            return rootJSON.getInt("forks_count");
        }
        public Number getSubscribersCount() {
            return rootJSON.getInt("subscribers_count");
        }
        public Number getSize() {
            return rootJSON.getInt("size");
        }
    }

    public Number getRemainingRateLimit() {
        JsonObject rootJSON = GHUtils.getJsonObject(ghToken, "https://api.github.com/rate_limit");
        return rootJSON.getJsonObject("rate").getInt("remaining");
    }

    Map<String, GHRepository> repositoryMap = new HashMap<>();
    @Scheduled(every="{gh.repo.cache.clean.period}")
    public void cleanGHRepositoryMap() {
        log.debug("cleanGHRepositoryMap invoked");
        if (repositoryMap.size() > 0) {
            repositoryMap.clear();
        }
    }
    private GHRepository getGHRepository(String name) {
        GHRepository repository = repositoryMap.get(name);
        if (repository == null) {
            repository = getRepository(name);
            repositoryMap.put(name, repository);
        }
        return repository;
    }

    private GHRepository getRepository(String repositoryName) {
        JsonObject rootJSON =
                GHUtils.getJsonObject(ghToken, "https://api.github.com/repos/" + repositoryName);
        return new GHRepository(rootJSON);
    }
}
