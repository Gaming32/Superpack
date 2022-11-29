package io.github.gaming32.superpack.labrinth

import com.google.gson.annotations.SerializedName
import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility
import java.net.URL

interface BaseProject : HasId {
    enum class ProjectType {
        @SerializedName("mod") MOD,
        @SerializedName("modpack") MODPACK
    }

    var slug: String
    var projectType: ProjectType
    var title: String
    var description: String
    var categories: List<String>
    var downloads: Int
    var followers: Int
    var iconUrl: URL?
    var clientSide: EnvCompatibility
    var serverSide: EnvCompatibility
}
