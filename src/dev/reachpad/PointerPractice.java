package dev.reachpad;

import android.content.Context;
import android.graphics.*;
import android.view.*;

/** Local sandbox: same pointer multiplier, never sends input to another app. */
final class PointerPractice extends View {
    private final Paint paint=new Paint(3);private final ReachUi ui;
    private float x=-1,y,lastX,lastY,startX,startY;private boolean moved;private int taps;
    PointerPractice(Context c){super(c);ui=new ReachUi(c);setBackground(ui.shape(ReachUi.SURFACE,8,true));setContentDescription(getContext().getString(R.string.pointer_practice_description));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(x<0){x=getWidth()/2f;y=getHeight()/2f;}paint.setColor(ReachUi.MUTED);paint.setTextSize(ui.dp(12));c.drawText(getContext().getString(R.string.practice_taps,taps),ui.dp(12),ui.dp(24),paint);paint.setColor(ReachUi.INK);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(ui.dp(6));c.drawCircle(x,y,ui.dp(7),paint);paint.setColor(ReachUi.ACCENT);paint.setStrokeWidth(ui.dp(3));c.drawCircle(x,y,ui.dp(7),paint);paint.setStyle(Paint.Style.FILL);}
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:getParent().requestDisallowInterceptTouchEvent(true);startX=lastX=e.getX();startY=lastY=e.getY();moved=false;return true;
            case MotionEvent.ACTION_MOVE:float dx=e.getX()-lastX,dy=e.getY()-lastY;float speed=PointerGeometry.speed(getContext().getSharedPreferences("regions",0).getFloat("pointer_speed",1));x=Math.max(ui.dp(10),Math.min(getWidth()-ui.dp(10),x+dx*speed));y=Math.max(ui.dp(38),Math.min(getHeight()-ui.dp(10),y+dy*speed));if(Math.abs(e.getX()-startX)+Math.abs(e.getY()-startY)>ui.dp(8))moved=true;lastX=e.getX();lastY=e.getY();invalidate();return true;
            case MotionEvent.ACTION_UP:if(!moved)performClick();getParent().requestDisallowInterceptTouchEvent(false);return true;
            case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();taps++;invalidate();return true;}
}
