package pers.hal42.ext;

import pers.hal42.lang.ArrayIterator;

import java.util.Iterator;
import java.util.function.Function;

/** generates a series of T's from objects that might be a mix of types and iterators */
public class DeepIterator<T> implements Iterator<T> {
  Function<Object, T> fier;
  NestingIterator<Object> things = new NestingIterator<>();

  public DeepIterator(Function<Object, T> fier, Object... things) {
    this.fier = fier;
    this.things.push(new ArrayIterator<>(things));
  }

  @Override
  public boolean hasNext() {
    return things.hasNext();
  }

  @SuppressWarnings("unchecked")
  @Override
  public T next() {
    do {
      Object thing = things.next();
      if (thing instanceof Iterable) {
        things.push(((Iterable<Object>) thing).iterator());
      } else if (thing instanceof Iterator) {
        things.push((Iterator<Object>) thing);
      } else if (thing instanceof Object[]) {
        things.push(new ArrayIterator((Object[]) thing));
      } else {
        return fier.apply(thing);
      }
    } while (hasNext());
    return fier.apply(null);
  }
}
