package io.quarkus.qe.metrics;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.metrics.ExtendedMetadataBuilder;
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
 * Metrics based on direct curl-like interaction with GitHub
 *
 * Operations like repository.getPullRequests(GHIssueState.OPEN).size() are too expensive from time and memory perspective.
 * Only the PRs count is needed, but the price involves fetching details and deserialization from JSON for all the PRs.
 */
@ApplicationScoped
public class GHRepositoryAdvancedMetrics {

    private static final Logger log = Logger.getLogger(GHRepositoryAdvancedMetrics.class);
    private String gitHubToken;

    public void initiateGH(String gitHubToken) {
        this.gitHubToken = gitHubToken;
    }

    public void ghAdvancedMetrics(MetricRegistry registry, String repositoryName, Tag... tags) {
        /*
         Issues
         PRs
          - global count
          - by one person
          - by group of people
          - per label
          - per more labels
          - issues
            - bug vs. feature vs. enhancement vs. question
        */

        /* Moved from Basic
        contributors  ...  getGHRepository(repositoryName).listContributors()
        commits ... getGHRepository(repositoryName).listCommits()
        releases ... getGHRepository(repositoryName).listReleases().iterator
         */
    }

}
