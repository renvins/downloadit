package it.renvins.downloadit.download.info;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class JenkinsInfo {

    private final String user;
    private final String password;
    private final String url;
}
