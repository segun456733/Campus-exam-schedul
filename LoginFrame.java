package com.campus.examscheduler.ui;

import com.campus.examscheduler.config.AppConfig;
import com.campus.examscheduler.model.Admin;
import com.campus.examscheduler.service.AuthService;
import com.campus.examscheduler.service.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

/** Local administrator login. */
public class LoginFrame extends JFrame {
    private final JTextField username = Ui.textField(20);
    private final JPasswordField password = new JPasswordField(20);
    private final JLabel message = new JLabel(" ");

    public LoginFrame() {
        super(AppConfig.APP_NAME + " - Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));

        JPanel left = new JPanel();
        left.setBackground(Theme.NAVY);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(80, 50, 40, 40));
        JLabel brand = new JLabel("Campus Examination");
        brand.setFont(Theme.bold(28)); brand.setForeground(Color.WHITE);
        JLabel brand2 = new JLabel("Scheduler System");
        brand2.setFont(Theme.bold(28)); brand2.setForeground(new Color(0x7FA6FF));
        JLabel tag = new JLabel("<html>School of Science<br>Automatic, clash-free examination timetables<br>for ND1 and ND2 students.</html>");
        tag.setFont(Theme.plain(14)); tag.setForeground(new Color(0xB8C4E8));
        left.add(brand); left.add(brand2); left.add(Box.createVerticalStrut(20)); left.add(tag);
        left.add(Box.createVerticalGlue());
        JLabel offline = new JLabel("Works fully offline - all data stays on this computer");
        offline.setFont(Theme.plain(11)); offline.setForeground(new Color(0x8794BF));
        left.add(offline);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Theme.BG);
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), 28);
        card.setPreferredSize(new Dimension(340, 340));
        Ui.Form form = new Ui.Form();
        password.setFont(Theme.plain(13));
        password.setBorder(username.getBorder());
        form.add("Username", username).add("Password", password);
        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false);
        char echo = password.getEchoChar();
        show.addActionListener(e -> password.setEchoChar(show.isSelected() ? (char) 0 : echo));
        message.setForeground(Theme.DANGER);
        message.setFont(Theme.plain(12));
        AppButton login = new AppButton("Sign in", AppButton.Kind.PRIMARY);
        login.setPreferredSize(new Dimension(100, 38));
        login.addActionListener(e -> doLogin());
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        JPanel opts = new JPanel(new BorderLayout());
        opts.setOpaque(false);
        opts.add(show, BorderLayout.WEST);
        south.add(opts, BorderLayout.NORTH);
        south.add(message, BorderLayout.CENTER);
        south.add(login, BorderLayout.SOUTH);
        JLabel title = Ui.heading("Welcome back");
        card.add(title, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout());
        mid.setOpaque(false);
        mid.add(Ui.muted("Sign in as administrator"), BorderLayout.NORTH);
        mid.add(form, BorderLayout.CENTER);
        card.add(mid, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        JPanel holder = new JPanel(new java.awt.GridBagLayout());
        holder.setOpaque(false);
        holder.add(card);
        right.add(holder, BorderLayout.CENTER);

        add(left);
        add(right);
        getRootPane().setDefaultButton(login);
        setSize(900, 540);
        setMinimumSize(new Dimension(780, 480));
        setLocationRelativeTo(null);
        username.requestFocusInWindow();
    }

    private void doLogin() {
        message.setText(" ");
        try {
            Admin admin = AuthService.login(username.getText(), password.getPassword());
            password.setText("");
            new MainFrame(admin).setVisible(true);
            dispose();
        } catch (ValidationException ex) {
            message.setText("<html>" + ex.getMessage().replace("\n", "<br>") + "</html>");
            password.setText("");
        } catch (RuntimeException ex) {
            Ui.showException(this, ex);
        }
    }
}
