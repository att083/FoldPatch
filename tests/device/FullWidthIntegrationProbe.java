package dev.foldpatch;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Shell-only check of full-width rendering and transitions back to damaged-screen layouts. */
public final class FullWidthIntegrationProbe {
    private static final File JOURNAL=new File("/data/local/tmp/reachpad-reflow.pending");
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static Object field(Object owner,String name)throws Exception{
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private static String ownership()throws Exception{
        StringBuilder result=new StringBuilder();
        boolean areas=false;
        for(String line:AreaOrganizer.snapshot().split("\n")){
            if(line.contains("Display areas in top down Z order:")){areas=true;continue;}
            if(areas&&line.contains("Task display areas in top down Z order:"))break;
            // Activity/task records also use '*'. Launcher relayout can change
            // those records without changing any display-area ownership.
            if(areas&&line.contains("* "))result.append(line.trim()).append('\n');
        }
        check(result.length()>0,"display-area ownership section missing");
        return result.toString();
    }
    public static void main(String[] args)throws Exception{
        Looper.prepareMainLooper();
        Method make=ShellBridge.class.getDeclaredMethod("createShellContext");make.setAccessible(true);
        NativeScreen screen=new NativeScreen((Context)make.invoke(null),args[0]);
        String baseline=ownership();
        check(!JOURNAL.exists(),"another session has pending recovery");
        try{
            // Repeated full-width starts must keep the pointer surface alive without
            // acquiring display areas, generating screen buffers or overriding bounds.
            for(int i=0;i<3;i++){
                Bundle full=screen.start(.50f,.50f);screen.presented(full.getLong("generation"));
                int w=full.getInt("width");
                check(full.getBoolean("active")&&full.getInt("sourceWidth")==w,"full width unavailable");
                check(full.getInt("left")==full.getInt("right"),"unexpected center gap");
                check(field(screen,"frame")==null&&field(screen,"background")==null,"unneeded composition");
                check(!((ReflowArea)field(screen,"reflow")).alive(),"unneeded reflow process");
                check(!JOURNAL.exists()&&baseline.equals(ownership()),"full width changed display areas");
                screen.cursor(true,w*.75f,650);screen.cursor(true,w*.25f,650);
                Thread.sleep(100);check(screen.check(),"full-width lease lost");
                check(screen.start(.50f,.50f).getLong("generation")==full.getLong("generation"),"unstable full-width surface");
                screen.stop();check(!JOURNAL.exists()&&baseline.equals(ownership()),"full-width cleanup changed display areas");
            }
            System.out.println("PASS full width: no gap, no screen mirror, no reflow, stable lease and clean stop");
            for(float[] range:new float[][]{{.45f,.55f},{.50f,.60f},{.40f,.50f}}){
                Bundle state=screen.start(range[0],range[1]);screen.presented(state.getLong("generation"));
                int w=state.getInt("width");
                check(state.getInt("sourceWidth")==Math.round(w*range[0])+w-Math.round(w*range[1]),"gap width");
                check(JOURNAL.exists()&&field(screen,"frame")!=null,"gap layout not active");
                Thread.sleep(100);check(screen.check(),"gap layout lease lost");
                Bundle full=screen.start(.50f,.50f);screen.presented(full.getLong("generation"));
                check(!JOURNAL.exists()&&baseline.equals(ownership()),"gap to full width did not restore display areas");
                check(full.getInt("sourceWidth")==w&&screen.check(),"gap to full width failed");
            }
            System.out.println("PASS damaged/full-width transitions and 50% on either side");
        }finally{screen.stop();}
        check(!JOURNAL.exists()&&baseline.equals(ownership()),"final cleanup");
        System.out.println("PASS FullWidthIntegrationProbe");System.exit(0);
    }
}
