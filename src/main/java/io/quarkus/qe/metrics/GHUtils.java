package io.quarkus.qe.metrics;

import org.jboss.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class GHUtils {
    private static final Logger log = Logger.getLogger(GHUtils.class);

    static void dumpHeaders(HttpURLConnection con) {
        con.getHeaderFields().forEach((key,value)-> {
            System.out.println(key + ": " + value);
        });
    }

    static int extractCountFromJSON(String ghToken, URL url) {
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

    static int extractCountFromLinkHeader(String ghToken, URL url) {
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

    public static JsonObject getJsonObject(String ghToken, String address) {
        HttpURLConnection con = null;
        try {
            URL url = new URL(address);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("Authorization", "token " + ghToken);
            con.setRequestProperty("User-Agent", "github-metrics");
            JsonReader jsonReader = Json.createReader(con.getInputStream());
            return jsonReader.readObject();
        } catch (IOException e) {
            log.error("Unable to get JsonObject data from URL " + address, e);
            dumpHeaders(con);
            return null;
        } finally {
            con.disconnect();
        }
    }

    public static boolean isTokenValid(String ghToken) {
        return getJsonObject(ghToken, "https://api.github.com/") != null;
    }
}
