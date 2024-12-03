package com.stateofflux.simpledb;

import org.junit.jupiter.api.Test;
import simpledb.file.BlockId;
import simpledb.file.FileMgr;
import simpledb.file.Page;
import simpledb.server.SimpleDB;

public class FileTest {
    /*
    Note:
    The file size will be 1200 bytes.  It is 400 bytes x 3 blocks (page of 2, is the third page).

    load the filetest/testfile up in emacs and put into hexl-mode.  From here it is eay to review the contents.
    The data is being written to block 2 at location 88.  e.g. 400*2+88 => 888.  The text has a prefix of 4 bytes,
    specifying the size of the string 0x000d (13), so the full length is 4 + 13 => 17.

    An integrer is written at location 105 (page offset 88 + 17), in hex 345 => 0x0159.
     */
    @Test
    void fileCreationTest() {
        SimpleDB db = new SimpleDB("filetest", 400, 8);
        FileMgr fm = db.fileMgr();
        BlockId blk = new BlockId("testfile", 2);
        int pos1 = 88;

        Page p1 = new Page(fm.blockSize());
        p1.setString(pos1, "abcdefghijklm");
        int size = Page.maxLength("abcdefghijklm".length());
        int pos2 = pos1 + size;
        p1.setInt(pos2, 345);
        fm.write(blk, p1);

        Page p2 = new Page(fm.blockSize());
        fm.read(blk, p2);
        System.out.println("offset " + pos2 + " contains " + p2.getInt(pos2));
        System.out.println("offset " + pos1 + " contains " + p2.getString(pos1));
    }
}
