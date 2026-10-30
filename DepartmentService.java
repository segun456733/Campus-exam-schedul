package com.campus.examscheduler.service;

import com.campus.examscheduler.dao.Db;
import com.campus.examscheduler.dao.DepartmentDao;
import com.campus.examscheduler.model.Department;

import java.util.ArrayList;
import java.util.List;

public final class DepartmentService {
    private DepartmentService() { }

    public static List<Department> list() { return Db.read(DepartmentDao::findAll); }

    /** Pure field validation (testable without a database). */
    public static List<String> validate(Department d) {
        List<String> errors = new ArrayList<>();
        String code = d.getCode() == null ? "" : d.getCode().trim();
        String name = d.getName() == null ? "" : d.getName().trim();
        if (code.isEmpty()) errors.add("Department code is required.");
        else if (!code.matches("[A-Za-z0-9]{2,8}")) errors.add("Department code must be 2-8 letters or digits (for example CSC).");
        if (name.isEmpty()) errors.add("Department name is required.");
        else if (name.length() < 3 || name.length() > 80) errors.add("Department name must be between 3 and 80 characters.");
        if (d.getDescription() != null && d.getDescription().length() > 200) errors.add("Description must not exceed 200 characters.");
        return errors;
    }

    /** Adds (id == 0) or updates a department. A new department automatically receives its ND1 and ND2 student groups. */
    public static void save(Department d) {
        d.setCode(d.getCode() == null ? "" : d.getCode().trim().toUpperCase());
        d.setName(d.getName() == null ? "" : d.getName().trim());
        List<String> errors = validate(d);
        if (!errors.isEmpty()) throw new ValidationException(errors);
        Db.inTransaction(c -> {
            Department sameCode = DepartmentDao.findByCode(c, d.getCode());
            if (sameCode != null && sameCode.getId() != d.getId())
                throw new ValidationException("A department with the code '" + d.getCode() + "' already exists.");
            Department sameName = DepartmentDao.findByName(c, d.getName());
            if (sameName != null && sameName.getId() != d.getId())
                throw new ValidationException("A department named '" + d.getName() + "' already exists.");
            if (d.getId() == 0) {
                long id = DepartmentDao.insert(c, d);
                DepartmentDao.insertGroup(c, d.getCode() + "-ND1", id, "ND1");
                DepartmentDao.insertGroup(c, d.getCode() + "-ND2", id, "ND2");
            } else {
                DepartmentDao.update(c, d);
                DepartmentDao.renameGroups(c, d.getId(), d.getCode());
            }
            return null;
        });
    }

    /** Deletes a department only when nothing depends on it. */
    public static void delete(long id) {
        Db.inTransaction(c -> {
            Department d = null;
            for (Department x : DepartmentDao.findAll(c)) if (x.getId() == id) d = x;
            if (d == null) throw new ValidationException("The department no longer exists.");
            if (d.getCourseCount() > 0)
                throw new ValidationException("'" + d.getName() + "' still has " + d.getCourseCount()
                        + " course(s). Delete or move its courses first.");
            if (DepartmentDao.countExtraGroupUses(c, id) > 0)
                throw new ValidationException("Students of '" + d.getName() + "' are still attached to shared courses of other departments. "
                        + "Remove them from those courses first.");
            DepartmentDao.delete(c, id);
            return null;
        });
    }
}
