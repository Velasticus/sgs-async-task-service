/*
 * Copyright 2009 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Services.
 *
 * Project Darkstar Services is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Services is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.services.impl.util;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.Properties;


/** Simple utility for handling some common SQL properties. */
public class SQLProperties {

    public static final String NAME = SQLProperties.class.getName();

    public static final String SQL_SCHEME_PROPERTY = NAME + ".scheme";
    public static final String DEFAULT_SCHEME = "jdbc:mysql";

    public static final String SQL_DRIVER_PROPERTY = NAME + ".driver";
    public static final String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";

    public static final String SQL_HOST_PROPERTY = NAME + ".host";
    public static final String DEFAULT_HOST = "localhost";

    public static final String SQL_PORT_PROPERTY = NAME + ".port";
    public static final String DEFAULT_PORT = "3306";

    public static final String SQL_DB_PROPERTY = NAME + ".db";
    public static final String DEFAULT_DB = "ds_data";

    public static final String SQL_USER_PROPERTY = NAME + ".user";
    public static final String DEFAULT_USER = "root";

    public static final String SQL_PASS_PROPERTY = NAME + ".pass";
    public static final String DEFAULT_PASS = "";

    private final String dbUrl;
    private final String dbUser;
    private final String dbPass;

    /** Creates an instance of {@code SQLProperties}. */
    public SQLProperties(Properties p) throws SQLException {
        String scheme = p.getProperty(SQL_SCHEME_PROPERTY, DEFAULT_SCHEME);
        String driverName = p.getProperty(SQL_DRIVER_PROPERTY, DEFAULT_DRIVER);
        String host = p.getProperty(SQL_HOST_PROPERTY, DEFAULT_HOST);
        int port = Integer.parseInt(p.getProperty(SQL_PORT_PROPERTY,
                                                  DEFAULT_PORT));
        String db = p.getProperty(SQL_DB_PROPERTY, DEFAULT_DB);

        try {
            Driver driver = (Driver) Class.forName(driverName).newInstance();
            DriverManager.registerDriver(driver);
        } catch (Exception e) {
            throw new SQLException("couldn't register driver", e);
        }
        dbUrl = scheme + "://" + host + ":" + port + "/" + db;

        dbUser = p.getProperty(SQL_USER_PROPERTY, DEFAULT_USER);
        dbPass = p.getProperty(SQL_PASS_PROPERTY, DEFAULT_PASS);
    }

    /** Returns a new connection to the database. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }

}
