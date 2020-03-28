# gh-exporter
GitHub metrics exporter to MP / Prometheus metrics

```bash
GH_TOKEN=YOUR_GH_TOKEN mvn clean package quarkus:dev

curl http://0.0.0.0:8080/metrics/ 2>/dev/null | grep gh_
```