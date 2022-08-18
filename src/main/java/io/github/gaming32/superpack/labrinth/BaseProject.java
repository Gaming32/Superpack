package io.github.gaming32.superpack.labrinth;

import java.net.URL;

import com.google.gson.annotations.SerializedName;

import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility;

public interface BaseProject extends HasId {
    enum ProjectType {
        @SerializedName("mod") MOD,
        @SerializedName("modpack") MODPACK
    }

    String getSlug();
    void setSlug(String slug);
    ProjectType getProjectType();
    void setProjectType(ProjectType projectType);
    String getTitle();
    void setTitle(String title);
    String getDescription();
    void setDescription(String description);
    String[] getCategories();
    void setCategories(String[] categories);
    int getDownloads();
    void setDownloads(int downloads);
    int getFollowers();
    void setFollowers(int followers);
    URL getIconUrl();
    void setIconUrl(URL iconUrl);
    EnvCompatibility getClientSide();
    void setClientSide(EnvCompatibility clientSide);
    EnvCompatibility getServerSide();
    void setServerSide(EnvCompatibility serverSide);
}
