package dev.reachpad;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/** Separate app process; a frame carries the original input timestamp in an RGB marker. */
public final class LatencyProbeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        setContentView(new View(this) {
            final Paint paint = new Paint();
            long inputTime;
            float x, y;
            @Override protected void onDraw(Canvas canvas) {
                canvas.drawColor(Color.rgb(22, 30, 41));
                paint.setColor(Color.WHITE); paint.setTextSize(42);
                canvas.drawText("반응 속도 측정 중", 40, getHeight() * .5f, paint);
                canvas.drawText("잠시 손을 떼어 주세요", 40, getHeight() * .55f, paint);
                paint.setColor(0xff70e0ac); canvas.drawCircle(x, y, 35, paint);
                // The rendered frame itself records the input it actually contains.
                paint.setColor(0xff000000 | ((int) inputTime & 0xffffff));
                canvas.drawRect(getWidth() - 64, 0, getWidth(), 64, paint);
            }
            @Override public boolean onTouchEvent(MotionEvent event) {
                // Generic axis retains the source tag across Android event-time resampling.
                inputTime = Math.round(event.getAxisValue(MotionEvent.AXIS_GENERIC_1)); x = event.getX(); y = event.getY();
                Log.i("ReachPadPerf", "target input=" + inputTime + " ageMs=" + (((SystemClock.uptimeMillis()&0xffffff)-inputTime)&0xffffff));
                invalidate();
                return true;
            }
        });
    }
}
