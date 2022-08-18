package io.github.gaming32.superpack.labrinth;

import java.net.URL;
import java.util.Date;

import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility;
import lombok.Data;

@Data
public class Project implements BaseProject {
    private ModrinthId id;
    private String slug;
    private String projectType;
    private ModrinthId team;
    private String title;
    private String description;
    private String body;
    private URL bodyUrl;
    private Date published;
    private Date updated;
    private Date approved;
    private String status;
    private ModeratorMessage moderatorMessage;
    private License license;
    private EnvCompatibility clientSide;
    private EnvCompatibility serverSide;
    private int downloads;
    private int followers;
    private String[] categories;
    private String[] additionalCategories;
    private ModrinthId[] versions;
    private URL iconUrl;
    private URL issuesUrl;
    private URL sourceUrl;
    private URL wikiUrl;
    private URL discordUrl;
    private DonationUrl[] donationUrls;
    private GalleryImage[] gallery;

    @Data
    public static class ModeratorMessage {
        private String message;
        private String body;
    }

    @Data
    public static class License implements HasId {
        private ModrinthId id;
        private String name;
        private URL url;
    }

    @Data
    public static class DonationUrl implements HasId {
        private ModrinthId id;
        private String platform;
        private URL url;
    }

    @Data
    public static class GalleryImage {
        private URL url;
        private boolean featured;
        private String title;
        private String description;
        private Date created;
    }
}
