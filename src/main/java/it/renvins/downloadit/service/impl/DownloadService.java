package it.renvins.downloadit.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.renvins.downloadit.DownloadItLoader;
import it.renvins.downloadit.api.DownloadItProvider;
import it.renvins.downloadit.download.Artifact;
import it.renvins.downloadit.download.Download;
import it.renvins.downloadit.download.info.JenkinsInfo;
import it.renvins.downloadit.download.info.RepoInfo;
import it.renvins.downloadit.service.IDownloadService;
import net.bobolabs.config.ConfigurationSection;
import net.bobolabs.config.TraversalMode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

public class DownloadService implements IDownloadService {

    private final Set<Download> downloads = new HashSet<>();
    private JenkinsInfo jenkinsInfo;

    @Override
    public void load() {
        DownloadItLoader.LOGGER.info("Loading download service...");

        loadCredentials();
        loadDownloads();

        downloads.forEach(this::download);
    }

    @Override
    public void download(Download download) {
        String url = jenkinsInfo.getUrl() + "job/" + download.getRepoInfo().getRepo() + "/job/" + download.getRepoInfo().getBranch();
        if (download.getRepoInfo().getVersion().equals("latest")) {
            url += "/lastSuccessfulBuild";
        }
        String headerAuth = "Basic " + encodeBase64(jenkinsInfo.getUser() + ":" + jenkinsInfo.getPassword());

        HttpClient httpClient = HttpClient.newHttpClient();
        try {
            DownloadItLoader.LOGGER.info("Connecting to Jenkins to download from " + download.getRepoInfo().getRepo() + "'s repository...");
            HttpResponse<String> response = httpClient
                    .send(HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(url + "/api/json"))
                            .headers("Authorization", headerAuth).build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                DownloadItLoader.LOGGER.info("Connected successfully!");
                if (download.getRepoInfo().getVersion().equals("latest")) {
                    selectArtifact(httpClient, url, headerAuth, JsonParser.parseString(response.body()).getAsJsonObject(), download);
                    return;
                }

                JsonArray builds = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("builds");
                for (JsonElement buildElement : builds) {
                    JsonObject tempBuild = buildElement.getAsJsonObject();
                    String buildUrl = tempBuild.get("url").getAsString();

                    if(!checkVersion(httpClient, buildUrl, headerAuth, download)) {
                        continue;
                    }
                    break;
                }
            } else {
                DownloadItLoader.LOGGER.severe("Error while connecting to Jenkins to download " + download.getRepoInfo().getRepo() + "'s repository!");
            }
        } catch (IOException | InterruptedException e) {
            DownloadItLoader.LOGGER.log(Level.SEVERE, "Can't download from " + download.getRepoInfo().getRepo() + "'s repository!", e);
        }
    }

    private boolean checkVersion(HttpClient httpClient, String buildUrl, String headerAuth, Download download) throws IOException, InterruptedException {
        HttpResponse<String> buildResponse = httpClient
                .send(HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(buildUrl + "/api/json"))
                        .headers("Authorization", headerAuth).build(), HttpResponse.BodyHandlers.ofString());

        JsonObject build = JsonParser.parseString(buildResponse.body()).getAsJsonObject();
        JsonArray actions = build.getAsJsonArray("actions");

        for (JsonElement actionElement : actions) {
            JsonObject action = actionElement.getAsJsonObject();
            if (!action.has("lastBuiltRevision")) {
                continue;
            }
            JsonObject lastRevision = action.getAsJsonObject("lastBuiltRevision");
            if (!lastRevision.has("SHA1")) {
                continue;
            }
            if (!lastRevision.get("SHA1").getAsString().equals(download.getRepoInfo().getVersion())) {
                return false;
            }
            selectArtifact(httpClient, buildUrl, headerAuth, build, download);
            return true;
        }
        return false;
    }

    private void selectArtifact(HttpClient httpClient, String buildUrl, String headerAuth, JsonObject build, Download download) throws IOException, InterruptedException {
        DownloadItLoader.LOGGER.info("Found matching version for " + download.getRepoInfo().getRepo() + "'s repository!");
        JsonArray artifacts = build.getAsJsonArray("artifacts");

        for (JsonElement artifactElement : artifacts) {
            JsonObject artifact = artifactElement.getAsJsonObject();
            String fileName = artifact.get("fileName").getAsString();

            Optional<Artifact> optionalArtifact = download.getArtifacts().stream().filter(art -> art.getName().equalsIgnoreCase(fileName)).findFirst();
            if(optionalArtifact.isEmpty()) {
                continue;
            }
            DownloadItLoader.LOGGER.info("Downloading " + fileName + "...");

            String relativePath = artifact.get("relativePath").getAsString();
            downloadArtifact(httpClient, buildUrl, relativePath, headerAuth, optionalArtifact.get());
        }
    }

    private void downloadArtifact(HttpClient httpClient, String buildUrl, String relativePath, String headerAuth, Artifact artifact) throws IOException, InterruptedException {
        HttpResponse<InputStream> downloadResponse = httpClient
                .send(HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(buildUrl + "/artifact/" + relativePath))
                        .headers("Authorization", headerAuth).build(), HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream inputStream = downloadResponse.body();
             FileOutputStream outputStream = new FileOutputStream(System.getProperty("user.dir")
                     + "/"
                     + artifact.getPath()
                     + artifact.getRelocatedName())) {

            // Leggi e scrivi i dati dallo stream di input al file di destinazione
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            DownloadItLoader.LOGGER.info(artifact.getRelocatedName()+ " downloaded correctly with relocated name!");
        }
    }

    private void loadCredentials() {
        DownloadItLoader.LOGGER.info("Loading Jenkins' credentials...");

        ConfigurationSection jenkinsSection = DownloadItProvider.getAPI().getConfig().getSection("jenkins");
        jenkinsInfo = new JenkinsInfo(jenkinsSection.getString("user"), jenkinsSection.getString("password"), jenkinsSection.getString("globalUrl"));
    }

    private void loadDownloads() {
        DownloadItLoader.LOGGER.info("Loading downloads in cache...");
        ConfigurationSection downloadsSection = DownloadItProvider.getAPI().getConfig().getSection("downloads");

        for (String plugin : downloadsSection.getKeys(TraversalMode.ROOT)) {
            ConfigurationSection pluginSection = downloadsSection.getSection(plugin);

            String repo = pluginSection.getString("repo");
            String branch = pluginSection.getString("branch");
            String version = pluginSection.getString("version");

            RepoInfo repoInfo = new RepoInfo(repo, branch, version);
            Set<Artifact> artifacts = loadArtifacts(pluginSection);

            downloads.add(new Download(repoInfo, artifacts));
        }
    }

    private Set<Artifact> loadArtifacts(ConfigurationSection pluginSection) {
        Set<Artifact> artifacts = new HashSet<>();
        ConfigurationSection artifactsSection = pluginSection.getSection("artifacts");

        for (String number : artifactsSection.getKeys(TraversalMode.ROOT)) {
            ConfigurationSection artifactSection = artifactsSection.getSection(number);

            String name = artifactSection.getString("name");
            String path = artifactSection.getString("path");
            String relocatedName = artifactSection.getString("relocatedName");

            artifacts.add(new Artifact(name, path, relocatedName));
        }
        return artifacts;
    }

    private String encodeBase64(String string) {
        return Base64.getEncoder().encodeToString(string.getBytes());
    }
}
