package com.campus.examscheduler;

import com.campus.examscheduler.config.AppConfig;
import com.campus.examscheduler.service.AppInit;
import com.campus.examscheduler.ui.LoginFrame;
import com.campus.examscheduler.ui.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Application entry point.
 * <ul>
 *   <li>no argument: start the desktop application</li>
 *   <li>{@code --selftest}: headless end-to-end check against a temporary database (used by build.bat)</li>
 *   <li>{@code --version}: print the version and exit</li>
 * </ul>
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length > 0 && "--version".equals(args[0])) {
            System.out.println(AppConfig.APP_NAME + " " + AppConfig.APP_VERSION);
            return;
        }
        if (args.length > 0 && "--selftest".equals(args[0])) {
            System.setProperty("java.awt.headless", "true");
            System.exit(SelfTest.run());
            return;
        }
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
