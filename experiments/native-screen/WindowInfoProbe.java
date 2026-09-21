package dev.foldpatch.experiment;
public class WindowInfoProbe {
 public static void main(String[] args)throws Exception{
 Object b=Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"window");
 Object wm=Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface",android.os.IBinder.class).invoke(null,b);
 for(Object item:(java.util.List<?>)wm.getClass().getMethod("getVisibleWindowInfoList").invoke(wm)){
  for(java.lang.reflect.Field f:item.getClass().getFields()) if(!java.lang.reflect.Modifier.isStatic(f.getModifiers()))System.out.print(f.getName()+"="+f.get(item)+" ");System.out.println();
 }
 }
}
