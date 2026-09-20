package dev.reachpad;

import android.app.Application;
import android.app.ActivityOptions;
import android.app.ActivityManager;
import android.graphics.Rect;
import android.view.KeyEvent;
import java.util.*;
import java.io.*;
import android.app.Instrumentation;
import android.content.AttributionSource;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.VirtualDisplayConfig;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.Surface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Runs as adb shell, never as root. All transactions are restricted to this APK's UID. */
public final class ShellBridge extends Binder {
    private final Context context;
    private final int appUid;
    private final DisplayManager displays;
    private volatile VirtualDisplay virtualDisplay;
    private Object tasks;
    private int pendingTask=-1, fullTask=-1, leftTask=-1, rightTask=-1, width, height, leftWidth;
    private String previousIme="";
    private static final String OWN_IME="dev.reachpad/.ReachKeyboard";
    private Object inputManager;
    private Method injectInput;
    private Method setInputDisplay;
    private final Handler main = new Handler(Looper.getMainLooper());
    private NativeScreen nativeScreen;
    private IBinder nativeClient;

    public ShellBridge() throws Exception { this(createShellContext()); }

    private ShellBridge(Context context) throws Exception {
        this.context = context;
        tasks=Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
        appUid = context.getPackageManager().getApplicationInfo("dev.reachpad", 0).uid;
        Constructor<DisplayManager> constructor = DisplayManager.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        displays = constructor.newInstance(context);
        Class<?> manager = Class.forName("android.hardware.input.InputManagerGlobal");
        inputManager = manager.getMethod("getInstance").invoke(null);
        injectInput = manager.getMethod("injectInputEvent", InputEvent.class, int.class);
        setInputDisplay = InputEvent.class.getMethod("setDisplayId", int.class);
        nativeScreen=new NativeScreen(context,context.getPackageManager().getApplicationInfo("dev.reachpad",0).sourceDir);
        main.post(new Runnable(){public void run(){
            boolean was=nativeScreen.active();
            try{if(!nativeScreen.check()&&was)restoreIme();}
            catch(Exception e){
                android.util.Log.w("ReachPadNative","Retry screen cleanup",e);
                try{restoreIme();}catch(Exception ignored){}
            }finally{main.postDelayed(this,1000);}
        }});
    }

    @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if(code==16777115 && (Binder.getCallingUid()==2000 || Binder.getCallingUid()==0 || Binder.getCallingUid()==appUid)){nativeScreen.stop();try{leave();}catch(Exception ignored){} System.exit(0);return true;}
        if (Binder.getCallingUid() != appUid) throw new SecurityException("Unexpected caller");
        data.enforceInterface("dev.reachpad.control");
        long identity = Binder.clearCallingIdentity();
        try {
            if (code == 1) {
                Surface surface = Surface.CREATOR.createFromParcel(data);
                int width = data.readInt(), height = data.readInt(), density = data.readInt();
                float requestedRate = data.readFloat();
                try {
                    this.width=width;this.height=height;
                    if(leftWidth<=0)leftWidth=width/2;
                    // PUBLIC, PRESENTATION, OWN_CONTENT_ONLY, SUPPORTS_TOUCH,
                    // TRUSTED, OWN_DISPLAY_GROUP, ALWAYS_UNLOCKED, OWN_FOCUS,
                    // TOUCH_FEEDBACK_DISABLED. No auto-mirroring of the host display.
                    int vdFlags = 1 | 2 | 8 | 64 | 1024 | 2048 | 4096 | 16384 | 8192;
                    if(virtualDisplay!=null){virtualDisplay.resize(width,height,density);virtualDisplay.setSurface(surface);}
                    else if (requestedRate > 60 && android.os.Build.VERSION.SDK_INT >= 34) {
                        VirtualDisplayConfig config = new VirtualDisplayConfig.Builder("ReachPad", width, height, density)
                            .setSurface(surface).setFlags(vdFlags).setRequestedRefreshRate(requestedRate).build();
                        virtualDisplay = displays.createVirtualDisplay(config);
                    } else {
                        virtualDisplay = displays.createVirtualDisplay("ReachPad", width, height, density, surface, vdFlags);
                    }
                    if (virtualDisplay == null) throw new IllegalStateException("No virtual display returned");
                    int id = virtualDisplay.getDisplay().getDisplayId();
                    System.out.println("REACHPAD_DISPLAY=" + id + " " + width + "x" + height);
                    setImePolicy(id);
                    reply.writeNoException(); reply.writeInt(id);
                } finally { surface.release(); }
            } else if (code == 2 || code == 6 || code == 8) {
                if (code==8?!nativeScreen.active():virtualDisplay==null) throw new IllegalStateException("Display not ready");
                MotionEvent event = MotionEvent.CREATOR.createFromParcel(data);
                try {
                    setInputDisplay.invoke(event, code==8?0:virtualDisplay.getDisplay().getDisplayId());
                    boolean accepted = (Boolean) injectInput.invoke(inputManager, event, 0);
                    if (reply != null) { reply.writeNoException(); reply.writeInt(accepted ? 1 : 0); }
                    if (!accepted) System.err.println("ReachPad input rejected");
                } finally { event.recycle(); }
            } else if (code == 3) {
                if (virtualDisplay != null) virtualDisplay.release();
                virtualDisplay = null;
                reply.writeNoException();
            } else if (code == 4) {
                if (virtualDisplay == null) throw new IllegalStateException("Display not ready");
                width=data.readInt();height=data.readInt();virtualDisplay.resize(width,height,data.readInt());
                reply.writeNoException();
            } else if (code == 5) {
                if (virtualDisplay == null) throw new IllegalStateException("Display not ready");
                int id = virtualDisplay.getDisplay().getDisplayId();
                // Fixed allow-list, never an arbitrary shell command from an app.
                String target = data.readString();
                String component;
                if ("probe".equals(target)) component = "dev.reachpad/.ProbeActivity";
                else if ("latency".equals(target)) component = "dev.reachpad/.LatencyProbeActivity";
                else if ("chrome".equals(target)) component = "com.android.chrome/com.google.android.apps.chrome.Main";
                else if ("youtube".equals(target)) component = "com.google.android.youtube/com.google.android.youtube.app.honeycomb.Shell$HomeActivity";
                else throw new IllegalArgumentException("Unknown app");
                new ProcessBuilder("/system/bin/am", "start", "--display", Integer.toString(id), "-n", component).inheritIO().start();
                reply.writeNoException();
            } else if(code==7){
                Bundle result=command(data.readString(),data.readBundle(getClass().getClassLoader()));
                reply.writeNoException();reply.writeBundle(result);
            } else if(code==9){
                nativeScreen.cursor(data.readInt()!=0,data.readFloat(),data.readFloat());
            } else return super.onTransact(code, data, reply, flags);
        } catch (Exception error) {
            error.printStackTrace();
            if (reply != null) reply.writeException(new IllegalStateException(error.toString()));
        } finally { Binder.restoreCallingIdentity(identity); }
        return true;
    }

    private Object invoke(String name, Class<?>[] signature, Object... args) throws Exception {
        return tasks.getClass().getMethod(name,signature).invoke(tasks,args);
    }
    @SuppressWarnings("unchecked") private List<ActivityManager.RunningTaskInfo> taskList() throws Exception {
        return (List<ActivityManager.RunningTaskInfo>)invoke("getTasks",new Class[]{int.class,boolean.class,boolean.class,int.class},80,false,false,-1);
    }
    private int taskDisplay(ActivityManager.RunningTaskInfo t) throws Exception {return t.getClass().getField("displayId").getInt(t);}
    private boolean normal(ActivityManager.RunningTaskInfo t) {
        if(t.topActivity==null)return false;
        String p=t.topActivity.getPackageName();
        if(p.equals("dev.reachpad")||p.equals("com.android.systemui")||p.equals("com.sec.android.app.launcher"))return false;
        return t.baseActivity!=null;
    }
    private int id(){return virtualDisplay==null?-1:virtualDisplay.getDisplay().getDisplayId();}
    private void position(int task, int display, int mode, Rect bounds) throws Exception {
        ActivityManager.RunningTaskInfo found=null;
        for(ActivityManager.RunningTaskInfo t:taskList())if(t.taskId==task){found=t;break;}
        if(found==null)throw new IllegalStateException("앱이 종료되었습니다");
        if(taskDisplay(found)!=display)invoke("moveRootTaskToDisplay",new Class[]{int.class,int.class},task,display);
        Object token=found.getClass().getField("token").get(found);
        Class<?> tokenClass=Class.forName("android.window.WindowContainerToken");
        Class<?> txClass=Class.forName("android.window.WindowContainerTransaction");
        Object tx=txClass.getConstructor().newInstance();
        txClass.getMethod("setWindowingMode",tokenClass,int.class).invoke(tx,token,mode);
        txClass.getMethod("setBounds",tokenClass,Rect.class).invoke(tx,token,bounds);
        txClass.getMethod("reorder",tokenClass,boolean.class).invoke(tx,token,true);
        Object organizer=Class.forName("android.window.WindowOrganizer").getConstructor().newInstance();
        organizer.getClass().getMethod("applyTransaction",txClass).invoke(organizer,tx);
        if(bounds!=null)invoke("resizeTask",new Class[]{int.class,Rect.class,int.class},task,bounds,0);
        invoke("setFocusedTask",new Class[]{int.class},task);
    }
    private String process(String... args) throws Exception {
        Process p=new ProcessBuilder(args).redirectErrorStream(true).start();
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[2048];int n;
        while((n=p.getInputStream().read(buf))>0)out.write(buf,0,n);
        int result=p.waitFor();String text=out.toString("UTF-8").trim();
        if(result!=0||text.startsWith("Error:"))throw new IllegalStateException(text);
        return text;
    }
    private Bundle command(String action,Bundle b) throws Exception {
        if(b==null)b=new Bundle();Bundle result=new Bundle();
        if(action.equals("enable_pointer_accessibility")){
            String component="dev.reachpad/dev.reachpad.PointerAccessibility";
            String enabled=process("/system/bin/settings","get","secure","enabled_accessibility_services");
            java.util.LinkedHashSet<String> services=new java.util.LinkedHashSet<>();
            if(!enabled.isEmpty()&&!enabled.equals("null"))services.addAll(java.util.Arrays.asList(enabled.split(":")));
            services.add(component);
            process("/system/bin/settings","put","secure","enabled_accessibility_services",android.text.TextUtils.join(":",services));
            process("/system/bin/settings","put","secure","accessibility_enabled","1");return result;
        }else if(action.equals("native")){
            if(!b.getBoolean("enabled")){nativeScreen.stop();restoreIme();return result;}
            IBinder client=b.getBinder("client");
            if(client==null)throw new IllegalArgumentException("Missing client lifetime");
            if(nativeClient!=client){nativeClient=client;client.linkToDeath(()->{if(nativeClient==client){nativeScreen.stop();try{restoreIme();}catch(Exception ignored){}}},0);}
            return nativeScreen.start(b.getFloat("left",.45f),b.getFloat("right",.55f));
        }else if(action.equals("native_attached")){
            nativeScreen.presented(b.getLong("generation"));return result;
        }else if(action.equals("native_key")){
            int key=b.getInt("key");
            if(key!=KeyEvent.KEYCODE_BACK&&key!=KeyEvent.KEYCODE_HOME&&key!=KeyEvent.KEYCODE_APP_SWITCH)throw new IllegalArgumentException("Unsupported navigation key");
            if(key==KeyEvent.KEYCODE_BACK&&nativeScreen.active()&&!b.getBoolean("ime")){
                int x=b.getBoolean("right")?(nativeScreen.leftEdge+nativeScreen.sourceWidth)/2:nativeScreen.leftEdge/2;
                int y=nativeScreen.height/2;
                if(b.getBoolean("whole")){float px=b.getFloat("x");x=Math.round(px<nativeScreen.leftEdge?px:px-(nativeScreen.rightEdge-nativeScreen.leftEdge));y=Math.round(b.getFloat("y"));}
                for(ActivityManager.RunningTaskInfo task:taskList())if(taskDisplay(task)==0&&normal(task)){
                    if(!task.getClass().getField("isVisible").getBoolean(task))continue;
                    Object configuration=task.getClass().getField("configuration").get(task);
                    Object wc=configuration.getClass().getField("windowConfiguration").get(configuration);
                    if(((Rect)wc.getClass().getMethod("getBounds").invoke(wc)).contains(x,y)){invoke("setFocusedTask",new Class[]{int.class},task.taskId);break;}
                }
            }
            long now=android.os.SystemClock.uptimeMillis();
            for(int a:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP}){KeyEvent e=new KeyEvent(now,now,a,key,0);setInputDisplay.invoke(e,0);injectInput.invoke(inputManager,e,0);}
        }else if(action.equals("capture")){
            pendingTask=-1;
            for(ActivityManager.RunningTaskInfo t:taskList()){
                if(taskDisplay(t)!=0)continue;
                if(t.topActivity!=null&&t.topActivity.getPackageName().equals("dev.reachpad"))continue;
                if(t.topActivity!=null&&t.topActivity.getPackageName().equals("com.sec.android.app.launcher"))break;
                if(normal(t)){pendingTask=t.taskId;break;}
            }
            result.putInt("task",pendingTask);
        }else if(action.equals("adopt")){
            leftWidth=b.getInt("left",width/2);
            if(pendingTask>=0&&id()>=0){
                int task=pendingTask;pendingTask=-1;
                position(task,id(),1,null);fullTask=task;leftTask=rightTask=-1;
                System.out.println("REACHPAD_ADOPT_TASK="+task);
            }
        }else if(action.equals("open")){
            if(id()<0)throw new IllegalStateException("화면 연결을 먼저 준비해 주세요");
            String component=b.getString("component");int side=b.getInt("side",0);
            ComponentName name=ComponentName.unflattenFromString(component);
            if(name==null)throw new IllegalArgumentException("앱을 찾을 수 없습니다");
            android.content.pm.ActivityInfo info=context.getPackageManager().getActivityInfo(name,0);
            if(!info.exported&&!name.getPackageName().equals("dev.reachpad")){
                Intent launcher=context.getPackageManager().getLaunchIntentForPackage(name.getPackageName());
                if(launcher==null||launcher.getComponent()==null)throw new SecurityException("No launcher entry");
                name=launcher.getComponent();component=name.flattenToString();info=context.getPackageManager().getActivityInfo(name,0);
                if(!info.exported)throw new SecurityException("Not a launchable app");
            }
            Intent check=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(name.getPackageName());
            if(context.getPackageManager().queryIntentActivities(check,0).isEmpty()&&!name.getPackageName().equals("dev.reachpad"))throw new SecurityException("No launcher entry");
            int task=-1;
            for(ActivityManager.RunningTaskInfo t:taskList())if(t.baseActivity!=null&&t.baseActivity.getPackageName().equals(name.getPackageName())){task=t.taskId;break;}
            if(task<0){
                process("/system/bin/am","start","-W","--display",Integer.toString(id()),"-n",component);
                for(ActivityManager.RunningTaskInfo t:taskList())if(t.baseActivity!=null&&t.baseActivity.getPackageName().equals(name.getPackageName())){task=t.taskId;break;}
            }
            if(task<0)throw new IllegalStateException("앱을 열지 못했습니다");
            place(task,side);result.putInt("task",task);
        }else if(action.equals("task")){
            int wanted=b.getInt("task",-1);boolean found=false;
            for(ActivityManager.RunningTaskInfo t:taskList())if(t.taskId==wanted&&normal(t)){found=true;break;}
            if(!found)throw new IllegalStateException("앱이 종료되었습니다");
            place(wanted,b.getInt("side",0));
        }else if(action.equals("layout")){
            leftWidth=b.getInt("left",width/2);
            if(leftTask>=0)position(leftTask,id(),5,new Rect(0,14,leftWidth,height));
            if(rightTask>=0)position(rightTask,id(),5,new Rect(leftWidth,14,width,height));
        }else if(action.equals("back")){
            int target=fullTask>=0?fullTask:(b.getBoolean("right")?rightTask:leftTask);
            int targetX=b.getBoolean("right")?(leftWidth+width)/2:leftWidth/2;
            for(ActivityManager.RunningTaskInfo task:taskList())if(taskDisplay(task)==id()&&normal(task)){
                try{if(!task.getClass().getField("isVisible").getBoolean(task))continue;}catch(NoSuchFieldException ignored){}
                Object configuration=task.getClass().getField("configuration").get(task);
                Object config=configuration.getClass().getField("windowConfiguration").get(configuration);
                Rect bounds=(Rect)config.getClass().getMethod("getBounds").invoke(config);
                if(bounds.contains(targetX,height/2)){target=task.taskId;break;}
            }
            if(target>=0&&!b.getBoolean("ime"))invoke("setFocusedTask",new Class[]{int.class},target);
            long now=android.os.SystemClock.uptimeMillis();
            for(int a:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP}){
                KeyEvent event=new KeyEvent(now,now,a,KeyEvent.KEYCODE_BACK,0);
                setInputDisplay.invoke(event,id());injectInput.invoke(inputManager,event,0);
            }
        }else if(action.equals("leave")){leave();
        }else if(action.equals("detach")){if(virtualDisplay!=null)virtualDisplay.setSurface(null);
        }else if(action.equals("host")){
            for(ActivityManager.RunningTaskInfo t:taskList())if(t.topActivity!=null&&t.topActivity.getClassName().equals("dev.reachpad.MainActivity")){invoke("setFocusedTask",new Class[]{int.class},t.taskId);break;}
        }else if(action.equals("start")){
            process("/system/bin/am","start","--display","0","-n","dev.reachpad/.MainActivity");
        }else if(action.equals("ime")){
            boolean enabled=b.getBoolean("enabled");
            if(enabled){
                String current=process("/system/bin/settings","get","secure","default_input_method");
                if(!current.equals(OWN_IME)&&!current.equals("dev.reachpad/dev.reachpad.ReachKeyboard"))previousIme=current;
                else if(previousIme.isEmpty())previousIme=b.getString("previous","");
                process("/system/bin/ime","enable",OWN_IME);process("/system/bin/ime","set",OWN_IME);
            }else restoreIme();
            result.putString("previous",previousIme);
        }else if(action.equals("status")){
            ArrayList<Bundle> list=new ArrayList<>();
            for(ActivityManager.RunningTaskInfo t:taskList())if(normal(t)){
                Bundle item=new Bundle();item.putInt("task",t.taskId);item.putInt("display",taskDisplay(t));
                Intent launcher=context.getPackageManager().getLaunchIntentForPackage(t.baseActivity.getPackageName());
                item.putString("component",launcher!=null&&launcher.getComponent()!=null?launcher.getComponent().flattenToString():t.baseActivity.flattenToString());item.putString("package",t.baseActivity.getPackageName());list.add(item);
            }
            result.putParcelableArrayList("tasks",list);result.putInt("display",id());
            result.putInt("full",fullTask);result.putInt("left",leftTask);result.putInt("right",rightTask);
        }else throw new IllegalArgumentException("Unknown command");
        return result;
    }
    private void place(int task,int side) throws Exception {
        if(side==0){position(task,id(),1,null);fullTask=task;leftTask=rightTask=-1;}
        else{
            if(fullTask>=0&&fullTask!=task){int other=fullTask;fullTask=-1;
                if(side==1){rightTask=other;position(other,id(),5,new Rect(leftWidth,14,width,height));}
                else{leftTask=other;position(other,id(),5,new Rect(0,14,leftWidth,height));}}
            fullTask=-1;
            if(side==1){if(rightTask==task)rightTask=-1;leftTask=task;position(task,id(),5,new Rect(0,14,leftWidth,height));}
            else{if(leftTask==task)leftTask=-1;rightTask=task;position(task,id(),5,new Rect(leftWidth,14,width,height));}
        }
    }
    private void restoreIme() throws Exception {
        if(!previousIme.isEmpty()&&!previousIme.equals("null")){
            String current=process("/system/bin/settings","get","secure","default_input_method");
            if(current.equals(OWN_IME)||current.equals("dev.reachpad/dev.reachpad.ReachKeyboard"))process("/system/bin/ime","set",previousIme);
        }
    }
    private void leave() throws Exception {
        restoreIme();int display=id();if(display<0)return;
        List<Integer> moving=new ArrayList<>();
        for(ActivityManager.RunningTaskInfo t:taskList())if(taskDisplay(t)==display&&normal(t))moving.add(t.taskId);
        // getTasks is most-recent-first; restore that task LAST to preserve the active app.
        Collections.reverse(moving);
        for(int task:moving)position(task,0,1,null);
        pendingTask=fullTask=leftTask=rightTask=-1;
        virtualDisplay.setSurface(null);
        System.out.println("REACHPAD_RETURN_TASKS="+moving);
    }

    private void setImePolicy(int id) {
        try {
            IBinder binder = (IBinder) Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "window");
            Object wm = Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, binder);
            wm.getClass().getMethod("setDisplayImePolicy", int.class, int.class).invoke(wm, id, 0);
        } catch (Exception e) { System.err.println("IME policy: " + e); }
    }

    public static void main(String[] args) throws Exception {
        Looper.prepareMainLooper();
        Context shell = createShellContext();
        ShellBridge helper = new ShellBridge(shell);
        Object am = Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);
        IBinder token = new Binder();
        Object holder = am.getClass().getMethod("getContentProviderExternal",String.class,int.class,IBinder.class,String.class)
            .invoke(am,"dev.reachpad.bridge",0,token,"ReachPad");
        if(holder==null)throw new IllegalStateException("Provider not found");
        Field providerField=holder.getClass().getDeclaredField("provider");providerField.setAccessible(true);
        Object provider=providerField.get(holder);
        Bundle extras=new Bundle();extras.putBinder("helper",helper);
        Bundle result=(Bundle)provider.getClass().getMethod("call",AttributionSource.class,String.class,String.class,String.class,Bundle.class)
            .invoke(provider,shell.getAttributionSource(),"dev.reachpad.bridge","register",null,extras);
        result.getBinder("lifetime").linkToDeath(()->{
            if(helper.virtualDisplay!=null)helper.virtualDisplay.release();System.exit(0);
        },0);
        System.out.println("REACHPAD_BRIDGE_CONNECTED");
        Looper.loop();
    }

    // ActivityThread initialization follows scrcpy's documented app_process workarounds.
    // See THIRD_PARTY_NOTICES.md for provenance and Apache-2.0 attribution.
    private static Context createShellContext() throws Exception {
        Class<?> type = Class.forName("android.app.ActivityThread");
        Constructor<?> ctor = type.getDeclaredConstructor(); ctor.setAccessible(true);
        Object thread = ctor.newInstance();
        set(type, null, "sCurrentActivityThread", thread);
        set(type, thread, "mSystemThread", true);
        Class<?> config = Class.forName("android.app.ConfigurationController");
        Constructor<?> configCtor = config.getDeclaredConstructor(Class.forName("android.app.ActivityThreadInternal"));
        configCtor.setAccessible(true);
        set(type, thread, "mConfigurationController", configCtor.newInstance(thread));
        Class<?> boundType = Class.forName("android.app.ActivityThread$AppBindData");
        Constructor<?> boundCtor = boundType.getDeclaredConstructor(); boundCtor.setAccessible(true);
        Object bound = boundCtor.newInstance();
        ApplicationInfo info = new ApplicationInfo(); info.packageName = "com.android.shell";
        set(boundType, bound, "appInfo", info); set(type, thread, "mBoundApplication", bound);
        Context system = (Context) type.getMethod("getSystemContext").invoke(thread);
        Context base = system.createPackageContext("com.android.shell", Context.CONTEXT_IGNORE_SECURITY);
        Context shell = new ContextWrapper(base) {
            @Override public String getPackageName() { return "com.android.shell"; }
            @Override public String getOpPackageName() { return "com.android.shell"; }
            @Override public Context getApplicationContext() { return this; }
            @Override public AttributionSource getAttributionSource() {
                return new AttributionSource.Builder(2000).setPackageName("com.android.shell").build();
            }
        };
        set(type, thread, "mInitialApplication", Instrumentation.newApplication(Application.class, shell));
        return shell;
    }
    private static void set(Class<?> type, Object object, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(object, value);
    }
}
