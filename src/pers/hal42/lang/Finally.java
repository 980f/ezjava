package pers.hal42.lang;

/**
 * Created by Andy on 5/9/2017.
 *
 * use with try-with-resources instead of finding your way to the finally to insert code there.
 * This is handy when you know what needs to be done on the way out when you are at the start, but don't want to have to remember (as a developer) to do it.
 * Consider this as a model, to be reimplemented with something done on creation that needs to be undone later, like popping a stack!
 */
public class Finally implements AutoCloseable {
  public interface Lambda {
    void pop();
  }
  Lambda agent;

  public Finally(Lambda lamda) {
    this.agent = lamda;
  }

  @Override
  public void close() {
    agent.pop();
  }
}
