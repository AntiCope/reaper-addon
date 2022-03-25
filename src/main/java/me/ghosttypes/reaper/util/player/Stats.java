package me.ghosttypes.reaper.util.player;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Stats {
    public static int kills = 0;
    public static int deaths = 0;
    public static int killStreak = 0;
    public static int highscore = 0;
    public static ArrayList<String> killfeed = new ArrayList<>();
    //public static long rpcStart = System.currentTimeMillis() / 1000L;
    public static long startTime = System.currentTimeMillis();
    //public static String onlineUsers = "";

    public static void reset() {
        kills = 0;
        deaths = 0;
        killStreak = 0;
        highscore = 0;

    }

    public static String getPlayTime() {
        return DurationFormatUtils.formatDuration(System.currentTimeMillis() - startTime, "HH:mm:ss");
    }

    public static String getKD() {
        if (Stats.deaths == 0) return String.valueOf(Stats.kills); //make sure we don't try to divide by 0
        Double rawKD = (double) (Stats.kills / Stats.deaths);
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(rawKD);
    }

    public static void addKill(String kill) {
        kills++;
        killStreak++;
        killfeed.removeIf(k -> k.contains(kill));
        killfeed.add(kill);
        if (killfeed.size() > 10) killfeed.remove(0);
    }

}
