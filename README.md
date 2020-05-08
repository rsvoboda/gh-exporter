# gh-exporter
GitHub metrics exporter to MP / Prometheus metrics

```bash
GH_TOKEN=YOUR_GH_TOKEN mvn clean package quarkus:dev

curl http://0.0.0.0:8080/metrics/ 2>/dev/null | grep gh_
```

## Docker image
https://hub.docker.com/r/rostasvo/gh-exporter

```
docker pull rostasvo/gh-exporter

docker run --env GH_TOKEN=CHANGE_ME -i --rm -p 8080:8080 rostasvo/gh-exporter:1.0-SNAPSHOT
```

## Metrics
All metrics have type `gauge`, there are 3 levels of details which can be reported.

### BASE
```
# HELP gh_rate_remaining Number of API queries remaining in the current window
# HELP gh_repo_forks Total number of forks for given repository
# HELP gh_repo_open_issues_and_prs Total number of open issues and PRs for given repository
# HELP gh_repo_size Size in kB for given repository
# HELP gh_repo_stars Total number of Stars for given repository
# HELP gh_repo_subscribers Total number of watchers/subscribers for given repository

gh_rate_remaining 4948.0
gh_repo_forks{repo="quarkusio/quarkus-http"} 14.0
gh_repo_open_issues_and_prs{repo="quarkusio/quarkus-http"} 1.0
gh_repo_size{repo="quarkusio/quarkus-http"} 11184.0
gh_repo_stars{repo="quarkusio/quarkus-http"} 11.0
gh_repo_subscribers{repo="quarkusio/quarkus-http"} 18.0
```
### ADVANCED
```
# HELP gh_rate_remaining Number of API queries remaining in the current window
# HELP gh_repo_closed_issues Total number of closed issues for given repository
# HELP gh_repo_closed_prs Total number of closed prs for given repository
# HELP gh_repo_commits Total number of commits for given repository
# HELP gh_repo_contributors Total number of contributors for given repository
# HELP gh_repo_forks Total number of forks for given repository
# HELP gh_repo_merged_prs Total number of merged prs for given repository
# HELP gh_repo_open_issues Total number of open issues for given repository
# HELP gh_repo_open_issues_and_prs Total number of open issues and PRs for given repository
# HELP gh_repo_open_prs Total number of open prs for given repository
# HELP gh_repo_size Size in kB for given repository
# HELP gh_repo_stars Total number of Stars for given repository
# HELP gh_repo_subscribers Total number of watchers/subscribers for given repository
# HELP gh_repo_tags Total number of tags/releases for given repository

gh_rate_remaining 4946.0
gh_repo_closed_issues{repo="quarkusio/quarkus-quickstarts"} 77.0
gh_repo_closed_issues{repo="quarkusio/quarkusio.github.io"} 169.0
gh_repo_closed_prs{repo="quarkusio/quarkus-quickstarts"} 376.0
gh_repo_closed_prs{repo="quarkusio/quarkusio.github.io"} 275.0
gh_repo_commits{repo="quarkusio/quarkus-quickstarts"} 817.0
gh_repo_commits{repo="quarkusio/quarkusio.github.io"} 739.0
gh_repo_contributors{repo="quarkusio/quarkus-quickstarts"} 73.0
gh_repo_contributors{repo="quarkusio/quarkusio.github.io"} 55.0
gh_repo_forks{repo="quarkusio/quarkus-quickstarts"} 400.0
gh_repo_forks{repo="quarkusio/quarkusio.github.io"} 140.0
gh_repo_merged_prs{repo="quarkusio/quarkus-quickstarts"} 305.0
gh_repo_merged_prs{repo="quarkusio/quarkusio.github.io"} 185.0
gh_repo_open_issues_and_prs{repo="quarkusio/quarkus-quickstarts"} 64.0
gh_repo_open_issues_and_prs{repo="quarkusio/quarkusio.github.io"} 55.0
gh_repo_open_issues{repo="quarkusio/quarkus-quickstarts"} 40.0
gh_repo_open_issues{repo="quarkusio/quarkusio.github.io"} 50.0
gh_repo_open_prs{repo="quarkusio/quarkus-quickstarts"} 24.0
gh_repo_open_prs{repo="quarkusio/quarkusio.github.io"} 3.0
gh_repo_size{repo="quarkusio/quarkus-quickstarts"} 1943.0
gh_repo_size{repo="quarkusio/quarkusio.github.io"} 72625.0
gh_repo_stars{repo="quarkusio/quarkus-quickstarts"} 580.0
gh_repo_stars{repo="quarkusio/quarkusio.github.io"} 53.0
gh_repo_subscribers{repo="quarkusio/quarkus-quickstarts"} 52.0
gh_repo_subscribers{repo="quarkusio/quarkusio.github.io"} 24.0
gh_repo_tags{repo="quarkusio/quarkus-quickstarts"} 49.0
gh_repo_tags{repo="quarkusio/quarkusio.github.io"} 0.0
```
### VERBOSE
```
# HELP gh_rate_remaining Number of API queries remaining in the current window
# HELP gh_repo_closed_issues Total number of closed issues for given repository
# HELP gh_repo_closed_issues_last_24h Total number of closed issues for given repository in last 24 hours
# HELP gh_repo_closed_prs Total number of closed prs for given repository
# HELP gh_repo_closed_prs_last_24h Total number of closed prs for given repository in last 24 hours
# HELP gh_repo_commits Total number of commits for given repository
# HELP gh_repo_contributors Total number of contributors for given repository
# HELP gh_repo_created_issues_last_24h Total number of created issues for given repository in last 24 hours
# HELP gh_repo_created_prs_last_24h Total number of created prs for given repository in last 24 hours
# HELP gh_repo_forks Total number of forks for given repository
# HELP gh_repo_merged_prs Total number of merged prs for given repository
# HELP gh_repo_merged_prs_last_24h Total number of merged prs for given repository in last 24 hours
# HELP gh_repo_open_issues Total number of open issues for given repository
# HELP gh_repo_open_issues_and_prs Total number of open issues and PRs for given repository
# HELP gh_repo_open_prs Total number of open prs for given repository
# HELP gh_repo_size Size in kB for given repository
# HELP gh_repo_stars Total number of Stars for given repository
# HELP gh_repo_subscribers Total number of watchers/subscribers for given repository
# HELP gh_repo_tags Total number of tags/releases for given repository

gh_rate_remaining 4885.0
gh_repo_closed_issues_last_24h{repo="quarkusio/quarkus"} 2.0
gh_repo_closed_issues{label="kind/bug",repo="quarkusio/quarkus"} 1309.0
gh_repo_closed_issues{label="kind/enhancement",repo="quarkusio/quarkus"} 582.0
gh_repo_closed_issues{label="kind/epic",repo="quarkusio/quarkus"} 25.0
gh_repo_closed_issues{label="kind/extension-proposal",repo="quarkusio/quarkus"} 14.0
gh_repo_closed_issues{label="kind/question",repo="quarkusio/quarkus"} 168.0
gh_repo_closed_issues{label="triage/duplicate",repo="quarkusio/quarkus"} 121.0
gh_repo_closed_issues{label="triage/invalid",repo="quarkusio/quarkus"} 239.0
gh_repo_closed_issues{repo="quarkusio/quarkus"} 2804.0
gh_repo_closed_prs_last_24h{repo="quarkusio/quarkus"} 2.0
gh_repo_closed_prs{label="triage/duplicate",repo="quarkusio/quarkus"} 11.0
gh_repo_closed_prs{label="triage/invalid",repo="quarkusio/quarkus"} 378.0
gh_repo_closed_prs{repo="quarkusio/quarkus"} 4621.0
gh_repo_commits{repo="quarkusio/quarkus"} 11016.0
gh_repo_contributors{repo="quarkusio/quarkus"} 261.0
gh_repo_created_issues_last_24h{repo="quarkusio/quarkus"} 3.0
gh_repo_created_prs_last_24h{repo="quarkusio/quarkus"} 6.0
gh_repo_forks{repo="quarkusio/quarkus"} 788.0
gh_repo_merged_prs_last_24h{repo="quarkusio/quarkus"} 1.0
gh_repo_merged_prs{repo="quarkusio/quarkus"} 4103.0
gh_repo_open_issues_and_prs{repo="quarkusio/quarkus"} 1026.0
gh_repo_open_issues{label="kind/bug",repo="quarkusio/quarkus"} 343.0
gh_repo_open_issues{label="kind/enhancement",repo="quarkusio/quarkus"} 368.0
gh_repo_open_issues{label="kind/epic",repo="quarkusio/quarkus"} 35.0
gh_repo_open_issues{label="kind/extension-proposal",repo="quarkusio/quarkus"} 31.0
gh_repo_open_issues{label="kind/question",repo="quarkusio/quarkus"} 34.0
gh_repo_open_issues{repo="quarkusio/quarkus"} 925.0
gh_repo_open_prs{repo="quarkusio/quarkus"} 101.0
gh_repo_size{repo="quarkusio/quarkus"} 50593.0
gh_repo_stars{repo="quarkusio/quarkus"} 4328.0
gh_repo_subscribers{repo="quarkusio/quarkus"} 170.0
gh_repo_tags{repo="quarkusio/quarkus"} 57.0
```
### CUSTOM
```
# HELP gh_rate_remaining Number of API queries remaining in the current window
# HELP gh_repo_closed_issues Total number of closed issues for given repository
# HELP gh_repo_open_issues Total number of open issues for given repository
```
Example for
```properties
gh.repos=quarkusio/quarkus
gh.details.level=CUSTOM
gh.open.issues=\
  label:priority/blocker
gh.closed.issues=${gh.open.issues},\
  label:kind/bug+-label:triage/invalid+-label:triage/duplicate,\
  label:kind/enhancement+-label:triage/invalid+-label:triage/duplicate
```
configuration:
```
gh_repo_closed_issues{label="label:kind/bug+-label:triage/invalid+-label:triage/duplicate",repo="quarkusio/quarkus"} 1229.0
gh_repo_closed_issues{label="label:kind/enhancement+-label:triage/invalid+-label:triage/duplicate",repo="quarkusio/quarkus"} 571.0
gh_repo_closed_issues{label="label:priority/blocker",repo="quarkusio/quarkus"} 4.0
gh_repo_open_issues{label="label:priority/blocker",repo="quarkusio/quarkus"} 1.0
```

## Calls to GitHub Search API
The Search API has a custom rate limit, you can make up to 30 authenticated requests per minute.

Number of GitHub Search API calls per gh-exporter details level:
- BASE: 0
- ADVANCED: 3
- VERBOSE: 22
- CUSTOM: `gh.open.issues` + `gh.closed.issues` entries

## Release
```bash
mvn release:prepare
mvn release:clean

git checkout $TAG

mvn clean package -Dnative -Dquarkus.native.container-build=true \
  -Dquarkus.container-image.build=true \
  -Dquarkus.container-image.push=true \
  -Dquarkus.container-image.username=rostasvo \
  -Dquarkus.container-image.password=$PASSWORD \
  -Dquarkus.container-image.registry=docker.io \
  -Dquarkus.container-image.group=rostasvo
```
