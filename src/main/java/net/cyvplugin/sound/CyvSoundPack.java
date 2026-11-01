package net.cyvplugin.sound;

import net.minecraft.client.resources.AbstractResourcePack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CyvSoundPack extends AbstractResourcePack {
    static final String DOMAIN = "cyvplugin";
    private static final String ASSETS = "assets/" + DOMAIN + "/";
    private static final String SOUNDS_PREFIX = ASSETS + "sounds/";
    private static final String PACK_MCMETA =
            "{\"pack\":{\"pack_format\":1,\"description\":\"cyvplugin custom sounds\"}}";

    public CyvSoundPack(File folder) {
        super(folder);
    }

    public static List<String> listOggNames(File folder) {
        List<String> names = new ArrayList<String>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (file.isFile() && name.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
                    names.add(name.substring(0, name.length() - 4));
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        if (name.equals(ASSETS + "sounds.json")) {
            return new ByteArrayInputStream(soundsJson().getBytes(StandardCharsets.UTF_8));
        }
        if (name.equals("pack.mcmeta")) {
            return new ByteArrayInputStream(PACK_MCMETA.getBytes(StandardCharsets.UTF_8));
        }
        if (name.equals("pack.png")) {
            return new ByteArrayInputStream(packPng());
        }
        if (name.startsWith(SOUNDS_PREFIX) && name.endsWith(".ogg")) {
            String key = name.substring(SOUNDS_PREFIX.length(), name.length() - 4);
            File file = new File(resourcePackFile, key + ".ogg");
            if (file.isFile()) {
                return new BufferedInputStream(new FileInputStream(file));
            }
        }
        throw new FileNotFoundException(name);
    }

    @Override
    protected boolean hasResourceName(String name) {
        if (name.equals(ASSETS + "sounds.json")
                || name.equals("pack.mcmeta")
                || name.equals("pack.png")) {
            return true;
        }
        if (name.startsWith(SOUNDS_PREFIX) && name.endsWith(".ogg")) {
            String key = name.substring(SOUNDS_PREFIX.length(), name.length() - 4);
            return new File(resourcePackFile, key + ".ogg").isFile();
        }
        return false;
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton(DOMAIN);
    }

    @Override
    public String getPackName() {
        return "cyvplugin-custom-sounds";
    }

    private String soundsJson() {
        List<String> names = listOggNames(resourcePackFile);
        StringBuilder builder = new StringBuilder("{\n");
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            builder.append("  \"").append(name)
                    .append("\": { \"category\": \"master\", \"sounds\": [\"")
                    .append(name).append("\"] }");
            if (i < names.size() - 1) builder.append(",");
            builder.append("\n");
        }
        return builder.append("}\n").toString();
    }

    private static byte[] packPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
