package com.campus.examscheduler.service;

import com.campus.examscheduler.dao.AdminDao;
import com.campus.examscheduler.dao.Db;
import com.campus.examscheduler.model.Admin;
import com.campus.examscheduler.util.PasswordUtil;

/** Local administrator authentication (no network involved). */
public final class AuthService {
    private AuthService() { }

    /** Returns the administrator or throws ValidationException with a helpful message. */
    public static Admin login(String username, char[] password) {
        String user = username == null ? "" : username.trim();
        if (user.isEmpty()) throw new ValidationException("Enter your username.");
        if (password == null || password.length == 0) throw new ValidationException("Enter your password.");
        Admin admin = Db.read(c -> AdminDao.findByUsername(c, user));
        String pw = new String(password);
        if (admin == null || !PasswordUtil.verify(pw, admin.getSalt(), admin.getIterations(), admin.getPasswordHash()))
            throw new ValidationException("Incorrect username or password.");
        return admin;
    }

    public static void changePassword(long adminId, String current, String newPassword, String confirm) {
        if (current == null || current.isEmpty()) throw new ValidationException("Enter your current password.");
        String weak = PasswordUtil.checkStrength(newPassword);
        if (weak != null) throw new ValidationException(weak);
        if (!newPassword.equals(confirm)) throw new ValidationException("The new password and its confirmation do not match.");
        if (newPassword.equals(current)) throw new ValidationException("The new password must be different from the current one.");
        Db.inTransaction(c -> {
            Admin admin = AdminDao.findById(c, adminId);
            if (admin == null) throw new ValidationException("Administrator account not found.");
            if (!PasswordUtil.verify(current, admin.getSalt(), admin.getIterations(), admin.getPasswordHash()))
                throw new ValidationException("The current password is incorrect.");
            String salt = PasswordUtil.newSalt();
            AdminDao.updatePassword(c, adminId, PasswordUtil.hash(newPassword, salt, PasswordUtil.ITERATIONS), salt, PasswordUtil.ITERATIONS);
            return null;
        });
    }
}
