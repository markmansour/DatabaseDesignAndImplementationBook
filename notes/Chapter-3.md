# Disk and File Management

## 3.1
Describes the basic mechanics of a disk drive and how platters work.  The general performance of a disk drive can be measured by four values: its
capacity, rotation speed, transfer rate, and seek time.
* The _capacity_ of a drive is the number of bytes that can be stored.
* The _rotation speed_ is the rate at which the platters spin and is usually given as
* revolutions per minute.
* The _transfer rate_ is the speed at which bytes pass by the disk head, to be
* transferred to/from memory.
* The _seek time_ is the time it takes for the actuator to move the disk head from its
* current location to a requested track.  On the average, you will have to wait ½ rotation until the platter is positioned where you want
  it. Thus, the average rotational delay is half of the rotation time.

Because disk drives are so slow, several techniques have been developed to help
improve access times:
* A disk cache is memory that is bundled with the disk drive and is usually large enough to store the contents of 
  thousands of sectors. However, this feature is not particularly useful for a database engine, because it is already 
  doing its own caching. The real value of a disk cache is its ability to pre-fetch sectors.
* The database system can improve disk access time by storing related information in
  nearby sectors. 
* Two small drives are faster than one large drive because they contain two independent actuators
  and thus can respond to two different sector requests simultaneously.  But the need to be kept buys, so the problem 
  is how to balance the workload among the multiple disks.  The disk striping strategy uses a controller to hide the 
  smaller disks from the operating system, giving it the illusion of a single large disk.

### Mirroring for improved disk reliability
The most obvious way to guard against disk failure is to keep a copy of the disk’s
contents.  This can be done by replicating every change to the disk at the moment it occurs.  As with striping, a 
controller is needed to manage the two mirrored disks. 

### Storing Party for improved disk reliability
Mirroring requires twice as many disks to store the same amount of data. And treating the individually is
difficult.  Instead, we can create a virtual disk and stripe over many small disks.  We can store parity information
on the backup disk instead.  The simplest version is parity bits:
* The parity of S is 1 if it contains an odd number of 1s.
* The parity of S is 0 if it contains an even number of 1s.

Parity has the following interesting and important property: The value of any bit
can be determined from the value of the other bits, as long as you also know the
parity.

> For example, suppose that S = {1, 0, 1}. The parity of S is 0 because it has an
even number of 1s. Suppose you lose the value of the first bit. Because the parity is
0, the set {?, 0, 1} must have had an even number of 1s; thus, you can infer that the
missing bit must be a 1. Similar deductions can be made for each of the other bits
(including the parity bit)

These operations need to go through a controller.  When the controller performs an update, it can update the physical 
disks, and it can calculate the parity bits.  Reads and writes are handles the same as with striping - the controller determines which disk holds
the requested sector and performs theat read/write operation.  But write requests must also update the corresponding
sector of the parity disk.  The controller can calculate the updated parity by determining which bits of teh modified 
sector changed.  Theus the controller requires four disk accesses to implement a sector-write:
* (2) read the sector and the corresponding parity sector
* (2) write the new content of both sectors.

Using parity reduces the efficiency of striping by a factor of about 20%.

These are all built into RAID.

### Flash Drives
* much faster than spinning disks (about 50 microseconds seek time, about 100 times faster than disks)
* transfer rates are between flash and disks are similar (dependent on the bus)
* striping is less important as seek times are so fast.

## Blocks
A block is an OS abstration similar to a sector.  Each block has the same fixed size for all disks.  The OS
maintains a mapping between sectors and blocks.

OS abstraction to read bits from a disk: sector >-- blocks >-- memory page.

* Blocks need read, write, allocate, deallocate methods (CRUD)
* A *disk map* is a sequence of bits mapping out which blocks are free (1) or used (0).
* A *free list* is a chain of contiguous unallocated blocks (chunks).  This will take up more space.

## File-level interface
The file system abstracts blocks, and instead deals with bytes.
* continuous allocation lays out the bytes of the file sequentially.  This can lead to 
  * internal fragmentation where space inside a file is wasted (having to overallocate space to ensure all data could fit)
  * external fragmentation where space between files is wasted (not enough room to store a file, but in aggregate there is lots of free space and the filesystem could easily accomodate the file)
* extent based allocated which is creates a sequence of fixed length extents and chains them together.
* indexed allocation allocates each block individually.  There are file limits due to the length of the index. To
  work around this, there can be multiple levels of index blocks.

# DBs and the OS
* choosing block level support - not constrained by the OS limitations on files (e.g. support larger files than the OS), 
  and we can optimize the location of files so that blocks accessed together are stored together. But it's
  also much more complex to manage.
* choosing file level support this is much easier to implement. But this isn't efficient.  Also, OSs are set up
  to manage I/O buffers that is not optimized for DB operations.

The compromise is to treat files as if they were raw disks.  The OS maps each logical block to a physical block.  This is
common with Microsoft access using a single file, and other systems using multiple files.

# SimpleDB File Manager
See the [simpledb.file](../src/main/java/simpledb/file) package.

Uses a file for each table and each index, a log file, and several catalog files.

Three classes
* [BlockId](../src/main/java/simpledb/file/BlockId.java)
* [Page](../src/main/java/simpledb/file/Page.java)
* [FileMgr](../src/main/java/simpledb/file/FileMgr.java)

sector >-- blocks >-- memory page >-- FileMgr

Notes:
* Page is backed by a Java `ByteBuffer`, that wraps a byte array with read and write methods to arbitrary locations 
  of the array.
* Any piece of data written has a prefix specifying the length of the piece of data stored.  e.g.
  the String being stored, "abcedfghijlm" is 13 bytes, but a 4 byte prefix is added that specifies the size.
* Otherwise, the implementation is very straight forward.  Modify a byte array that is read/written
  to disk.
* When opening files (`RandomAccessFile`), disk io delaying is turned off to reduce the change that data lost (but 
  doesn't take advantage of the OS' buffering). 
* They use a singleton pattern for the FileMgr which handles thread synchronization of disk operations so that multiple
  threads don't write to the same buffer.  If this wasn't synchronize then reads and writes would not be atomic. 
* The `FileMgr` constructor also cleans up temp files each time it is instantiated.

Reference:
* It seems that https://kaitai.io could be a great way to develop a parser for binary structures.  There are already 
  readers for SQLite that may be a good starting point.  See [awesome-kaitai](https://github.com/kaitai-io/awesome-kaitai).
* The [Bytes Utility Library](https://github.com/patrickfav/bytes-java/tree/main) for Java may be useful.  Bytes is a utility library that makes it easy to create, parse, 
  transform, validate and convert byte arrays in Java. It's main class Bytes is a collections of bytes and the main 
  API. It supports endianness as well as copy-on-write and mutable access, so the caller may decide to favor 
  performance.
* [Chronicle Bytes](https://github.com/OpenHFT/Chronicle-Bytes) has a similar purpose to Java NIO's ByteBuffer with 
  many extensions.  This may be a good low level library to build on top of.
