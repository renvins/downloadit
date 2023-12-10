package it.renvins.downloadit.api;

import it.renvins.downloadit.service.IDownloadService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bobolabs.config.Configuration;

@RequiredArgsConstructor @Getter
public class DownloadItAPI implements IDownloadItAPI {

    private final Configuration config;
    private final IDownloadService downloadService;
}
