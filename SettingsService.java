package com.campus.examscheduler.service;

import com.campus.examscheduler.dao.Db;
import com.campus.examscheduler.dao.SettingsDao;
import com.campus.examscheduler.scheduler.SchedulingRules;

import java.time.LocalDate;
import java.util.List;

public final class SettingsService {
    private SettingsService() { }

    /** Saved rules, or sensible defaults when nothing has been saved yet. */
    public static SchedulingRules load() {
        SchedulingRules r = Db.read(SettingsDao::load);
        if (r != null) return r;
        r = new SchedulingRules();
        r.setStartDate(LocalDate.now().plusDays(14));
        r.setEndDate(LocalDate.now().plusDays(28));
        return r;
    }

    /** Validates then saves. Throws ValidationException listing every problem. */
    public static void save(SchedulingRules rules) {
        List<String> errors = rules.validate();
        if (!errors.isEmpty()) throw new ValidationException(errors);
        Db.inTransaction(c -> { SettingsDao.save(c, rules); return null; });
    }
}
