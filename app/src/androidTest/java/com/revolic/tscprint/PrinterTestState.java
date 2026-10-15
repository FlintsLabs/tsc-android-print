package com.revolic.tscprint;

import android.content.*;
import java.util.*;

/** Test-only settings snapshot so loopback/UI fixtures never leak into another test. */
final class PrinterTestState implements AutoCloseable {
 private final SharedPreferences printer,paper;
 private final Map<String,?> printerValues,paperValues;
 PrinterTestState(Context context) {
  printer=PrinterSettings.preferences(context);paper=PaperSettings.preferences(context);
  printerValues=new HashMap<>(printer.getAll());paperValues=new HashMap<>(paper.getAll());
 }
 /** Start UI/socket fixtures independently of previously saved multi-printer profiles. */
 static PrinterTestState isolated(Context context) {
  PrinterTestState saved=new PrinterTestState(context);
  // Paper first: an active discovery listener can migrate a cleared catalog immediately.
  PaperSettings.preferences(context).edit().clear().commit();
  PaperSettings.select(context,com.revolic.tscprint.core.PaperSize.LABEL);
  PrinterSettings.preferences(context).edit().clear().commit();
  PrinterSettings.save(context,"127.0.0.1");
  return saved;
 }
 @SuppressWarnings("unchecked")
 private void restore(SharedPreferences prefs,Map<String,?> values) {
  SharedPreferences.Editor e=prefs.edit().clear();
  for(Map.Entry<String,?> v:values.entrySet()) {
   if(v.getValue() instanceof String)e.putString(v.getKey(),(String)v.getValue());
   else if(v.getValue() instanceof Set)e.putStringSet(v.getKey(),new HashSet<>((Set<String>)v.getValue()));
  }
  e.commit();
 }
 @Override public void close() {restore(paper,paperValues);restore(printer,printerValues);}
}
