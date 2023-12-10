package it.renvins.downloadit.download;

import it.renvins.downloadit.download.info.RepoInfo;
import lombok.Getter;

import java.util.Set;
@Getter
public class Download {

    private final RepoInfo repoInfo;
    private final Set<Artifact> artifacts;

    public Download(RepoInfo repoInfo, Set<Artifact> artifacts) {
        this.repoInfo = repoInfo;
        this.artifacts = artifacts;
    }
}
