package io.quarkus.qe.metrics;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.metrics.ExtendedMetadataBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Metrics based on details available in GHRepository object
 */
@ApplicationScoped
public class GHRepositoryBaseMetrics {

    private static final Logger log = Logger.getLogger(GHRepositoryBaseMetrics.class);
    private GitHub github;

    public void initiateGH(String gitHubToken) {
        try {
            github = new GitHubBuilder().withOAuthToken(gitHubToken).build();
        } catch (IOException e) {
            log.error("Token was rejected", e);
        }
    }

    public boolean isGHInitiated() {
        return github != null;
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

    public void rateLimits(MetricRegistry registry) {
        registry.register(
                new ExtendedMetadataBuilder()
                        .withName("gh_rate_remaining")
                        .withType(MetricType.GAUGE)
                        .withDescription("Number of API queries remaining in the current window")
                        .skipsScopeInOpenMetricsExportCompletely(true)
                        .prependsScopeToOpenMetricsName(false)
                        .build(),
                (Gauge<Number>) () -> {
                    try {
                        return github.getRateLimit().getRemaining();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        return 0;
                    }
                }
        );
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
        try {
            GHRepository repository = repositoryMap.get(name);
            if (repository == null) {
                repository = github.getRepository(name);
                repositoryMap.put(name, repository);
            }
            return repository;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
