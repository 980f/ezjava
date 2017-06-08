package pers.hal42.database;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * Created by Andy on 6/8/2017.
 *
 * a blob wrapper tha doesn't throw pointless exceptions.
 * It also doesn't implement mutators, it does just enough to work with queries
 */
public class Blobber implements java.sql.Blob {
  public byte []content;

  public Blobber(byte[] content) {
    this.content = content;
  }

  @Override
  public long length() throws SQLException {
    return content.length;
  }

  @Override
  public byte[] getBytes(long pos, int length) throws SQLException {
    //todo: guard args and if not equal to this then create new byte[]
    return content;
  }

  @Override
  public InputStream getBinaryStream() throws SQLException {
    return new ByteArrayInputStream(content);
  }

  @Override
  public long position(byte[] pattern, long start) throws SQLException {
    return -1;
  }

  @Override
  public long position(Blob pattern, long start) throws SQLException {
    return -1;
  }

  @Override
  public int setBytes(long pos, byte[] bytes) throws SQLException {
    return 0;
  }

  @Override
  public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
    return 0;
  }

  @Override
  public OutputStream setBinaryStream(long pos) throws SQLException {
    return null;
  }

  @Override
  public void truncate(long len) throws SQLException {

  }

  @Override
  public void free() throws SQLException {
    content=null;
  }

  @Override
  public InputStream getBinaryStream(long pos, long length) throws SQLException {
    return  new ByteArrayInputStream(content);
  }
}
