package io.quarkus.qe.metrics;

import io.smallrye.metrics.ExtendedMetadataBuilder;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Custom metrics based on direct curl-like interaction with GitHub
 *
 */
@ApplicationScoped
public class GHRepositoryCustomMetrics {

    private static final Logger log = Logger.getLogger(GHRepositoryCustomMetrics.class);
    private String ghToken;

    @Inject
    Config config;

    public void initiateGH(String ghToken) {
        this.ghToken = ghToken;
    }

    public void ghCustomMetrics(MetricRegistry registry, String repositoryName, Tag... tags) {
        try {
            String[] openIssues = config.getValue("gh.open.issues", String[].class);

            for (String openIssuesQuery : openIssues) {
                URL url = new URL("https://api.github.com/search/issues?per_page=1&" +
                        "q=repo:" + repositoryName + "+is:issue+is:open+" + openIssuesQuery.trim());
                registerMetric("gh_repo_open_issues", "Total number of open issues for given repository",
                        url, registry, addLabelToTags(tags, openIssuesQuery.trim()));
            }
        } catch (NoSuchElementException ex) {
            log.info("Configuration gh.open.issues is not defined");
        } catch (MalformedURLException e) {
            log.error("Malformed URL", e);
        }

        try {
            String[] closedIssues = config.getValue("gh.closed.issues", String[].class);

            for (String closedIssuesQuery : closedIssues) {
                URL url = new URL("https://api.github.com/search/issues?per_page=1&" +
                        "q=repo:" + repositoryName + "+is:issue+is:closed+" + closedIssuesQuery.trim());
                registerMetric("gh_repo_closed_issues", "Total number of closed issues for given repository",
                        url, registry, addLabelToTags(tags, closedIssuesQuery.trim()));
            }
        } catch (NoSuchElementException ex) {
            log.info("Configuration gh.closed.issues is not defined");
        } catch (MalformedURLException e) {
            log.error("Malformed URL", e);
        }
    }

    private Tag[] addLabelToTags(Tag[] originalTags, String label) {
        List<Tag> tagsList = new ArrayList<>();
        tagsList.addAll(Arrays.asList(originalTags));
        tagsList.add(new Tag("label", label.trim()));
        return tagsList.toArray(new Tag[0]);
    }

    private void registerMetric(String name, String description, URL url, MetricRegistry registry, Tag... tags) {
        log.debug("Registering metric for URL " + url.toExternalForm());
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
    }

}
