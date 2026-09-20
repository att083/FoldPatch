package dev.reachpad;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/** A real resizable activity for testing layout, aspect ratio and delivered touches. */
public final class ProbeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        setContentView(new Probe());
    }
    final class Probe extends View {
        final Paint p=new Paint(3); int taps=0; float touchX=-1,touchY=-1,offset=0,lastY;
        Probe(){super(ProbeActivity.this);}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){android.util.Log.i("ReachPadProbe","layout="+w+"x"+h+" density="+getResources().getDisplayMetrics().densityDpi);}
        void text(Canvas c,String s,float x,float y,int size,int color){p.setColor(color);p.setTextSize(size);p.setTextAlign(Paint.Align.LEFT);c.drawText(s,x,y,p);}
        @Override protected void onDraw(Canvas c){
            c.drawColor(Color.rgb(19,25,33));float w=getWidth(),h=getHeight();
            p.setColor(Color.CYAN);c.drawCircle(200,500,40,p);c.drawCircle(w-200,500,40,p);
            for(int i=0;i<16;i++){
                p.setColor(Color.HSVToColor(new float[]{i*22.5f,.55f,.85f}));
                c.drawRect(i*w/16,h*.33f,(i+1)*w/16,h*.44f,p);
                text(c,Integer.toString(i+1),i*w/16+8,h*.395f,42,Color.BLACK);
            }
            text(c,"1–16 : 빠진 번호 없이 이어져야 합니다",30,h*.30f,42,Color.WHITE);
            text(c,"왼쪽 화면",30,h*.51f,48,0xff9ed7ee);
            text(c,"오른쪽 화면",w*.55f,h*.51f,48,0xffefce77);
            text(c,"누른 횟수: "+taps,w*.55f,h*.57f,42,Color.WHITE);
            text(c,"스크롤: "+Math.round(offset),w*.55f,h*.62f,36,Color.LTGRAY);
            text(c,"왼쪽에서 커서를 움직여",30,h*.74f,38,Color.WHITE);
            text(c,"오른쪽을 눌러 보세요",30,h*.79f,38,Color.WHITE);
            for(int row=0;row<4;row++)text(c,"행 "+(row+1)+"  ·  ABCDEFGHIJKLMNOP",30,h*.86f+row*65+offset%65,32,0xff718397);
            if(touchX>=0){p.setStyle(Paint.Style.STROKE);p.setColor(Color.GREEN);p.setStrokeWidth(5);c.drawCircle(touchX,touchY,35,p);p.setStyle(Paint.Style.FILL);}
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN)lastY=e.getY();
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE){offset+=e.getY()-lastY;lastY=e.getY();}
            if(e.getActionMasked()==MotionEvent.ACTION_UP){taps++;touchX=e.getX();touchY=e.getY();android.util.Log.i("ReachPadProbe","tap="+taps+" x="+touchX+" y="+touchY+" display="+getDisplay().getDisplayId());}
            invalidate();return true;
        }
    }
}
