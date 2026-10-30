package com.campus.examscheduler.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Flat rounded button (custom painted so it looks the same on every Windows theme). */
public class AppButton extends JButton {
    public enum Kind { PRIMARY, SECONDARY, DANGER }

    private final Kind kind;

    public AppButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFont(Theme.bold(12));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(kind == Kind.SECONDARY ? Theme.TEXT : Color.WHITE);
        setPreferredSize(new Dimension(Math.max(96, getFontMetrics(getFont()).stringWidth(text) + 34), 34));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color base = switch (kind) {
            case PRIMARY -> Theme.ACCENT;
            case DANGER -> Theme.DANGER;
            case SECONDARY -> Color.WHITE;
        };
        if (!isEnabled()) base = new Color(0xD5DBEA);
        else if (getModel().isPressed()) base = base.darker();
        else if (getModel().isRollover()) base = kind == Kind.SECONDARY ? Theme.HEADER_BG : base.brighter();
        g2.setColor(base);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        if (kind == Kind.SECONDARY) {
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
