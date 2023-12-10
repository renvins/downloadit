package it.renvins.downloadit;

import it.renvins.downloadit.api.DownloadItAPI;
import it.renvins.downloadit.api.DownloadItProvider;
import it.renvins.downloadit.log.DownloadItFormatter;
import it.renvins.downloadit.service.IDownloadService;
import it.renvins.downloadit.service.impl.DownloadService;
import lombok.Getter;
import net.bobolabs.config.Configuration;
import net.bobolabs.config.ConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadItLoader implements Service {

    public final static Logger LOGGER = Logger.getLogger("[downloadit]");

    @Getter private final Configuration config;
    private final IDownloadService downloadService;

    public DownloadItLoader() {
        setupLogger();

        this.config = ConfigurationLoader
                .fromFile(new File(System.getProperty("user.dir")), "config.yml")
                .setDefaultResource("config.yml")
                .load();

        this.downloadService = new DownloadService();

        DownloadItProvider.setAPI(new DownloadItAPI(config, downloadService));
    }

    @Override
    public void load() {
        downloadService.load();

        ProcessBuilder processBuilder = new ProcessBuilder(config.getStringList("commands"));
        try {
            LOGGER.info("Running commands...");
            processBuilder.start();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can't execute the commands!", e);
        }
    }

    private void setupLogger() {
        LOGGER.setUseParentHandlers(false);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new DownloadItFormatter());

        LOGGER.addHandler(consoleHandler);
    }
}
