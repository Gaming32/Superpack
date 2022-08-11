package io.github.gaming32.superpack.util;

import java.security.MessageDigest;

public final class MultiMessageDigest extends MessageDigest {
    private final MessageDigest[] digests;

    public MultiMessageDigest(MessageDigest... digests) {
        super("MULTI-" + digests.length);
        this.digests = digests;
    }

    public MessageDigest[] getDigests() {
        return digests;
    }

    @Override
    protected void engineUpdate(byte input) {
        for (MessageDigest digest : digests) {
            digest.update(input);
        }
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        for (MessageDigest digest : digests) {
            digest.update(input, offset, len);
        }
    }

    @Override
    protected int engineGetDigestLength() {
        int len = 0;
        for (MessageDigest digest : digests) {
            len += digest.getDigestLength();
        }
        return len;
    }

    @Override
    protected byte[] engineDigest() {
        byte[] result = new byte[engineGetDigestLength()];
        int i = 0;
        for (MessageDigest digest : digests) {
            byte[] singleDigest = digest.digest();
            System.arraycopy(singleDigest, 0, result, i, singleDigest.length);
            i += singleDigest.length;
        }
        return result;
    }

    @Override
    protected void engineReset() {
        for (MessageDigest digest : digests) {
            digest.reset();
        }
    }
}
