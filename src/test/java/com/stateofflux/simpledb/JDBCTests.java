package com.stateofflux.simpledb;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;

public class JDBCTests {
    @Test
    void testConnection()  {
        // Connect to the database (create if it doesn't exist)
        String url = "jdbc:derby:memory:myDB;create=true";  // using a memory backed db for testing

        try (Connection conn = DriverManager.getConnection(url)){
            assertNotNull(conn);
            System.out.println("Database Created");
            // Create a table
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE MyTable (id INT, name VARCHAR(255))");

            conn.close();
            assertTrue(conn.isClosed());
        }
        catch(SQLException e) {
            e.printStackTrace();
            fail();
        }
    }
}
