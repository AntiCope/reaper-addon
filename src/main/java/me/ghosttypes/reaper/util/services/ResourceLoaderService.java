package me.ghosttypes.reaper.util.services;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.util.os.FileHelper;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ResourceLoaderService {

    // user lists
    //public static String DEV_DB_URL = "";
    public static String BETA_DB_URL = "https://pastebin.com/raw/2CyKb1Un";
    public static String USER_DB_URL = "https://pastebin.com/raw/rAevfDYC";

    public static ArrayList<String> DEVELOPERS = new ArrayList<>();
    public static ArrayList<String> BETA = new ArrayList<>();
    public static ArrayList<String> USER = new ArrayList<>();

    public static void initDB(ArrayList<String> db, String url) {
        TL.cached.execute(() -> {
            ArrayList<String> data = FileHelper.downloadList(url);
            if (data == null || data.isEmpty()) return;
            db.clear();
            db.addAll(data);
        });
    }

    public static void initUserDB() {
        //DEVELOPERS.addAll(List.of("GhostTypes", "EurekaEffect", "Kiriyaga", "Wide_Cat"));
        //initDB(BETA, BETA_DB_URL);
        //initDB(USER, USER_DB_URL);
    }


    // dummy identifiers
    public static final Identifier LOGO = new Identifier("reaper", "cope_1");
    public static final Identifier LOGO_BEAMS = new Identifier("reaper", "cope_2");
    public static final Identifier LOGO_COLORSPLASH = new Identifier("reaper", "cope_3");
    public static final Identifier LOGO_GALAXY = new Identifier("reaper", "cope_4");
    public static final Identifier LOGO_PURPLE = new Identifier("reaper", "cope_5");
    public static final Identifier LOGO_RED = new Identifier("reaper", "cope_6");

    public static ArrayList<Resource> serverResources = new ArrayList<>();

    // public static void initRSC() {
    //     // assets that will be loaded and registered to an identifier
    //     serverResources.add(new Resource(LOGO, "https://raw.githubusercontent.com/GhostTypes/reaper-assets/main/reaper_white.png", "reaper_white"));
    //     serverResources.add(new Resource(LOGO_BEAMS, "https://raw.githubusercontent.com/GhostTypes/reaper-assets/main/icon_beams-min.png", "icon_beams-min"));
    //     serverResources.add(new Resource(LOGO_COLORSPLASH, "https://raw.githubusercontent.com/GhostTypes/reaper-assets/main/icon_colorsplash-min.png", "icon_colorsplash-min"));
    //     serverResources.add(new Resource(LOGO_GALAXY, "https://raw.githubusercontent.com/GhostTypes/reaper-assets/main/icon_galaxy_1-min.png", "icon_galaxy_1-min"));
    //     serverResources.add(new Resource(LOGO_PURPLE, "https://raw.githubusercontent.com/GhostTypes/reaper-assets/main/icon_purple_galaxy-min.png", "icon_purple_galaxy-min"));
    //     serverResources.add(new Resource(LOGO_RED, "https://raw.githubusercontent.com/GhostTypes/reaper-assets/main/icon_red-min.png", "icon_red-min"));
    // }

    public static void init() {
        initUserDB(); // load the dev/beta/user list
        for (Resource r : serverResources) if (!r.isCached()) r.cache(); // download anything that isn't cached yet
        TL.cached.execute(() -> {
            while (mc.world == null) {
                try {Thread.sleep(500);} catch (Exception ignored) {} // wait for the world to load
            }
            Reaper.log("Loading assets");
            serverResources.forEach(Resource::load);
        });
    }


    public static void bindAssetFromURL(Identifier asset, String url) {
        if (mc.world == null || asset == null || url == null) return;
        TL.cached.execute(() -> {
            try {
                var data = NativeImage.read(Http.get(url).sendInputStream());
                mc.getTextureManager().registerTexture(asset, new NativeImageBackedTexture(data));
            } catch (Exception ignored) {
                //e.printStackTrace();
            }
        });
    }

    public static void bindAssetFromFile(Identifier asset, String fileName) {
        if (mc.world == null || asset == null || fileName == null) return;
        if (!Reaper.USER_ASSETS.exists()) return;
        TL.cached.execute(() -> {
            File[] userAssets = Reaper.USER_ASSETS.listFiles();
            if (userAssets == null || userAssets.length < 1) return;
            for (File f : userAssets) {
                String fn = f.getName();
                if (fn.equalsIgnoreCase(fileName) || fn.equalsIgnoreCase(fileName + ".png")) {
                    try {
                        InputStream is = new FileInputStream(f);
                        var rsc = NativeImage.read(is);
                        mc.getTextureManager().registerTexture(asset, new NativeImageBackedTexture(rsc));
                        is.close();
                    } catch (Exception ignored) {
                        //e.printStackTrace();
                    }
                }
            }
        });
    }

    // todo finish later too tired
    /*public static void bindRandomAssetFromFile(Identifier asset) {
        if (mc.world == null || asset == null) return;
    }*/


    public static class Resource {
        private final Identifier identifier;
        private final String url;
        private final String name;

        public Resource(Identifier identifier, String url, String name) {
            this.identifier = identifier;
            this.url = url;
            this.name = name;
        }

        public Identifier getIdentifier() { return this.identifier; }
        public String getUrl() { return this.url; }
        public String getName() { return this.name;}


        public String getFileName() {return this.name + ".png";}
        public File getAsFile() {return new File(Reaper.ASSETS, this.getFileName());}
        public boolean isCached() {return this.getAsFile().exists();}

        public void cache() {
            TL.cached.execute(() -> {
                try {
                    File outFile = this.getAsFile();
                    if (!outFile.exists()) outFile.createNewFile();
                    InputStream is = Http.get(this.url).sendInputStream();
                    Reaper.log("Downloading asset " + this.name);
                    Reaper.log(outFile.getAbsolutePath());
                    FileUtils.copyInputStreamToFile(is, outFile);
                    is.close();
                } catch (Exception ignored) {
                    Reaper.log("Failed to download asset " + this.name);
                    //e.printStackTrace();
                }
            });
        }

        public void load() {
            TL.cached.execute(() -> {
                File asset = this.getAsFile();
                if (asset == null || !asset.exists()) return;
                try {
                    InputStream is = new FileInputStream(asset);
                    var rsc = NativeImage.read(is);
                    mc.getTextureManager().registerTexture(this.getIdentifier(), new NativeImageBackedTexture(rsc));
                    Reaper.log("Loaded asset " + this.name);
                    is.close();
                } catch (Exception ignored) {
                    Reaper.log("Failed to load asset from cache " + this.name);
                    //e.printStackTrace();
                }
            });
        }
    }
}
