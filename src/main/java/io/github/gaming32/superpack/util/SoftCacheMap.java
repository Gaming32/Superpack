package io.github.gaming32.superpack.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class SoftCacheMap<K, V> {
    private final Map<K, SoftReference<V>> map;
    private final IdentityHashMap<SoftReference<V>, K> reverseMap;
    private final ReferenceQueue<V> queue;
    private final Function<K, V> factory;

    public SoftCacheMap(Function<K, V> factory) {
        map = new ConcurrentHashMap<>();
        reverseMap = new IdentityHashMap<>();
        queue = new ReferenceQueue<>();
        this.factory = factory;
    }

    public SoftCacheMap() {
        this(null);
    }

    private SoftReference<V> newValue(V value) {
        return new SoftReference<>(value, queue);
    }

    @SuppressWarnings("unchecked")
    private void expungeStaleEntries() {
        SoftReference<V> ref;
        while ((ref = (SoftReference<V>)queue.poll()) != null) {
            final K key;
            synchronized (reverseMap) {
                key = reverseMap.remove(ref);
            }
            if (key != null) {
                map.remove(key);
            }
        }
    }

    public V get(K key) {
        return get(key, factory);
    }

    public V get(K key, Function<K, V> factory) {
        Objects.requireNonNull(factory, "Must supply factory if no default is specified");
        expungeStaleEntries();
        final SoftReference<V> ref = map.get(key);
        if (ref == null) {
            final V value = factory.apply(key);
            if (value != null) {
                final SoftReference<V> newRef = newValue(value);
                map.put(key, newRef);
                synchronized (reverseMap) {
                    reverseMap.put(newRef, key);
                }
                return value;
            }
        }
        return ref != null ? ref.get() : null;
    }

    public V regenerate(K key) {
        return regenerate(key, factory);
    }

    public V regenerate(K key, Function<K, V> factory) {
        Objects.requireNonNull(factory, "Must supply factory if no default is specified");
        expungeStaleEntries();
        final V value = factory.apply(key);
        if (value == null) {
            final SoftReference<V> oldRef = map.remove(key);
            if (oldRef != null) {
                synchronized (reverseMap) {
                    reverseMap.remove(oldRef);
                }
            }
            return null;
        }
        final SoftReference<V> newRef = newValue(value);
        final SoftReference<V> oldRef = map.replace(key, newRef);
        if (oldRef != null) {
            oldRef.clear();
            synchronized (reverseMap) {
                reverseMap.remove(oldRef);
            }
        }
        synchronized (reverseMap) {
            reverseMap.put(newRef, key);
        }
        return value;
    }

    public V put(K key, V value) {
        expungeStaleEntries();
        if (value == null) {
            final SoftReference<V> oldRef = map.remove(key);
            if (oldRef != null) {
                synchronized (reverseMap) {
                    reverseMap.remove(oldRef);
                }
            }
            return null;
        }
        final SoftReference<V> newRef = newValue(value);
        final SoftReference<V> oldRef = map.replace(key, newRef);
        final V oldValue;
        if (oldRef != null) {
            oldValue = oldRef.get();
            oldRef.clear();
            synchronized (reverseMap) {
                reverseMap.remove(oldRef);
            }
        } else {
            oldValue = null;
        }
        synchronized (reverseMap) {
            reverseMap.put(newRef, key);
        }
        return oldValue;
    }
}
