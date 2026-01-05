package cn.sonata.vpn.upstream.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Simple java.io based file writer for upstream packet bodies (no NIO).
 */
public final class PacketFileWriter {

    private PacketFileWriter() {
    }

    public static void writeBytes(File file, byte[] bytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            fos.flush();
        }
    }
}

