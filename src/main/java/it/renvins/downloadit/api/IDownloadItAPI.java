package it.renvins.downloadit.api;

import it.renvins.downloadit.service.IDownloadService;
import net.bobolabs.config.Configuration;

public interface IDownloadItAPI {

    Configuration getConfig();
    IDownloadService getDownloadService();

}
