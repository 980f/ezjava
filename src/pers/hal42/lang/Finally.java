package pers.hal42.lang;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by Andy on 5/9/2017.
 */
public class Finally implements Closeable {
  interface Lambda{
    void act();
  }
  Lambda agent;
  public Finally(Lambda lamda){
    this.agent=lamda;
  }

  @Override
  public void close() throws IOException {
    agent.act();
  }
}
