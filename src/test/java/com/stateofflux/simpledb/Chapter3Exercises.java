package com.stateofflux.simpledb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import simpledb.file.BlockId;
import simpledb.file.FileMgr;
import simpledb.file.Page;
import simpledb.jdbc.embedded.EmbeddedDriver;
import simpledb.server.SimpleDB;

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
    public void testWithBasicAction() {
        String url = "jdbc:derby:memory:myDB;create=true";  // using a memory backed db for testing

        try (Connection conn = DriverManager.getConnection(url)) {
            assertNotNull(conn);

            // Create a table
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE MyTable (id INT, name VARCHAR(255))");
            stmt.executeUpdate("INSERT INTO MyTable values (1, 'aaaa')");
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * 3.15. A database system often contains diagnostic routines.
     * (a) Modify the class FileMgr so that it keeps useful statistics, such as the
     * number of blocks read/written. Add new method(s) to the class that will
     * return these statistics.
     * (b) Modify the methods commit and rollback of the class
     * RemoteConnectionImpl (in the simpledb.jdbc.network
     * package) so that they print these statistics. Do the same for the class
     * EmbeddedConnection (in the simpledb.jdbc.embedded pack-
     * age). The result will be that the engine prints the statistics for each SQL
     * statement it executes
     *
     * @throws SQLException
     */
    @Test
    public void testWithDiagnosis() throws SQLException {
        Properties p = new Properties() {{
            setProperty("create", "true");
        }};
        EmbeddedDriver driver = new EmbeddedDriver();
        Connection conn = driver.connect("jdbc:simpledb:myDB", p);  // embedded
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE TABLE MyTable (id INT, name VARCHAR(255))");
//        stmt = conn.createStatement();
        stmt.executeUpdate("INSERT INTO MyTable (id, name) values (1, 'aaaa')");
        conn.close();

        String stdoutput = outputStreamCaptor.toString();
        System.out.println(stdoutput);
        assertTrue(stdoutput.contains("reads:"));
        assertTrue(stdoutput.contains("writes:"));
        assertTrue(stdoutput.contains("appends:"));
    }

    /**
     * 3.16. The methods setInt, setBytes, and setString of class Page do not
     * check that the new value ts in the page.
     * (a) Modify the code to perform the checks. What should you do if the check
     * fails?
     * (b) Give a reason why it is reasonable to not perform the checks.
     *
     * @throws SQLException
     */
    @Test
    public void testIfIntegerDoesntFit() throws SQLException {
        Properties p = new Properties() {{
            setProperty("create", "true");
        }};
        EmbeddedDriver driver = new EmbeddedDriver();
        Connection conn = driver.connect("jdbc:simpledb:myDB", p);  // embedded
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE TABLE MyTable (id INT, name VARCHAR(255))");
        stmt.executeUpdate("INSERT INTO MyTable (id, name) values (1, 'aaaa')");
        conn.close();

        String stdoutput = outputStreamCaptor.toString();
        assertTrue(stdoutput.contains("reads:"));
        assertTrue(stdoutput.contains("writes:"));
        assertTrue(stdoutput.contains("appends:"));
    }

    @Nested
    // Page 78
    class ThreePointSixteen {
        @Test
        // it is reasonable for the set* methods to not perform the checks, as the underlying buffer does the checks.
        public void testSetIntThatOverrunsTheBoundary() {
            SimpleDB db = new SimpleDB("dbs/chapter3", 17, 8);
            FileMgr fm = db.fileMgr();
            BlockId blk = new BlockId("exercise16.db", 0);
            int pos1 = 0;
            Page p1 = new Page(fm.blockSize());

            // Note: When writing a string, it writes the length in the first 4 bytes, then the string itself.
            p1.setString(pos1, "1234567890");  // 14 bytes total

            // calculate the next location to write at, which should be just after the string we wrote.
            int size = Page.maxLength("1234567890".length());
            int pos2 = pos1 + size;  // find the offset after the string to write the next piece of data

            // write the integer, it will not fit in the first page and an exception will be thrown
            assertThrows(IndexOutOfBoundsException.class, () -> {
                p1.setInt(pos2, Integer.MAX_VALUE);
            });

            fm.write(blk, p1);
        }
    }
}
