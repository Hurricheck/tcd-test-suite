package com.deliveryhero.demo.testsuite.util.db;

import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.logging.Level;


@Component
public class PostgresUtil {
    private static final String INIT_QUERY = "CREATE TABLE public.login_messages (id integer NULL,\"name\" varchar NULL,password_hash varchar NULL,login_ts varchar NULL);";

    private String url;
    private String username;
    private String password;

    public void init(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        executeQuery(INIT_QUERY);
    }

    public void executeQuery(String query) {
        try (Connection con = DriverManager.getConnection(url, username, password);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {

        } catch (SQLException ex) {
            System.out.println(ex.getMessage() + "\n" + ex);
        }
    }
}
