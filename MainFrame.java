package com.campus.examscheduler.ui;

import com.campus.examscheduler.config.AppConfig;
import com.campus.examscheduler.model.Admin;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Main window: navy sidebar + CardLayout workspace. */
public class MainFrame extends JFrame {
    public static final String DASHBOARD = "Dashboard", DEPARTMENTS = "Departments", COURSES = "Courses", VENUES = "Venues",
            SETTINGS = "Examination Settings", GENERATE = "Generate Timetable", TIMETABLE = "Timetable", REPORTS = "Reports",
            ADMIN = "Administrator Settings", LOGOUT = "Logout";

    private final Admin admin;
    private final CardLayout cards = new CardLayout();
    private final JPanel workspace = new JPanel(cards);
    private final Map<String, JComponent> screens = new LinkedHashMap<>();
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final JLabel crumb = new JLabel();

    public MainFrame(Admin admin) {
        super(AppConfig.APP_NAME);
        this.admin = admin;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        screens.put(DASHBOARD, new DashboardPanel(this::navigate));
        screens.put(DEPARTMENTS, new DepartmentsPanel());
        screens.put(COURSES, new CoursesPanel());
        screens.put(VENUES, new VenuesPanel());
        screens.put(SETTINGS, new ExamSettingsPanel());
        screens.put(GENERATE, new GeneratePanel(this::navigate));
        screens.put(TIMETABLE, new TimetablePanel());
        screens.put(REPORTS, new ReportsPanel());
        screens.put(ADMIN, new AdminSettingsPanel(admin));
        for (Map.Entry<String, JComponent> e : screens.entrySet()) workspace.add(e.getValue(), e.getKey());

        JPanel sidebar = buildSidebar();
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 26, 12, 26)));
        crumb.setFont(Theme.bold(13));
        crumb.setForeground(Theme.TEXT);
        JLabel who = new JLabel("Signed in as " + admin.getUsername() + "   |   "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)));
        who.setFont(Theme.plain(12));
        who.setForeground(Theme.MUTED);
        top.add(crumb, BorderLayout.WEST);
        top.add(who, BorderLayout.EAST);

        JPanel right = new JPanel(new BorderLayout());
        right.add(top, BorderLayout.NORTH);
        right.add(workspace, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        setSize(1280, 760);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        navigate(DASHBOARD);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(Theme.NAVY);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(240, 600));
        side.setBorder(BorderFactory.createEmptyBorder(22, 14, 18, 14));
        JLabel logo = new JLabel("Exam Scheduler");
        logo.setFont(Theme.bold(19));
        logo.setForeground(Color.WHITE);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 10, 2, 0));
        JLabel sub = new JLabel("School of Science");
        sub.setFont(Theme.plain(11));
        sub.setForeground(new Color(0x8794BF));
        sub.setBorder(BorderFactory.createEmptyBorder(0, 10, 22, 0));
        side.add(logo);
        side.add(sub);
        for (String name : new String[]{DASHBOARD, DEPARTMENTS, COURSES, VENUES, SETTINGS, GENERATE, TIMETABLE, REPORTS, ADMIN}) {
            NavButton b = new NavButton(name);
            b.addActionListener(e -> navigate(name));
            navButtons.put(name, b);
            side.add(b);
            side.add(Box.createVerticalStrut(3));
        }
        side.add(Box.createVerticalGlue());
        NavButton logout = new NavButton(LOGOUT);
        logout.addActionListener(e -> logout());
        side.add(logout);
        return side;
    }

    /** Opens a screen and reloads its data. */
    public void navigate(String name) {
        JComponent screen = screens.get(name);
        if (screen == null) return;
        if (screen instanceof Refreshable r) {
            try {
                r.refresh();
            } catch (RuntimeException ex) {
                Ui.showException(this, ex);
            }
        }
        cards.show(workspace, name);
        crumb.setText(name);
        navButtons.forEach((n, b) -> b.setSelectedItem(n.equals(name)));
    }

    private void logout() {
        if (!Ui.confirm(this, "Log out", "Do you want to log out?")) return;
        dispose();
        new LoginFrame().setVisible(true);
    }

    /** Sidebar item. */
    private static final class NavButton extends JButton {
        private boolean selectedItem;

        NavButton(String text) {
            super(text);
            setFont(Theme.plain(13));
            setForeground(new Color(0xC9D3F2));
            setHorizontalAlignment(LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 10));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setPreferredSize(new Dimension(200, 40));
            setAlignmentX(LEFT_ALIGNMENT);
        }

        void setSelectedItem(boolean s) {
            selectedItem = s;
            setForeground(s ? Color.WHITE : new Color(0xC9D3F2));
            setFont(s ? Theme.bold(13) : Theme.plain(13));
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selectedItem) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else if (getModel().isRollover()) {
                g2.setColor(Theme.NAVY_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
