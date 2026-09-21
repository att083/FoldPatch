package dev.foldpatch.experiment;
public final class ApiProbe {
 public static void main(String[] args)throws Exception {
  for(String name:args){System.out.println("CLASS "+name);for(java.lang.reflect.Method m:Class.forName(name).getDeclaredMethods()){
   String n=m.getName().toLowerCase();if(n.contains("split")||n.contains("task")||n.contains("surface")||n.contains("screenshot")||n.contains("capture")||n.contains("size")||n.contains("offset")||n.contains("displayarea")||n.contains("inset")||n.contains("fold")||n.contains("projection")||n.contains("visiblewindow"))System.out.println(m);
  }}
 }
}
