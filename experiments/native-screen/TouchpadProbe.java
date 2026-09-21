package dev.foldpatch.experiment;
import android.os.*;
import android.view.*;
/** Diagnostic only: Android rewrites injected device IDs to -1. Simultaneous helper injection
 * collides with this gesture. Do NOT use it as evidence of real-finger touchpad scrolling. */
public final class TouchpadProbe {
 public static void main(String[] args)throws Exception{
  Object wb=Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"window");
  Object wm=Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,wb);
  boolean own=false;
  for(Object info:(java.util.List<?>)wm.getClass().getMethod("getVisibleWindowInfoList").invoke(wm))
   if(info.getClass().getField("focused").getBoolean(info)&&String.valueOf(info.getClass().getField("name").get(info)).equals("dev.foldpatch/dev.foldpatch.ProbeActivity"))own=true;
  if(!own)throw new IllegalStateException("Requires focused FoldPatch ProbeActivity");
  Class<?> c=Class.forName("android.hardware.input.InputManagerGlobal");Object im=c.getMethod("getInstance").invoke(null);
  java.lang.reflect.Method inject=c.getMethod("injectInputEvent",InputEvent.class,int.class);
  MotionEvent.PointerProperties[] p=new MotionEvent.PointerProperties[2];MotionEvent.PointerCoords[] xy=new MotionEvent.PointerCoords[2];
  for(int i=0;i<2;i++){p[i]=new MotionEvent.PointerProperties();p[i].id=i;p[i].toolType=MotionEvent.TOOL_TYPE_FINGER;xy[i]=new MotionEvent.PointerCoords();xy[i].x=300+i*200;xy[i].y=1250;xy[i].pressure=1;xy[i].size=1;}
  long down=SystemClock.uptimeMillis();
  for(int step=0;step<24;step++){
   int action=step==0?0:step==1?261:step==22?262:step==23?1:2;
   int count=step==0||step==23?1:2;
   if(step>=2&&step<=21)for(MotionEvent.PointerCoords q:xy)q.y=1250-(step-1)*15;
   MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,count,p,xy,0,0,1,1,-43,0,InputDevice.SOURCE_TOUCHSCREEN,0);
   InputEvent.class.getMethod("setDisplayId",int.class).invoke(e,0);inject.invoke(im,e,0);e.recycle();Thread.sleep(20);
  }
  System.out.println("TWO_FINGER_300PX_SENT");
 }
}
