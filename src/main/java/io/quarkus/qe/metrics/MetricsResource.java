package io.quarkus.qe.metrics;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

@Path("/gh")
public class MetricsResource {

    @Inject
    GitHubMetrics m;

    @Inject
    MetricRegistry registry;

    @ConfigProperty(name = "gh.token")
    String gitHubToken;

    @ConfigProperty(name = "gh.repos", defaultValue = "quarkusio/quarkus, quarkusio/quarkus-quickstarts, quarkusio/quarkusio.github.io")
    public List<String> ghRepos;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws IOException {

//        registry.register("rs", (Gauge<Object>) () -> System.currentTimeMillis());
//        registry.register("rs_size", (Gauge<Object>) () -> ghRepos.size());
//        MetricRegistries.get(MetricRegistry.Type.VENDOR).concurrentGauge("tryit");
//        Metadata metadata = new ExtendedMetadata("rscounter", "rscounter", "awesome", MetricType.COUNTER,
//                "none", null, false, Optional.of(false));
//        MetricRegistries.get(MetricRegistry.Type.VENDOR).counter(metadata);
//        registry.concurrentGauge("foollll");
//        Metric starsMetric = (Gauge<Number>) () -> ThreadLocalRandom.current().nextInt(10, 1000);


//        GitHub github = GitHub.connectAnonymously();
        GitHub github = new GitHubBuilder().withOAuthToken(gitHubToken).build();
        GHRepository ghRepo = github.getRepository("quarkusio/quarkus");

        return ghRepo.toString();
    }

}