package dev.foldpatch;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Assistive overlays on the existing Galaxy display; never opens or reparents app tasks. */
public final class NativeService extends Service implements DisplayManager.DisplayListener {
    static NativeService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final IBinder lifetime=new Binder();
    private WindowManager windows;private DisplayManager displays;private SharedPreferences prefs;
    private LinearLayout bar;private ScrollView barWindow;private Pad pad;private WindowManager.LayoutParams barParams;
    private boolean cursorPending;
    private int barLevel=FloatingControls.PINNED;
    private boolean active,busy,stopping,requested,wasInner,imeSet,refreshAgain;
    private String notificationState="";
    private long busySince,frameGeneration;
    private int width,height,cut,leftEdge,rightEdge,desiredBarY;
    private float cursorX,cursorY,presentationScale=1f,pointerSpeed=1f;
    private int target; // Physical target: 0 left, 1 right, 2 whole.
    private boolean touchRight;
    private int inputStart(){return TouchSide.start(touchRight,rightEdge);}
    private int inputEnd(){return TouchSide.end(touchRight,leftEdge,width);}
    private int inputWidth(){return inputEnd()-inputStart();}
    private boolean usesPad(){return target!=(touchRight?1:0);}
    private int clampBarX(int x){return TouchSide.barX(x,barParams.width,touchRight,leftEdge,rightEdge,width);}
    private String barKey(String axis){return "native_bar_"+(touchRight?"right_":"")+axis;}
    private boolean pointerInitialized,accessibilityRequested;
    private int error;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(){
        super.onCreate();instance=this;prefs=getSharedPreferences("regions",0);Bridge.init(this);
        // A new service cannot own an old activity's unconfirmed preview.
        RangePreview.rollback(prefs);TouchSide.rollback(prefs);
        windows=getSystemService(WindowManager.class);displays=getSystemService(DisplayManager.class);
        loadControls();wasInner=ReachService.inner(this);
        Set<String> pinned=new HashSet<>(prefs.getStringSet("native_pinned",new HashSet<>(Arrays.asList("mode","back"))));
        if(pinned.remove("method"))prefs.edit().putStringSet("native_pinned",pinned).apply();
        NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("native",getString(R.string.notification_channel),NotificationManager.IMPORTANCE_LOW));
        PendingIntent settings=PendingIntent.getActivity(this,30,new Intent(this,NativeActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,31,new Intent(this,NativeService.class).setAction("pause"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(30,new Notification.Builder(this,"native").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle(getString(R.string.app_name)).setContentText(getString(R.string.notification_initial)).setContentIntent(settings).addAction(new Notification.Action.Builder(null,getString(R.string.pause),stop).build()).setOngoing(true).build());
        displays.registerDisplayListener(this,handler);
        IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);
        handler.post(tick);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?"":intent.getAction();
        if("pause".equals(action)){requested=false;prefs.edit().putBoolean("native_enabled",false).apply();deactivate();}
        else if("start".equals(action)){requested=true;prefs.edit().putBoolean("native_enabled",true).apply();}
        else if("watch".equals(action))requested=prefs.getBoolean("native_enabled",false);
        else requested=prefs.getBoolean("native_enabled",false)||prefs.getBoolean("auto_open",false);
        handler.removeCallbacks(tick);handler.post(tick);updateNotification();return START_STICKY;
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration config){
        super.onConfigurationChanged(config);
        // Services survive locale changes: update their UI without restarting assistance.
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("native",getString(R.string.notification_channel),NotificationManager.IMPORTANCE_LOW));
        notificationState="";updateNotification();rebuildBar();
    }
    @Override public IBinder onBind(Intent intent){return null;}
    private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){ConnectionHistory.record(Intent.ACTION_SCREEN_OFF.equals(i.getAction())?"screen_off":Intent.ACTION_SCREEN_ON.equals(i.getAction())?"screen_on":"unlocked");handler.removeCallbacks(tick);handler.post(tick);}};
    private final Runnable tick=new Runnable(){public void run(){refresh();handler.postDelayed(this,2000);}};
    private void refresh(){
        if(stopping)return;
        RangePreview.expire(prefs);if(TouchSide.expire(prefs)){removeOverlays();loadControls();ReachKeyboard.settingsChanged();}
        boolean inner=ReachService.inner(this);
        if(!inner&&wasInner){ConnectionHistory.record("folded");requested=false;prefs.edit().putBoolean("native_enabled",false).apply();}
        if(inner&&!wasInner&&prefs.getBoolean("auto_open",false)){ConnectionHistory.record("unfolded");requested=true;prefs.edit().putBoolean("native_enabled",true).apply();}
        wasInner=inner;
        if(NativeRangeActivity.visible&&NativeRangeActivity.measuring){deactivate();return;}
        boolean use=requested&&inner&&getSystemService(PowerManager.class).isInteractive()&&!getSystemService(KeyguardManager.class).isKeyguardLocked();
        Display d=displays.getDisplay(0);if(d==null||d.getRotation()!=Surface.ROTATION_0)use=false;
        if(!use){deactivate();return;}
        if(getSystemService(android.view.accessibility.AccessibilityManager.class).isTouchExplorationEnabled()){deactivate();error=R.string.error_reader;updateNotification();return;}
        if(!Settings.canDrawOverlays(this)){deactivate();error=R.string.error_overlay;updateNotification();return;}
        Bridge.tryBind();
        if(BridgeProvider.helper==null){removeOverlays();if(PointerAccessibility.instance!=null)PointerAccessibility.instance.detachPresentation();active=false;imeSet=false;error=R.string.error_shizuku;updateNotification();return;}
        if(Build.VERSION.SDK_INT<35){deactivate();error=R.string.error_android;updateNotification();return;}
        if(PointerAccessibility.instance==null){
            deactivate();error=R.string.preparing_controls;
            if(!accessibilityRequested){accessibilityRequested=true;Bridge.async("enable_pointer_accessibility",new Bundle(),r->{accessibilityRequested=false;},e->{accessibilityRequested=false;error=R.string.error_accessibility;updateNotification();});}
            return;
        }
        WindowManager assistWindows=PointerAccessibility.instance.getSystemService(WindowManager.class);
        if(windows!=assistWindows){removeOverlays();windows=assistWindows;}
        if(busy){refreshAgain=true;if(SystemClock.uptimeMillis()-busySince>10000)Bridge.restartStalledHelper();return;}busy=true;busySince=SystemClock.uptimeMillis();refreshAgain=false;
        Bundle b=new Bundle();b.putBoolean("enabled",true);b.putFloat("left",prefs.getFloat("left",.45f));b.putFloat("right",prefs.getFloat("right",.55f));b.putBinder("client",lifetime);
        final IBinder owner=BridgeProvider.helper;
        Bridge.worker.execute(()->{try{Bundle result=Bridge.command("native",b,owner);handler.post(()->{
            busy=false;
            if(owner!=BridgeProvider.helper){SurfaceControl stale=result.getParcelable("surface",SurfaceControl.class);if(stale!=null)stale.release();handler.removeCallbacks(tick);handler.post(tick);return;}
            if(stopping){Bundle off=new Bundle();off.putBoolean("enabled",false);Bridge.async("native",off);return;}
            Display current=displays.getDisplay(0);
            if(!requested||(NativeRangeActivity.visible&&NativeRangeActivity.measuring)||!ReachService.inner(this)||current==null||current.getRotation()!=Surface.ROTATION_0||!getSystemService(PowerManager.class).isInteractive()||getSystemService(KeyguardManager.class).isKeyguardLocked()){
                active=true;deactivate();return;
            }
            SurfaceControl surface=result.getParcelable("surface",SurfaceControl.class);
            PointerAccessibility access=PointerAccessibility.instance;
            if(access==null){if(surface!=null)surface.release();active=true;deactivate();return;}
            if(!access.attachPresentation(surface,result.getLong("generation"))){active=true;deactivate();error=R.string.error_screen;return;}
            Bundle attached=new Bundle();attached.putLong("generation",result.getLong("generation"));Bridge.async("native_attached",attached);
            int oldCut=cut;
            width=result.getInt("width");height=result.getInt("height");cut=result.getInt("cut");leftEdge=result.getInt("left");rightEdge=result.getInt("right");
            presentationScale=result.getFloat("scale",1f);long generation=result.getLong("generation");if(!active||frameGeneration!=generation){ConnectionHistory.record("screen_ready");imeSet=false;}frameGeneration=generation;active=true;error=0;prefs.edit().putInt("keyboard_width",NativeRangeActivity.visible?Math.min(inputWidth(),NativeRangeActivity.controlWidth):inputWidth()).putInt("keyboard_source_width",leftEdge+width-rightEdge).apply();
            if(!pointerInitialized){cursorX=touchRight?leftEdge/2f:(rightEdge+width)/2f;cursorY=height/2f;pointerInitialized=true;}
            cursorX=PointerGeometry.moveTarget(cursorX,0,leftEdge,rightEdge,width,target);cursorY=Math.max(0,Math.min(height*presentationScale-1,cursorY));
            if(oldCut!=cut)removeOverlays();ensureOverlays();updateNotification();
            if(refreshAgain){handler.removeCallbacks(tick);handler.post(tick);}
            if(!imeSet){imeSet=true;Bundle ime=new Bundle();ime.putBoolean("enabled",true);ime.putString("previous",prefs.getString("previous_ime",""));Bridge.async("ime",ime,r->{String prev=r.getString("previous","");if(!prev.isEmpty())prefs.edit().putString("previous_ime",prev).apply();},e->{imeSet=false;error=R.string.error_keyboard;updateNotification();});}
        });}catch(Exception e){android.util.Log.e("FoldPatchNative","Screen setup",e);handler.post(()->{busy=false;if(owner!=BridgeProvider.helper){handler.removeCallbacks(tick);handler.post(tick);return;}active=true;deactivate();error=R.string.error_connection;updateNotification();if(refreshAgain){handler.removeCallbacks(tick);handler.post(tick);}});}});
    }
    private void deactivate(){
        removeOverlays();if(PointerAccessibility.instance!=null)PointerAccessibility.instance.detachPresentation();if(!active&&!imeSet){updateNotification();return;}if(active)ConnectionHistory.record("screen_stopped");active=false;imeSet=false;updateNotification();
        Bundle b=new Bundle();b.putBoolean("enabled",false);Bridge.async("native",b);
    }
    static boolean requested(Context c){return instance!=null?instance.requested:c.getSharedPreferences("regions",0).getBoolean("native_enabled",false);}
    static boolean isActive(){return instance!=null&&instance.active&&BridgeProvider.helper!=null;}
    static boolean matches(float l,float r){NativeService s=instance;return s!=null&&s.active&&BridgeProvider.helper!=null&&s.leftEdge==Math.round(s.width*l)&&s.rightEdge==Math.round(s.width*r);}
    static String statusLabel(Context c){
        if(!requested(c))return c.getString(R.string.state_off);
        if(NativeRangeActivity.visible&&NativeRangeActivity.measuring)return c.getString(R.string.state_adjusting);
        if(!ReachService.inner(c))return c.getString(R.string.state_folded);
        if(c.getSystemService(DisplayManager.class).getDisplay(0).getRotation()!=Surface.ROTATION_0)return c.getString(R.string.state_portrait);
        if(BridgeProvider.helper==null){if(!rikka.shizuku.Shizuku.pingBinder())return c.getString(R.string.connection_stopped);try{if(rikka.shizuku.Shizuku.checkSelfPermission()!=android.content.pm.PackageManager.PERMISSION_GRANTED)return c.getString(R.string.connection_permission);}catch(Exception ignored){}return c.getString(R.string.connection_retrying);}
        NativeService s=instance;
        if(s!=null&&s.error!=0)return c.getString(s.error);
        return isActive()?c.getString(R.string.state_active):c.getString(R.string.state_starting);
    }
    private void updateNotification(){
        String label=statusLabel(this);if(label.equals(notificationState))return;notificationState=label;
        PendingIntent settings=PendingIntent.getActivity(this,30,new Intent(this,NativeActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent toggle=PendingIntent.getService(this,requested?31:32,new Intent(this,NativeService.class).setAction(requested?"pause":"start"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        getSystemService(NotificationManager.class).notify(30,new Notification.Builder(this,"native").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle(getString(R.string.app_name)+" · "+label).setContentText(getString(R.string.notification_settings)).setContentIntent(settings).addAction(new Notification.Action.Builder(null,requested?getString(R.string.pause):getString(R.string.resume),toggle).build()).setOngoing(true).build());
    }
    private WindowManager.LayoutParams params(int w,int h,int flags){
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|flags,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.setFitInsetsTypes(0);return p;
    }
    private void ensureOverlays(){
        if(NativeActivity.visible||NativeRangeActivity.visible){removeOverlays();return;}
        if(!Settings.canDrawOverlays(this)){error=R.string.error_overlay;deactivate();return;}
        if(pad==null&&usesPad()){if(bar!=null){windows.removeViewImmediate(barWindow);bar=null;barWindow=null;}pad=new Pad();
        }else if(pad!=null&&!usesPad()){pad.cancel();pad=null;}
        if(target==2&&PointerAccessibility.instance==null&&!accessibilityRequested){accessibilityRequested=true;Bridge.async("enable_pointer_accessibility",new Bundle(),r->{accessibilityRequested=false;},e->{accessibilityRequested=false;error=R.string.error_accessibility;updateNotification();});}
        PointerAccessibility.refresh();
        updateCursor();
        if(bar==null){bar=new LinearLayout(this);bar.setOrientation(1);bar.setPadding(dp(4),dp(4),dp(4),dp(4));bar.setBackground(new ReachUi(this).shape(0xfa243033,12,true));
            barParams=params(Math.min(inputWidth(),dp(248)),-2,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            barParams.x=prefs.getInt(barKey("x"),touchRight?width-barParams.width-dp(6):dp(6));desiredBarY=Math.max(dp(30),prefs.getInt(barKey("y"),dp(105)));barParams.y=desiredBarY;
            barWindow=new ScrollView(this);barWindow.setFillViewport(false);barWindow.setClipToPadding(false);barWindow.addView(bar);windows.addView(barWindow,barWindowParams());rebuildBar();}
        keyboardLayout();
    }
    private void rebuildBar(){
        if(bar==null)return;
        View.OnTouchListener drag=new View.OnTouchListener(){float x,y;int bx,by;boolean moving;public boolean onTouch(View v,MotionEvent e){
            if(e.getActionMasked()==0){v.getParent().requestDisallowInterceptTouchEvent(true);x=e.getRawX();y=e.getRawY();bx=barParams.x;by=barParams.y;moving=false;}
            else if(e.getActionMasked()==2){if(Math.abs(e.getRawX()-x)+Math.abs(e.getRawY()-y)>dp(9))moving=true;if(moving){barParams.x=clampBarX(bx+Math.round(e.getRawX()-x));desiredBarY=by+Math.round(e.getRawY()-y);clampBar();return true;}}
            else if(e.getActionMasked()==1||e.getActionMasked()==3){v.getParent().requestDisallowInterceptTouchEvent(false);if(moving){desiredBarY=barParams.y;prefs.edit().putInt(barKey("x"),barParams.x).putInt(barKey("y"),barParams.y).apply();return true;}}return false;}};
        FloatingControls controls=new FloatingControls(this,prefs,target,barLevel,Math.min(inputWidth(),dp(268)),this::barAction,drag);
        bar.setPadding(0,0,0,0);bar.setBackgroundColor(Color.TRANSPARENT);bar.removeAllViews();bar.addView(controls.view);
        barParams.width=controls.width;barParams.x=clampBarX(prefs.getInt(barKey("x"),touchRight?width-controls.width-dp(6):dp(6)));barWindow.scrollTo(0,0);bar.post(()->{if(barWindow==null)return;barWindow.setSystemGestureExclusionRects(Collections.singletonList(new Rect(0,0,barParams.width,dp(Math.round(56*FloatingControls.sizePercent(prefs)/100f)))));clampBar();});
    }
    private void barAction(String id){
        if(id.equals("more")){barLevel=FloatingControls.nextLevel(barLevel);prefs.edit().putInt("native_bar_level",barLevel).apply();rebuildBar();return;}
        if(id.equals("left")||id.equals("right")||id.equals("whole")){setMode(id.equals("left")?0:id.equals("right")?1:2);return;}
        if(id.equals("settings")){startActivity(new Intent(this,NativeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return;}
        Bundle a=new Bundle();a.putInt("key",id.equals("back")?KeyEvent.KEYCODE_BACK:id.equals("home")?KeyEvent.KEYCODE_HOME:KeyEvent.KEYCODE_APP_SWITCH);a.putBoolean("right",target==1||(target==2&&cursorX>=rightEdge));a.putBoolean("whole",target==2);a.putFloat("x",cursorX);a.putFloat("y",cursorY);a.putBoolean("ime",ReachKeyboard.shown);Bridge.async("native_key",a);
    }
    private void loadControls(){barLevel=Math.max(FloatingControls.COLLAPSED,Math.min(FloatingControls.FULL,prefs.getInt("native_bar_level",FloatingControls.PINNED)));touchRight=TouchSide.right(prefs);target=Math.max(0,Math.min(2,prefs.getInt("native_target",prefs.getBoolean("trackpad",false)?(touchRight?0:1):(touchRight?1:0))));pointerSpeed=PointerGeometry.speed(prefs.getFloat("pointer_speed",1f));}
    private void setMode(int next){if(target==next)return;if(next==2&&Build.VERSION.SDK_INT<35){Toast.makeText(this,getString(R.string.whole_requires_android),Toast.LENGTH_LONG).show();return;}removeOverlays();target=next;if(next==2)accessibilityRequested=false;prefs.edit().putInt("native_target",next).putBoolean("trackpad",next!=(touchRight?1:0)).apply();cursorX=PointerGeometry.moveTarget(cursorX,0,leftEdge,rightEdge,width,next);ensureOverlays();rebuildBar();}
    private WindowManager.LayoutParams barWindowParams(){WindowManager.LayoutParams p=new WindowManager.LayoutParams();p.copyFrom(barParams);if(touchRight)p.x-=rightEdge-leftEdge;return p;}
    private void clampBar(){if(bar==null)return;int bottom=ReachKeyboard.shown?ReachKeyboard.top:height;barParams.y=Math.max(dp(30),Math.min(Math.max(dp(30),bottom-(barWindow==null?bar.getHeight():barWindow.getHeight())-dp(12)),desiredBarY));int available=Math.max(dp(48),bottom-dp(42));barParams.height=bar.getHeight()>available?available:WindowManager.LayoutParams.WRAP_CONTENT;try{windows.updateViewLayout(barWindow,barWindowParams());}catch(Exception ignored){}}
    private Rect padArea(){
        // A gesture that starts on a system edge belongs to Android for its entire stream.
        Insets gestures=Insets.NONE,status=Insets.NONE;
        try{WindowInsets insets=windows.getMaximumWindowMetrics().getWindowInsets();gestures=insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemGestures());status=insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars());}catch(Exception ignored){}
        int left=inputStart(),right=inputEnd();
        if(touchRight)right=Math.max(left+1,right-Math.max(gestures.right,dp(24)));
        else left=Math.min(Math.max(gestures.left,dp(24)),Math.max(0,right-1));
        int top=Math.max(status.top,dp(32)),bottom=height-Math.max(gestures.bottom,dp(32));
        if(ReachKeyboard.shown)bottom=Math.min(bottom,ReachKeyboard.top);
        return new Rect(left,top,right,Math.max(top+1,bottom));
    }
    private void keyboardLayout(){clampBar();}
    static void keyboardChanged(){if(instance!=null)instance.handler.post(instance::keyboardLayout);}
    private void removeOverlays(){if(pad!=null)pad.cancel();for(View v:new View[]{barWindow})if(v!=null)try{windows.removeViewImmediate(v);}catch(Exception ignored){}pad=null;bar=null;barWindow=null;PointerAccessibility.refresh();updateCursor();}
    private void updateCursor(){if(cursorPending)return;cursorPending=true;Choreographer.getInstance().postFrameCallback(time->{cursorPending=false;Parcel p=Bridge.data();try{p.writeInt(active&&usesPad()&&pad!=null?1:0);p.writeFloat(cursorX);p.writeFloat(cursorY);IBinder b=BridgeProvider.helper;if(b!=null)b.transact(9,p,null,IBinder.FLAG_ONEWAY);}catch(Exception ignored){}finally{p.recycle();}});}
    private void send(MotionEvent e){Parcel p=Bridge.data();try{e.writeToParcel(p,0);IBinder b=BridgeProvider.helper;if(b!=null)b.transact(8,p,null,IBinder.FLAG_ONEWAY);}catch(Exception failure){android.util.Log.e("FoldPatchNative","Touch",failure);}finally{p.recycle();}}
    final class Pad extends View {
        TrackpadGesture gesture;
        final Runnable hold=()->{if(gesture!=null)gesture.hold(SystemClock.uptimeMillis());};
        Pad(){super(NativeService.this);setBackgroundColor(Color.TRANSPARENT);}
        void cancel(){handler.removeCallbacks(hold);if(gesture!=null)gesture.cancel(SystemClock.uptimeMillis());gesture=null;}
        private TrackpadGesture create(){
            int usable=leftEdge+width-rightEdge;
            return new TrackpadGesture(new TrackpadGesture.Sink(){
                public void pointer(float x,float y){cursorX=x>=leftEdge?x+rightEdge-leftEdge:x;cursorY=y;updateCursor();}
                public void hold(){if(bar!=null)bar.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);}
                public void touch(int action,int count,float x,float y,float x2,float y2,long down,long time){
                    MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];
                    for(int i=0;i<count;i++){props[i]=new MotionEvent.PointerProperties();props[i].id=i;props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=i==0?x:x2;coords[i].y=i==0?y:y2;coords[i].pressure=action==1?0:1;coords[i].size=1;}
                    MotionEvent event=MotionEvent.obtain(down,time,action,count,props,coords,0,0,1,1,0,1<<20,InputDevice.SOURCE_TOUCHSCREEN,0);send(event);event.recycle();
                }
            },usable,height,target==1?leftEdge:0,target==0?leftEdge-1:usable-1,pointerSpeed,dp(8),cursorX>=rightEdge?cursorX-(rightEdge-leftEdge):cursorX,cursorY);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if((e.getEdgeFlags()&(1<<20))!=0)return true;
            if(gesture==null)gesture=create();int a=e.getActionMasked();
            if(a==0){requestUnbufferedDispatch(e);gesture.x=cursorX>=rightEdge?cursorX-(rightEdge-leftEdge):cursorX;gesture.y=cursorY;handler.postDelayed(hold,350);}
            if(a==1||a==3||a==5)handler.removeCallbacks(hold);
            gesture.event(a,e.getPointerCount(),e.getX(0),e.getY(0),e.getPointerCount()>1?e.getX(1):0,e.getPointerCount()>1?e.getY(1):0,e.getEventTime());
            updateCursor();return true;
        }
    }
    public void onDisplayAdded(int id){}public void onDisplayRemoved(int id){}public void onDisplayChanged(int id){if(id==0){handler.removeCallbacks(tick);handler.post(tick);}}
    static boolean captureTouch(){NativeService s=instance;return s!=null&&s.active&&s.requested&&BridgeProvider.helper!=null&&ReachService.inner(s)&&s.getSystemService(PowerManager.class).isInteractive()&&!s.getSystemService(KeyguardManager.class).isKeyguardLocked()&&!s.getSystemService(android.view.accessibility.AccessibilityManager.class).isTouchExplorationEnabled();}
    boolean startsOnPad(float x,float y){return padArea().contains((int)x,(int)y)&&!(bar!=null&&x>=barParams.x&&x<barParams.x+bar.getWidth()&&y>=barParams.y&&y<barParams.y+(barWindow==null?bar.getHeight():barWindow.getHeight()));}
    void physicalPadEvent(MotionEvent event){if(pad!=null)pad.onTouchEvent(event);}
    private float physicalOffset;private boolean physicalBlocked;
    boolean padInput(float x,float y){return usesPad()&&pad!=null&&!NativeActivity.visible&&!NativeRangeActivity.visible&&startsOnPad(x,y);}
    void forwardPhysical(MotionEvent event){
        if(event.getActionMasked()==0){boolean toolbar=bar!=null&&event.getX()>=barParams.x&&event.getX()<barParams.x+bar.getWidth()&&event.getY()>=barParams.y&&event.getY()<barParams.y+barWindow.getHeight();physicalOffset=event.getX()>=rightEdge?rightEdge-leftEdge:0;physicalBlocked=!toolbar&&event.getX()>=leftEdge&&event.getX()<rightEdge;}
        if(physicalBlocked)return;int count=event.getPointerCount();MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];
        for(int i=0;i<count;i++){props[i]=new MotionEvent.PointerProperties();event.getPointerProperties(i,props[i]);coords[i]=new MotionEvent.PointerCoords();event.getPointerCoords(i,coords[i]);coords[i].x-=physicalOffset;}
        MotionEvent copy=MotionEvent.obtain(event.getDownTime(),event.getEventTime(),event.getAction(),count,props,coords,event.getMetaState(),event.getButtonState(),1,1,event.getDeviceId(),event.getEdgeFlags()|(1<<20),event.getSource(),event.getFlags());send(copy);copy.recycle();
    }
    static void pointerDisconnected(){NativeService s=instance;if(s!=null){s.deactivate();s.handler.removeCallbacks(s.tick);s.handler.post(s.tick);}}
    static void cancelPointerGesture(){if(instance!=null&&instance.pad!=null)instance.pad.cancel();}
    static void pointerSpeedChanged(){if(instance!=null){instance.pointerSpeed=PointerGeometry.speed(instance.prefs.getFloat("pointer_speed",1f));if(instance.pad!=null)instance.pad.cancel();}}
    static void settingsChanged(){if(instance!=null){instance.removeOverlays();instance.loadControls();instance.handler.removeCallbacks(instance.tick);instance.handler.post(instance.tick);}}
    @Override public void onDestroy(){stopping=true;deactivate();handler.removeCallbacksAndMessages(null);displays.unregisterDisplayListener(this);unregisterReceiver(receiver);instance=null;super.onDestroy();}
}
