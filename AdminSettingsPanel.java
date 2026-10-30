package com.campus.examscheduler.ui;

import com.campus.examscheduler.config.AppConfig;
import com.campus.examscheduler.model.Admin;
import com.campus.examscheduler.service.AppInit;
import com.campus.examscheduler.service.AuthService;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** Change password, reload demonstration data, database location. */
public class AdminSettingsPanel extends Page {
    private final Admin admin;
    private final JPasswordField current = field();
    private final JPasswordField newPw = field();
    private final JPasswordField confirm = field();

    public AdminSettingsPanel(Admin admin) {
        super("Administrator Settings", "Password, demonstration data and storage location");
        this.admin = admin;

        Ui.Form pw = new Ui.Form();
        pw.add("Current password", current).add("New password", newPw).add("Confirm new password", confirm);
        RoundedPanel pwCard = new RoundedPanel(new BorderLayout(0, 10), 18);
        pwCard.add(Ui.sectionTitle("Change password (" + admin.getUsername() + ")"), BorderLayout.NORTH);
        pwCard.add(pw, BorderLayout.CENTER);
        JPanel pwSouth = new JPanel(new BorderLayout(0, 6));
        pwSouth.setOpaque(false);
        pwSouth.add(Ui.muted("At least 8 characters, with a letter and a digit."), BorderLayout.NORTH);
        pwSouth.add(Ui.buttonRow(Ui.button("Change password", AppButton.Kind.PRIMARY, this::changePassword)), BorderLayout.SOUTH);
        pwCard.add(pwSouth, BorderLayout.SOUTH);

        RoundedPanel dataCard = new RoundedPanel(new BorderLayout(0, 10), 18);
        dataCard.add(Ui.sectionTitle("Data"), BorderLayout.NORTH);
        JPanel info = new JPanel(new GridLayout(0, 1, 0, 6));
        info.setOpaque(false);
        info.add(Ui.muted("Database file: " + AppConfig.dbFile()));
        info.add(Ui.muted("The sample departments, courses and venues are DEMONSTRATION data. A real institution replaces them."));
        dataCard.add(info, BorderLayout.CENTER);
        dataCard.add(Ui.buttonRow(
                Ui.button("Reload demonstration data", AppButton.Kind.SECONDARY, this::resetDemo),
                Ui.button("Remove all data (start empty)", AppButton.Kind.DANGER, this::clearAll)), BorderLayout.SOUTH);

        RoundedPanel about = new RoundedPanel(new BorderLayout(0, 6), 18);
        about.add(Ui.sectionTitle("About"), BorderLayout.NORTH);
        about.add(Ui.muted(AppConfig.APP_NAME + " " + AppConfig.APP_VERSION + " - Java Swing + SQLite, runs fully offline."), BorderLayout.CENTER);

        JPanel grid = new JPanel(new GridLayout(3, 1, 0, 16));
        grid.setOpaque(false);
        grid.add(pwCard);
        grid.add(dataCard);
        grid.add(about);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(grid, BorderLayout.NORTH);
        setBody(wrap);
    }

    private static JPasswordField field() {
        JPasswordField f = new JPasswordField(18);
        f.setFont(Theme.plain(13));
        f.setBorder(Ui.textField(1).getBorder());
        return f;
    }

    @Override public void refresh() { current.setText(""); newPw.setText(""); confirm.setText(""); }

    private void changePassword() {
        AuthService.changePassword(admin.getId(), new String(current.getPassword()), new String(newPw.getPassword()),
                new String(confirm.getPassword()));
        refresh();
        Ui.info(this, "Password changed. Use the new password the next time you sign in.");
    }

    private void resetDemo() {
        if (!Ui.confirm(this, "Reload demonstration data", "This DELETES all current departments, courses, venues, settings and the saved "
                + "timetable, and loads the sample data again.\n\nContinue?")) return;
        AppInit.resetDemoData();
        Ui.info(this, "Demonstration data restored.");
    }

    private void clearAll() {
        if (!Ui.confirm(this, "Remove all data", "This DELETES all departments, courses, venues, settings and the saved timetable "
                + "so you can enter your own data.\n\nContinue?")) return;
        if (!Ui.confirm(this, "Are you sure?", "This cannot be undone. Remove all data?")) return;
        AppInit.clearAllData();
        Ui.info(this, "All data removed. Add your departments, venues and courses, then set the examination period.");
    }
}
