package io.github.gaming32.superpack.labrinth;

import java.util.Objects;

public final class ModrinthId {
    private final String id;

    public ModrinthId(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModrinthId)) {
            return false;
        }
        final ModrinthId other = (ModrinthId)o;
        return id.equals(other.id);
    }
}
