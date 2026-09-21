package dev.foldpatch;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import java.io.File;
import java.lang.reflect.*;

/** Shell-only emulator probe of the production pipeline. Not exported or included in release. */
public final class ReflowIntegrationProbe {
    private static final File JOURNAL=new File("/data/local/tmp/reachpad-reflow.pending");
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Looper.prepareMainLooper();
        Method make=ShellBridge.class.getDeclaredMethod("createShellContext");make.setAccessible(true);
        NativeScreen screen=new NativeScreen((Context)make.invoke(null),args[0]);
        ReflowPolicy.Path expected=ReflowPolicy.Path.valueOf(args[1]);
        AreaOrganizer competitor=null;
        try{
            for(float[] range:new float[][]{{.45f,.55f},{.25f,.70f}}){
                Bundle state=screen.start(range[0],range[1]);screen.presented(state.getLong("generation"));
                check(state.getBoolean("active"),"inactive");
                check(state.getInt("sourceWidth")==Math.round(1768*range[0])+1768-Math.round(1768*range[1]),"width");
                try(java.io.BufferedReader in=new java.io.BufferedReader(new java.io.FileReader(JOURNAL))){
                    check(ReflowPolicy.Saved.parse(in.readLine()).path==expected,"wrong backend");
                }
                Thread.sleep(400);check(screen.check(),"lease lost");screen.stop();
                check(!JOURNAL.exists(),"normal cleanup journal");
            }
            System.out.println("PASS "+expected+" default and asymmetric ranges: start/present/check/restore");

            screen.start(.45f,.55f);
            Field r=NativeScreen.class.getDeclaredField("reflow");r.setAccessible(true);Object reflow=r.get(screen);
            Field g=ReflowArea.class.getDeclaredField("guard");g.setAccessible(true);Process guard=(Process)g.get(reflow);
            Field pid=guard.getClass().getDeclaredField("pid");pid.setAccessible(true);
            android.system.Os.kill(pid.getInt(guard),android.system.OsConstants.SIGKILL);
            guard.waitFor();screen.stop();check(!JOURNAL.exists(),"killed guard journal");
            System.out.println("PASS SIGKILL guard: persisted bounds recovered");

            screen.start(.45f,.55f);
            // Deliberately replace ONLY the feature this test just acquired itself.
            competitor=new AreaOrganizer();
            java.util.List<?> areas=competitor.register(expected.feature);
            for(Object item:areas)((android.view.SurfaceControl)item.getClass().getMethod("getLeash").invoke(item)).release();
            Thread.sleep(300);
            try{screen.start(.45f,.55f);}catch(Exception expectedLoss){}
            Thread.sleep(400);
            try{screen.stop();}catch(IllegalStateException expectedBusy){}
            check(competitor.valid(),"foreign owner displaced");
            check(JOURNAL.exists(),"unsafe cleanup erased pending bounds");
            competitor.close();competitor=null;
            screen.stop();check(!JOURNAL.exists(),"deferred recovery failed");
            System.out.println("PASS ownership loss: foreign owner preserved; recovery after release");
        }finally{if(competitor!=null)competitor.close();screen.stop();}
        System.out.println("PASS ReflowIntegrationProbe");System.exit(0);
    }
}
