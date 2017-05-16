package pers.hal42.database;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.stream.ObjectFifo;

import java.sql.Connection;

/**
 * a fifo of connections, but for some reason with a more restricted interface than the ObjectFifo it iss built from.
 * alh thinks that is because of uncertainty of how it should be implemented so the implemenation is forcefully hidden.
 */

public class ConnectionFifo {

  private ObjectFifo<Connection> content = new ObjectFifo<>(100); //todo:2 use a stack instead?
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(ConnectionFifo.class);

  public ConnectionFifo() {
  }

  public synchronized Connection next() {
    return content.next();
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
