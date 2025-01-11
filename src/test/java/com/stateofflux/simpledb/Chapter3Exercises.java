package com.stateofflux.simpledb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simpledb.jdbc.embedded.EmbeddedDriver;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class Chapter3Exercises {
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    public void testDiagnostics() {
        String url = "jdbc:derby:memory:myDB;create=true";  // using a memory backed db for testing

        try (Connection conn = DriverManager.getConnection(url)){
            assertNotNull(conn);

            // Create a table
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE MyTable (id INT, name VARCHAR(255))");
            stmt.executeUpdate("INSERT INTO MyTable values (1, 'aaaa')");
            conn.commit();
        }
        catch(SQLException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testD() throws SQLException {
        Properties p = new Properties() {{
            setProperty("create", "true");
        }};
        EmbeddedDriver driver = new EmbeddedDriver();
        Connection conn =driver.connect("java:simple:myDB", p);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE TABLE MyTable (id INT, name VARCHAR(255))");
        stmt.executeUpdate("INSERT INTO MyTable (id, name) values (1, 'aaaa')");
        conn.close();

        String stdoutput = outputStreamCaptor.toString();
        assertTrue(stdoutput.contains("reads:"));
        assertTrue(stdoutput.contains("writes:"));
        assertTrue(stdoutput.contains("appends:"));
    }
}
