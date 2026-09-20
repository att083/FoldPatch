package dev.reachpad;

import android.app.Activity;
import android.graphics.*;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;

/** Debug-only end-to-end test: synthetic pad streams, real Binder injection and app delivery.
 * Does not test hardware digitizer events or accessibility's hardware-input capture. */
public final class TouchSideProbeActivity extends Activity {
    final Handler handler=new Handler();
    int taps;float lastX,lastY,dx,dy;
    private Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(5894);setContentView(new Board());
        handler.postDelayed(()->runTest(),1800);
    }
    private void runTest(){
        try{
            NativeService s=NativeService.instance;
            if(s==null||!NativeService.isActive())throw new IllegalStateException("Service not active");
            boolean right=TouchSide.right(getSharedPreferences("regions",0));
            int target=getIntent().getIntExtra("target",right?0:1);
            Method mode=NativeService.class.getDeclaredMethod("setMode",int.class);mode.setAccessible(true);mode.invoke(s,target);
            int left=(Integer)field(s,"leftEdge"),edge=(Integer)field(s,"rightEdge"),width=(Integer)field(s,"width");
            float px=right?(edge+width)/2f:left/2f,py=1400;
            if(!s.startsOnPad(px,py))throw new AssertionError("Working-side center is not on pad");
            if(s.startsOnPad(right?left/2f:(edge+width)/2f,py))throw new AssertionError("Broken-side center is on pad");
            float cursor=target==0?left/2f:(edge+width)/2f;
            set(s,"cursorX",cursor);set(s,"cursorY",1300f);
            single(s,0,px,py);single(s,2,px+40,py);single(s,1,px+40,py);
            float speed=(Float)field(s,"pointerSpeed");
            if(Math.abs((Float)field(s,"cursorX")-(cursor+40*speed))>1)throw new AssertionError("Pointer movement is wrong");
            set(s,"cursorX",cursor);
            single(s,0,px,py);single(s,1,px,py);
            handler.postDelayed(()->{
                try{
                    if(taps<1)throw new AssertionError("Click not delivered");
                    float expected=target==0?left/2f:(2*left+width-edge)/2f;
                    if(Math.abs(lastX-expected)>8)throw new AssertionError("Click coordinate "+lastX+" expected "+expected);
                    if(target==2){set(s,"cursorX",left/2f);single(s,0,px,py);single(s,1,px,py);}
                    single(s,0,px,py);multi(s,5|(1<<8),px,py);multi(s,2,px+70,py+100);multi(s,6|(1<<8),px+70,py+100);single(s,1,px+70,py+100);
                    handler.postDelayed(()->{
                        if(taps<(target==2?3:2)||Math.abs(dx)<40||Math.abs(dy)<60)android.util.Log.e("ReachPadSideTest","FAIL scrolling dx="+dx+" dy="+dy);
                        else android.util.Log.i("ReachPadSideTest","PASS side="+(right?"right":"left")+" target="+target+" taps="+taps+" dx="+dx+" dy="+dy);
                    },350);
                }catch(Throwable e){android.util.Log.e("ReachPadSideTest","FAIL",e);}
            },350);
        }catch(Throwable e){android.util.Log.e("ReachPadSideTest","FAIL",e);}
    }
    private long down;
    private void single(NativeService s,int action,float x,float y)throws Exception{if(action==0)down=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);dispatch(s,e);}
    private void multi(NativeService s,int action,float x,float y)throws Exception{
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[2];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[2];
        for(int i=0;i<2;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=i;properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=x+(i==0?-20:20);coords[i].y=y;coords[i].pressure=1;coords[i].size=1;}
        MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,2,properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);dispatch(s,e);
    }
    private void dispatch(NativeService s,MotionEvent e)throws Exception{
        try{
            s.physicalPadEvent(e);
        }finally{e.recycle();}
    }
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
    final class Board extends View {
        final Paint p=new Paint(3);
        Board(){super(TouchSideProbeActivity.this);}
        @Override protected void onDraw(Canvas c){c.drawColor(0xff161c1e);p.setColor(0xffaed5b4);p.setTextSize(40);c.drawText("ReachPad touch-side test",40,450,p);c.drawText("taps="+taps+" dx="+dx+" dy="+dy,40,550,p);}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==2){dx+=e.getX()-lastX;dy+=e.getY()-lastY;}if(e.getActionMasked()==1)taps++;lastX=e.getX();lastY=e.getY();invalidate();return true;}
    }
}
