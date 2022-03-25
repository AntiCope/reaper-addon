package me.ghosttypes.reaper.util.render;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ExternalWindow extends JPanel {

    private ArrayList<String> data = new ArrayList<>();
    private Font font;
    private FontMetrics metrics;

    public ExternalWindow() {}

    public ExternalWindow(int w, int h) {this.set(w, h);}

    public void set(int w, int h) {
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(w, h));
        this.font = new Font("Microsoft Sans Serif", Font.PLAIN, 20);
        //font = new Font("Verdana", Font.PLAIN, 20);
        this.metrics = getFontMetrics(font);
    }

    public void setData(ArrayList<String> newData) {
        this.data = newData;
        this.repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.render(g);
    }

    public void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setFont(this.font);
        short offset = 40;
        for (String s : this.data) {
            g.drawString(s, (this.getWidth() - this.metrics.stringWidth(s)) / 2, offset);
            offset += 20;
        }
        Toolkit.getDefaultToolkit().sync();
    }

}
