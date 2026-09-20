package dev.reachpad;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;

public final class ReachService extends Service implements DisplayManager.DisplayListener {
    static ReachService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private DisplayManager displays;private boolean opened;private TextView rescue;private WindowManager windows;
    private SharedPreferences prefs;
    static boolean visible,paused;
    public static boolean inner(Context c){
        Display d=((DisplayManager)c.getSystemService(DISPLAY_SERVICE)).getDisplay(0);
        if(d==null)return false;Display.Mode mode=d.getMode();
        return Math.min(mode.getPhysicalWidth(),mode.getPhysicalHeight())>=1200;
    }
    @Override public void onCreate(){
        super.onCreate();instance=this;prefs=getSharedPreferences("regions",0);Bridge.init(this);
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("reach",getString(R.string.notification_channel),NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(20,new Notification.Builder(this,"reach").setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.app_name)).setContentText("눌러서 열기 · 왼쪽 조작 도구").setContentIntent(open).setOngoing(true).build());
        displays=getSystemService(DisplayManager.class);windows=getSystemService(WindowManager.class);
        opened=inner(this);displays.registerDisplayListener(this,handler);
        IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_USER_PRESENT);f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);
        handler.post(reconnect);
    }
    @Override public int onStartCommand(Intent i,int flags,int id){if(i!=null&&"open".equals(i.getAction()))open();return START_STICKY;}
    @Override public IBinder onBind(Intent i){return null;}
    private final Runnable reconnect=new Runnable(){public void run(){Bridge.tryBind();refresh();handler.postDelayed(this,5000);}};
    private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
        if(Intent.ACTION_USER_PRESENT.equals(i.getAction())&&inner(c)&&prefs.getBoolean("auto_open",false)&&!paused)open();
        refresh();
    }};
    public void onDisplayAdded(int id){}public void onDisplayRemoved(int id){}
    public void onDisplayChanged(int id){if(id!=0)return;handler.removeCallbacks(foldChanged);handler.postDelayed(foldChanged,250);}
    private final Runnable foldChanged=()->{
        boolean now=inner(this);if(now==opened)return;opened=now;
        if(!now){
            paused=false;
            Bridge.async("leave",null,b->{MainActivity a=MainActivity.current;if(a!=null)a.finishForFold();});
        }else if(prefs.getBoolean("auto_open",false)&&!getSystemService(KeyguardManager.class).isKeyguardLocked()){
            paused=false;open();
        }
        refresh();
    };
    void open(){
        paused=false;
        if(BridgeProvider.helper!=null)Bridge.async("capture",null,b->Bridge.async("start",null));
        else try{startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){android.util.Log.w("ReachPad","Open from notification instead",e);}
    }
    static void changed(){if(instance!=null)instance.handler.post(instance::refresh);}
    void refresh(){
        boolean show=inner(this)&&!visible&&!paused&&!getSystemService(KeyguardManager.class).isKeyguardLocked()
            &&getSystemService(PowerManager.class).isInteractive()&&Settings.canDrawOverlays(this);
        if(show&&rescue==null){
            rescue=new TextView(this);rescue.setText("FP\n열기");rescue.setTextSize(13);rescue.setTextColor(Color.WHITE);rescue.setGravity(Gravity.CENTER);rescue.setBackgroundColor(0xee234438);
            int size=Math.round(52*getResources().getDisplayMetrics().density);
            WindowManager.LayoutParams p=new WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
            p.gravity=Gravity.TOP|Gravity.LEFT;p.y=Math.round(100*getResources().getDisplayMetrics().density);
            rescue.setOnClickListener(v->open());try{windows.addView(rescue,p);}catch(Exception e){rescue=null;}
        }else if(!show&&rescue!=null){try{windows.removeView(rescue);}catch(Exception ignored){}rescue=null;}
    }
    @Override public void onDestroy(){instance=null;handler.removeCallbacksAndMessages(null);displays.unregisterDisplayListener(this);unregisterReceiver(receiver);
        if(rescue!=null)windows.removeView(rescue);super.onDestroy();}
}
