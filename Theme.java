package com.campus.examscheduler.ui;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.util.Enumeration;

/** Colours and fonts of the modern education-dashboard look (deep navy sidebar, light workspace, blue accents). */
public final class Theme {
    public static final Color NAVY = new Color(0x0F1B3D);
    public static final Color NAVY_HOVER = new Color(0x1A2A57);
    public static final Color ACCENT = new Color(0x2F6BFF);
    public static final Color ACCENT_DARK = new Color(0x2457D6);
    public static final Color BG = new Color(0xF4F6FB);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(0x1B2559);
    public static final Color MUTED = new Color(0x7B86AB);
    public static final Color BORDER = new Color(0xE3E8F3);
    public static final Color HEADER_BG = new Color(0xEEF2FB);
    public static final Color SELECTION = new Color(0xDCE7FF);
    public static final Color SUCCESS = new Color(0x15803D);
    public static final Color WARNING = new Color(0xB45309);
    public static final Color DANGER = new Color(0xDC2626);

    public static final String FAMILY = "Segoe UI";

    private Theme() { }

    public static Font plain(int size) { return new Font(FAMILY, Font.PLAIN, size); }
    public static Font bold(int size) { return new Font(FAMILY, Font.BOLD, size); }

    /** Applies the default font to every Swing component. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // falls back to the default look and feel
        }
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof FontUIResource) UIManager.put(key, new FontUIResource(plain(13)));
        }
        UIManager.put("OptionPane.messageFont", new FontUIResource(plain(13)));
        UIManager.put("OptionPane.buttonFont", new FontUIResource(plain(13)));
    }
}
