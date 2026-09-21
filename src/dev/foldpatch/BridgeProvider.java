package dev.foldpatch;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public final class BridgeProvider extends ContentProvider {
    static volatile IBinder helper;
    static volatile Runnable listener;
    private final IBinder lifetime = new Binder();
    @Override public boolean onCreate() { return true; }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (Binder.getCallingUid() != 2000) throw new SecurityException("Only adb shell may register");
        if (!"register".equals(method)) throw new IllegalArgumentException("Unknown operation");
        accept(extras.getBinder("helper"));
        Bundle result = new Bundle(); result.putBinder("lifetime", lifetime); return result;
    }
    static void accept(IBinder binder){
        if(binder==null)return;
        synchronized(BridgeProvider.class){helper=binder;}
        try{binder.linkToDeath(()->{clear(binder);},0);}
        catch(android.os.RemoteException e){clear(binder);}
        notifyListener();
    }
    static void clear(IBinder expected){if(expected==null)return;synchronized(BridgeProvider.class){if(helper!=expected)return;helper=null;}notifyListener();Bridge.helperLost();}
    private static void notifyListener() { Runnable r=listener;if(r!=null)r.run(); }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
    @Override public String getType(Uri u){return null;}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
