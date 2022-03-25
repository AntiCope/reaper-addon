package me.ghosttypes.reaper.util.render;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.util.misc.MathUtil;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class ExternalRenderers {

    public static int activeFrames = 0;


    public static int getUpdateFreq() {
        if (activeFrames < 1) return 15;
        return activeFrames * 10;
    }

    public static class ExternalWindow extends JPanel {
        private ArrayList<String> text = new ArrayList<>();
        private Font font;
        private Color backColor = Color.BLACK;
        private Color textColor = Color.WHITE;
        private long lastRepaint = MathUtil.now() - 2500;

        public ExternalWindow() {}

        public ExternalWindow(int w, int h) {this.set(w, h);}

        public void set(int w, int h) {
            this.setBackground(Color.BLACK);
            this.setFocusable(true);
            this.setPreferredSize(new Dimension(w, h));
            this.font = new Font("Microsoft Sans Serif", Font.PLAIN, 20);
        }

        public Font getFont() { return this.font; }
        public void setFont(Font f) { this.font = f; }

        public ArrayList<String> getText() { return this.text;}
        public void setText(ArrayList<String> body) {
            text.clear();
            text.addAll(body);
            if (MathUtil.msPassed(lastRepaint) > getUpdateFreq()) { // dynamically reduces the update frequency based on how many external windows are open
                this.repaint();
                lastRepaint = MathUtil.now();
            }
        }

        public Color getTextColor() { return this.textColor; }
        public Color getBackColor() { return this.backColor; }
        public void setTextColor(Color c) { this.textColor = c; }
        public void setBackColor(Color c) {
            this.backColor = c;
            this.setBackground(c);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            this.render(g);
        }

        public void render(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(this.textColor);
            g.setFont(this.font);
            short offset = 20;
            for (String s : text) {
                g.drawString(s, 0, offset);
                // todo make alignments to left / center / right etc.
                offset += 20;
            }
            Toolkit.getDefaultToolkit().sync();
        }
    }

    public static class ExternalFrame extends JFrame {
        private ExternalWindow window;
        private String title;
        private Module parent;

        public ExternalFrame(int w, int h, String t, Module p) {
            try {
                title = t; // set window data
                parent = p;
                window = new ExternalWindow(w, h);
                this.loadUI(); // load the window
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void loadUI() throws IOException {
            this.add(window);
            this.setResizable(true);
            this.pack();
            this.setTitle(title);
            this.setLocationRelativeTo(null);
            this.addWindowListener(new WindowAdapter() {
                @Override // disable the module that opened the window when the window is closed
                public void windowClosing(WindowEvent e) {
                    Module m = Modules.get().get(parent.getClass());
                    if (m.isActive()) m.toggle();
                }
            });
            try {
                Image image = ImageIO.read(Objects.requireNonNull(Reaper.class.getResourceAsStream("/assets/reaper/16.png")));
                this.setIconImage(image);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // proxy methods to modify the window responsible for this frame
        // modules should always access windows through the Frame class.
        public ArrayList<String> getText() {return window.getText();}
        public Color getTextColor() {return window.getTextColor();}
        public void setText(ArrayList<String> body) {window.setText(body);}
        public void setTextColor(Color c) {window.setTextColor(c);}
        public void setBackColor(Color c) {window.setBackColor(c);}
        public Color getBackColor() {return window.getBackColor();}
    }
}
