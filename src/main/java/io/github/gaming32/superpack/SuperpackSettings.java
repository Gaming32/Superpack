package io.github.gaming32.superpack;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.gaming32.superpack.labrinth.LabrinthGson;
import io.github.gaming32.superpack.themes.Theme;
import io.github.gaming32.superpack.themes.Themes;
import io.github.gaming32.superpack.util.GeneralUtilKt;
import lombok.Data;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.SortedSet;

@Data
public final class SuperpackSettings {
    private static final Logger LOGGER = GeneralUtilKt.getLogger();

    public static final Gson GSON = LabrinthGson.GSON.newBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
        .registerTypeAdapterFactory(new TypeAdapterFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                if (type.getRawType() != MyPacks.class) return null;
                final var packsAdapter = gson.getAdapter(new TypeToken<SortedSet<MyPacks.Modpack>>() {});
                return (TypeAdapter<T>)new TypeAdapter<MyPacks>() {
                    @Override
                    public void write(JsonWriter out, MyPacks value) throws IOException {
                        packsAdapter.write(out, value.getPacks());
                    }

                    @Override
                    public MyPacks read(JsonReader in) throws IOException {
                        return new MyPacks(packsAdapter.read(in));
                    }
                };
            }
        })
        .registerTypeHierarchyAdapter(File.class, new TypeAdapter<File>() {
            @Override
            public void write(JsonWriter out, File value) throws IOException {
                out.value(value.getPath());
            }

            @Override
            public File read(JsonReader in) throws IOException {
                return new File(in.nextString());
            }
        }.nullSafe())
        .registerTypeAdapter(Theme.class, new TypeAdapter<Theme>() {
            @Override
            public void write(JsonWriter out, Theme theme) throws IOException {
                out.value(theme.getId());
            }

            @Override
            public Theme read(JsonReader in) throws IOException {
                final String id = in.nextString();
                final Theme theme = Themes.getTheme(id);
                if (theme == null) {
                    LOGGER.warn("Unknown theme {}", id);
                    return Themes.DEFAULT;
                }
                return theme;
            }
        }.nullSafe())
        .create();
    public static final SuperpackSettings INSTANCE = new SuperpackSettings();

    private Theme theme = Themes.DEFAULT;
    private int parallelDownloadCount = Runtime.getRuntime().availableProcessors() - 1;

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public void copyTo(SuperpackSettings other) {
        other.theme = theme;
        other.parallelDownloadCount = parallelDownloadCount;
    }

    public void copyFrom(SuperpackSettings other) {
        copyTo(other);
    }

    public static SuperpackSettings read(Reader reader) {
        return GSON.fromJson(reader, SuperpackSettings.class);
    }

    public void copyFromRead(Reader reader) {
        read(reader).copyTo(this);
    }

    public void write(Writer writer) {
        GSON.toJson(this, writer);
    }
}
