package dev.foldpatch.experiment;
import android.os.SystemClock;
import android.view.*;
/** Sends one bounded touch stream, including an optional stationary hold for recents. */
public final class GestureInputProbe {
 public static void main(String[] args)throws Exception {
  float x0=Float.parseFloat(args[0]),y0=Float.parseFloat(args[1]),x1=Float.parseFloat(args[2]),y1=Float.parseFloat(args[3]);
  int duration=Integer.parseInt(args[4]),hold=Integer.parseInt(args[5]);
  if(duration<1||duration>2000||hold<0||hold>2000)throw new IllegalArgumentException("Bounded gesture only");
  Object im=Class.forName("android.hardware.input.InputManagerGlobal").getMethod("getInstance").invoke(null);
  java.lang.reflect.Method inject=im.getClass().getMethod("injectInputEvent",InputEvent.class,int.class);
  long down=SystemClock.uptimeMillis();send(im,inject,down,0,x0,y0);
  for(int i=1;i<=20;i++){SystemClock.sleep(duration/20);send(im,inject,down,2,x0+(x1-x0)*i/20,y0+(y1-y0)*i/20);}
  SystemClock.sleep(hold);send(im,inject,down,1,x1,y1);
 }
 static void send(Object im,java.lang.reflect.Method inject,long down,int action,float x,float y)throws Exception {
  MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
  InputEvent.class.getMethod("setDisplayId",int.class).invoke(e,0);inject.invoke(im,e,2);e.recycle();
 }
}
