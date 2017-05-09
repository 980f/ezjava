package pers.hal42.database;

/**
 * <p>Title: $Source: /cvs/src/net/paymate/database/ConnectionFifo.java,v $</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: PayMate.net</p>
 * @author PayMate.net
 * @version $Revision: 1.1 $
 */

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.stream.ObjectFifo;

import java.sql.Connection;

public class ConnectionFifo {

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(ConnectionFifo.class);

  public ConnectionFifo() {
  }

  private ObjectFifo content = new ObjectFifo(100); // +++ use a stack instead

  public synchronized Connection next() {
    return (Connection) content.next();
  }

  public synchronized void put(Connection cnxn) {
    content.put(cnxn);
  }

  public int size() {
    int s = content.Size();
    dbg.VERBOSE("returning size " + s);
    return s;
  }

}
