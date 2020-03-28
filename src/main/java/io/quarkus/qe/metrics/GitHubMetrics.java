package io.quarkus.qe.metrics;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.metrics.ExtendedMetadataBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GitHubMetrics {

    @ConfigProperty(name = "gh.repos", defaultValue = "quarkusio/quarkus, quarkusio/quarkus-quickstarts, quarkusio/quarkusio.github.io")
    public List<String> ghRepos;

    @ConfigProperty(name = "gh.token")
    String gitHubToken;

    @Inject
    MetricRegistry registry;

    @Inject
    GHRepositoryBaseMetrics ghRepositoryBaseMetrics;

    private static final Logger log = Logger.getLogger(GitHubMetrics.class);

    void onStart(@Observes StartupEvent ev) throws IOException {
        log.info("The application is starting ...");
//        MetricRegistry metricRegistry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
//        github = new GitHubBuilder().withOAuthToken(gitHubToken).build();
//        GHRepositoryBaseMetrics ghRepositoryBaseMetrics = new GHRepositoryBaseMetrics(gitHubToken);

        ghRepositoryBaseMetrics.initiateGH(gitHubToken);
        if (! ghRepositoryBaseMetrics.isGHInitiated()) {
            return;
        }

        for (String repo : ghRepos) {
            String repositoryName = repo.trim();
            Tag repositoryTag = new Tag("repo", repositoryName);
            log.info("Processing: '" + repositoryName + "'");

//            GHRepository repository = github.getRepository(repo.trim());
//            log.info(repository.getPullRequests(GHIssueState.OPEN).size());

            ghRepositoryBaseMetrics.ghBaseMetrics(registry, repositoryName, repositoryTag);
        }

        ghRepositoryBaseMetrics.rateLimits(registry);
    }
}
