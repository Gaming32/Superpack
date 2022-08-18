package io.github.gaming32.superpack.labrinth;

import java.io.IOException;
import java.net.URL;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import io.github.gaming32.mrpacklib.util.GsonHelper;

public final class LabrinthGson {
    public static final class EmptyMeansNullUrl extends TypeAdapter<URL> {
        @Override
        public void write(JsonWriter out, URL value) throws IOException {
            out.value(value == null ? "" : value.toExternalForm());
        }

        @Override
        public URL read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            final String value = in.nextString();
            if (value.isEmpty()) return null;
            return new URL(value);
        }
    }

    public static final Gson GSON = GsonHelper.GSON.newBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setDateFormat("yyyy-MM-dd'T'HH:mmZ")
        .registerTypeAdapter(ModrinthId.class, new TypeAdapter<ModrinthId>() {
            @Override
            public void write(JsonWriter out, ModrinthId value) throws IOException {
                out.value(value.getId());
            }

            @Override
            public ModrinthId read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                return new ModrinthId(in.nextString());
            }
        })
        .create();

    private LabrinthGson() {
    }
}
