package me.ghosttypes.reaper.util.render;

import meteordevelopment.meteorclient.utils.render.color.Color;

public class Colors { // Simple condensed color util. need to add more later
    public static final Color DEFAULT = new Color(218, 112, 214);

    public static final Color LIGHT_RED = new Color(255,99,71);
    public static final Color RED = new Color(255, 25, 25);
    public static final Color DARK_RED = new Color(139,0,0);

    public static final Color LIGHT_ORANGE = new Color(255,127,80);
    public static final Color ORANGE = new Color(255,165,0);
    public static final Color DARK_ORANGE = new Color(255,140,0);

    public static final Color LIGHT_YELLOW = new Color(240,230,140);
    public static final Color YELLOW = new Color(255,255,0);
    public static final Color DARK_YELLOW = new Color(218,165,32);

    public static final Color LIGHT_GREEN = new Color(124, 252, 0);
    public static final Color GREEN = new Color(0, 128, 0);
    public static final Color DARK_GREEN = new Color(0, 100, 0);

    public static final Color LIGHT_BLUE = new Color(0, 255, 255);
    public static final Color BLUE = new Color(0, 0, 255);
    public static final Color DARK_BLUE = new Color(0, 0, 139);

    public static final Color MAGENTA = new Color(255, 0, 255);
    public static final Color PURPLE = new Color(128, 0, 128);
    public static final Color PINK = new Color(255, 20, 147);

    public static final Color WHITE = new Color(255, 25, 25);

    public static final Color SLATE_GRAY = new Color(112, 128, 144);
    public static final Color GRAY = new Color(128, 128, 128);


    public static Color getColor(ColorType colorType) {
        Color color = DEFAULT;
        switch (colorType) {
            case LightRed -> color = LIGHT_RED;
            case Red -> color = RED;
            case DarkRed -> color = DARK_RED;

            case LightOrange -> color = LIGHT_ORANGE;
            case Orange -> color = ORANGE;
            case DarkOrange -> color = DARK_ORANGE;

            case LightYellow -> color = LIGHT_YELLOW;
            case Yellow -> color = YELLOW;
            case DarkYellow -> color = DARK_YELLOW;

            case LightGreen -> color = LIGHT_GREEN;
            case Green -> color = GREEN;
            case DarkGreen -> color = DARK_GREEN;

            case LightBlue -> color = LIGHT_BLUE;
            case Blue -> color = BLUE;
            case DarkBlue -> color = DARK_BLUE;

            case Magenta -> color = MAGENTA;
            case Purple -> color = PURPLE;
            case Pink -> color = PINK;

            case White -> color = WHITE;

            case SlateGray -> color = SLATE_GRAY;
            case Gray -> color = GRAY;
        }
        return color;
    }

    public enum ColorType {
        LightRed,
        Red,
        DarkRed,
        LightOrange,
        Orange,
        DarkOrange,
        LightYellow,
        Yellow,
        DarkYellow,
        LightGreen,
        Green,
        DarkGreen,
        LightBlue,
        Blue,
        DarkBlue,
        Magenta,
        Purple,
        Pink,
        White,
        SlateGray,
        Gray
    }
}
