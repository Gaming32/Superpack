package io.github.gaming32.superpack;

import io.github.gaming32.superpack.util.GeneralUtilKt;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.*;

public final class MyPacks {
    @Data
    public static final class Modpack implements Comparable<Modpack> {
        private File path;
        private URL iconUrl;
        private String filename; // Might differ in the case of downloadCache files
        private String name;
        private String description;
        private byte[] hash;
        @Setter(AccessLevel.NONE)
        private transient long longHash;
        private transient ByteArrayHashWrapper hashWrapper;

        @Override
        public int compareTo(Modpack o) {
            return Objects.compare(name, o.name, String.CASE_INSENSITIVE_ORDER);
        }

        public void setHash(byte[] hash) {
            this.hash = hash;
            longHash = GeneralUtilKt.longHash(hash);
        }

        public long getLongHash() {
            if (longHash == 0L) {
                // Possible if hash is set via reflection (i.e. from Gson)
                return longHash = GeneralUtilKt.longHash(hash);
            }
            return longHash;
        }

        ByteArrayHashWrapper getHashWrapper() {
            return Objects.requireNonNullElseGet(hashWrapper, () -> hashWrapper = new ByteArrayHashWrapper(this));
        }

        void setHashWrapper(ByteArrayHashWrapper hashWrapper) {
            this.hashWrapper = hashWrapper;
            this.hash = hashWrapper.value;
            this.longHash = hashWrapper.longHash;
        }
    }

    private static final class ByteArrayHashWrapper {
        final byte[] value;
        final long longHash; // Faster comparison

        ByteArrayHashWrapper(byte[] value) {
            this.value = value;
            this.longHash = GeneralUtilKt.longHash(value);
        }

        ByteArrayHashWrapper(Modpack container) {
            value = container.hash;
            longHash = container.getLongHash();
        }

        @Override
        public int hashCode() {
            return Long.hashCode(longHash);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ByteArrayHashWrapper)) return false;
            final ByteArrayHashWrapper other = (ByteArrayHashWrapper)o;
            return longHash == other.longHash && Arrays.equals(value, other.value);
        }
    }

    public static final MyPacks INSTANCE = new MyPacks();

    @Getter
    private final SortedSet<Modpack> packs = new TreeSet<>();
    private Map<ByteArrayHashWrapper, Modpack> lookup = null;

    /**
     * Flag used to indicate whether the GUI needs updating
     */
    @Getter
    private boolean dirty = false;

    public MyPacks() {
    }

    public MyPacks(Set<Modpack> packs) {
        this.packs.addAll(packs);
    }

    public void setDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    private Map<ByteArrayHashWrapper, Modpack> computeLookup() {
        if (lookup != null) return lookup;
        lookup = new HashMap<>();
        for (final Modpack pack : packs) {
            lookup.put(pack.getHashWrapper(), pack);
        }
        return lookup;
    }

    public Modpack getPack(byte[] hash) {
        return computeLookup().get(new ByteArrayHashWrapper(hash));
    }

    public void addPack(Modpack pack) {
        packs.add(pack);
        computeLookup().put(pack.getHashWrapper(), pack);
    }

    public Modpack removePack(byte[] hash) {
        final Modpack pack = computeLookup().remove(new ByteArrayHashWrapper(hash));
        if (pack == null) return null;
        packs.remove(pack);
        return pack;
    }

    public void removePack(Modpack pack) {
        packs.remove(pack);
        computeLookup().remove(pack.getHashWrapper());
    }

    public boolean removeMissing() {
        boolean anyMissing = false;
        final Iterator<Modpack> it = packs.iterator();
        while (it.hasNext()) {
            if (!it.next().path.isFile()) {
                it.remove();
                anyMissing = true;
            }
        }
        return anyMissing;
    }

    public void copyTo(MyPacks other) {
        other.lookup = null;
        other.packs.clear();
        other.packs.addAll(packs);
    }

    public void copyFrom(MyPacks other) {
        copyTo(other);
    }

    public static MyPacks read(Reader reader) {
        return SuperpackSettings.GSON.fromJson(reader, MyPacks.class);
    }

    public void copyFromRead(Reader reader) {
        read(reader).copyTo(this);
    }

    public void write(Writer writer) {
        SuperpackSettings.GSON.toJson(this, writer);
    }
}
