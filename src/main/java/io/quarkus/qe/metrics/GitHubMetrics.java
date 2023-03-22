package io.quarkus.qe.metrics;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class GitHubMetrics {

    @ConfigProperty(name = "gh.repos", defaultValue = "quarkusio/quarkus")
    public List<String> ghRepos;

    @ConfigProperty(name = "gh.details.level", defaultValue = "BASE")
    GHDetailsLevel ghDetailsLevel;

    @ConfigProperty(name = "gh.token")
    String ghToken;

    @Inject
    MetricRegistry registry;

    @Inject
    GHRepositoryBaseMetrics ghRepositoryBaseMetrics;

    @Inject
    GHRepositoryAdvancedMetrics ghRepositoryAdvancedMetrics;

    @Inject
    GHRepositoryCustomMetrics ghRepositoryCustomMetrics;

    private static final Logger log = Logger.getLogger(GitHubMetrics.class);

    void onStart(@Observes StartupEvent ev) throws IOException {
        log.info("The application is starting in " + ghDetailsLevel + " details level");

        ghRepositoryBaseMetrics.initiateGH(ghToken);
        if (! ghRepositoryBaseMetrics.isGHInitiated()) {
            return;
        }
        ghRepositoryAdvancedMetrics.initiateGH(ghToken);
        ghRepositoryCustomMetrics.initiateGH(ghToken);

        for (String repo : ghRepos) {
            String repositoryName = repo.trim();
            Tag repositoryTag = new Tag("repo", repositoryName);
            log.info("Processing: '" + repositoryName + "'");

            switch (ghDetailsLevel) {
                case VERBOSE:
                    ghRepositoryAdvancedMetrics.ghVerboseMetrics(registry, repositoryName, repositoryTag);
                case ADVANCED:
                    ghRepositoryAdvancedMetrics.ghAdvancedMetrics(registry, repositoryName, repositoryTag);
                case BASE:
                    ghRepositoryBaseMetrics.ghBaseMetrics(registry, repositoryName, repositoryTag);
                    break;
                case CUSTOM:
                    ghRepositoryCustomMetrics.ghCustomMetrics(registry, repositoryName, repositoryTag);
                    break;
            }
        }

        ghRepositoryBaseMetrics.ghRateLimitMetrics(registry);
    }

    enum GHDetailsLevel {
        BASE,
        ADVANCED,
        VERBOSE,
        CUSTOM
    }
}
