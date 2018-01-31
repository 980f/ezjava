package pers.hal42.database;

import pers.hal42.lang.StringX;
import pers.hal42.stream.IOX;
import pers.hal42.transport.CsvTransport;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.function.Consumer;

public class Whiner<Complaint> implements Consumer<Complaint>, AutoCloseable {
  public HashSet<String> alreadyWhined = new HashSet<>(100);
  protected CsvTransport.LineWriter lw;

  protected Whiner(Class complaint, String filename) throws IOException {
    filename = StringX.TrivialDefault(filename, complaint.getName() + ".tsv");
    final CsvTransport ct = new CsvTransport(complaint);
    IOX.makeBackup(filename);
    lw = ct.new LineWriter(new BufferedWriter(new FileWriter(filename)));
  }

  /**
   * override this
   */
  protected String getKey(Complaint item) {
    return item.toString();
  }

  public void accept(Complaint item) {
    String key = getKey(item);
    if (!alreadyWhined.contains(key)) {
      alreadyWhined.add(key);
      lw.write(item);
    }
  }

  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }

  @Override
  public void close() {
    IOX.Close(lw);
    lw = null;
  }
}
