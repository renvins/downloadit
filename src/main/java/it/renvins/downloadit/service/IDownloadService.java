package it.renvins.downloadit.service;

import it.renvins.downloadit.download.Download;

import java.io.IOException;

public interface IDownloadService extends Service {
    void download(Download download) throws IOException, InterruptedException;
}
