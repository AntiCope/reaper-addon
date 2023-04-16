package me.ghosttypes.reaper.modules.chat;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.Formatter;
import me.ghosttypes.reaper.util.misc.MessageUtil;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import meteordevelopment.meteorclient.mixin.ClientPlayerEntityAccessor;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class ChatTweaks extends ReaperModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    public final Setting<Boolean> emotes = sgGeneral.add(new BoolSetting.Builder().name("emotes").description("Enables the Reaper emote system.").defaultValue(false).build());
    public final Setting<Boolean> customPrefix = sgGeneral.add(new BoolSetting.Builder().name("custom-prefix").description("Lets you set a custom prefix.").defaultValue(false).build());
    public final Setting<String> prefixText = sgGeneral.add(new StringSetting.Builder().name("custom-prefix-text").description("Override the [Reaper] prefix.").defaultValue("Reaper").visible(customPrefix::get).build());
    public final Setting<Boolean> customPrefixColor = sgGeneral.add(new BoolSetting.Builder().name("custom-prefix-color").description("Lets you set a custom prefix.").defaultValue(false).build());
    public final Setting<Boolean> chromaPrefix = sgGeneral.add(new BoolSetting.Builder().name("chroma-prefix").description("Lets you set a custom prefix.").defaultValue(false).build());
    public final Setting<Double> chromaSpeed = sgGeneral.add(new DoubleSetting.Builder().name("chroma-speed").description("Speed of the chroma color.").defaultValue(0.09).min(0.01).sliderMax(5).decimalPlaces(2).visible(chromaPrefix::get).build());
    public final Setting<SettingColor> prefixColor = sgGeneral.add(new ColorSetting.Builder().name("prefix-color").description("Color of the prefix text.").defaultValue(new SettingColor(255, 255, 255)).visible(customPrefixColor::get).build());
    public final Setting<Boolean> themeBrackets = sgGeneral.add(new BoolSetting.Builder().name("apply-to-brackets").description("Apply the current prefix theme to the brackets.").defaultValue(false).build());
    public final Setting<Boolean> customBrackets = sgGeneral.add(new BoolSetting.Builder().name("custom-brackets").description("Set custom brackets.").defaultValue(false).build());
    public final Setting<String> leftBracket = sgGeneral.add(new StringSetting.Builder().name("left-bracket").description("").defaultValue("[").visible(customBrackets::get).build());
    public final Setting<String> rightBracket = sgGeneral.add(new StringSetting.Builder().name("right-bracket").description("").defaultValue("]").visible(customBrackets::get).build());
    private final Setting<Boolean> easyReply = sgGeneral.add(new BoolSetting.Builder().name("easy-reply").description("Lets you use /r on every server.").defaultValue(false).build());
    private final Setting<Boolean> cancelErrors = sgGeneral.add(new BoolSetting.Builder().name("cancel-reply-errors").description("Cancels the reply errors.").defaultValue(true).visible(easyReply::get).build());

    RainbowColor prefixChroma = new RainbowColor();
    private String whisperSender;
    private boolean whispered;

    public ChatTweaks() {
        super(ML.M, "chat-tweaks", "Various chat improvements.");
    }


    @Override
    public void onActivate() {
        ChatUtils.registerCustomPrefix("me.ghosttypes.reaper", this::getPrefix);
        whispered = false;
        whisperSender = null;
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        String message = event.message;
        if (emotes.get()) message = Formatter.applyEmotes(message);
        event.message = message;
    }

    @EventHandler
    private void onPacketRecieve(PacketEvent.Receive event) {
        if (event.packet instanceof GameMessageS2CPacket packet2) {
            String s = packet2.content().getString();

            if (easyReply.get()) {
                if (s.contains("whispers")) {
                    whisperSender = s.split(" ")[0];
                    whispered = true;
                    info("New whisper reply target: " + whisperSender);
                } else if (MessageUtil.isServerMessage(packet2) && (s.contains("Unknown or incomplete command") || s.contains("<--[HERE]"))) {
                    if (cancelErrors.get()) event.cancel();
                }
            }
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof ChatMessageC2SPacket packet) {
            String s = packet.chatMessage();
            if (easyReply.get() && whisperSender != null && whispered && s.split(" ")[0].equalsIgnoreCase("/r")) {
                event.cancel();
                mc.player.sendMessage(Text.of("/msg " + whisperSender + " " + s.substring(3)), false);
                //mc.getNetworkHandler().sendPacket(new ChatMessageC2SPacket();
            }
        }
    }

    public Text getPrefix() {
        MutableText logo = MutableText.of(new LiteralTextContent(""));
        MutableText prefix = MutableText.of(new LiteralTextContent(""));
        String logoT = "Reaper";
        if (customPrefix.get()) logoT = prefixText.get();
        if (customPrefixColor.get() && !chromaPrefix.get()) logo.append(Text.literal(logoT).setStyle(logo.getStyle().withColor(TextColor.fromRgb(prefixColor.get().getPacked()))));
        if (chromaPrefix.get() && !customPrefixColor.get()) {
            prefixChroma.setSpeed(chromaSpeed.get() / 100);
            for(int i = 0, n = logoT.length() ; i < n ; i++) logo.append(Text.literal(String.valueOf(logoT.charAt(i)))).setStyle(logo.getStyle().withColor(TextColor.fromRgb(prefixChroma.getNext().getPacked())));
        }
        if (!customPrefixColor.get() && !chromaPrefix.get()) {
            if (customPrefix.get()) { logo.append(prefixText.get());
            } else { logo.append("Reaper"); }
            logo.setStyle(logo.getStyle().withFormatting(Formatting.RED));
        }
        if (themeBrackets.get()) {
            if (customPrefixColor.get() && !chromaPrefix.get()) prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(prefixColor.get().getPacked())));
            if (chromaPrefix.get() && !customPrefixColor.get()) {
                prefixChroma.setSpeed(chromaSpeed.get() / 100);
                prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(prefixChroma.getNext().getPacked())));
            }
            if (customBrackets.get()) {
                prefix.append(leftBracket.get());
                prefix.append(logo);
                prefix.append(rightBracket.get() + " ");
            } else {
                prefix.append("[");
                prefix.append(logo);
                prefix.append("] ");
            }
        } else {
            prefix.setStyle(prefix.getStyle().withFormatting(Formatting.GRAY));
            prefix.append("[");
            prefix.append(logo);
            prefix.append("] ");
        }
        return prefix;
    }
}
