package it.renvins.downloadit.download.info;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class RepoInfo {

    private final String repo;
    private final String branch;
    private final String version;
}
