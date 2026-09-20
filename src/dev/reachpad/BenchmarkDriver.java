package dev.reachpad;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;
import java.lang.reflect.Method;

/** ADB-shell-only, device-local input source: repeatable timing without Wi-Fi in the measurement. */
public final class BenchmarkDriver {
    public static void main(String[] args) throws Exception {
        if (android.os.Process.myUid()!=2000) throw new SecurityException("Run through adb shell");
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 360;
        int interval = args.length > 1 ? Integer.parseInt(args[1]) : 8;
        if (count<1 || count>2000 || interval<4 || interval>50) throw new IllegalArgumentException("Invalid run size");
        Class<?> type = Class.forName("android.hardware.input.InputManagerGlobal");
        Object manager=type.getMethod("getInstance").invoke(null);
        Method inject=type.getMethod("injectInputEvent",InputEvent.class,int.class);
        Method setDisplay=InputEvent.class.getMethod("setDisplayId",int.class);
        long start=SystemClock.uptimeMillis();
        StringBuilder timestamps=new StringBuilder();
        MotionEvent.PointerProperties property=new MotionEvent.PointerProperties();property.id=0;property.toolType=MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords coordinates=new MotionEvent.PointerCoords();coordinates.pressure=1;coordinates.size=1;
        for(int i=0;i<count+2;i++) {
            long target=start+(long)i*interval;
            long wait=target-SystemClock.uptimeMillis(); if(wait>0)SystemClock.sleep(wait);
            int action=i==0?MotionEvent.ACTION_DOWN:i==count+1?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;
            float y=1100+300*(float)Math.sin(i*.075);
            long now=SystemClock.uptimeMillis();int stamp=(int)(now&0xffffff);
            coordinates.x=500;coordinates.y=y;coordinates.setAxisValue(MotionEvent.AXIS_GENERIC_1,stamp);
            MotionEvent event=MotionEvent.obtain(start,now,action,1,new MotionEvent.PointerProperties[]{property},
                new MotionEvent.PointerCoords[]{coordinates},0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
            if(i>0)timestamps.append(',');timestamps.append(stamp);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);setDisplay.invoke(event,0);
            inject.invoke(manager,event,0);event.recycle();
        }
        System.out.println("BENCHMARK_INPUTS="+(count+2));
        System.out.println("BENCHMARK_TIMES="+timestamps);
    }
}
