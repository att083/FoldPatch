package dev.foldpatch;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import rikka.shizuku.Shizuku;

/** Control work never blocks the touch/UI thread. Touches use the separate one-way path. */
final class Bridge {
    static final ExecutorService worker=Executors.newSingleThreadExecutor();
    static final Handler ui=new Handler(Looper.getMainLooper());
    private static boolean initialized,binding;
    private static int generation;
    private static long retryAt,lastRestart=-60000;
    private static Context app;
    private static ServiceConnection connection;
    private static Shizuku.UserServiceArgs args;
    static void init(Context context){
        app=context.getApplicationContext();if(initialized)return;initialized=true;ConnectionHistory.init(app);
        Shizuku.addBinderReceivedListenerSticky(()->ui.post(()->{ConnectionHistory.record("server_available");resetBinding();tryBind();}));
        Shizuku.addBinderDeadListener(()->ui.post(()->{ConnectionHistory.record("server_lost");resetBinding();}));
        Shizuku.addRequestPermissionResultListener((code,result)->ui.post(()->{ConnectionHistory.record(result==PackageManager.PERMISSION_GRANTED?"permission_granted":"permission_denied");if(result==PackageManager.PERMISSION_GRANTED)tryBind();}));
    }
    private static void resetBinding(){
        generation++;binding=false;retryAt=0;
        if(connection!=null&&args!=null&&Shizuku.pingBinder())try{Shizuku.unbindUserService(args,connection,false);}catch(Exception ignored){}
        connection=null;
    }
    static void helperLost(){ui.post(()->{ConnectionHistory.record("helper_lost");resetBinding();tryBind();});}
    static void tryBind(){
        if(Looper.myLooper()!=Looper.getMainLooper()){ui.post(Bridge::tryBind);return;}
        if(binding||BridgeProvider.helper!=null||!Shizuku.pingBinder()||SystemClock.uptimeMillis()<retryAt)return;
        try{
            if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED)return;
            binding=true;final int attempt=++generation;ConnectionHistory.record("bind_started");
            args=new Shizuku.UserServiceArgs(new ComponentName(app,ShellBridge.class)).daemon(true).processNameSuffix("control").version((int)(app.getPackageManager().getPackageInfo(app.getPackageName(),0).lastUpdateTime/1000));
            connection=new ServiceConnection(){
                public void onServiceConnected(ComponentName n,IBinder b){ui.post(()->{if(attempt!=generation)return;binding=false;ConnectionHistory.record("helper_connected");BridgeProvider.accept(b);});}
                public void onServiceDisconnected(ComponentName n){ui.post(()->{if(attempt!=generation)return;BridgeProvider.clear(BridgeProvider.helper);resetBinding();retryAt=SystemClock.uptimeMillis()+500;ui.postDelayed(Bridge::tryBind,500);});}
            };
            Shizuku.bindUserService(args,connection);
            ui.postDelayed(()->{if(binding&&attempt==generation){ConnectionHistory.record("bind_timeout");resetBinding();retryAt=SystemClock.uptimeMillis()+1500;ui.postDelayed(Bridge::tryBind,1500);}},5000);
        }catch(Exception e){resetBinding();retryAt=SystemClock.uptimeMillis()+1500;ConnectionHistory.record("bind_failed");ui.postDelayed(Bridge::tryBind,1500);}
    }
    static void restartStalledHelper(){
        if(Looper.myLooper()!=Looper.getMainLooper()){ui.post(Bridge::restartStalledHelper);return;}
        if(SystemClock.uptimeMillis()-lastRestart<60000||args==null||connection==null||!Shizuku.pingBinder())return;
        lastRestart=SystemClock.uptimeMillis();ConnectionHistory.record("helper_unresponsive");
        try{Shizuku.unbindUserService(args,connection,true);}catch(Exception ignored){}
        BridgeProvider.clear(BridgeProvider.helper);resetBinding();retryAt=SystemClock.uptimeMillis()+1500;ui.postDelayed(Bridge::tryBind,1500);
    }
    static void request(){if(Shizuku.pingBinder()){if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED)Shizuku.requestPermission(17);else tryBind();}}
    static Parcel data(){Parcel p=Parcel.obtain();p.writeInterfaceToken("dev.foldpatch.control");return p;}
    static Bundle command(String action,Bundle args) throws Exception {
        return command(action,args,BridgeProvider.helper);
    }
    static Bundle command(String action,Bundle args,IBinder helper) throws Exception {
        if(helper==null)throw new IllegalStateException("사용 준비가 필요해요");
        Parcel p=data(),r=Parcel.obtain();
        try{p.writeString(action);p.writeBundle(args);helper.transact(7,p,r,0);r.readException();return r.readBundle(Bridge.class.getClassLoader());}
        catch(android.os.DeadObjectException e){BridgeProvider.clear(helper);throw e;}
        finally{p.recycle();r.recycle();}
    }
    static void async(String action,Bundle args,Consumer<Bundle> success){
        async(action,args,success,null);
    }
    static void async(String action,Bundle args,Consumer<Bundle> success,Consumer<Exception> failure){
        worker.execute(()->{try{Bundle result=command(action,args);if(success!=null)ui.post(()->success.accept(result));}
            catch(Exception e){android.util.Log.e("FoldPatch","control "+action,e);ui.post(()->{if(failure!=null){failure.accept(e);return;}MainActivity a=MainActivity.current;if(a!=null)a.report("작업을 완료하지 못했어요. 설정에서 연결 상태를 확인해 주세요.");});}});
    }
    static void async(String action,Bundle args){async(action,args,null);}
}
