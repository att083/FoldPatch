package dev.foldpatch;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.view.SurfaceControl;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Owns a temporary display-area override. EOF/expired lease restores it even if the helper dies. */
public final class ReflowGuard {
    private static final File JOURNAL = new File("/data/local/tmp/reachpad-reflow.pending");
    private AreaOrganizer organizer;
    private ReflowPolicy.Path path;
    private RandomAccessFile lockFile;
    private java.nio.channels.FileLock lock;
    private final List<Object> tokens = new ArrayList<>();
    private final List<SurfaceControl> leashes = new ArrayList<>();
    private Class<?> tokenClass, transactionClass;
    private boolean changed,ownsJournal;
    private volatile long lease = SystemClock.uptimeMillis();
    private static Object call(Object o,String name,Class<?>[] types,Object... args)throws Exception{return o.getClass().getMethod(name,types).invoke(o,args);}
    private static int number(Object o,String n)throws Exception{return o.getClass().getField(n).getInt(o);}

    private void bounds(Rect rect)throws Exception{
        Object tx=transactionClass.getConstructor().newInstance();
        for(Object token:tokens)call(tx,"setBounds",new Class<?>[]{tokenClass,Rect.class},token,rect);
        organizer.apply(tx);
    }
    private synchronized void acquire(int w,int h,int usable)throws Exception{
        lockFile=new RandomAccessFile("/data/local/tmp/reachpad-reflow.lock","rw");
        lock=lockFile.getChannel().tryLock();
        if(lock==null)throw new IllegalStateException("화면 복구가 진행 중이에요");
        ReflowPolicy.Saved saved=null;
        if(JOURNAL.exists())try(BufferedReader in=new BufferedReader(new FileReader(JOURNAL))){saved=ReflowPolicy.Saved.parse(in.readLine());}
        String snapshot=AreaOrganizer.snapshot();
        path=saved==null?ReflowPolicy.select(snapshot):saved.path;
        ReflowPolicy.requireFree(snapshot,path.feature);
        if(path==ReflowPolicy.Path.COMBINED&&!ReflowPolicy.combinedShape(snapshot))throw new IllegalStateException("지원하지 않는 화면 영역 구성이에요");
        tokenClass=Class.forName("android.window.WindowContainerToken");
        transactionClass=Class.forName("android.window.WindowContainerTransaction");
        organizer=new AreaOrganizer();
        List<?> areas=organizer.register(path.feature);
        boolean pending=saved!=null;
        Rect abandoned=pending?new Rect(0,0,saved.usable,saved.height):null;
        for(Object item:areas){
            Object area=call(item,"getDisplayAreaInfo",new Class<?>[0]);
            leashes.add((SurfaceControl)call(item,"getLeash",new Class<?>[0]));
            if(number(area,"displayId")!=0)continue;
            if(path==ReflowPolicy.Path.COMBINED&&!leashes.get(leashes.size()-1).toString().contains("OneHanded:0:23"))continue;
            Object cfg=area.getClass().getField("configuration").get(area);
            Object wc=cfg.getClass().getField("windowConfiguration").get(cfg);
            Rect original=(Rect)call(wc,"getBounds",new Class<?>[0]);
            if(!original.equals(new Rect(0,0,w,h))&&!(pending&&original.equals(abandoned)))throw new IllegalStateException("기존 화면 범위를 보존하기 위해 보정을 중단했어요");
            tokens.add(area.getClass().getField("token").get(area));
        }
        if(tokens.size()!=1)throw new IllegalStateException("지원하지 않는 화면 영역 구성이에요");
        if(!organizer.valid())throw new IllegalStateException("화면 영역 연결이 끊겼어요");
        if(pending){ownsJournal=true;changed=true;bounds(new Rect());JOURNAL.delete();changed=false;ownsJournal=false;}
        if(usable==0)return;
        try(FileOutputStream out=new FileOutputStream(JOURNAL)){out.write(new ReflowPolicy.Saved(path,w,h,usable).encode().getBytes("UTF-8"));out.getFD().sync();}
        ownsJournal=true;changed=true;bounds(new Rect(0,0,usable,h));
    }
    private synchronized void restore(){
        if(organizer==null){unlock();return;}
        boolean restored=!changed;
        // Never reset bounds after another owner has taken over. Keep the journal
        // for a later recovery when the recorded feature is free again.
        if(changed&&organizer.valid())try{bounds(new Rect());restored=true;changed=false;}catch(Exception e){android.util.Log.e("FoldPatchReflow","Restore bounds",e);}
        try{organizer.close();}catch(Exception e){android.util.Log.w("FoldPatchReflow","Unregister",e);}
        for(SurfaceControl s:leashes)s.release();leashes.clear();organizer=null;
        if(restored&&ownsJournal)JOURNAL.delete();
        unlock();
    }
    private void unlock(){
        try{if(lock!=null)lock.release();}catch(IOException ignored){}lock=null;
        try{if(lockFile!=null)lockFile.close();}catch(IOException ignored){}lockFile=null;
    }
    public static void main(String[] args)throws Exception{
        Looper.prepareMainLooper();
        int w=Integer.parseInt(args[0]),h=Integer.parseInt(args[1]),usable=Integer.parseInt(args[2]);
        if(usable==0){
            // A recovery may run after folding, or from a newly connected helper.
            // Compare against today's display, not the old inner-display dimensions.
            Object dm=Class.forName("android.hardware.display.DisplayManagerGlobal").getMethod("getInstance").invoke(null);
            Object d=call(dm,"getDisplayInfo",new Class<?>[]{int.class},0);
            w=number(d,"logicalWidth");h=number(d,"logicalHeight");
        }
        ReflowGuard guard=new ReflowGuard();
        Runtime.getRuntime().addShutdownHook(new Thread(guard::restore));
        Thread timeout=new Thread(()->{try{while(SystemClock.uptimeMillis()-guard.lease<10000)Thread.sleep(250);guard.restore();System.exit(2);}catch(InterruptedException ignored){}},"reflow-lease");
        timeout.setDaemon(true);timeout.start();
        try{
            guard.acquire(w,h,usable);
            System.out.println("READY "+guard.path.name());System.out.flush();
            if(usable!=0){
                Object dm=Class.forName("android.hardware.display.DisplayManagerGlobal").getMethod("getInstance").invoke(null);
                BufferedReader input=new BufferedReader(new InputStreamReader(System.in));
                String line;
                while((line=input.readLine())!=null){
                    if(!line.equals("PING"))break;
                    Object d=call(dm,"getDisplayInfo",new Class<?>[]{int.class},0);
                    if(!guard.organizer.valid()||number(d,"logicalWidth")!=w||number(d,"logicalHeight")!=h||number(d,"rotation")!=0||number(d,"state")!=2)break;
                    guard.lease=SystemClock.uptimeMillis();
                }
            }
        }finally{guard.restore();}
        System.exit(0);
    }
}
