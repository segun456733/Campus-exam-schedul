package com.campus.examscheduler;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards against the "Could not find or load main class" problem: the build FAILS if the entry point is missing,
 * in the wrong package, or has the wrong signature.
 */
class MainClassPackagingTest {

    @Test
    void mainClassExistsWithCorrectNameAndSignature() throws Exception {
        Class<?> main = Class.forName("com.campus.examscheduler.Main");
        Method m = main.getMethod("main", String[].class);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    void resourcesAndKeyClassesArePackaged() {
        assertNotNull(Main.class.getResource("/schema.sql"), "schema.sql must be on the classpath");
        for (String c : new String[]{"com.campus.examscheduler.ui.LoginFrame", "com.campus.examscheduler.ui.MainFrame",
                "com.campus.examscheduler.scheduler.TimetableGenerator", "com.campus.examscheduler.dao.Db",
                "com.campus.examscheduler.SelfTest"}) {
            assertDoesNotThrow(() -> Class.forName(c, false, getClass().getClassLoader()), c + " is missing");
        }
    }
}
