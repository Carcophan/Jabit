/*
 * Copyright 2015 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.bitmessage.repository;

import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The base configuration for all JDBC based repositories. You should only make one instance,
 * as flyway initializes/updates the database at object creation.
 */
public class JdbcConfig {
    protected final Flyway flyway;
    protected final String dbUrl;
    protected final String dbUser;
    protected final String dbPassword;

    public JdbcConfig(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.flyway = new Flyway();
        flyway.setDataSource(dbUrl, dbUser, dbPassword);

        // TODO: refactor! never do IO or heavy stuff from inside a constructor
        flyway.migrate();
    }

    public static JdbcConfig newH2JdbcConfig(){
        return new JdbcConfig("jdbc:h2:~/jabit;AUTO_SERVER=TRUE", "sa", null);
    }

    public static JdbcConfig newMysqlJdbcConfig(){
        return new JdbcConfig("jdbc:mysql://localhost/jabit", "admin", "admin");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
