package app.musiceditorclient.infrastructure;

import app.musiceditorclient.view.TrackPane;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AppFileUtils {

    private static final String APP_DIR_NAME = "MusicEditor";
    private static final String PROJECTS_DIR_NAME = "projects";
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String PROJECT_EXTENSION = ".musicproject";

    public static String getPlatformFolderName() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "mac";
        }
        return "linux";
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static Path getAppDataDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            return Paths.get(System.getenv("APPDATA"), APP_DIR_NAME);
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return Paths.get(home, "Library", "Application Support", APP_DIR_NAME);
        }
        return Paths.get(home, ".local", "share", APP_DIR_NAME);
    }

    public static Path getProjectsDir() {
        return getAppDataDir().resolve(PROJECTS_DIR_NAME);
    }

    public static Path createMusicProjectFile(String fileName) throws IOException {
        Files.createDirectories(getProjectsDir());

        String normalizedName = fileName.endsWith(PROJECT_EXTENSION)
                ? fileName
                : fileName + PROJECT_EXTENSION;

        Path projectFile = getProjectsDir().resolve(normalizedName);
        if (Files.notExists(projectFile)) {
            Files.createFile(projectFile);
        }

        return projectFile;
    }

    public static void writeTrackPanesToMusicProject(Path file, List<TrackPane> trackPanes) throws IOException {
        Files.createDirectories(file.getParent());

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(file))) {
            objectOutputStream.writeObject(new ArrayList<>(trackPanes));
        }
    }

    @SuppressWarnings("unchecked")
    public static List<TrackPane> readTrackPanesFromMusicProject(Path file) throws IOException, ClassNotFoundException {
        if (Files.notExists(file)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(file))) {
            return (List<TrackPane>) objectInputStream.readObject();
        }
    }

    public static String readProperty(String propertyName) throws IOException {
        Properties properties = loadPropertiesFromResources();
        return properties.getProperty(propertyName);
    }

    public static void writeProperty(String propertyName, String propertyValue) throws IOException {
        Properties properties = loadPropertiesFromResources();
        properties.setProperty(propertyName, propertyValue);
        storePropertiesToResources(properties);
    }

    private static Properties loadPropertiesFromResources() throws IOException {
        Properties properties = new Properties();

        try (InputStream resourceStream = AppFileUtils.class.getResourceAsStream(
                "/app/musiceditorclient/infrastructure/" + CONFIG_FILE_NAME)) {
            if (resourceStream != null) {
                properties.load(resourceStream);
            }
        }

        return properties;
    }

    private static void storePropertiesToResources(Properties properties) throws IOException {
        URL resourceUrl = AppFileUtils.class.getResource("/app/musiceditorclient/infrastructure/" + CONFIG_FILE_NAME);
        if (resourceUrl == null) {
            throw new IOException("No se ha encontrado el archivo de propiedades en resources");
        }

        Path configPath = Paths.get(resourceUrl.getPath());
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(configPath.toFile()))) {
            properties.store(outputStream, "MusicEditor configuration");
        }
    }
}
