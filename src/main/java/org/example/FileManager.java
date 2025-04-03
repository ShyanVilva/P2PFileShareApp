package org.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class FileManager {
    public static class FileInfo implements Serializable {
        private String name;
        private long size;

        public FileInfo(String name, long size) {
            this.name = name;
            this.size = size;
        }

        @Override
        public String toString() {
            return String.format("%s - %d bytes", name, size);
        }
    }

    public static List<FileInfo> getDirectoryListing(String directoryPath) {
        List<FileInfo> files = new ArrayList<>();
        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Invalid directory path: " + directoryPath);
            return files;
        }

        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    files.add(new FileInfo(file.getName(), file.length()));
                }
            }
        }

        return files;
    }
}