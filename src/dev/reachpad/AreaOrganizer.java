package dev.reachpad;

import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.view.SurfaceControl;
import java.lang.reflect.*;
import java.util.List;

/** Hidden organizer API adapter with explicit ownership-loss notifications.
 * Transaction IDs are resolved from this OS's Stub, never hard-coded. */
final class AreaOrganizer extends Binder {
    static String snapshot()throws Exception{
        Process process=new ProcessBuilder("/system/bin/dumpsys","window","displays").redirectErrorStream(true).start();
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        java.util.concurrent.atomic.AtomicReference<Exception> error=new java.util.concurrent.atomic.AtomicReference<>();
        Thread reader=new Thread(()->{try(java.io.InputStream in=process.getInputStream()){
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
        }catch(Exception e){error.set(e);}},"display-area-snapshot");reader.setDaemon(true);reader.start();
        try{
            if(!process.waitFor(3,java.util.concurrent.TimeUnit.SECONDS))throw new java.io.IOException("Display area query timed out");
            reader.join(500);
            if(reader.isAlive()||error.get()!=null||process.exitValue()!=0)throw new java.io.IOException("Display area query failed",error.get());
            return bytes.toString("UTF-8");
        }finally{if(process.isAlive())process.destroy();}
    }
    private static final String DESCRIPTOR="android.window.IDisplayAreaOrganizer";
    private final Object owner,controller,callback;
    private final Class<?> iface;
    private final Parcelable.Creator<?> infoCreator;
    private final int appeared,vanished,changed;
    private volatile boolean lost;
    private boolean registered;
    AreaOrganizer()throws Exception{
        Class<?> type=Class.forName("android.window.DisplayAreaOrganizer");
        owner=type.getConstructor(java.util.concurrent.Executor.class).newInstance((java.util.concurrent.Executor)Runnable::run);
        Method get=type.getDeclaredMethod("getController");get.setAccessible(true);controller=get.invoke(owner);
        if(controller==null)throw new IllegalStateException("Display area controller unavailable");
        iface=Class.forName(DESCRIPTOR);Class<?> stub=Class.forName(DESCRIPTOR+"$Stub");
        callback=stub.getMethod("asInterface",android.os.IBinder.class).invoke(null,this);
        appeared=code(stub,"onDisplayAreaAppeared");vanished=code(stub,"onDisplayAreaVanished");changed=code(stub,"onDisplayAreaInfoChanged");
        infoCreator=(Parcelable.Creator<?>)Class.forName("android.window.DisplayAreaInfo").getField("CREATOR").get(null);
    }
    private static int code(Class<?> stub,String name)throws Exception{Field f=stub.getDeclaredField("TRANSACTION_"+name);f.setAccessible(true);return f.getInt(null);}
    private static Object call(Object o,String n,Class<?>[] types,Object...args)throws Exception{return o.getClass().getMethod(n,types).invoke(o,args);}
    List<?> register(int feature)throws Exception{
        registered=true;
        Object slice=call(controller,"registerOrganizer",new Class<?>[]{iface,int.class},callback,feature);
        return (List<?>)call(slice,"getList",new Class<?>[0]);
    }
    void apply(Object transaction)throws Exception{call(owner,"applyTransaction",new Class<?>[]{Class.forName("android.window.WindowContainerTransaction")},transaction);}
    boolean valid(){return registered&&!lost;}
    void close()throws Exception{if(registered){call(controller,"unregisterOrganizer",new Class<?>[]{iface},callback);registered=false;}}
    @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
        if(code==INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(DESCRIPTOR);return true;}
        if(code!=appeared&&code!=vanished&&code!=changed)return super.onTransact(code,data,reply,flags);
        data.enforceInterface(DESCRIPTOR);
        try{
            if(data.readInt()!=0)infoCreator.createFromParcel(data);
            if(code==appeared){if(data.readInt()!=0)SurfaceControl.CREATOR.createFromParcel(data).release();lost=true;}
            if(code==vanished)lost=true;
            // Bounds updates generate info-changed callbacks, including our own transaction.
            // Topology additions/removals invalidate the session instead of guessing a new shape.
            return true;
        }catch(RuntimeException e){lost=true;android.util.Log.e("ReachPadReflow","Organizer callback",e);return true;}
    }
}
