package io.github.gaming32.superpack.labrinth;

import java.net.URL;
import java.util.Date;

import lombok.Data;

@Data
public class Version implements HasId {
    private ModrinthId id;
    private ModrinthId projectId;
    private ModrinthId authorId;
    private boolean featured;
    private String name;
    private String versionNumber;
    private String changelog;
    private URL changelogUrl;
    private Date datePublished;
    private int downloads;
    private String versionType;
    private Version.File[] files;
    private Dependency[] dependencies;
    private String[] gameVersions;
    private String[] loaders;

    @Data
    public static class File {
        private Hashes hashes;
        private URL url;
        private String filename;
        private boolean primary;
        private long size;

        @Data
        public static class Hashes {
            private byte[] sha1;
            private byte[] sha512;
        }
    }

    @Data
    public static class Dependency {
        private ModrinthId versionId;
        private ModrinthId projectId;
        private String fileName;
        private String dependencyType;
    }
}
