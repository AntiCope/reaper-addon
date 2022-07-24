package me.ghosttypes.reaper.modules.hud;


import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.services.ResourceLoaderService;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Identifier;

public class CustomImage extends HudElement {

    public static final HudElementInfo<CustomImage> INFO = new HudElementInfo<>(Reaper.HUD_GROUP, "custom-image", "Displays a custom image", CustomImage::new);

    public enum LogoMode {File, URL}

    private final Identifier IMAGE = new Identifier("reaper", "custom_png");

    private static final RainbowColor RAINBOW = new RainbowColor();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<LogoMode> logoMode = sgGeneral.add(new EnumSetting.Builder<LogoMode>().name("logo").description("Which logo to use.").defaultValue(LogoMode.File).onChanged(fileName1 -> setTexture()).build());
    private final Setting<String> fileName = sgGeneral.add(new StringSetting.Builder().name("file-name").description("The file to load the texture from").defaultValue("cope.png").visible(() -> logoMode.get() == LogoMode.File).onChanged(fileName1 -> setTexture()).build());
    private final Setting<String> url = sgGeneral.add(new StringSetting.Builder().name("url").description("The URL to load the texture from").defaultValue("cope.com").visible(() -> logoMode.get() == LogoMode.URL).onChanged(fileName2 -> setTexture()).build());
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder().name("scale").onChanged(aDouble -> calculateSize()).defaultValue(2).min(1).sliderMin(1).sliderMax(5).build());
    private final Setting<Double> boxW = sgGeneral.add(new DoubleSetting.Builder().name("box-width").onChanged(aDouble -> calculateSize()).defaultValue(100).min(1).sliderMin(1).sliderMax(600).build());
    private final Setting<Double> boxH = sgGeneral.add(new DoubleSetting.Builder().name("box-height").onChanged(aDouble -> calculateSize()).description("The scale.").defaultValue(100).min(1).sliderMin(1).sliderMax(600).build());
    public final Setting<Boolean> update = sgGeneral.add(new BoolSetting.Builder().name("refresh").description("Reload the image after a set period of time").defaultValue(false).visible(() -> logoMode.get() == LogoMode.URL).build());
    public final Setting<Integer> updateDelay = sgGeneral.add(new IntSetting.Builder().name("refresh-delay").defaultValue(3).min(1).sliderMax(10).visible(update::get).build());
    public final Setting<Boolean> chroma = sgGeneral.add(new BoolSetting.Builder().name("chroma").description("Chroma logo animation.").defaultValue(false).build());
    private final Setting<Double> chromaSpeed = sgGeneral.add(new DoubleSetting.Builder().name("speed").defaultValue(0.09).min(0.01).sliderMax(5).decimalPlaces(2).visible(chroma::get).build());
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder().name("background-color").defaultValue(new SettingColor(255, 255, 255)).visible(() -> !chroma.get()).build());

    private long lastRefresh = MathUtil.now();

    public CustomImage() {
        super(INFO);
        calculateSize();
    }

    @EventHandler
    public void onGameJoin(GameJoinedEvent event) { setTexture();} // so it loads when the player first joins the game

    public void calculateSize() {
        setSize(boxW.get() * scale.get(), boxH.get() * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;
        double x = getX();
        double y = getY();
        int w = getWidth();
        int h = getHeight();

        if (update.get() && logoMode.get() == LogoMode.URL && MathUtil.msPassed(lastRefresh) >= updateDelay.get() * 1000) { // updating from URL
            lastRefresh = MathUtil.now();
            setTexture();
        }

        GL.bindTexture(IMAGE);
        Renderer2D.TEXTURE.begin();
        if (chroma.get()) {
            RAINBOW.setSpeed(chromaSpeed.get() / 100);
            Renderer2D.TEXTURE.texQuad(x, y, w, h, RAINBOW.getNext(renderer.delta));
        } else {
            Renderer2D.TEXTURE.texQuad(x, y, w, h, color.get());
        }
        Renderer2D.TEXTURE.render(null);
    }

    private void setTexture() {
        switch (logoMode.get()) {
            case File -> ResourceLoaderService.bindAssetFromFile(IMAGE, fileName.get()); // load from file
            case URL -> ResourceLoaderService.bindAssetFromURL(IMAGE, url.get()); // load from url
        }
    }

}
