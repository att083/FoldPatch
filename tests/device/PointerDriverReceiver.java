package dev.foldpatch;
import android.content.*;import android.os.*;import android.view.*;import java.lang.reflect.*;
/** Debug-only, DUMP-protected driver. Exercises the actual Pad instead of bypassing its mapping. */
public final class PointerDriverReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){new Handler(Looper.getMainLooper()).post(()->{try{
  NativeService s=NativeService.instance;if(s==null||!NativeService.isActive())throw new IllegalStateException("inactive");
  if(i.hasExtra("healthy_right")){c.getSharedPreferences("regions",0).edit().putBoolean("touch_right",i.getBooleanExtra("healthy_right",false)).apply();NativeService.settingsChanged();}
  if(i.hasExtra("target")){Method m=NativeService.class.getDeclaredMethod("setMode",int.class);m.setAccessible(true);m.invoke(s,i.getIntExtra("target",2));}
  if(!i.hasExtra("x"))return;
  for(String key:new String[]{"cursorX","cursorY"}){Field f=NativeService.class.getDeclaredField(key);f.setAccessible(true);f.setFloat(s,i.getFloatExtra(key.equals("cursorX")?"x":"y",500));}
  long t=SystemClock.uptimeMillis();float px=c.getSharedPreferences("regions",0).getBoolean("touch_right",false)?1300:300;
  for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(t,t,action,px,900,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);s.physicalPadEvent(e);e.recycle();}
  android.util.Log.i("FoldPatchDriver","tap dispatched");
 }catch(Exception e){android.util.Log.e("FoldPatchDriver","FAIL",e);}});}
}
