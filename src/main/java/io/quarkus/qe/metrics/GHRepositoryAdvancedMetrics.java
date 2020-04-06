package io.quarkus.qe.metrics;

import io.smallrye.metrics.ExtendedMetadataBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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

    String[] issueStates = {"open", "closed"};
    String[] prStates = {"open", "closed", "merged"};

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
            String[] prLabels = {labelBug, labelEnhancement};

            for (String label : issueLabels) {
                for (String state : issueStates) {
                    registerMetric(registry, repositoryName, "issue", state, label, tags);
                }
            }
            for (String label : prLabels) {
                for (String state : prStates) {
                    registerMetric(registry, repositoryName, "pr", state, label, tags);
                }
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL", e);
        }
    }

        /*
        prometheus =>                gh_repo_merged_prs{repo="quarkusio/quarkus",label!~"kind.*"}

         Issues / PRs
          - by one person
          - by group of people
          - per label
          - per more labels
        */

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
                    (Gauge<Number>) () -> extractCountFromJSON(url),
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
                    (Gauge<Number>) () -> extractCountFromLinkHeader(url),
                    tags);
        }
    }

    private int extractCountFromLinkHeader(URL url) {
        int count = 0;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", "token " + ghToken);
        con.setRequestProperty("User-Agent", "github-metrics");
        String link = con.getHeaderField("Link");
        if (link != null) {
            // extract page from Link
            // <https://api.github.com/repositories/139914932/pulls?per_page=1&page=2>; rel="next", <https://api.github.com/repositories/139914932/pulls?per_page=1&page=90>; rel="last"
            String countString = link.substring(link.lastIndexOf("&page=")+6);
            countString = countString.substring(0,countString.lastIndexOf(">"));
            count = Integer.parseInt(countString);
        } else {
            // 0 PRs => no content + no link, 1 PR  => content + no link, example: 0 => 2, 1 => 14685
            count = con.getContentLength() > 10 ? 1 : 0;
        }
        } catch (IOException e) {
            log.error("Unable to get expected data from URL " + url, e);
            dumpHeaders(con);
        } finally {
            con.disconnect();
        }
        return count;
    }

    private int extractCountFromJSON(URL url) {
        int count = 0;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("Authorization", "token " + ghToken);
            con.setRequestProperty("User-Agent", "github-metrics");
            JsonReader jsonReader = Json.createReader(con.getInputStream());
            JsonObject rootJSON = jsonReader.readObject();
            count = rootJSON.getInt("total_count");
        } catch (IOException e) {
            log.error("Unable to get expected data from URL " + url, e);
            dumpHeaders(con);
        } finally {
            con.disconnect();
        }
        return count;
    }

    private void dumpHeaders(HttpURLConnection con) {
        con.getHeaderFields().forEach((key,value)-> {
            System.out.println(key + ": " + value);
        });
    }
}
