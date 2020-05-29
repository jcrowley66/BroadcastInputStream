package org.apache.commons.io;

import java.io.InputStream;
import java.io.IOException;

/** LICENSE - this is provided under the terms of the Apache 2.0 license - see https://spdx.org/licenses/Apache-2.0.html */

/** One of the BroadcastConsumers.
 *
 *  The read() operation is NOT synchronized nor thread-safe (the same as in the base InputStream).
 *
 *  It is expected that a single separate thread will consume all of the data in this consumer.
 *
 *  All of the shared buffer-switching logic is thread-safe.
 */
public class BroadcastConsumer extends InputStream {
  public  final String                lbl;
  public  final BroadcastInputStream  baseStream;

  // Informational vars
  public final long   started = System.currentTimeMillis();
  private      long   finished = -1;
  private Exception   ex = null;
  private long        threadID = -1;
  private long        numWaits = 0;
  private long        ttlReads = 0;       // Each read is 1 byte

  // Operating vars to read through the actual data stream
  private boolean         isOpen = true;
  private BroadcastBuffer bfr;
  private int             amtPresent;
  private int             indx = 0;

  /** Constructor - package visibile */
  BroadcastConsumer(BroadcastInputStream strmBase, String lbl, BroadcastBuffer startBfr) {
    baseStream  = strmBase;
    this.lbl    = lbl;
    bfr         = startBfr;
    amtPresent  = startBfr.amtPresent;
  }

  @Override
  public int read() throws IOException {
    if(threadID == -1) threadID = Thread.currentThread().getId();
    while(isOpen) try {
      if(indx == amtPresent){
        if(bfr.next != null){       // There is another data buffer, so release this and get next buffer
          bfr        = baseStream.finishedWithBuffer(bfr, lbl);
          amtPresent = bfr.amtPresent;
          indx       = 0;
        } else {
          Thread.sleep(baseStream.waitMillis);
          numWaits++;
        }
      } else {
        int rslt = bfr.data[indx++];
        if(rslt == -1) close();
        else           ttlReads++;
        return rslt;                              // NORMAL EXIT - 1 byte of data!
      }
    } catch( Exception e ){
      if(baseStream.bDebug){
        baseStream.ln(lbl + " closing from Exception: " + e.toString());
        ex.printStackTrace(System.out);
      }
      close();
      if(e instanceof IOException) throw (IOException)e;
      else                         ex = e;
    }
    return -1;
  }
  /** May explicitly close this consumer externally also */
  synchronized public void close() throws IOException {
    if(isOpen){
      finished = System.currentTimeMillis();
      if(baseStream.bDebug){ baseStream.ln(lbl + " close(): " + (ex==null ? "" : ex.toString())); }
      isOpen = false;
      baseStream.finishedWithBuffer(bfr, lbl);
      baseStream.closeConsumer();
    }
  }
  public int available() throws IOException {
    return baseStream.available(bfr, indx);
  }

  /** The mark() and reset() operations are NOT supported. */
  public boolean markSupported() { return false; }
  public void mark() {}
  public void reset() {}

  /** Force setting of the Thread ID for this consumer.
   * Usually set automatically by calling read(), but in unusual (or test) cases may need to force it.
   */
  synchronized public void setThreadID(long ID) { threadID = ID; }

  @Override
  public String toString(){
    return String.format("BroadcastConsumer -- %s, isOpen: %b, BytesRead: %,d, Waits: %,d, TtlWait: %,d", lbl, isOpen, ttlReads, numWaits, numWaits * baseStream.waitMillis);
  }

  /**********************************************************************************************/
  /* All of the following methods are expected to be called infrequently, and usually from a    */
  /* different thread. Hence these are synchronized to avoid declaring each referenced variable */
  /* as 'volatile', especially variables used within the read() loop.                           */
  /**********************************************************************************************/
  /** Did an error occur during processing? Note: An IOException will be thrown directly by read() */
  synchronized public Boolean   hadError()         { return ex != null; }
  /** Return any exception thrown, null if none */
  synchronized public Exception getError()         { return ex; }
  /** Is this consumer still open and able to process a read() */
  synchronized public boolean   isStillOpen()      { return isOpen; }
  /** Return the Thread ID of the thread actually invoking the run() method. -1 if run() has not yet been called. */
  synchronized public long      getThreadID()      { return threadID; }
  /** Get the DISBuffer currently being processed. */
  synchronized public BroadcastBuffer getBuffer()  { return bfr; }
  /** Get the number of read's present in the current buffer */
  synchronized public int       getAmtPresent()    { return amtPresent; }
  /** Get the current index, 0..(amtPresent-1) in the current buffer */
  synchronized public int       getIndex()         { return indx; }
  /** Get the total number of characters read */
  synchronized public long      getTotalReads()    { return ttlReads; }
  /** Get the total time in milliseconds that this consumer spent waiting for data */
  synchronized public long      getTotalWait()     { return numWaits * baseStream.waitMillis; }
  /** Get total elapsed time, returns -1 if still open */
  synchronized public long      getElapsedMillis() { return finished > 0 ? finished - started : -1; }
  /** Get a basic report on this consumer */
  synchronized public String    getReport(int i) { return String.format("[(%1$d) Label: %2$s -- Thrd: %3$d, TtlRead: %4$,d, Waits: %5$,d, TtlWait: %6$,d, Open: %7$b, TotalMillis: %8$,d] ",
          i, lbl, threadID, ttlReads, numWaits, getTotalWait(), isOpen, getElapsedMillis());}
}
