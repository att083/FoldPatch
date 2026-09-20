package dev.reachpad.experiment;
public class InsetsProbe {
 public static void main(String[] args)throws Exception{
 Object b=Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"window");
 Object wm=Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface",android.os.IBinder.class).invoke(null,b);
 android.graphics.Rect r=new android.graphics.Rect();wm.getClass().getMethod("getStableInsets",int.class,android.graphics.Rect.class).invoke(wm,0,r);System.out.println("STABLE "+r);
 Class<?> sc=Class.forName("android.view.InsetsState");Object state=sc.getConstructor().newInstance();System.out.println(wm.getClass().getMethod("getWindowInsets",int.class,android.os.IBinder.class,sc).invoke(wm,0,null,state));
 int count=(Integer)sc.getMethod("sourceSize").invoke(state);for(int i=0;i<count;i++)System.out.println(sc.getMethod("sourceAt",int.class).invoke(state,i));
 }
}
