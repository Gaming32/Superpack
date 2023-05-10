package io.github.gaming32.superpack.labrinth;

import com.google.gson.annotations.SerializedName;
import io.github.gaming32.superpack.util.GeneralUtilKt;
import lombok.Data;

import java.net.URL;
import java.util.Arrays;
import java.util.Date;

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
    private VersionType versionType;
    private Version.File[] files;
    private Dependency[] dependencies;
    private String[] gameVersions;
    private String[] loaders;

    public File getPrimaryFile() {
        return Arrays.stream(files)
            .filter(Version.File::isPrimary)
            .findFirst()
            .orElse(files[0]);
    }

    public enum VersionType {
        @SerializedName("release") RELEASE,
        @SerializedName("beta") BETA,
        @SerializedName("alpha") ALPHA;

        @Override
        public String toString() {
            return GeneralUtilKt.capitalize(name());
        }
    }

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
        private DependencyType dependencyType;

        public enum DependencyType {
            @SerializedName("required") REQUIRED,
            @SerializedName("optional") OPTIONAL,
            @SerializedName("incompatible") INCOMPATIBLE,
            @SerializedName("embedded") EMBEDDED
        }
    }
}
