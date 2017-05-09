package pers.hal42.stream;

import java.util.*;

public abstract interface StreamEventListener extends EventListener {

  public abstract void notify(EventObject object);

}
