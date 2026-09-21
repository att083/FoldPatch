package dev.foldpatch;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Settings stay on the working touch side. Navigation never implicitly changes the assistive service. */
public final class NativeActivity extends Activity {
    static boolean visible;
    private SharedPreferences prefs;
    private ReachUi ui;
    private LinearLayout column,footer,panel,connection;
    private TextView state,connectionText,sideCountdown,shizukuStatus;
    private long connectionCheckDeadline;
    private int connectionFeedbackMessage,connectionFeedbackState;
    private long connectionFeedbackUntil;
    private Switch power;
    private ScrollView scroll;
    private int renderedSafeWidth,renderedDensity,renderedOrientation;
    private String page="home";
    private boolean syncing;
    private SetupFlow.Step setupStep;
    private Button setupAction;
    private TextView setupLive;
    private boolean setupRangeReady;
    private int setupGuide;

    private int previewLevel=FloatingControls.PINNED;
    private LinearLayout toolbarPreview,toolbarStage,toolbarPins;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable statusTick=new Runnable(){public void run(){if(TouchSide.expire(prefs)){sideChanged();}else if(page.equals("side")&&sideCountdown!=null&&!TouchSide.pending(prefs)){render();}updateSideCountdown();updateSetup();updateState();handler.postDelayed(this,500);}};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);ui=new ReachUi(this);prefs=getSharedPreferences("regions",0);Bridge.init(this);
        ScreenUi.prepare(this,this::handleBack,false);
        if(!NativeRangeActivity.visible)RangePreview.rollback(prefs);TouchSide.rollback(prefs);
        if(NativeService.instance==null&&(prefs.getBoolean("native_enabled",false)||prefs.getBoolean("auto_open",false)))startForegroundService(new Intent(this,NativeService.class).setAction("watch"));
        // Existing installations keep their completed setup and all personal settings.
        SetupFlow.initialize(prefs);
        if(saved!=null)page=saved.getString("page","home");
        else if(!prefs.getBoolean("onboard_complete",false))page="setup";
        // Older saved activities may still point at the removed keyboard settings.
        if(page.equals("keyboard"))page="home";
        getWindow().setStatusBarColor(ReachUi.BG);getWindow().setNavigationBarColor(ReachUi.BG);render();
    }
    @Override protected void onResume(){super.onResume();visible=true;render();NativeService.settingsChanged();handler.post(statusTick);}
    @Override protected void onPause(){visible=false;handler.removeCallbacks(statusTick);TouchSide.rollback(prefs);ReachKeyboard.settingsChanged();NativeService.settingsChanged();super.onPause();}
    @Override protected void onSaveInstanceState(Bundle b){b.putString("page",page);super.onSaveInstanceState(b);}
    @Override public void onConfigurationChanged(Configuration c){
        super.onConfigurationChanged(c);
        // IME/reflow can change the full viewport while the working column stays
        // the same width. Retain its editor and composition; rebuild when its
        // actual width, density or orientation changes.
        if(renderedSafeWidth==safeWidth()
                &&renderedDensity==c.densityDpi&&renderedOrientation==c.orientation)return;
        int y=scroll.getScrollY();render();scroll.post(()->scroll.scrollTo(0,y));
    }
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
    private void go(String next){if(!next.equals("side")&&TouchSide.rollback(prefs)){NativeService.settingsChanged();ReachKeyboard.settingsChanged();}page=next;render();}
    private int safeWidth(){int w=getResources().getDisplayMetrics().widthPixels;if(!ReachService.inner(this))return w;return Math.min(w,Math.round(getDisplay().getMode().getPhysicalWidth()*TouchSide.ratio(prefs)));}
    private void render(){
        renderedSafeWidth=safeWidth();
        renderedDensity=getResources().getConfiguration().densityDpi;renderedOrientation=getResources().getConfiguration().orientation;
        state=null;power=null;connection=null;sideCountdown=null;shizukuStatus=null;setupAction=null;setupLive=null;setupStep=null;
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(ReachUi.BG);
        panel=ui.column();root.addView(panel,new FrameLayout.LayoutParams(safeWidth(),-1,TouchSide.right(prefs)?Gravity.RIGHT:Gravity.LEFT));ScreenUi.fitControls(panel);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        column=ui.column();column.setPadding(ui.dp(20),ui.dp(24),ui.dp(20),ui.dp(8));scroll.addView(column);
        footer=ui.column();footer.setPadding(ui.dp(20),ui.dp(8),ui.dp(20),ui.dp(16));panel.addView(footer,ui.fill(-2));setContentView(root);
        if(page.equals("setup")&&!prefs.getBoolean("onboard_side",false)&&ReachService.inner(this)){
            // Both physical edges must be reachable before a working side is known.
            panel.setVisibility(View.GONE);
            for(boolean right:new boolean[]{false,true}){
                LinearLayout choice=ui.column();
                LinearLayout safeChoice=ui.column();safeChoice.setPadding(ui.dp(16),ui.dp(28),ui.dp(16),ui.dp(20));choice.addView(safeChoice,ui.fill(-1));ScreenUi.fitControls(choice);
                safeChoice.addView(ui.text(getString(R.string.wizard_side_title),23,ReachUi.TEXT,true));ui.gap(safeChoice,16);
                safeChoice.addView(ui.text(getString(R.string.wizard_side_body),14,ReachUi.MUTED,false));ui.gap(safeChoice,24);
                safeChoice.addView(ui.button(getString(right?R.string.wizard_use_right:R.string.wizard_use_left),true,()->chooseSetupSide(right)),ui.fill(56));
                int width=Math.round(getDisplay().getMode().getPhysicalWidth()*.40f);
                root.addView(choice,new FrameLayout.LayoutParams(Math.min(width,getResources().getDisplayMetrics().widthPixels/2),-1,right?Gravity.RIGHT:Gravity.LEFT));
            }
            setupStep=SetupFlow.Step.SIDE;return;
        }
        if(page.equals("home"))home();else if(page.equals("setup"))setup();else if(page.equals("side"))side();else if(page.equals("method"))method();else if(page.equals("toolbar"))toolbar();else if(page.equals("range"))rangeGate();else help();
        updateState();
    }
    private void home(){
        LinearLayout identity=ui.row();
        boolean roomForMark=safeWidth()>=ui.dp(240);
        ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.ic_launcher);mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mark.setBackground(ui.shape(ReachUi.ACCENT,12,false));mark.setClipToOutline(true);
        mark.setVisibility(roomForMark?View.VISIBLE:View.GONE);
        identity.addView(mark,new LinearLayout.LayoutParams(ui.dp(44),ui.dp(44)));
        TextView brand=ui.text(getString(R.string.brand_name),28,ReachUi.TEXT,true);brand.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));brand.setMaxLines(1);brand.setAutoSizeTextTypeUniformWithConfiguration(12,28,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams nameSize=new LinearLayout.LayoutParams(0,-2,1);nameSize.leftMargin=roomForMark?ui.dp(12):0;identity.addView(brand,nameSize);column.addView(identity,ui.fill(-2));
        ui.gap(column,12);column.addView(ui.text(getString(R.string.brand_tagline),13,ReachUi.MUTED,false));ui.gap(column,20);
        LinearLayout status=ui.column();status.setPadding(ui.dp(16),ui.dp(16),ui.dp(16),ui.dp(16));status.setBackground(ui.shape(ReachUi.SURFACE,20,false));
        LinearLayout statusRow=ui.row(),texts=ui.column();texts.addView(ui.text(getString(R.string.assist_title),20,ReachUi.TEXT,true));ui.gap(texts,6);
        state=ui.text("",14,ReachUi.ACCENT,true);state.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);texts.addView(state);
        statusRow.addView(texts,new LinearLayout.LayoutParams(0,-2,1));power=ui.toggle(false);power.setContentDescription(getString(R.string.assist_toggle));LinearLayout.LayoutParams toggleSize=new LinearLayout.LayoutParams(-2,-2);toggleSize.leftMargin=ui.dp(8);statusRow.addView(power,toggleSize);status.addView(statusRow,ui.fill(-2));
        power.setOnCheckedChangeListener((b,on)->{if(!syncing)setPower(on);});
        connection=ui.column();ui.gap(connection,16);connectionText=ui.text("",14,ReachUi.MUTED,false);connection.addView(connectionText);ui.gap(connection,10);
        connection.addView(ui.button(getString(R.string.setup),false,()->go("setup")),ui.fill(48));status.addView(connection);connection.setVisibility(View.GONE);column.addView(status,ui.fill(-2));
        section(getString(R.string.display_section));
        switchRow(getString(R.string.auto_start),getString(R.string.auto_start_description),prefs.getBoolean("auto_open",false),on->{if(on&&!prefs.getBoolean("setup_accepted",false)){go("setup");return;}prefs.edit().putBoolean("auto_open",on).apply();if(on)startForegroundService(new Intent(this,NativeService.class).setAction("watch"));});
        ui.line(column);nav(getString(R.string.touch_side),sideLabel(false),getString(R.string.side_summary,sideLabel(false)),()->go("side"));ui.line(column);nav(getString(R.string.range_title),null,regions(),this::openRange);ui.line(column);section(getString(R.string.controls_section));
        nav(getString(R.string.pointer_title),null,controlSummary(),()->go("method"));ui.line(column);
        nav(getString(R.string.toolbar_title),getString(prefs.getBoolean("native_vertical",false)?R.string.vertical:R.string.horizontal),(prefs.getBoolean("native_vertical",false)?getString(R.string.vertical):getString(R.string.horizontal))+" · "+pinnedSummary(),()->go("toolbar"));
        TextView help=ui.text(getString(R.string.help_title),13,ReachUi.MUTED,false);help.setGravity(Gravity.CENTER);help.setMinHeight(ui.dp(44));help.setBackground(ui.background(Color.TRANSPARENT,8,false));help.setOnClickListener(v->go("help"));footer.addView(help,ui.fill(-2));
        footer.addView(ui.button(getString(R.string.return_to_app),true,this::finish),ui.fill(54));
    }
    private void setPower(boolean on){
        if(on&&(!prefs.getBoolean("setup_accepted",false)||!Settings.canDrawOverlays(this)||connectionMessage()!=R.string.shizuku_connected)){go("setup");return;}
        startForegroundService(new Intent(this,NativeService.class).setAction(on?"start":"pause"));handler.postDelayed(this::updateState,80);
    }
    private void updateState(){
        int message=connectionMessage();
        if(shizukuStatus!=null){
            boolean feedback=SystemClock.uptimeMillis()<connectionFeedbackUntil&&message==connectionFeedbackState;
            String text=getString(feedback?connectionFeedbackMessage:message);
            if(!text.contentEquals(shizukuStatus.getText()))shizukuStatus.setText(text);
            shizukuStatus.setTextColor(feedback&&message==R.string.shizuku_connected?ReachUi.ACCENT:ReachUi.MUTED);
        }
        if(state==null)return;
        boolean requested=NativeService.requested(this);syncing=true;power.setChecked(requested);syncing=false;
        String label=NativeService.statusLabel(this);if(!state.getText().toString().equals(label))state.setText(label);
        boolean needs=message!=R.string.shizuku_connected||!prefs.getBoolean("onboard_complete",false);connection.setVisibility(needs?View.VISIBLE:View.GONE);
        if(needs)connectionText.setText(getString(message==R.string.shizuku_connected?R.string.wizard_resume:message));
    }
    private int connectionMessage(){
        if(!rikka.shizuku.Shizuku.pingBinder())return R.string.connection_stopped;
        try{if(rikka.shizuku.Shizuku.checkSelfPermission()!=android.content.pm.PackageManager.PERMISSION_GRANTED)return R.string.connection_permission;}
        catch(Exception e){return R.string.connection_stopped;}
        IBinder helper=BridgeProvider.helper;
        if(helper!=null&&helper.isBinderAlive()){connectionCheckDeadline=0;return R.string.shizuku_connected;}
        return connectionCheckDeadline>0&&SystemClock.uptimeMillis()>=connectionCheckDeadline?R.string.connection_check_failed:R.string.connection_retrying;
    }
    private void connectionFeedback(int message){
        connectionFeedbackMessage=message==R.string.shizuku_connected?R.string.connection_verified:message;
        connectionFeedbackState=connectionMessage();connectionFeedbackUntil=SystemClock.uptimeMillis()+4000;
        updateState();
    }
    private void addConnectionStatus(){
        shizukuStatus=ui.text(getString(connectionMessage()),14,ReachUi.MUTED,false);
        shizukuStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        column.addView(shizukuStatus,ui.fill(-2));ui.gap(column,12);
    }
    private void prepare(){
        if(!prefs.getBoolean("setup_accepted",false)){go("setup");return;}
        int message=connectionMessage();
        if(message==R.string.shizuku_connected){connectionFeedback(message);return;}
        if(message==R.string.connection_stopped){
            connectionCheckDeadline=0;
            Intent manager=getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
            connectionFeedback(manager!=null?R.string.connection_stopped:R.string.connection_install);
            if(manager!=null)startActivity(manager);else openWeb("https://shizuku.rikka.app/download/");
            return;
        }
        try{
            if(message==R.string.connection_permission){
                connectionCheckDeadline=0;connectionFeedback(R.string.connection_permission);
                if(rikka.shizuku.Shizuku.shouldShowRequestPermissionRationale()){
                    Intent manager=getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                    if(manager!=null)startActivity(manager);else openWeb("https://shizuku.rikka.app/download/");
                }else Bridge.request();
                return;
            }
            connectionCheckDeadline=SystemClock.uptimeMillis()+8000;
            IBinder helper=BridgeProvider.helper;if(helper!=null&&!helper.isBinderAlive())BridgeProvider.clear(helper);
            Bridge.tryBind();connectionFeedback(R.string.connection_retrying);
        }catch(Exception e){connectionCheckDeadline=SystemClock.uptimeMillis();connectionFeedback(rikka.shizuku.Shizuku.pingBinder()?R.string.connection_check_failed:R.string.connection_stopped);}
    }
    private void openWeb(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,getString(R.string.open_shizuku_browser),Toast.LENGTH_LONG).show();}}
    private SetupFlow.Step nextSetup(){
        int connection=connectionMessage();
        return SetupFlow.next(prefs.getBoolean("onboard_side",false),prefs.getBoolean("onboard_intro",false),
            getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api")!=null,
            rikka.shizuku.Shizuku.pingBinder(),connection!=R.string.connection_stopped&&connection!=R.string.connection_permission,
            connection==R.string.shizuku_connected,Settings.canDrawOverlays(this),prefs.getBoolean("onboard_access",false),
            prefs.getBoolean("onboard_keyboard",false),prefs.getBoolean("onboard_range",false),prefs.getBoolean("onboard_practice",false));
    }
    private void setupDone(String key){prefs.edit().putBoolean("onboard_"+key,true).commit();render();}
    private void chooseSetupSide(boolean right){TouchSide.apply(prefs,right);TouchSide.save(prefs);prefs.edit().putBoolean("onboard_side",true).commit();sideChanged();}
    private void setupBody(int text){setupBody(getString(text));}
    private void setupBody(String text){column.addView(ui.text(text,15,ReachUi.MUTED,false));ui.gap(column,20);}
    private void setupButton(int text,Runnable action){setupAction=ui.button(getString(text),true,action);footer.addView(setupAction,ui.fill(56));ui.gap(footer,8);}
    private void setupLink(int text,Runnable action){column.addView(ui.button(getString(text),false,action),ui.fill(52));ui.gap(column,12);}
    private void systemSettings(String action){
        try{startActivity(new Intent(action));}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_SETTINGS));}
    }
    private boolean requireDeveloperOptions(){
        try{if(Settings.Global.getInt(getContentResolver(),Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,0)!=0)return true;}
        catch(SecurityException e){return true;} // Some OEMs may hide the state; keep their manual path available.
        prefs.edit().putInt("onboard_guide",0).apply();render();
        if(setupLive!=null){setupLive.setText(getString(R.string.wizard_developer_required));setupLive.setVisibility(View.VISIBLE);}
        return false;
    }
    private void openShizuku(){
        Intent manager=getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
        if(manager!=null)startActivity(manager);else openWeb("https://shizuku.rikka.app/download/");
    }
    private void updateSetup(){
        if(!page.equals("setup")||setupStep==null)return;
        if(nextSetup()!=setupStep){render();return;}
        if(setupStep==SetupFlow.Step.RANGE&&setupLive!=null){
            boolean inner=ReachService.inner(this)&&getDisplay().getRotation()==Surface.ROTATION_0;
            setupRangeReady=inner&&NativeService.isActive();
            setupLive.setText(!inner?getString(R.string.wizard_unfold):NativeService.statusLabel(this));
            setupAction.setText(getString(setupRangeReady?R.string.range_title:R.string.start_assistance));ui.enabled(setupAction,inner);
        }
    }
    private void setup(){
        setupStep=nextSetup();
        int group=setupStep==SetupFlow.Step.SIDE?1:setupStep.ordinal()<=SetupFlow.Step.CONNECT.ordinal()?2:setupStep.ordinal()<=SetupFlow.Step.KEYBOARD.ordinal()?3:setupStep==SetupFlow.Step.RANGE?4:5;
        column.addView(ui.text(getString(R.string.wizard_progress,group),13,ReachUi.ACCENT,true));ui.gap(column,16);
        int title;
        switch(setupStep){
            case SIDE:title=R.string.wizard_side_title;break;
            case INTRO:title=R.string.wizard_intro_title;break;
            case INSTALL:title=R.string.wizard_install_title;break;
            case START:
                setupGuide=prefs.getInt("onboard_guide",prefs.getBoolean("onboard_complete",false)?2:0);
                title=setupGuide==0?R.string.wizard_developer_title:setupGuide==1?R.string.wizard_wireless_title:
                        prefs.getBoolean("onboard_complete",false)?R.string.wizard_restart_title:R.string.wizard_pair_title;
                break;
            case AUTHORIZE:title=R.string.wizard_authorize_title;break;
            case CONNECT:title=R.string.wizard_connect_title;break;
            case OVERLAY:title=R.string.wizard_overlay_title;break;
            case ACCESS:title=R.string.wizard_access_title;break;
            case KEYBOARD:title=R.string.wizard_keyboard_title;break;
            case RANGE:title=R.string.range_title;break;
            case PRACTICE:title=R.string.wizard_practice_title;break;
            default:title=R.string.wizard_ready_title;
        }
        column.addView(ui.text(getString(title),26,ReachUi.TEXT,true));ui.gap(column,16);
        switch(setupStep){
            case SIDE:
                setupBody(R.string.wizard_side_body);setupButton(R.string.wizard_use_left,()->chooseSetupSide(false));
                setupLink(R.string.wizard_use_right,()->chooseSetupSide(true));break;
            case INTRO:
                setupBody(R.string.setup_description);setupBody(R.string.wizard_intro_body);
                setupButton(R.string.wizard_continue,()->setupDone("intro"));break;
            case INSTALL:
                setupBody(R.string.wizard_install_body);setupButton(R.string.wizard_install,()->openWeb("https://shizuku.rikka.app/download/"));break;
            case START:
                setupGuide=prefs.getInt("onboard_guide",prefs.getBoolean("onboard_complete",false)?2:0);
                if(setupGuide==0){
                    setupBody(R.string.wizard_developer_body);
                    setupLive=ui.text("",14,ReachUi.ACCENT,true);setupLive.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);setupLive.setVisibility(View.GONE);column.addView(setupLive);
                    setupLink(R.string.wizard_open_about,()->systemSettings(Settings.ACTION_DEVICE_INFO_SETTINGS));
                    setupButton(R.string.wizard_next,()->{if(!requireDeveloperOptions())return;prefs.edit().putInt("onboard_guide",1).apply();render();});
                }else if(setupGuide==1){
                    setupBody(R.string.wizard_wireless_body);
                    setupLink(R.string.wizard_open_developer,()->{if(requireDeveloperOptions())systemSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);});
                    setupButton(R.string.wizard_next,()->{prefs.edit().putInt("onboard_guide",2).apply();render();});
                }else{
                    setupBody(prefs.getBoolean("onboard_complete",false)?R.string.wizard_restart_body:R.string.wizard_pair_body);
                    setupButton(R.string.wizard_open_shizuku,this::openShizuku);
                }
                if(setupGuide<2)setupLink(R.string.wizard_open_shizuku,this::openShizuku);
                setupLink(R.string.setup_shizuku_guide,()->openWeb("https://shizuku.rikka.app/guide/setup/"));
                if(setupGuide>0)setupLink(R.string.back,()->{prefs.edit().putInt("onboard_guide",setupGuide-1).apply();render();});
                addConnectionStatus();break;
            case AUTHORIZE:
                setupBody(R.string.wizard_authorize_body);addConnectionStatus();
                setupButton(R.string.wizard_authorize,()->{
                    try{if(rikka.shizuku.Shizuku.shouldShowRequestPermissionRationale())openShizuku();else Bridge.request();}
                    catch(Exception e){render();}
                });break;
            case CONNECT:
                setupBody(R.string.wizard_connect_body);addConnectionStatus();
                setupButton(R.string.check_shizuku,()->{connectionCheckDeadline=SystemClock.uptimeMillis()+8000;Bridge.tryBind();updateState();});
                setupLink(R.string.wizard_open_shizuku,this::openShizuku);break;
            case OVERLAY:
                setupBody(R.string.wizard_overlay_body);setupButton(R.string.wizard_open_permission,()->{
                    try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:"+getPackageName())));}
                    catch(ActivityNotFoundException e){systemSettings(Settings.ACTION_SETTINGS);}
                });break;
            case ACCESS:
                setupBody(R.string.wizard_access_body);setupButton(R.string.wizard_agree,()->setupDone("access"));break;
            case KEYBOARD:
                setupBody(R.string.wizard_keyboard_body);setupBody(R.string.keyboard_languages);
                setupButton(R.string.wizard_agree,()->{prefs.edit().putBoolean("setup_accepted",true).putBoolean("onboard_keyboard",true).commit();render();});break;
            case RANGE:
                setupBody(R.string.wizard_range_body);setupLive=ui.text("",15,ReachUi.ACCENT,true);column.addView(setupLive);
                setupButton(R.string.start_assistance,()->{if(setupRangeReady)startActivity(new Intent(this,NativeRangeActivity.class));else setPower(true);});
                updateSetup();break;
            case PRACTICE:
                setupBody(R.string.pointer_practice_description);column.addView(new PointerPractice(this),ui.fill(180));ui.gap(column,20);
                setupBody(getString(R.string.wizard_bar_body,getString(R.string.whole)));
                LinearLayout demo=ui.column();column.addView(demo,ui.fill(-2));setupBar(demo,FloatingControls.COLLAPSED);
                setupButton(R.string.wizard_practice_done,()->setupDone("practice"));break;
            case READY:
                setupBody(R.string.wizard_ready_body);addConnectionStatus();
                switchRow(getString(R.string.auto_start),getString(R.string.auto_start_description),prefs.getBoolean("auto_open",false),on->prefs.edit().putBoolean("auto_open",on).apply());
                if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
                    setupBody(R.string.wizard_notification_body);setupLink(R.string.allow_notifications,()->requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},18));
                }
                setupButton(R.string.wizard_finish,()->{if(nextSetup()!=SetupFlow.Step.READY){render();return;}prefs.edit().putBoolean("onboard_complete",true).commit();go("home");setPower(true);});break;
        }
        footer.addView(ui.button(getString(prefs.getBoolean("onboard_complete",false)?R.string.back_settings:R.string.wizard_later),false,()->go("home")),ui.fill(48));
    }
    private void setupBar(LinearLayout demo,int level){
        demo.removeAllViews();FloatingControls controls=new FloatingControls(this,prefs,2,level,Math.max(ui.dp(80),safeWidth()-ui.dp(40)),id->{if(id.equals("more"))setupBar(demo,FloatingControls.nextLevel(level));},null);
        demo.addView(controls.view,new LinearLayout.LayoutParams(controls.width,-2));
    }
    private String sideLabel(boolean opposite){return getString(TouchSide.right(prefs)!=opposite?R.string.right:R.string.left);}
    private void sideChanged(){NativeService.settingsChanged();ReachKeyboard.settingsChanged();render();}
    private void side(){
        header(getString(R.string.touch_side),getString(R.string.side_description));
        for(boolean right:new boolean[]{false,true}){
            String label=right?getString(R.string.right):getString(R.string.left);
            column.addView(ui.button(label,TouchSide.right(prefs)==right,()->{TouchSide.apply(prefs,right);sideChanged();}),ui.fill(56));ui.gap(column,12);
        }
        column.addView(ui.text(getString(R.string.side_modes_description,sideLabel(false),sideLabel(true)),14,ReachUi.MUTED,false));
        if(TouchSide.pending(prefs)){
            ui.gap(column,20);sideCountdown=ui.text("",16,ReachUi.ACCENT,true);column.addView(sideCountdown);updateSideCountdown();
            footer.removeAllViews();footer.addView(ui.button(getString(R.string.confirm_side),true,()->{if(TouchSide.expire(prefs)){sideChanged();return;}TouchSide.save(prefs);render();}),ui.fill(56));ui.gap(footer,8);
            footer.addView(ui.button(getString(R.string.revert_side),false,()->{TouchSide.rollback(prefs);sideChanged();}),ui.fill(52));
        }else{ui.gap(column,20);column.addView(ui.text(getString(R.string.side_safety),14,ReachUi.MUTED,false));}
    }
    private void updateSideCountdown(){if(sideCountdown!=null&&TouchSide.pending(prefs))sideCountdown.setText(getString(R.string.side_countdown,Math.max(0,(prefs.getLong("touch_side_expires",0)-System.currentTimeMillis()+999)/1000)));}
    private String controlSummary(){int target=prefs.getInt("native_target",TouchSide.directTarget(prefs));return target==TouchSide.directTarget(prefs)?getString(R.string.direct_summary,sideLabel(false)):getString(R.string.pad_summary,target==2?getString(R.string.whole):sideLabel(true),getString(R.string.speed_value,PointerGeometry.speed(prefs.getFloat("pointer_speed",1))));}
    private String regions(){return getString(R.string.regions_summary,percent(prefs.getFloat("left",.45f)),percent(1-prefs.getFloat("right",.55f)));}
    static String percent(float n){float p=Math.round(n*1000)/10f;return (p==Math.round(p)?String.valueOf(Math.round(p)):String.format(Locale.US,"%.1f",p))+"%";}
    private String pinnedSummary(){Set<String> s=prefs.getStringSet("native_pinned",new HashSet<>(Arrays.asList("mode","back")));ArrayList<String> labels=new ArrayList<>();if(s.contains("mode"))labels.add(getString(R.string.mode_switch));if(s.contains("back"))labels.add(getString(R.string.back));if(s.contains("home"))labels.add(getString(R.string.home));if(s.contains("recent"))labels.add(getString(R.string.recent_apps));return labels.isEmpty()?getString(R.string.in_more):android.text.TextUtils.join(" · ",labels);}
    private void section(String label){ui.gap(column,18);TextView heading=ui.text(label,13,ReachUi.MUTED,true);heading.setLetterSpacing(.04f);column.addView(heading);ui.gap(column,8);}
    private void nav(String title,String value,String summary,Runnable action){
        LinearLayout row=ui.row();row.setPadding(0,ui.dp(14),0,ui.dp(14));row.setMinimumHeight(ui.dp(60));
        row.addView(ui.text(title,16,ReachUi.TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
        if(value!=null){TextView current=ui.text(value,14,ReachUi.MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.leftMargin=ui.dp(8);row.addView(current,p);}
        ImageView chevron=new ImageView(this);chevron.setImageDrawable(ui.icon("chevron_right",ReachUi.MUTED));LinearLayout.LayoutParams arrow=new LinearLayout.LayoutParams(ui.dp(20),ui.dp(24));arrow.leftMargin=ui.dp(8);row.addView(chevron,arrow);
        row.setContentDescription(title+", "+summary);row.setBackground(ui.background(Color.TRANSPARENT,12,false));row.setOnClickListener(v->action.run());column.addView(row,ui.fill(-2));
    }
    private void switchRow(String title,String description,boolean checked,java.util.function.Consumer<Boolean> change){
        LinearLayout row=ui.row();row.setPadding(0,ui.dp(8),0,ui.dp(8));row.setMinimumHeight(ui.dp(60));row.addView(ui.text(title,16,ReachUi.TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
        Switch s=ui.toggle(checked);s.setContentDescription(title+", "+description);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.leftMargin=ui.dp(10);row.addView(s,p);s.setOnCheckedChangeListener((b,on)->change.accept(on));column.addView(row,ui.fill(-2));
    }
    private void header(String title,String description){
        LinearLayout row=ui.row();row.addView(ui.iconButton("arrow_back",getString(R.string.back_settings),()->go("home")),new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));
        row.addView(ui.text(getString(R.string.settings),15,ReachUi.MUTED,false));column.addView(row);ui.gap(column,16);column.addView(ui.text(title,24,ReachUi.TEXT,true));ui.gap(column,12);column.addView(ui.text(description,14,ReachUi.MUTED,false));ui.gap(column,24);
        footer.addView(ui.button(getString(R.string.back_settings),true,()->go("home")),ui.fill(54));
    }
    private void method(){
        header(getString(R.string.pointer_title),getString(R.string.pointer_description,sideLabel(true),sideLabel(false)));
        column.addView(ui.text(getString(R.string.pointer_speed),19,ReachUi.TEXT,true));ui.gap(column,8);
        column.addView(ui.text(getString(R.string.speed_description),13,ReachUi.MUTED,false));ui.gap(column,12);
        TextView speedValue=ui.text("",23,ReachUi.ACCENT,true);column.addView(speedValue);
        SeekBar speed=new SeekBar(this);speed.setMax(25);speed.setProgress(Math.round((PointerGeometry.speed(prefs.getFloat("pointer_speed",1f))-.5f)*10));speed.setContentDescription(getString(R.string.pointer_speed));speed.setProgressTintList(android.content.res.ColorStateList.valueOf(ReachUi.ACCENT));speed.setThumbTintList(android.content.res.ColorStateList.valueOf(ReachUi.ACCENT));column.addView(speed,ui.fill(48));
        java.util.function.IntConsumer change=progress->{float value=.5f+progress*.1f;prefs.edit().putFloat("pointer_speed",value).apply();speedValue.setText(getString(R.string.speed_value,value));speed.setStateDescription(speedValue.getText());NativeService.pointerSpeedChanged();};
        change.accept(speed.getProgress());
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int value,boolean user){change.accept(value);}});
        LinearLayout steps=ui.row();steps.addView(ui.button("−",false,()->speed.setProgress(speed.getProgress()-1)),new LinearLayout.LayoutParams(0,ui.dp(48),1));steps.addView(ui.button(getString(R.string.speed_reset),false,()->speed.setProgress(5)),new LinearLayout.LayoutParams(0,ui.dp(48),2));steps.addView(ui.button("+",false,()->speed.setProgress(speed.getProgress()+1)),new LinearLayout.LayoutParams(0,ui.dp(48),1));for(int i=1;i<steps.getChildCount();i++){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)steps.getChildAt(i).getLayoutParams();lp.leftMargin=ui.dp(4);}column.addView(steps);
        ui.gap(column,6);column.addView(ui.text(getString(R.string.speed_limits),12,ReachUi.MUTED,false));ui.gap(column,16);
        column.addView(ui.text(getString(R.string.gesture_help),15,ReachUi.MUTED,false));ui.gap(column,16);
        column.addView(new PointerPractice(this),ui.fill(140));
    }
    private void toolbar(){
        header(getString(R.string.toolbar_title),getString(R.string.toolbar_description));
        LinearLayout directions=ui.row();
        for(boolean vertical:new boolean[]{false,true}){Button b=ui.button(vertical?getString(R.string.vertical):getString(R.string.horizontal),prefs.getBoolean("native_vertical",false)==vertical,()->setVertical(vertical));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ui.dp(48),1);lp.rightMargin=ui.dp(4);directions.addView(b,lp);}column.addView(directions);ui.gap(column,20);toolbarSize();ui.gap(column,20);
        column.addView(ui.text(getString(R.string.toolbar_preview),12,ReachUi.MUTED,false));ui.gap(column,8);
        toolbarStage=ui.column();column.addView(toolbarStage);toolbarPreview=ui.column();toolbarPins=ui.column();toolbarStage.addView(toolbarPreview);toolbarStage.addView(toolbarPins);
        toolbarPins.addView(ui.text(getString(R.string.always_visible),16,ReachUi.TEXT,true));ui.gap(toolbarPins,6);
        Set<String> selected=new HashSet<>(prefs.getStringSet("native_pinned",new HashSet<>(Arrays.asList("mode","back"))));
        String[] keys={"mode","back","home","recent"},labels={getString(R.string.mode_buttons),getString(R.string.back),getString(R.string.home),getString(R.string.recent_apps)};
        for(int i=0;i<keys.length;i++){final String key=keys[i];CheckBox b=new CheckBox(this);b.setText(labels[i]);b.setTextSize(16);b.setTextColor(ReachUi.TEXT);b.setButtonTintList(android.content.res.ColorStateList.valueOf(ReachUi.ACCENT));b.setMinHeight(ui.dp(48));b.setChecked(selected.contains(key));b.setOnCheckedChangeListener((v,on)->{if(on)selected.add(key);else selected.remove(key);prefs.edit().putStringSet("native_pinned",new HashSet<>(selected)).apply();NativeService.settingsChanged();renderToolbarPreview();});toolbarPins.addView(b,ui.fill(-2));}
        renderToolbarPreview();ui.gap(column,12);column.addView(ui.text(getString(R.string.unpinned_help),13,ReachUi.MUTED,false));
        ui.gap(column,8);column.addView(ui.text(getString(R.string.drag_toolbar_help),13,ReachUi.MUTED,false));
    }
    private void toolbarSize(){
        column.addView(ui.text(getString(R.string.toolbar_size),19,ReachUi.TEXT,true));ui.gap(column,6);
        column.addView(ui.text(getString(R.string.toolbar_size_description),13,ReachUi.MUTED,false));ui.gap(column,10);
        TextView value=ui.text(FloatingControls.sizePercent(prefs)+"%",23,ReachUi.ACCENT,true);column.addView(value);
        SeekBar size=new SeekBar(this);size.setMax(20);size.setProgress(Math.round((FloatingControls.sizePercent(prefs)-60)/5f));size.setContentDescription(getString(R.string.toolbar_size));size.setStateDescription(value.getText());size.setProgressTintList(android.content.res.ColorStateList.valueOf(ReachUi.ACCENT));size.setThumbTintList(android.content.res.ColorStateList.valueOf(ReachUi.ACCENT));column.addView(size,ui.fill(48));
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int progress,boolean user){int percent=60+progress*5;prefs.edit().putInt("native_control_percent",percent).apply();value.setText(percent+"%");b.setStateDescription(value.getText());NativeService.settingsChanged();renderToolbarPreview();}});
        LinearLayout steps=ui.row();
        Button minus=ui.button("−",false,()->size.setProgress(size.getProgress()-1)),reset=ui.button(getString(R.string.toolbar_size_reset),false,()->size.setProgress(8)),plus=ui.button("+",false,()->size.setProgress(size.getProgress()+1));
        minus.setContentDescription(getString(R.string.toolbar_smaller));plus.setContentDescription(getString(R.string.toolbar_larger));
        for(Button b:new Button[]{minus,reset,plus}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ui.dp(48),b==reset?2:1);if(b!=minus)lp.leftMargin=ui.dp(4);steps.addView(b,lp);}column.addView(steps);
        ui.gap(column,6);column.addView(ui.text(getString(R.string.toolbar_size_limits),12,ReachUi.MUTED,false));
    }
    private void renderToolbarPreview(){
        if(toolbarPreview==null)return;toolbarPreview.removeAllViews();

        FloatingControls controls=new FloatingControls(this,prefs,prefs.getInt("native_target",TouchSide.directTarget(prefs)),previewLevel,Math.min(safeWidth()-ui.dp(40),ui.dp(268)),id->{if(id.equals("more")){previewLevel=FloatingControls.nextLevel(previewLevel);renderToolbarPreview();}},null);
        toolbarPreview.addView(controls.view,new LinearLayout.LayoutParams(controls.width,-2));
        boolean alongside=prefs.getBoolean("native_vertical",false)&&previewLevel!=FloatingControls.FULL&&safeWidth()-ui.dp(56)-controls.width>=ui.dp(130);
        toolbarStage.setOrientation(alongside?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(alongside?controls.width:-1,-2);pp.rightMargin=alongside?ui.dp(16):0;pp.bottomMargin=alongside?0:ui.dp(16);toolbarPreview.setLayoutParams(pp);
        toolbarPins.setLayoutParams(new LinearLayout.LayoutParams(alongside?0:-1,-2,alongside?1:0));
    }
    private void setVertical(boolean vertical){prefs.edit().putBoolean("native_vertical",vertical).apply();NativeService.settingsChanged();render();}
    private void openRange(){if(NativeService.isActive()&&ReachService.inner(this))startActivity(new Intent(this,NativeRangeActivity.class));else go("range");}
    private void rangeGate(){
        header(getString(R.string.range_title),getString(R.string.range_gate_description));
        column.addView(ui.text(getString(R.string.range_gate_body),16,ReachUi.TEXT,false));ui.gap(column,16);
        column.addView(ui.button(getString(R.string.start_assistance),true,()->{setPower(true);go("home");}),ui.fill(54));
    }
    private void help(){
        header(getString(R.string.help_title),getString(R.string.help_description));
        column.addView(ui.button(getString(R.string.setup_permissions),false,()->go("setup")),ui.fill(52));ui.gap(column,24);
        column.addView(ui.text(getString(R.string.saved_range),16,ReachUi.TEXT,true));ui.gap(column,8);column.addView(ui.text(regions(),15,ReachUi.MUTED,false));ui.gap(column,16);
        column.addView(ui.text(getString(R.string.previous_regions,percent(prefs.getFloat("previous_left",prefs.getFloat("left",.45f))),percent(1-prefs.getFloat("previous_right",prefs.getFloat("right",.55f)))),14,ReachUi.ACCENT,false));ui.gap(column,12);
        column.addView(ui.button(getString(R.string.restore_range),false,()->{RangePreview.restoreLast(prefs);NativeService.settingsChanged();render();Toast.makeText(this,getString(R.string.range_restored),Toast.LENGTH_SHORT).show();}),ui.fill(56));
        ui.gap(column,28);column.addView(ui.text(getString(R.string.shizuku_connection),18,ReachUi.TEXT,true));ui.gap(column,8);addConnectionStatus();column.addView(ui.button(getString(R.string.check_shizuku),false,this::prepare),ui.fill(52));ui.gap(column,8);column.addView(ui.text(getString(R.string.shizuku_restart_help),13,ReachUi.MUTED,false));
        ui.gap(column,28);column.addView(ui.text(getString(R.string.turn_off_title),18,ReachUi.TEXT,true));ui.gap(column,8);column.addView(ui.text(getString(R.string.turn_off_body),14,ReachUi.MUTED,false));
    }
    private void handleBack(){if(page.equals("home"))finish();else go("home");}
}
