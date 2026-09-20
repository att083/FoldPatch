package dev.reachpad;
import android.app.*;import android.os.*;import android.view.*;import android.graphics.*;import java.lang.reflect.*;
/** Real helper injection into an app: raw/local coordinate consistency, far edges and multi-touch. */
public final class InputIntegrityProbeActivity extends Activity {
 final Handler h=new Handler();NativeService s;int events,two,longPresses;float zoom=1;boolean bad;float lastX,lastY;
 Object get(String name)throws Exception{Field f=NativeService.class.getDeclaredField(name);f.setAccessible(true);return f.get(s);}
 void set(String name,Object value)throws Exception{Field f=NativeService.class.getDeclaredField(name);f.setAccessible(true);f.set(s,value);}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);setContentView(new Board());h.postDelayed(this::run,5000);}
 void tap(float x,float y)throws Exception{set("cursorX",x);set("cursorY",y);event(0,1,300,1500,0,0);event(1,1,300,1500,0,0);}
 void run(){try{s=NativeService.instance;if(!NativeService.isActive())throw new Exception("inactive");Method m=NativeService.class.getDeclaredMethod("setMode",int.class);m.setAccessible(true);m.invoke(s,2);int width=(Integer)get("width"),left=(Integer)get("leftEdge"),right=(Integer)get("rightEdge");tap(width-40,650);
  h.postDelayed(()->{try{if(events<2||Math.abs(lastX-(width-40-(right-left)))>2)throw new Exception("far right coordinate "+lastX);tap(400,650);
   h.postDelayed(()->{try{if(Math.abs(lastX-400)>2)throw new Exception("left coordinate");set("cursorX",(right+width)/2f);set("cursorY",650f);pinch(left);
    h.postDelayed(()->{try{
     if(bad||two<2||zoom<=1)throw new Exception("raw/local or pinch events="+events+" two="+two+" zoom="+zoom+" minimumSpan="+ViewConfiguration.get(this).getScaledMinimumScalingSpan());
     event(0,1,300,1500,0,0);
     h.postDelayed(()->{event(1,1,300,1500,0,0);if(longPresses<1)android.util.Log.e("ReachPadIntegrity","FAIL native long press");else android.util.Log.i("ReachPadIntegrity","PASS far edges, raw/local, native zoom="+zoom+" longPress="+longPresses);},1100);
    }catch(Exception e){fail(e);}},750);
   }catch(Exception e){fail(e);}},500);
  }catch(Exception e){fail(e);}},500);
 }catch(Exception e){fail(e);}}
 // AOSP uses a physical 32 mm minimum span. The old 400 px fixture never
 // crossed it on a 420 dpi emulator, so native scaling could not begin.
 void pinch(int workingWidth){
  float center=workingWidth/2f, endSpan=workingWidth-40;
  event(0,1,center-50,1500,0,0);event(5,2,center-50,1500,center+50,1500);
  for(int i=1;i<=10;i++){final float radius=(100+(endSpan-100)*i/10f)/2;
   h.postDelayed(()->event(2,2,center-radius,1500,center+radius,1500),i*40);
  }
  h.postDelayed(()->{event(6,2,center-endSpan/2,1500,center+endSpan/2,1500);event(1,1,center-endSpan/2,1500,0,0);},440);
 }
 void fail(Exception e){android.util.Log.e("ReachPadIntegrity","FAIL",e);}
 long down;
 void event(int action,int count,float x,float y,float x2,float y2){if(action==0)down=SystemClock.uptimeMillis();MotionEvent.PointerProperties[] p=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] c=new MotionEvent.PointerCoords[count];for(int i=0;i<count;i++){p[i]=new MotionEvent.PointerProperties();p[i].id=i;p[i].toolType=1;c[i]=new MotionEvent.PointerCoords();c[i].x=i==0?x:x2;c[i].y=i==0?y:y2;c[i].pressure=1;c[i].size=1;}MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action==5?261:action==6?262:action,count,p,c,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);s.physicalPadEvent(e);e.recycle();}
 final class Board extends View {
  Paint p=new Paint(3);
  final ScaleGestureDetector scale=new ScaleGestureDetector(InputIntegrityProbeActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){zoom*=d.getScaleFactor();return true;}});
  final GestureDetector taps=new GestureDetector(InputIntegrityProbeActivity.this,new GestureDetector.SimpleOnGestureListener(){public boolean onDown(MotionEvent e){return true;}public void onLongPress(MotionEvent e){longPresses++;}});
  Board(){super(InputIntegrityProbeActivity.this);}protected void onDraw(Canvas c){c.drawColor(0xff172125);p.setColor(Color.WHITE);p.setTextSize(36);c.drawText("ReachPad input check",40,300,p);c.drawText("events="+events+" multi="+two,40,400,p);}public boolean onTouchEvent(MotionEvent e){scale.onTouchEvent(e);taps.onTouchEvent(e);events++;if(e.getPointerCount()==2)two++;int[] loc=new int[2];getLocationOnScreen(loc);for(int i=0;i<e.getPointerCount();i++)if(Math.abs(e.getRawX(i)-e.getX(i)-loc[0])>1||Math.abs(e.getRawY(i)-e.getY(i)-loc[1])>1)bad=true;lastX=e.getX();lastY=e.getY();invalidate();return true;}}
 @Override protected void onDestroy(){h.removeCallbacksAndMessages(null);super.onDestroy();}
}
