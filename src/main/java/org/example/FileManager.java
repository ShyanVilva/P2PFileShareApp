package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    public static class FileInfo {
        private final String name;
        private final long size;

        public FileInfo(String name, long size) {
            this.name = name;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }

        @Override
        public String toString() {
            return String.format("  - %s (%d bytes)", name, size);
        }
    }

    public static List<FileInfo> getDirectoryListing(String directory) {
        List<FileInfo> files = new ArrayList<>();
        File dir = new File(directory);

        if (!dir.exists()) {
            dir.mkdirs();
            return files;
        }

        File[] fileList = dir.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    files.add(new FileInfo(file.getName(), file.length()));
                }
            }
        }
        return files;
    }

    // Function to get file data to be sent to the client and send it to them
    public static byte[] getFileData(String filePath) {
        File file = new File(filePath);
        byte[] data = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(data);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return data;
    }




}