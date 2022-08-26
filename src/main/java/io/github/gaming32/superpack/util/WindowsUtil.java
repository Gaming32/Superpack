package io.github.gaming32.superpack.util;

import java.io.File;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

public final class WindowsUtil {
    public interface Shell32 extends com.sun.jna.platform.win32.Shell32 {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class, W32APIOptions.DEFAULT_OPTIONS);

        int SHOpenFolderAndSelectItems(Pointer pidlFolder, int cidl, Pointer[] apidl, long dwFlags);
        void SHParseDisplayName(WString name, Pointer bindingContext, PointerByReference pidl, int sfgaoIn, IntByReference psfgaoOut);
    }

    private WindowsUtil() {
    }

    public static void browseFileDirectory(File file) {
        Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);

        final PointerByReference fileName = new PointerByReference();
        Shell32.INSTANCE.SHParseDisplayName(
            new WString(file.getAbsolutePath()),
            null,
            fileName,
            0,
            new IntByReference()
        );
        if (fileName.getValue() == null) {
            throw new RuntimeException("Failed to parse file name");
        }

        final int result = Shell32.INSTANCE.SHOpenFolderAndSelectItems(fileName.getValue(), 0, null, 0);
        Ole32.INSTANCE.CoTaskMemFree(fileName.getValue());
        if (W32Errors.FAILED(result)) {
            throw new RuntimeException(
                "Windows error code " + W32Errors.HRESULT_CODE(result) +
                "; severity " + W32Errors.HRESULT_SEVERITY(result) +
                "; facility " + W32Errors.HRESULT_FACILITY(result)
            );
        }
    }
}
