package io.quarkus.qe.metrics;

import io.smallrye.metrics.ExtendedMetadataBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Metrics based on direct curl-like interaction with GitHub
 *
 * Operations like repository.getPullRequests(GHIssueState.OPEN).size() are too expensive from time and memory perspective.
 * Only the PRs count is needed, but the price involves fetching details and deserialization from JSON for all the PRs.
 */
@ApplicationScoped
public class GHRepositoryAdvancedMetrics {

    private static final Logger log = Logger.getLogger(GHRepositoryAdvancedMetrics.class);
    private String ghToken;

    @ConfigProperty(name = "gh.label.question", defaultValue = "kind/question")
    String labelQuestion;
    @ConfigProperty(name = "gh.label.bug", defaultValue = "kind/bug")
    String labelBug;
    @ConfigProperty(name = "gh.label.epic", defaultValue = "kind/epic")
    String labelEpic;
    @ConfigProperty(name = "gh.label.enhancement", defaultValue = "kind/enhancement")
    String labelEnhancement;
    @ConfigProperty(name = "gh.label.proposal  ", defaultValue = "kind/extension-proposal")
    String labelProposal;
    @ConfigProperty(name = "gh.label.invalid", defaultValue = "triage/invalid")
    String labelInvalid;
    @ConfigProperty(name = "gh.label.duplicate  ", defaultValue = "triage/duplicate")
    String labelDuplicate;

    String[] issueStates = {"open", "closed"};

    public void initiateGH(String ghToken) {
        this.ghToken = ghToken;
    }

    public void ghAdvancedMetrics(MetricRegistry registry, String repositoryName, Tag... tags) {
        try {
            URL contributorsURL = new URL("https://api.github.com/repos/" + repositoryName + "/contributors?per_page=1");
            URL commitsURL = new URL("https://api.github.com/repos/" + repositoryName + "/commits?per_page=1");
            URL tagsURL = new URL("https://api.github.com/repos/" + repositoryName + "/tags?per_page=1");

            registerMetric("gh_repo_contributors", "Total number of contributors for given repository", contributorsURL, registry, tags);
            registerMetric("gh_repo_commits", "Total number of commits for given repository", commitsURL, registry, tags);
            registerMetric("gh_repo_tags", "Total number of tags/releases for given repository", tagsURL, registry, tags);

            URL openIssuesURL = new URL("https://api.github.com/search/issues?q=repo:" + repositoryName + "+is:issue+is:open&per_page=1");
            URL closedIssuesURL = new URL("https://api.github.com/search/issues?q=repo:" + repositoryName + "+is:issue+is:closed&per_page=1");

            registerMetric("gh_repo_open_issues", "Total number of open issues for given repository", openIssuesURL, registry, tags);
            registerMetric("gh_repo_closed_issues", "Total number of closed issues for given repository", closedIssuesURL, registry, tags);

            URL openPRsURL = new URL("https://api.github.com/repos/" + repositoryName + "/pulls?per_page=1");
            URL closedPRsURL = new URL("https://api.github.com/repos/" + repositoryName + "/pulls?per_page=1&state=closed");
            URL mergedPRsURL = new URL("https://api.github.com/search/issues?q=repo:" + repositoryName + "+is:pr+is:merged&per_page=1");

            registerMetric("gh_repo_open_prs", "Total number of open prs for given repository", openPRsURL, registry, tags);
            registerMetric("gh_repo_closed_prs", "Total number of closed prs for given repository", closedPRsURL, registry, tags);
            registerMetric("gh_repo_merged_prs", "Total number of merged prs for given repository", mergedPRsURL, registry, tags);

        } catch (MalformedURLException e) {
            log.error("Malformed URL", e);
        }
    }

    public void ghVerboseMetrics(MetricRegistry registry, String repositoryName, Tag... tags) {
        try {
            String[] issueLabels = {labelBug, labelEnhancement, labelEpic, labelProposal, labelQuestion};

            for (String label : issueLabels) {
                for (String state : issueStates) {
                    // prometheus =>        gh_repo_open_issues{repo="quarkusio/quarkus",label!~"kind.*"}
                    registerMetric(registry, repositoryName, "issue", state, label, tags);
                }
            }

            String[] closedOnlyLabels = {labelInvalid, labelDuplicate};
            for (String label : closedOnlyLabels) {
                registerMetric(registry, repositoryName, "issue", "closed", label, tags);
                registerMetric(registry, repositoryName, "pr", "closed", label, tags);
            }

            String[] issueActions = {"created", "closed"};
            String[] prActions = {"created", "closed", "merged"};

            for (String action : issueActions) {
                registerLast24hMetric(registry, repositoryName, "issue", action, tags);
            }
            for (String action : prActions) {
                registerLast24hMetric(registry, repositoryName, "pr", action, tags);
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL", e);
        }
    }

    private void registerLast24hMetric(MetricRegistry registry, String repositoryName, String type, String action, Tag... tags) throws MalformedURLException {
        String baseURL = "https://api.github.com/search/issues?per_page=1&q=repo:" + repositoryName +
                "+is:" + type + "+" + action + ":>";     // e.g. "+is:pr+created:>"
        log.debug("Registering metric for base URL " + baseURL);
        registry.register(
                new ExtendedMetadataBuilder()
                        .withName("gh_repo_" + action + "_" + type + "s_last_24h")
                        .withType(MetricType.GAUGE)
                        .withDescription("Total number of " + action + " " + type + "s for given repository in last 24 hours")
                        .skipsScopeInOpenMetricsExportCompletely(true)
                        .prependsScopeToOpenMetricsName(false)
                        .build(),
                (Gauge<Number>) () -> {
                    String timeSuffix = LocalDateTime.now(Clock.systemUTC()).minusDays(1).withNano(0).toString();
                    try {
                        log.debug(baseURL+timeSuffix);
                        return GHUtils.extractCountFromJSON(ghToken, new URL(baseURL+timeSuffix));
                    } catch (MalformedURLException e) {
                        log.error("Unable to construct URL " + baseURL+timeSuffix, e);
                        return 0;
                    }
                },
                tags);
    }

    private void registerMetric(MetricRegistry registry, String repositoryName, String type, String state, String label, Tag... tags) throws MalformedURLException {
        List<Tag> tagsList = new ArrayList<>();
        tagsList.addAll(Arrays.asList(tags));
        tagsList.add(new Tag("label", label));
        URL url = new URL("https://api.github.com/search/issues?q=repo:" + repositoryName +
                "+is:" + type +
                "+is:" + state +
                "+label:" + label +
                "&per_page=1");
        registerMetric("gh_repo_" + state + "_" + type + "s",
                "Total number of " + state + " " + type + "s for given repository",
                url, registry, tagsList.toArray(new Tag[0]));
    }

    private void registerMetric(String name, String description, URL url, MetricRegistry registry, Tag... tags) {
        log.debug("Registering metric for URL " + url.toExternalForm());
        if (url.getPath().contains("/search/issues")) {
            registry.register(
                    new ExtendedMetadataBuilder()
                            .withName(name)
                            .withType(MetricType.GAUGE)
                            .withDescription(description)
                            .skipsScopeInOpenMetricsExportCompletely(true)
                            .prependsScopeToOpenMetricsName(false)
                            .build(),
                    (Gauge<Number>) () -> GHUtils.extractCountFromJSON(ghToken, url),
                    tags);
        } else {
            registry.register(
                    new ExtendedMetadataBuilder()
                            .withName(name)
                            .withType(MetricType.GAUGE)
                            .withDescription(description)
                            .skipsScopeInOpenMetricsExportCompletely(true)
                            .prependsScopeToOpenMetricsName(false)
                            .build(),
                    (Gauge<Number>) () -> GHUtils.extractCountFromLinkHeader(ghToken, url),
                    tags);
        }
    }
}
