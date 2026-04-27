package app.musiceditorclient.infrastructure;

import app.musiceditorclient.view.TrackPane;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AppFileUtils {

    private static final String APP_DIR_NAME = "MusicEditor";
    private static final String PROJECTS_DIR_NAME = "projects";
    private static final String SAMPLE_PACKS_DIR_NAME = "samples";
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

    public record MusicProjectData(List<TrackPane> trackPanes, List<Path> sampleTreeRoots) {}

    public static void writeMusicProject(Path file, List<TrackPane> trackPanes, List<Path> sampleTreeRoots) throws IOException {
        Files.createDirectories(file.getParent());

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(file))) {
            objectOutputStream.writeObject(new ArrayList<>(trackPanes));
            objectOutputStream.writeObject(sampleTreeRoots.stream().map(Path::toString).toList());
        }
    }

    @SuppressWarnings("unchecked")
    public static MusicProjectData readMusicProject(Path file) throws IOException, ClassNotFoundException {
        if (Files.notExists(file)) {
            return new MusicProjectData(new ArrayList<>(), new ArrayList<>());
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(file))) {
            List<TrackPane> trackPanes = (List<TrackPane>) objectInputStream.readObject();
            List<String> sampleTreeRootsRaw = (List<String>) objectInputStream.readObject();
            List<Path> sampleTreeRoots = sampleTreeRootsRaw.stream().map(Paths::get).toList();
            return new MusicProjectData(trackPanes, sampleTreeRoots);
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

    public static File resolveSamplesDir() {
        URL samplesUrl = AppFileUtils.class.getResource("/app/musiceditorclient/" + SAMPLE_PACKS_DIR_NAME);
        if (samplesUrl != null) {
            return new File(samplesUrl.getPath());
        }
        return new File(System.getProperty("user.home"));
    }

    public static void extractZipIntoSamplesDir(File pack) {
        if (pack == null || !pack.exists() || !pack.getName().toLowerCase().endsWith(".zip")) {
            return;
        }

        File samplesDir = resolveSamplesDir();
        if (!samplesDir.exists() && !samplesDir.mkdirs()) {
            return;
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(pack))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File outFile = new File(samplesDir, entry.getName());

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    try (OutputStream outputStream = new FileOutputStream(outFile)) {
                        zipInputStream.transferTo(outputStream);
                    }
                }

                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readManualMd() throws IOException {
        try (InputStream inputStream = AppFileUtils.class.getResourceAsStream(
                "/app/musiceditorclient/Manual.md")) {
            if (inputStream == null) {
                throw new IOException("manual.md not found in resources");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
