package io.github.gaming32.superpack.labrinth;

import java.net.URL;
import java.util.Date;

import com.google.gson.annotations.JsonAdapter;

import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility;
import lombok.Data;

@Data
public class SearchResults {
    private Result[] hits;
    private int offset;
    private int limit;
    private int totalHits;

    @Data
    public static class Result implements HasId {
        private ModrinthId projectId;
        private String projectType;
        private String slug;
        private String author;
        private String title;
        private String description;
        private String[] categories;
        private String[] displayCategories;
        private String[] versions;
        private int downloads;
        private int follows;
        @JsonAdapter(LabrinthGson.EmptyMeansNullUrl.class)
        private URL iconUrl;
        private Date dateCreated;
        private Date dateModified;
        private String latestVersion;
        private String license;
        private EnvCompatibility clientSide;
        private EnvCompatibility serverSide;
        private URL[] gallery;

        @Override
        public ModrinthId getId() {
            return projectId;
        }

        @Override
        public void setId(ModrinthId id) {
            setProjectId(id);
        }
    }
}
