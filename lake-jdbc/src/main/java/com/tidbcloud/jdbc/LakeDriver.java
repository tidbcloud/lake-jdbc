package com.tidbcloud.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LakeDriver extends NonRegisteringLakeDriver {
    static {
        try {
            DriverManager.registerDriver(new LakeDriver());
        } catch (SQLException e) {
            Logger.getLogger(LakeDriver.class.getPackage().getName())
                    .log(Level.SEVERE, "Failed to register driver", e);
            throw new RuntimeException("Failed to register LakeDriver", e);
        }
    }
}
