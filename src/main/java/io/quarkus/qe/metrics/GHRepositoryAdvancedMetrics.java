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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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

        try {
            URL openPRsURL = new URL("https://api.github.com/repos/" + repositoryName + "/pulls?per_page=1");
            URL closedPRsURL = new URL("https://api.github.com/repos/" + repositoryName + "/pulls?per_page=1&state=closed");
            URL openIssuesURL = new URL("https://api.github.com/repos/" + repositoryName + "/issues?per_page=1");
            URL closedIssuesURL = new URL("https://api.github.com/repos/" + repositoryName + "/issues?per_page=1&state=closed");

            URL contributorsURL = new URL("https://api.github.com/repos/" + repositoryName + "/contributors?per_page=1");
            URL commitsURL = new URL("https://api.github.com/repos/" + repositoryName + "/commits?per_page=1");
            URL tagsURL = new URL("https://api.github.com/repos/" + repositoryName + "/tags?per_page=1");

            /*
            kind%2Fquestion
            kind%2Fbug
            kind%2Fepic
            kind%2Fenhancement
            kind%2Fextension-proposal

            System.out.println("kind/extension-proposal");
            System.out.println(URLEncoder.encode("kind/extension-proposal", StandardCharsets.UTF_8));

https://api.github.com/search/issues?q=repo:quarkusio/quarkus+is:pr+is:open+label:kind/enhancement

https://api.github.com/search/issues?q=repo:quarkusio/quarkus+is:pr+is:open&per_page=1
https://api.github.com/search/issues?q=repo:quarkusio/quarkus+is:pr+is:closed&per_page=1

https://api.github.com/search/issues?q=repo:quarkusio/quarkus+is:pr+is:merged&per_page=1
https://api.github.com/search/issues?q=repo:quarkusio/quarkus+is:issue+is:open&per_page=1
https://api.github.com/search/issues?q=repo:quarkusio/quarkus+is:issue+is:closed&per_page=1
   <https://api.github.com/search/issues?q=repo%3Aquarkusio%2Fquarkus+is%3Aissue+is%3Aclosed&per_page=1&page=2>; rel="next", <https://api.github.com/search/issues?q=repo%3Aquarkusio%2Fquarkus+is%3Aissue+is%3Aclosed&per_page=1&page=1000>; rel="last"
   "total_count": 2694,
            */

            registerMetric("gh_repo_contributors", "Total number of contributors for given repository", contributorsURL, registry, tags);
            registerMetric("gh_repo_commits", "Total number of commits for given repository", commitsURL, registry, tags);
            registerMetric("gh_repo_tags", "Total number of tags/releases for given repository", tagsURL, registry, tags);

            registerMetric("gh_repo_open_prs", "Total number of open PRs for given repository", openPRsURL, registry, tags);
            registerMetric("gh_repo_closed_prs", "Total number of closed PRs for given repository", closedPRsURL, registry, tags);

            registerMetric("gh_repo_open_issues", "Total number of open issues for given repository", openIssuesURL, openPRsURL, registry, tags);
            registerMetric("gh_repo_closed_issues", "Total number of closed issues for given repository", closedIssuesURL, closedPRsURL, registry, tags);

        } catch (MalformedURLException e) {
            log.error("Malformed URL", e);
        }

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
    }

    private void registerMetric(String name, String description, URL url, MetricRegistry registry, Tag... tags) {
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
    private void registerMetric(String name, String description, URL baseURL, URL decrementURL, MetricRegistry registry, Tag... tags) {
        registry.register(
                new ExtendedMetadataBuilder()
                        .withName(name)
                        .withType(MetricType.GAUGE)
                        .withDescription(description)
                        .skipsScopeInOpenMetricsExportCompletely(true)
                        .prependsScopeToOpenMetricsName(false)
                        .build(),
                (Gauge<Number>) () -> extractCountFromLinkHeader(baseURL) - extractCountFromLinkHeader(decrementURL),
                tags);
    }

    private int extractCountFromLinkHeader(URL url) {
        int count = 0;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", "token " + gitHubToken);
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
        } finally {
            con.disconnect();
        }
        return count;
    }
}
