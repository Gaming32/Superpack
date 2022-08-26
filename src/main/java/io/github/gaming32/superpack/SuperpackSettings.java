package io.github.gaming32.superpack;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.SortedSet;

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
import lombok.Data;

@Data
public final class SuperpackSettings {
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
                return Themes.getTheme(in.nextString());
            }
        }.nullSafe())
        .create();
    public static final SuperpackSettings INSTANCE = new SuperpackSettings();

    private Theme theme;

    public void copyTo(SuperpackSettings other) {
        other.theme = theme;
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
