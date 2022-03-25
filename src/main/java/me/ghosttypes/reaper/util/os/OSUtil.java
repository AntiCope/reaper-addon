package me.ghosttypes.reaper.util.os;

import com.sun.jna.Platform;
import me.ghosttypes.reaper.Reaper;
import org.apache.commons.codec.digest.DigestUtils;

import javax.swing.*;

public class OSUtil {

    public static boolean isWindows = false;

    public static void init() {
        if (getOS().equals(OSType.Windows)) isWindows = true;
    }


    public static OSType getOS() {
        if (Platform.isWindows()) return OSType.Windows;
        if (Platform.isLinux()) return OSType.Linux;
        if (Platform.isMac()) return OSType.Mac;
        return OSType.Unsupported;

    }

    public enum OSType {
        Windows,
        Linux,
        Mac,
        Unsupported
    }

    public static void messageBox(String title, String msg, int type) {
        try { JOptionPane.showMessageDialog(null, msg, title, type); } catch (Exception ignored) {}
    }


    public static void info(String msg) {
        messageBox("Reaper " + Reaper.VERSION, msg, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warning(String msg) {
        messageBox("Reaper " + Reaper.VERSION, msg, JOptionPane.WARNING_MESSAGE);
    }

    public static void error(String msg) {
        messageBox("Reaper " + Reaper.VERSION, msg, JOptionPane.ERROR_MESSAGE);
    }

    public static void bcope() {
        error("""
            Reaper has detected banana-plus in your mods folder.
            Banana-plus is known to contain malicious code, and for your safety
            Reaper will now exit.
            """);
    }

    public static void debug() {
        error("Reaper has detected a possible debugging attempt.\n" +
                "If you believe this is an error, please contact support.");
    }

    public static void tamper() {
        error("Reaper has detected a possible tampering attempt.\n" +
                "If you believe this is an error, please contact support.");
    }

    public static void authError() {
        warning("There was an error contacting the authentication server.\nPlease try again later, or check your firewall / VPN");
    }

    public static void hwidError() {
        warning("There was an error generating a hardware ID for your system.\nPlease report this bug, and specify your PC's OS.");
    }

    public static void invalidError() {
        String h = "error";
        try {
            h = DigestUtils.sha256Hex(System.getProperty("user.name") + java.net.InetAddress.getLocalHost().getHostName() + "cope_harder");
        } catch (Exception ignored) {}
        error("You do not have an active license.\n" +
            "Visit " + Reaper.INVITE_LINK + " to purchase a license.\n" +
            "If you believe this is an error, please contact support.\n" +
            "Your unique hash is " + h);
    }

}
