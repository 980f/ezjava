package pers.hal42.transport;

/**
 * if a class implements "isEasy" then it has load and save from properties functions, i.e. portable public access via text
 */

public interface isEasy {//for eventual reflective invocation of load and save

  void save(EasyCursor ezc);

  void load(EasyCursor ezc);
}
/*when we get class<->properties via reflection then this interface will be emptied, remaining as permission to do the translations.
another option at that time is to have 'beforeSave()' and 'afterLoad()' members for preparing and interpreting the fields ...but this ain't C++ so we won't bother ourselves with that.

any isEasy class that needs to combine info from its fields must either always do that dynamically, or have another class that uses an easy class to do such work.

TODO: replace with annotations

*/
