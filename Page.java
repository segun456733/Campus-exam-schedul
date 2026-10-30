package com.campus.examscheduler.ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Base class of every screen: title + subtitle at the top, content below. */
public abstract class Page extends JPanel implements Refreshable {
    protected Page(String title, String subtitle) {
        super(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(Ui.heading(title));
        head.add(javax.swing.Box.createVerticalStrut(4));
        head.add(Ui.muted(subtitle));
        add(head, BorderLayout.NORTH);
    }

    protected void setBody(JComponent body) { add(body, BorderLayout.CENTER); }
}
