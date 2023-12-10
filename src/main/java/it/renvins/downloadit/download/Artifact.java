package it.renvins.downloadit.download;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class Artifact {

    private final String name;
    private final String path;
    private final String relocatedName;
}
