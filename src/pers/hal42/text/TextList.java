package pers.hal42.text;

import org.jetbrains.annotations.NotNull;
import pers.hal42.ext.Span;
import pers.hal42.lang.Bool;
import pers.hal42.lang.OS;
import pers.hal42.lang.ReversedCompare;
import pers.hal42.lang.StringX;
import pers.hal42.transport.EasyUrlString;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import static java.text.MessageFormat.format;

/**
 * a vector of strings with handy functions. It has gotten big enough to have a baser class extracted without so many fancy things like support for TextColumn class
 */

public class TextList {
  public Vector<String> storage;
  public boolean allowStretch = true;//only is set to false when debugging blowups
  public static final boolean SMARTWRAP_ON = true;
  //wrap all of Vector's functions that we preserve:
  public static final boolean SMARTWRAP_OFF = false;
  private static final String[] trashArray = new String[0];

  //////////////////////
  public TextList(int initialCapacity, int capacityIncrement) {
    storage = new Vector<>(initialCapacity, capacityIncrement);
  }

  public TextList(int initialCapacity) {
    storage = new Vector<>(initialCapacity);
  }

  public TextList() {
    storage = new Vector<>();
  }

  public TextList(String[] array) {
    storage = new Vector<>();
    fromStringArray(array);
  }

  public TextList(Object[] array) {
    storage = new Vector<>();
    fromObjectArray(array);
  }

  /**
   * this was created at first for wrapping already existing array of strings.
   */
  public TextList(Vector<String> trusted) {
    storage = trusted;
  }

  public TextList(String toSplit, int lineLength, boolean smartWrap) {
    this();
    this.split(toSplit, lineLength, smartWrap);
  }

  // --- this may have too much info in it;
  // maybe it isn't supposed to do the Ascii.bracket stuff ???
  // maybe the ascii stuff should be in the EasyURLString class itself ???
  public TextList(EasyUrlString toSplit, int lineLength, boolean smartWrap) {
    this();
    if (toSplit != null) {
      this.split(Ascii.imageBracketSpace(toSplit.rawValue().getBytes()).toString(), lineLength, smartWrap);
    }
  }

  public TextList(EasyUrlString toSplit) {
    this(toSplit, 40, true);
  }

  public TextList add(String o) {
    storage.addElement(safe(o));
    return this;
  }

  public TextList add(StringBuffer o) {
    return add(String.valueOf(o));
  }

  public TextList add(long ell) {
    return add(Long.toString(ell));
  }

  public TextList add(int eye) {
    return add(Integer.toString(eye));
  }

  public TextList add(char sea) {
    return add("" + sea);//java is full of little holes.
  }

  public TextList add(boolean truth) {
    return add(Bool.toString(truth));
  }

  private String nvprefix(String name) {
    return StringX.OnTrivial(name, "") + "= ";
  }

  public TextList add(String name, String value) {
    return add(nvprefix(name) + value);
  }

  /**
   * @param obj is expected to have a nontrivial toString() function
   */
  public TextList add(String name, Object obj) {
    return add(nvprefix(name) + String.valueOf(obj));
  }

  public TextList add(String name, long ell) {
    return add(nvprefix(name) + ell);
  }

  public TextList add(String name, int eye) {
    return add(nvprefix(name) + eye);
  }

  public TextList add(String name, char sea) {
    return add(nvprefix(name) + sea);
  }

  public TextList add(String name, boolean truth) {
    return add(nvprefix(name) + truth);
  }

  public TextList add(TextList o) {
    if (o != null) {
      appendMore(o);
    }
    return this;
  }

  private void expandIfNeeded(int i) {
    if (i >= size()) {
      if (allowStretch) {
        setSize(i + 1);
      }
    }
  }

  public void insert(int i, String o) {//an insertion
    expandIfNeeded(i);
    storage.add(i, safe(o));
  }

  public String set(int i, String o) {//an overwrite
    expandIfNeeded(i);
    return storage.set(i, safe(o));
  }

  public void ensureCapacity(int newsize) {
    storage.ensureCapacity(newsize);
  }

  public int size() {
    return storage.size();
  }

  public boolean isEmpty() {
    return storage.size() == 0;
  }

  public void setSize(int newsize) {
    storage.setSize(newsize);
  }

  public int indexOf(String lurker) {
    return storage.indexOf(lurker);
  }

  public int lastIndexOf(String lurker, int before) {
    while (before-- > 0) {
      if (itemAt(before).equals(lurker)) {
        break;
      }
    }
    return before;
  }

  public int lastIndexOf(String lurker) {
    return lastIndexOf(lurker, size());
  }

  public boolean contains(String key) {
    return storage.contains(key); //contains uses .equals
  }

  public Object[] toArray() {
    return storage.toArray();
  }

  public String[] toStringArray() {
    return storage.toArray(trashArray);
  }

  public TextList clear() {
    storage.clear();
    return this;
  }

  public String remove(int index) {
    try {
      return itemAt(index);
    } finally {
      storage.remove(index);
    }
  }

  /**
   * Add an item to list and
   *
   * @return index of item just added
   */
  public int Add(String o) {
    storage.addElement(safe(o));
    return storage.size() - 1;
  }

  /**
   * assure that this string is in list, by content not object match!
   *
   * @return true if string was already present
   * String.equals
   */
  public boolean assurePresent(String s) {
    if (this.contains(s)) {
      return true;
    }
    Add(s);
    return false;
  }

  public boolean assurePresent(char c) {
    return assurePresent(String.valueOf(c));
  }

  /**
   * the items indexed by @param enump are CONDITIONALLY inserted into the textlist.
   */
  public TextList assureItems(Enumeration enump) {
    while (enump.hasMoreElements()) {
      String name = String.valueOf(enump.nextElement());
      if (name != null) {
        assurePresent(name);
      }
    }
    return this;
  }

  public TextList assureItems(TextList items) {
    if (items != null) {
      for (int i = items.size(); i-- > 0; ) {
        String name = items.itemAt(i);
        if (name != null) {
          assurePresent(name);
        }
      }
    }
    return this;
  }

  /**
   * @return width of widest entry in list
   */
  public int longestEntry() {
    int max = 0;
    for (String item : storage) {//
      int esize = item.length();
      if (max < esize) {
        max = esize;
      }
    }
    return max;
  }

  public TextList split(String toSplit, int lineLength, boolean smartWrap) {
    if (StringX.NonTrivial(toSplit)) {
      if (lineLength < 1) {
        lineLength = 1;
      }
      storage.ensureCapacity(size() + (toSplit.length() / lineLength) + 1);
      boolean canSplit;

      while (toSplit.length() > 0) {
        int scanner = -1;
        int lastspace = -1;
        boolean foundSplit = false;
        if (lineLength > toSplit.length()) {//rest of input can fit
          lineLength = toSplit.length(); //so we are just looking for new lines now
          smartWrap = false;
        }
        while (++scanner < lineLength) {
          char ch = toSplit.charAt(scanner);
          if (Character.isWhitespace(ch)) {//any word separator
            if (Character.isSpaceChar(ch)) {//just blanks
              lastspace = scanner;
            } else {//is some form of line terminator
              foundSplit = true;
              break;
            }
          }
        }
        if (smartWrap && !foundSplit && lastspace >= 0) {
          scanner = lastspace;
          foundSplit = true;
        }

        storage.addElement(toSplit.substring(0, scanner).trim());//substring excludes last char in range
        if (foundSplit) {//almost always true
          ++scanner;
        }
        toSplit = StringX.restOfString(toSplit, scanner);
      }
    }
    return this;
  }

  // this is a special function that takes a single string
  // and breaks it into several strings of a specified length
  public TextList oldersplit(String toSplit, int lineLength, boolean smartWrap) {
    if (lineLength < 1) {
      lineLength = 1;
    }
    if (toSplit != null) {
      storage.ensureCapacity(toSplit.length() / lineLength + 1);
      //+++add even more to above if smartwrapping
      StringBuffer buff = new StringBuffer(toSplit);
      int index;
      int cutat;
      while (buff.length() > 0) {

        index = Math.min(lineLength, buff.length());
        cutat = String.valueOf(buff).indexOf('\n');//+++ use System property

        int forFun = String.valueOf(buff).indexOf("\\n");
        if (cutat < 0 || cutat >= index) {//gotta cut short of the newline
          cutat = index;//will seek down from here
          if (smartWrap && (lineLength < buff.length())) {
            while ((cutat > 1) && !Character.isWhitespace(buff.charAt(cutat))) {
              --cutat;
            }
            if (cutat < 2) {//didn't find a breaking point
              cutat = index;
            } else {
              ++cutat;//to point to whitespace character
            }
          }
        }
        String piece = buff.substring(0, cutat);
        add(piece);
        //+++ collapse multi-whitespace at cutpoint.
        if (cutat < index) {//then remove the whitespace
          ++cutat;
        }
        buff.delete(0, cutat);
      }
    }
    return this;
  }

  public String itemAt(int index) {//casting and protection for access
    if ((index < storage.size()) && (index >= 0)) {
      try {
        return storage.elementAt(index);
      } catch (ClassCastException ex) {
        return String.valueOf(storage.elementAt(index));
      }
    }
    return "";//the empty string, not a null object!
  }

  /**
   * append text list to end of this one
   *
   * @return how many added
   * todo:1 decide if strings should be cloned
   */

  public int appendMore(TextList from) {
    if (from != null) {
      storage.addAll(from.storage);
      return from.storage.size();
    } else {
      return 0;
    }
  }

  /**
   * append strings from array of strings
   *
   * @return how many it added
   */

  public int fromStringArray(String[] array) {
    // empty first?
    int len = array.length;
    storage.ensureCapacity(size() + len);
    for (String anArray : array) {
      add(anArray);
    }
    return len;
  }

  public int fromObjectArray(Object[] array) {
    // empty first?
    int len = array.length;
    storage.ensureCapacity(size() + len);
    for (Object anArray : array) {
      add(anArray.toString());
    }
    return len;
  }

  /**
   * return new text list made from rest of this one, without modifying this one.
   */
  public TextList tail(int startIndex) {
    TextList newone = new TextList();
    for (int i = startIndex; i < size(); i++) {
      newone.add(itemAt(i));
    }
    return newone;
  }

  /**
   * @param span defines center, its value is the width of the centered data
   * @return new textlist made by centering the lines of this one
   */
  public TextList centered(int span) {//span=this.longest... is a cute choice
    TextList fatter = new TextList(this.size());
    for (int i = 0; i < this.size(); i++) {
      fatter.add(Fstring.centered(this.itemAt(i), span, ' '));
    }
    return fatter;
  }

  /**
   * @param span defines center, its value is the width of the centered data
   * @return new textlist made by left aligning  the lines of this one
   */
  public TextList leftAligned(int span) {//span=this.longest... is a cute choice
    TextList fatter = new TextList(this.size());
    for (int i = 0; i < this.size(); i++) {
      fatter.add(Fstring.fill(this.itemAt(i), span, ' '));
    }
    return fatter;
  }

  /**
   * @param span defines center, its value is the width of the centered data
   * @return new textlist made by left aligning  the lines of this one
   */
  public TextList rightAligned(int span) {//span=this.longest... is a cute choice
    TextList fatter = new TextList(this.size());
    for (int i = 0; i < this.size(); i++) {
      fatter.add(Fstring.righted(this.itemAt(i), span, ' '));
    }
    return fatter;
  }

  /**
   * @return comma separated value string
   * @ see CSVstream
   * todo:1 recognize decimal strings and DON'T quote them.
   * note: this preserves leading and trailing space as well as internal space
   */
  public StringBuffer csv(boolean started) {
    StringBuffer block = new StringBuffer(250); //wag
    boolean quoteit;
    for (String item : storage) {
      String thing = StringX.unNull(item);
      quoteit = thing.indexOf('"') >= 0; //todo:1 need to add more whitespace chars
      if (started) {
        block.append(quoteit ? ",\"" : ",");
      } else {
        started = true;
        if (quoteit) {
          block.append('"');
        }
      }
      block.append(thing);
      if (quoteit) {
        block.append('"');
      }
    }
    return block;
  }

  /***
   *  Append strings from @param csv separating at commas. Empty fields are retained.
   */
  public TextList simpleCsv(String csv) {
    return simpleParse(csv, ',', true);
  }

  /***
   * @return this after appending strings cut at splitchar
   */
  @NotNull
  public TextList simpleParse(String csv, char splitchar, boolean keepEmpties) {
    Span cutter = new Span();
    if (StringX.NonTrivial(csv)) {
      cutter.lowest = 0;
      for (int index = 0; index < csv.length(); ) {
        if (csv.charAt(index++) == splitchar) {
          cutter.highest = index;
          String element = cutter.subString(csv, 0).trim();
          if (StringX.NonTrivial(element) || keepEmpties) {
            add(element);
          }
        }
      }
      add(cutter.subString(csv, 0).trim());//add final field
    }
    return this;
  }

  public String asParagraph(String specialPrefix, String specialEOL) {
    return String.valueOf(Paragraph(specialPrefix, specialEOL));
  }

  public StringBuffer Paragraph(String specialPrefix, String specialEOL) {
    StringBuffer block = new StringBuffer(250); //wag
    String EOL = StringX.TrivialDefault(specialEOL, OS.EOL);
    String PREFIX = StringX.TrivialDefault(specialPrefix, "");

    for (int i = 0; i < storage.size(); i++) {
      block.append(PREFIX);
      block.append(itemAt(i));
      block.append(EOL);//make this be conditional for last item. [see where done below]
    }
    // this is especially used when created QueryStrings, so don't break it!
    if ((block.length() > 0) && (EOL.length() > 0)) { // at least one item was added and the EOL has length.  Now kill the last EOL
      block.setLength(block.length() - EOL.length());//remove trailing EOL
    }
    return block;
  }

  public String asParagraph(String specialEOL) {//tl
    return asParagraph(null, specialEOL);//tl
  }

  public String asParagraph() {//tl
    return asParagraph(null, null);//tl
  }

  public String toString() {
    return asParagraph();//tl
  }

  public Vector<String> Vector() {
    return storage;
  }

  public void toLowerCase() {
    for (int i = 0; i < size(); i++) {
      set(i, itemAt(i).toLowerCase());
    }
  }

  public TextList sort(boolean ascending) {
    if (ascending) {
      Collections.sort(storage);
    } else {
      //noinspection unchecked
      storage.sort(ReversedCompare.New());//untested
    }
    return this;
  }

  public TextList sort(/* +++ boolean ascending */) {
    return sort(true);
  }

  public TextList uniqueify() {
    for (int i = size(); i-- > 0; ) {
      if (indexOf(itemAt(i)) < i) {
        remove(i);
      }
    }
    return this;
  }

  /**
   * @return this, after adding pieces of @param sentence per @see cutword's definitoin of "word"
   */
  public TextList wordsOfSentence(String sentence) {
    if (StringX.NonTrivial(sentence)) {
      StringBuffer parsee = new StringBuffer(sentence);
      while (true) {
        String word = StringX.cutWord(parsee);
        if (StringX.NonTrivial(word)) {
          add(word);
        } else {
          break;
        }
      }
    }
    return this;
  }

  /**
   * @return new textlist each of whose elements is two from original list
   */
  public TextList foldAt(int columnone) {
    TextList newone = TextList.Empty();
    if (columnone > 0) {
      int halfsize = (size() + 1) / 2;
      for (int i = 0; i < halfsize; i++) {//#presever order
        String left = itemAt(i);
        String right = itemAt(i + halfsize);
        newone.add(Fstring.fill(left, columnone, ' ') + right);
      }
    }
    return newone;
  }

  /**
   * @return new array of @param interleave textlists made by distributing
   */
  public TextList[] demultiplex(int interleave) {
    TextList[] demux = new TextList[interleave];
    int length = (size() + interleave - 1) / interleave;
    int leaf;
    for (leaf = interleave; leaf-- > 0; ) {
      demux[leaf] = new TextList(length);
    }

    for (int incoming = 0; incoming < size(); incoming++) {//#preserving order
      demux[leaf].add(itemAt(incoming));
      if (++leaf >= interleave) {//inc leaf modulo interleave
        leaf = 0;
      }
    }
    return demux;
  }

  public void add(StringIterator it) {
    while (it.hasNext()) {
      add(it.next());
    }
  }

  public StringIterator iterator() {

    return new StringIterator() {
      int pointer = 0;

      @Override
      public boolean hasNext() {
        return pointer < storage.size();
      }

      /**
       * @returns null if hasNext() returned false;
       */
      @Override
      public String next() {
        return storage.get(pointer++);
      }

      /** makes hasNext() return false, frees and resources */
      @Override
      public void discard() {
        pointer = storage.size();
      }
    };
  }

  public static String safe(String o) {
    return StringX.TrivialDefault(o, ""); //# DO NOT USE StringX.OnTrivial: It turns "" into " ", which is NOT what we need here.
  }

  /**
   * @return new textlist made from @param array of strings
   */
  public static TextList CreateFrom(String[] array) {
    TextList newone = new TextList();
    newone.fromStringArray(array);
    return newone;
  }

  public static TextList CreateFrom(String csvLine) {
    TextList newone = new TextList();
    newone.simpleCsv(csvLine);
    return newone;
  }

  public static boolean NonTrivial(TextList arf) {
    return arf != null && arf.size() > 0;
  }

  public static TextList Empty() {
    return new TextList();
  }

  public static TextList clone(TextList from) {
    TextList ret = Empty();
    ret.appendMore(from);
    return ret;
  }

  public static TextList EnumMenu(Class<? extends Enum> eclaz) {
    TextList newone = TextList.Empty();
    final Enum[] ees = eclaz.getEnumConstants();
    for (Enum item : ees) {
      newone.add(format("{0}[{1}]", item.toString(), item.ordinal()));
    }
    return newone;
  }
}

