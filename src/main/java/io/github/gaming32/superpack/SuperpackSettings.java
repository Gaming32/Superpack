package io.github.gaming32.superpack;

import java.io.Reader;
import java.io.Writer;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;

import io.github.gaming32.superpack.labrinth.LabrinthGson;
import lombok.Data;

@Data
public final class SuperpackSettings {
    public static final Gson GSON = LabrinthGson.GSON.newBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
        .create();
    public static final SuperpackSettings INSTANCE = new SuperpackSettings();

    private boolean checkForPackOnModrinth = true;

    public void copyTo(SuperpackSettings other) {
        other.checkForPackOnModrinth = checkForPackOnModrinth;
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
