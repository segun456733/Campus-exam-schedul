package com.campus.examscheduler;

import com.campus.examscheduler.config.AppConfig;
import com.campus.examscheduler.service.AppInit;
import com.campus.examscheduler.ui.LoginFrame;
import com.campus.examscheduler.ui.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Application entry point. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> {
            try {
                AppInit.start();                         // database + first-launch demo data
            } catch (Throwable t) {
                t.printStackTrace();
                JOptionPane.showMessageDialog(null, "The application could not open its database:\n" + t.getMessage()
                        + "\n\nDatabase folder: " + AppConfig.dataDir(), "Start-up error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
