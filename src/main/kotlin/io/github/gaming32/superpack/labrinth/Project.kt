package io.github.gaming32.superpack.labrinth

import com.google.gson.annotations.SerializedName
import io.github.gaming32.mrpacklib.Mrpack.EnvCompatibility
import java.net.URL
import java.util.*

data class Project(
    override var id: ModrinthId?,
    override var slug: String,
    override var projectType: BaseProject.ProjectType,
    var team: ModrinthId?,
    override var title: String,
    override var description: String,
    var body: String,
    var bodyUrl: URL?,
    var published: Date,
    var updated: Date,
    var approved: Date,
    var status: Status,
    var moderatorMessage: ModeratorMessage?,
    var license: License,
    override var clientSide: EnvCompatibility,
    override var serverSide: EnvCompatibility,
    override var downloads: Int,
    override var followers: Int,
    override var categories: List<String>,
    var additionalCategories: List<String>,
    var versions: List<ModrinthId>,
    override var iconUrl: URL?,
    var issuesUrl: URL?,
    var sourceUrl: URL?,
    var wikiUrl: URL?,
    var discordUrl: URL?,
    var donationUrls: List<DonationUrl>,
    var gallery: List<GalleryImage>
) : BaseProject {
    enum class Status {
        @SerializedName("approved") APPROVED,
        @SerializedName("rejected") REJECTED,
        @SerializedName("draft") DRAFT,
        @SerializedName("unlisted") UNLISTED,
        @SerializedName("archived") ARCHIVED,
        @SerializedName("processing") PROCESSING,
        @SerializedName("unknown") UNKNOWN
    }

    data class ModeratorMessage(
        var message: String,
        var body: String?
    )

    data class License(
        var id: String,
        var name: String,
        var url: URL?
    )

    data class DonationUrl(
        var id: String,
        var platform: String,
        var url: URL
    )

    data class GalleryImage(
        var url: URL,
        var featured: Boolean,
        var title: String?,
        var description: String?,
        var created: Date
    )
}
