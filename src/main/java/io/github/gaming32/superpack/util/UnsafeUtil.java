package io.github.gaming32.superpack.util;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

class UnsafeUtil {
    static final Unsafe UNSAFE;
    static final MethodHandles.Lookup IMPL_LOOKUP;

    static {
        try {
            final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe)unsafeField.get(null);

            final Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            IMPL_LOOKUP = (MethodHandles.Lookup)UNSAFE.getObject(
                UNSAFE.staticFieldBase(implLookupField),
                UNSAFE.staticFieldOffset(implLookupField)
            );
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
