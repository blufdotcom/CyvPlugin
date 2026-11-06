package net.cyvplugin.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyvplugin.CyvPlugin;
import net.minecraftforge.fml.common.Loader;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Updater {
    public static final String GITHUB_REPO = "blufdotcom/CyvPlugin";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;
    private static final Pattern SHA256 = Pattern.compile("[a-fA-F0-9]{64}");
    private static volatile boolean running = false;
    private static volatile String failedVersion;

    private Updater() {}

    public static void start() {
        if (UpdateState.updateReady) return;
        synchronized (Updater.class) {
            if (running) return;
            running = true;
        }
        Thread thread = new Thread(Updater::runCheck, "CyvPlugin-Updater");
        thread.setDaemon(true);
        thread.start();
    }

    private static void runCheck() {
        try {
            check();
        } finally {
            running = false;
        }
    }

    private static void check() {
        UpdateState.status = UpdateState.STATUS_CHECKING;
        try {
            String json = httpGet(API_URL, "application/vnd.github+json");
            if (json == null) {
                fail("Could not reach GitHub");
                return;
            }

            JsonObject release = new JsonParser().parse(json).getAsJsonObject();
            if (release.has("prerelease") && release.get("prerelease").getAsBoolean()) return;
            if (!release.has("tag_name")) return;

            String tag = release.get("tag_name").getAsString();
            String latestVersion = stripV(tag);
            if (!Version.isNewer(latestVersion, CyvPlugin.VERSION)) {
                CyvPlugin.LOGGER.info("CyvPlugin up to date (" + CyvPlugin.VERSION + ")");
                UpdateState.status = UpdateState.STATUS_UP_TO_DATE;
                return;
            }

            if (latestVersion.equals(failedVersion)) {
                UpdateState.status = UpdateState.STATUS_ERROR;
                return;
            }

            String jarUrl = null;
            String shaUrl = null;
            if (release.has("assets")) {
                JsonArray assets = release.getAsJsonArray("assets");
                for (JsonElement element : assets) {
                    JsonObject asset = element.getAsJsonObject();
                    String name = asset.has("name") ? asset.get("name").getAsString() : "";
                    String url = asset.has("browser_download_url") ? asset.get("browser_download_url").getAsString() : null;
                    if (url == null) continue;
                    if (name.endsWith(".jar.sha256")) shaUrl = url;
                    else if (name.endsWith(".jar")) jarUrl = url;
                }
            }
            if (jarUrl == null || shaUrl == null) {
                fail("Release without jar/sha256 assets");
                return;
            }

            File current = currentJar();
            if (current == null) {
                CyvPlugin.LOGGER.info("CyvPlugin updater skipped (not running from a jar)");
                fail("Could not locate the mod jar");
                return;
            }

            String expected = readSha(httpGet(shaUrl, "text/plain"));
            if (expected == null) {
                fail("Could not read the SHA-256");
                return;
            }

            UpdateState.newVersion = latestVersion;
            UpdateState.status = UpdateState.STATUS_DOWNLOADING;
            File temp = new File(current.getParentFile(), ".cyvplugin-" + latestVersion + ".download");
            String actual = download(jarUrl, temp);
            if (actual == null) {
                temp.delete();
                fail("Download failed");
                CyvPlugin.LOGGER.warn("CyvPlugin update aborted: could not download " + jarUrl);
                return;
            }
            if (!actual.equals(expected)) {
                temp.delete();
                fail("SHA-256 mismatch");
                CyvPlugin.LOGGER.warn("CyvPlugin update aborted: sha256 mismatch (expected "
                        + expected + ", got " + actual + ")");
                return;
            }

            try {
                replace(temp, current, latestVersion);
            } catch (Exception e) {
                failedVersion = latestVersion;
                throw e;
            } finally {
                temp.delete();
            }
            UpdateState.updateReady = true;
            UpdateState.status = UpdateState.STATUS_RESTART;
            UpdateState.error = null;
            CyvPlugin.LOGGER.info("CyvPlugin updated to " + latestVersion + "; restart to apply");
        } catch (Exception e) {
            fail(e.getMessage());
            CyvPlugin.LOGGER.warn("CyvPlugin update check failed", e);
        }
    }

    private static void fail(String message) {
        UpdateState.error = message;
        UpdateState.status = UpdateState.STATUS_ERROR;
    }

    private static File currentJar() {
        File fromCodeSource = jarFromCodeSource();
        if (fromCodeSource != null) return fromCodeSource;
        File fromForge = jarFromForgeContainer();
        if (fromForge != null) return fromForge;
        return jarFromClasspath();
    }

    private static File jarFromCodeSource() {
        try {
            URL location = CyvPlugin.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(location.toURI());
            if (file.isFile() && file.getName().endsWith(".jar")) return file;
        } catch (Exception ignored) {
        }
        return null;
    }

    private static File jarFromForgeContainer() {
        try {
            Object container = Loader.instance().getIndexedModList().get(CyvPlugin.MODID);
            if (container != null) {
                File file = (File) container.getClass().getMethod("getSource").invoke(container);
                if (file != null && file.isFile() && file.getName().endsWith(".jar")) return file;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static File jarFromClasspath() {
        try {
            String cp = System.getProperty("java.class.path");
            if (cp == null) return null;
            for (String entry : cp.split(File.pathSeparator)) {
                if (entry.endsWith(".jar") && entry.toLowerCase().contains("cyvplugin")) {
                    File file = new File(entry);
                    if (file.isFile()) return file;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String httpGet(String url, String accept) {
        HttpURLConnection connection = null;
        try {
            connection = open(url);
            connection.setRequestProperty("Accept", accept);
            connection.setRequestProperty("User-Agent", "CyvPlugin-Updater");
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return null;
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String download(String url, File target) {
        HttpURLConnection connection = null;
        try {
            connection = open(url);
            connection.setRequestProperty("User-Agent", "CyvPlugin-Updater");
            // Java sends no Accept by default; GitHub release assets answer 406 without one.
            connection.setRequestProperty("Accept", "*/*");
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return null;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                }
            }
            return toHex(digest.digest());
        } catch (Exception e) {
            target.delete();
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void replace(File source, File target, String version) throws Exception {
        try {
            Files.move(source.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return;
        } catch (Exception ignored) {
        }
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        } catch (Exception e) {
            if (!isWindows()) throw e;
        }
        // Windows: the running jar is locked, so save the new jar under its own name
        // and delete the old one once the game has closed.
        File fresh = new File(target.getParentFile(), "cyvplugin-" + version + ".jar");
        Files.move(source.toPath(), fresh.toPath(), StandardCopyOption.REPLACE_EXISTING);
        deleteAfterExit(target, fresh);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    // A single delayed delete races the rest of JVM shutdown (the jar is still locked),
    // so a helper script retries for ~60s after the game closes and sweeps stray jars.
    private static void deleteAfterExit(final File old, final File keep) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    spawnCleanup(old, keep);
                } catch (Exception ignored) {
                    old.delete();
                }
            }
        }, "CyvPlugin-Cleanup"));
    }

    private static void spawnCleanup(File old, File keep) throws Exception {
        File script = File.createTempFile("cyvplugin-", ".bat");
        StringBuilder body = new StringBuilder();
        body.append("@echo off\r\n");
        body.append("setlocal\r\n");
        body.append("set \"keep=").append(keep.getAbsolutePath()).append("\"\r\n");
        body.append("set \"old=").append(old.getAbsolutePath()).append("\"\r\n");
        body.append("set \"mods=").append(old.getParentFile().getAbsolutePath()).append("\"\r\n");
        body.append("set tries=0\r\n");
        body.append(":loop\r\n");
        body.append("ping -n 2 127.0.0.1 >nul\r\n");
        body.append("if exist \"%old%\" del /f /q \"%old%\" >nul 2>&1\r\n");
        body.append("set \"stale=\"\r\n");
        body.append("for %%f in (\"%mods%\\cyvplugin*.jar\") do (\r\n");
        body.append("  if /i not \"%%~ff\"==\"%keep%\" (\r\n");
        body.append("    del /f /q \"%%~f\" >nul 2>&1\r\n");
        body.append("    if exist \"%%~f\" set \"stale=1\"\r\n");
        body.append("  )\r\n");
        body.append(")\r\n");
        body.append("if exist \"%old%\" goto retry\r\n");
        body.append("if defined stale goto retry\r\n");
        body.append("del /f /q \"%~f0\" >nul 2>&1\r\n");
        body.append("exit /b 0\r\n");
        body.append(":retry\r\n");
        body.append("set /a tries=%tries%+1 >nul\r\n");
        body.append("if %tries% LSS 60 goto loop\r\n");
        body.append("del /f /q \"%~f0\" >nul 2>&1\r\n");
        Files.write(script.toPath(), body.toString().getBytes());
        new ProcessBuilder("cmd", "/c", "call", "\"" + script.getAbsolutePath() + "\"").start();
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setInstanceFollowRedirects(true);
        if (connection instanceof HttpsURLConnection) {
            try {
                SSLContext context = SSLContext.getInstance("TLSv1.2");
                context.init(null, null, null);
                ((HttpsURLConnection) connection).setSSLSocketFactory(context.getSocketFactory());
            } catch (Exception ignored) {
            }
        }
        return connection;
    }

    private static String readSha(String text) {
        if (text == null) return null;
        Matcher matcher = SHA256.matcher(text);
        return matcher.find() ? matcher.group().toLowerCase() : null;
    }

    private static String stripV(String tag) {
        return (tag.startsWith("v") || tag.startsWith("V")) ? tag.substring(1) : tag;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }
}
