package it.renvins.downloadit.api;

public class DownloadItProvider {

    private static IDownloadItAPI api;

    public static void setAPI(IDownloadItAPI a) {
        if (api != null) {
            throw new IllegalStateException("API already set!");
        }
        api = a;
    }

    public static IDownloadItAPI getAPI() {
        return api;
    }
}
