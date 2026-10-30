package com.campus.examscheduler.service;

import com.campus.examscheduler.dao.Db;
import com.campus.examscheduler.dao.VenueDao;
import com.campus.examscheduler.model.Venue;

import java.util.ArrayList;
import java.util.List;

public final class VenueService {
    private VenueService() { }

    public static List<Venue> list() { return Db.read(VenueDao::findAll); }

    public static List<String> validate(Venue v) {
        List<String> errors = new ArrayList<>();
        String name = v.getName() == null ? "" : v.getName().trim();
        if (name.isEmpty()) errors.add("Venue name is required.");
        else if (name.length() < 2 || name.length() > 60) errors.add("Venue name must be between 2 and 60 characters.");
        if (v.getCapacity() < 1) errors.add("Capacity must be at least 1.");
        else if (v.getCapacity() > 10000) errors.add("Capacity must not exceed 10000.");
        if (v.getType() != null && v.getType().length() > 40) errors.add("Venue type must not exceed 40 characters.");
        return errors;
    }

    public static void save(Venue v) {
        v.setName(v.getName() == null ? "" : v.getName().trim());
        List<String> errors = validate(v);
        if (!errors.isEmpty()) throw new ValidationException(errors);
        Db.inTransaction(c -> {
            Venue same = VenueDao.findByName(c, v.getName());
            if (same != null && same.getId() != v.getId()) throw new ValidationException("A venue named '" + v.getName() + "' already exists.");
            if (v.getId() == 0) VenueDao.insert(c, v); else VenueDao.update(c, v);
            return null;
        });
    }

    public static void delete(long id) {
        Db.inTransaction(c -> {
            int used = VenueDao.countScheduledIn(c, id);
            if (used > 0) throw new ValidationException("This venue hosts " + used + " scheduled examination(s). "
                    + "Reschedule them first, or disable the venue instead of deleting it.");
            VenueDao.delete(c, id);
            return null;
        });
    }
}
