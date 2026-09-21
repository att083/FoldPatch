package dev.foldpatch;

import java.io.*;
import java.util.concurrent.TimeUnit;

/** Parent side of the reflow guard; no shell interpolation or permanent display settings. */
final class ReflowArea {
    private final String apk;
    private Process guard, recovery;
    private OutputStream commands;
    private int width,height;
    private ReflowPolicy.Path path;
    boolean combined(){return path==ReflowPolicy.Path.COMBINED;}
    ReflowArea(String apk){this.apk=apk;}
    void start(int w,int h,int usable)throws Exception{
        width=w;height=h;stop();
        ProcessBuilder b=new ProcessBuilder("/system/bin/app_process","/system/bin","dev.foldpatch.ReflowGuard",String.valueOf(w),String.valueOf(h),String.valueOf(usable));
        b.environment().put("CLASSPATH",apk);b.redirectError(ProcessBuilder.Redirect.INHERIT);
        guard=b.start();commands=guard.getOutputStream();
        BufferedReader response=new BufferedReader(new InputStreamReader(guard.getInputStream()));
        long until=android.os.SystemClock.uptimeMillis()+6000;
        while(!response.ready()&&guard.isAlive()&&android.os.SystemClock.uptimeMillis()<until)Thread.sleep(20);
        String ready=response.ready()?response.readLine():null;
        if(ready==null||!ready.startsWith("READY ")){stop();throw new IllegalStateException("화면 재배치를 준비하지 못했어요");}
        try{path=ReflowPolicy.Path.valueOf(ready.substring(6));}catch(IllegalArgumentException e){stop();throw e;}
        ping();
    }
    boolean alive(){return guard!=null&&guard.isAlive();}
    void ping()throws IOException{commands.write("PING\n".getBytes("UTF-8"));commands.flush();}
    private static void forceKill(Process child)throws IOException {
        // Android 15 UNIXProcess inherits Process.destroyForcibly(), which calls
        // destroy() (SIGTERM) again. Explicitly signal only our retained child.
        synchronized(child){
            if(!child.isAlive())return;
            try{
                if(!child.getClass().getName().equals("java.lang.UNIXProcess"))throw new IOException("Unknown child process implementation");
                java.lang.reflect.Field field=child.getClass().getDeclaredField("pid");field.setAccessible(true);
                int pid=field.getInt(child);
                if(pid<=0||pid==android.os.Process.myPid())throw new IOException("Invalid child process");
                android.system.Os.kill(pid,android.system.OsConstants.SIGKILL);
            }catch(android.system.ErrnoException e){
                if(e.errno!=android.system.OsConstants.ESRCH)throw new IOException("Cannot stop child",e);
            }catch(ReflectiveOperationException e){throw new IOException("Cannot identify child",e);}
        }
    }
    void stop(){
        if(commands!=null)try{commands.close();}catch(IOException ignored){}commands=null;
        try{
            // Keep ownership until the process is confirmed dead. In particular, never
            // let NativeScreen release descendant leashes while this ancestor may live.
            ProcessShutdown.finish(guard,1500,500,1000,ReflowArea::forceKill);guard=null;
            if(recovery!=null){
                ProcessShutdown.finish(recovery,1500,500,1000,ReflowArea::forceKill);
                int code=recovery.exitValue();recovery=null;
                if(code!=0)throw new IOException("Previous bounds recovery failed");
            }
            File journal=new File("/data/local/tmp/reachpad-reflow.pending");
            if(!journal.exists())return;
            ProcessBuilder b=new ProcessBuilder("/system/bin/app_process","/system/bin","dev.foldpatch.ReflowGuard",String.valueOf(width),String.valueOf(height),"0");
            b.environment().put("CLASSPATH",apk);b.redirectError(ProcessBuilder.Redirect.INHERIT);
            recovery=b.start();recovery.getOutputStream().close();
            ProcessShutdown.finish(recovery,3000,500,1000,ReflowArea::forceKill);
            int code=recovery.exitValue();recovery=null;
            if(code!=0||journal.exists())throw new IOException("Bounds recovery is incomplete");
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();throw new IllegalStateException("Interrupted screen cleanup",e);
        }catch(IOException e){throw new IllegalStateException("Screen cleanup is incomplete",e);}
    }
}
