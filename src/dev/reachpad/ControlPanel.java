package dev.reachpad;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.IntConsumer;
import org.json.*;
import rikka.shizuku.Shizuku;

/** All controls, including dialogs and recovery, are constrained to the healthy left side. */
final class ControlPanel {
    private final MainActivity a;private final FrameLayout root;private final LinearLayout panel;
    private final TextView status;private final Handler handler=new Handler();
    boolean expanded;private float x,y;private AlertDialog regionConfirmation;private Runnable rollback;
    private final ArrayList<AlertDialog> dialogs=new ArrayList<>();
    private static final String[] IDS={"mode","back","home","recent","apps","pairs"};
    private static final String[] NAMES={"조작 전환","뒤로","홈","최근 앱","앱 목록","앱 조합"};
    ControlPanel(MainActivity activity,FrameLayout frame,TextView info){
        a=activity;root=frame;status=info;status.setTextSize(12);status.setTextColor(0xffb8c9cb);
        panel=new LinearLayout(a);panel.setOrientation(1);panel.setPadding(dp(4),dp(4),dp(4),dp(4));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xf0223038);bg.setCornerRadius(dp(13));panel.setBackground(bg);
        root.addView(panel,new FrameLayout.LayoutParams(-2,-2));
        x=a.prefs.getFloat("panel_x",.02f);y=a.prefs.getFloat("panel_y",.15f);rebuild();
    }
    private int dp(int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private int buttonSize(){return dp(a.prefs.getInt("button_size",48));}
    void layout(){panel.post(()->{
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)panel.getLayoutParams();
        String pinned=a.prefs.getString("pinned","mode,back");int pinCount=pinned.isEmpty()?0:pinned.split(",").length;
        p.width=Math.max(dp(48),Math.min(expanded?dp(278):dp(48+Math.min(2,pinCount)*72),Math.max(dp(48),a.leftEdge-dp(8))));
        p.leftMargin=Math.max(0,Math.min(Math.round(x*a.leftEdge),a.leftEdge-p.width));
        int available=(ReachKeyboard.shown&&ReachKeyboard.onDisplay==a.displayId)?Math.min(a.physicalHeight,ReachKeyboard.top):a.physicalHeight;
        p.topMargin=Math.max(0,Math.min(Math.round(y*a.physicalHeight),available-panel.getHeight()-dp(8)));
        panel.setLayoutParams(p);
    });}
    void open(){expanded=true;rebuild();}void close(){expanded=false;rebuild();}
    private Button button(String text,Runnable action){
        Button b=new Button(a);b.setText(text);b.setTextSize(a.prefs.getInt("button_size",48)>52?14:12);b.setAllCaps(false);b.setPadding(dp(2),0,dp(2),0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setTextColor(Color.WHITE);b.setOnClickListener(v->action.run());return b;
    }
    void rebuild(){
        panel.removeAllViews();String pinned=","+a.prefs.getString("pinned","mode,back")+",";
        LinearLayout row=new LinearLayout(a);panel.addView(row);Button handle=button(expanded?"접기":"메뉴",()->{expanded=!expanded;rebuild();});
        row.addView(handle,new LinearLayout.LayoutParams(dp(48),buttonSize()));
        handle.setOnTouchListener(new View.OnTouchListener(){float sx,sy,ox,oy;boolean drag;
            public boolean onTouch(View v,MotionEvent e){if(e.getAction()==0){sx=e.getRawX();sy=e.getRawY();ox=x;oy=y;drag=false;}
                else if(e.getAction()==2){float dx=e.getRawX()-sx,dy=e.getRawY()-sy;if(Math.hypot(dx,dy)>dp(8))drag=true;if(drag){x=ox+dx/Math.max(1,a.leftEdge);y=oy+dy/Math.max(1,a.physicalHeight);layout();}}
                else if(e.getAction()==1&&drag){x=Math.max(0,Math.min(1,x));y=Math.max(0,Math.min(1,y));a.prefs.edit().putFloat("panel_x",x).putFloat("panel_y",y).apply();return true;}return drag;}});
        int count=1;
        for(int i=0;i<IDS.length;i++)if(pinned.contains(","+IDS[i]+",")){
            if(count==3){row=new LinearLayout(a);panel.addView(row);count=0;}
            final String id=IDS[i];Button b=button(label(id),()->action(id));if(id.equals("mode"))b.setTextColor(a.trackpad?0xffffdc76:0xff8be0c4);
            row.addView(b,new LinearLayout.LayoutParams(0,buttonSize(),1));count++;
        }
        if(expanded){
            if(status.getParent()!=null)((android.view.ViewGroup)status.getParent()).removeView(status);panel.addView(status);
            LinearLayout line=null;int j=0;
            for(int i=0;i<IDS.length;i++)if(!pinned.contains(","+IDS[i]+",")){
                if(j++%2==0){line=new LinearLayout(a);panel.addView(line);}final String id=IDS[i];line.addView(button(label(id),()->action(id)),new LinearLayout.LayoutParams(0,buttonSize(),1));
            }
            panel.addView(button("설정",this::settings),new LinearLayout.LayoutParams(-1,buttonSize()));
        }
        layout();
    }
    private String label(String id){if(id.equals("mode"))return a.trackpad?"오른쪽\n조작":"왼쪽\n조작";for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return NAMES[i];return id;}
    private void action(String id){
        if(id.equals("mode")){a.setTrackpad(!a.trackpad);return;}if(id.equals("back")){a.back();return;}
        close();if(id.equals("recent"))recent();else if(id.equals("pairs"))pairs();else apps();
    }
    private LinearLayout column(){LinearLayout c=new LinearLayout(a);c.setOrientation(1);c.setPadding(dp(8),dp(4),dp(8),dp(4));return c;}
    private void text(LinearLayout c,String value){TextView t=new TextView(a);t.setText(value);t.setTextColor(0xffd5e4e6);t.setTextSize(14);t.setPadding(dp(4),dp(8),dp(4),dp(8));c.addView(t);}
    private void item(LinearLayout c,String label,Runnable action){c.addView(button(label,action),new LinearLayout.LayoutParams(-1,dp(48)));}
    private AlertDialog dialog(String title,LinearLayout content){
        if(BridgeProvider.helper!=null)Bridge.async("host",null);
        ScrollView scroll=new ScrollView(a);scroll.addView(content);
        AlertDialog d=new AlertDialog.Builder(a).setTitle(title).setView(scroll).setNegativeButton("닫기",null).create();d.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);dialogs.add(d);
        d.setOnDismissListener(v->dialogs.remove(d));d.show();
        int healthy=a.leftEdge>0?a.leftEdge:Math.round(a.getResources().getDisplayMetrics().widthPixels*a.leftFraction);
        Window w=d.getWindow();w.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);w.setLayout(Math.max(dp(48),healthy-dp(8)),Math.min(dp(580),a.getResources().getDisplayMetrics().heightPixels-dp(70)));
        return d;
    }
    private void check(LinearLayout c,String label,String key,boolean def,Runnable changed){CheckBox b=new CheckBox(a);b.setText(label);b.setTextColor(Color.WHITE);b.setChecked(a.prefs.getBoolean(key,def));c.addView(b);b.setOnCheckedChangeListener((v,on)->{a.prefs.edit().putBoolean(key,on).apply();if(changed!=null)changed.run();});}
    void settings(){
        LinearLayout c=column();text(c,"사용 중인 앱을 그대로 이어갑니다. 저장한 조합은 직접 불러올 때만 열립니다.");
        check(c,"펼치면 자동 시작","auto_open",false,ReachService::changed);
        item(c,"화면 범위 조절",this::regions);item(c,"항상 보일 버튼 선택",this::pins);
        check(c,"노트북식 커서 조작","cursor_mode",false,a::applyGeometry);
        check(c,"왼쪽 전용 키보드","left_keyboard",true,()->{if(a.prefs.getBoolean("left_keyboard",true))a.enableKeyboard();else{Bundle b=new Bundle();b.putBoolean("enabled",false);Bridge.async("ime",b);}});
        text(c,"버튼 크기");slider(c,a.prefs.getInt("button_size",48),44,64,n->{a.prefs.edit().putInt("button_size",n).apply();rebuild();});
        item(c,"사용 준비 / 연결 상태",this::setup);item(c,"번호로 화면 확인",()->a.launch("probe"));
        item(c,"잠시 쉬기 · 휴대전화로 돌아가기",()->{dismissAll();a.leave();});
        dialog(a.getString(R.string.app_name)+" 설정",c);
    }
    private void pins(){
        LinearLayout c=column();text(c,"체크한 버튼은 항상 표시됩니다. 나머지는 메뉴를 누르면 보입니다.");String saved=","+a.prefs.getString("pinned","mode,back")+",";
        CheckBox[] boxes=new CheckBox[IDS.length];
        for(int i=0;i<IDS.length;i++){CheckBox b=new CheckBox(a);b.setText(NAMES[i]);b.setChecked(saved.contains(","+IDS[i]+","));boxes[i]=b;c.addView(b);}
        item(c,"저장",()->{ArrayList<String> ids=new ArrayList<>();for(int i=0;i<IDS.length;i++)if(boxes[i].isChecked())ids.add(IDS[i]);a.prefs.edit().putString("pinned",String.join(",",ids)).apply();rebuild();a.report("버튼 구성을 저장했어요");});dialog("항상 보일 버튼",c);
    }
    private SeekBar slider(LinearLayout c,int value,int min,int max,IntConsumer change){
        LinearLayout row=new LinearLayout(a);TextView number=new TextView(a);number.setText(""+value);number.setGravity(Gravity.CENTER);
        SeekBar s=new SeekBar(a);s.setMax(max-min);s.setProgress(value-min);
        row.addView(button("−",()->s.setProgress(s.getProgress()-1)),new LinearLayout.LayoutParams(dp(44),dp(44)));
        row.addView(number,new LinearLayout.LayoutParams(0,dp(44),1));row.addView(button("+",()->s.setProgress(s.getProgress()+1)),new LinearLayout.LayoutParams(dp(44),dp(44)));c.addView(row);c.addView(s);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){number.setText(""+(p+min));change.accept(p+min);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});return s;
    }
    private void regions(){
        LinearLayout c=column();text(c,"왼쪽 끝 (%)");SeekBar l=slider(c,Math.round(a.leftFraction*100),10,50,n->{});
        text(c,"오른쪽 시작 (%)");SeekBar r=slider(c,Math.round(a.rightFraction*100),50,90,n->{});
        item(c,"이 범위로 적용",()->{
            if(rollback!=null)rollback.run();float oldL=a.leftFraction,oldR=a.rightFraction;
            a.prefs.edit().putFloat("previous_left",oldL).putFloat("previous_right",oldR).putBoolean("pending_regions",true)
                .putFloat("left",(l.getProgress()+10)/100f).putFloat("right",(r.getProgress()+50)/100f).apply();
            dismissAll();a.applyGeometry();LinearLayout confirm=column();text(confirm,"버튼이 잘 보이고 눌리나요? 15초 안에 확인하지 않으면 이전 범위로 돌아갑니다.");
            rollback=()->{a.prefs.edit().putFloat("left",oldL).putFloat("right",oldR).putBoolean("pending_regions",false).apply();rollback=null;if(regionConfirmation!=null)regionConfirmation.dismiss();a.applyGeometry();};
            item(confirm,"잘 보여요 · 이 범위 사용",()->{handler.removeCallbacksAndMessages(null);rollback=null;a.prefs.edit().putBoolean("pending_regions",false).apply();regionConfirmation.dismiss();});
            regionConfirmation=dialog("범위 확인",confirm);handler.postDelayed(()->{if(rollback!=null)rollback.run();},15000);
        });
        item(c,"마지막 정상 설정으로 되돌리기",()->{a.prefs.edit().putFloat("left",a.prefs.getFloat("previous_left",.45f)).putFloat("right",a.prefs.getFloat("previous_right",.55f)).putBoolean("pending_regions",false).apply();dismissAll();a.applyGeometry();});dialog("화면 범위",c);
    }
    void setup(){
        LinearLayout c=column();text(c,BridgeProvider.helper!=null?"조작 연결이 준비됐어요.":"조작 연결을 준비해 주세요. 준비가 끝나면 컴퓨터 없이 앱에서 연결할 수 있습니다.");
        item(c,"Shizuku 연결 허용",()->{if(Shizuku.pingBinder()){Bridge.request();}else{Intent i=a.getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");if(i!=null)a.startActivity(i);else a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/download/")));}});
        text(c,"휴대전화를 재시작한 뒤 연결이 안 되면 Shizuku에서 시작한 후 돌아오세요.");
        item(c,Settings.canDrawOverlays(a)?"복귀 버튼 권한: 사용 중":"왼쪽 복귀 버튼 사용 허용",()->a.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:dev.reachpad"))));
        item(c,"왼쪽 키보드 다시 연결",a::enableKeyboard);
        item(c,"빠른 설정 버튼 추가",()->{
            if(android.os.Build.VERSION.SDK_INT>=33)a.getSystemService(android.app.StatusBarManager.class).requestAddTileService(new ComponentName(a,ReachTile.class),a.getString(R.string.tile_label),android.graphics.drawable.Icon.createWithResource(a,android.R.drawable.ic_menu_view),a.getMainExecutor(),n->{});
        });dialog("사용 준비",c);
    }
    private void apps(){
        LinearLayout c=column();EditText search=new EditText(a);search.setSingleLine();search.setHint("앱 검색");c.addView(search);
        LinearLayout list=column();c.addView(list);ArrayList<ResolveInfo> apps=new ArrayList<>(a.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0));
        apps.sort((x,y)->x.loadLabel(a.getPackageManager()).toString().compareToIgnoreCase(y.loadLabel(a.getPackageManager()).toString()));
        Runnable fill=()->{list.removeAllViews();String q=search.getText().toString().toLowerCase(java.util.Locale.ROOT);
            for(ResolveInfo app:apps){if(app.activityInfo.packageName.equals("dev.reachpad"))continue;String name=app.loadLabel(a.getPackageManager()).toString();if(!name.toLowerCase(java.util.Locale.ROOT).contains(q))continue;
                String component=new ComponentName(app.activityInfo.packageName,app.activityInfo.name).flattenToString();item(list,name,()->choose(name,component,-1));}};
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int x,int y,int z){}public void onTextChanged(CharSequence s,int x,int y,int z){fill.run();}public void afterTextChanged(android.text.Editable e){}});fill.run();dialog("앱 열기",c);
    }
    private String name(String pkg){try{return a.getPackageManager().getApplicationLabel(a.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){return pkg;}}
    private void recent(){Bridge.async("status",null,result->{LinearLayout c=column();ArrayList<Bundle> tasks=result.getParcelableArrayList("tasks");if(tasks!=null)for(Bundle t:tasks)item(c,name(t.getString("package")),()->choose(name(t.getString("package")),t.getString("component"),t.getInt("task")));dialog("최근 앱",c);});}
    private void choose(String label,String component,int task){LinearLayout c=column();for(int side:new int[]{0,1,2})item(c,side==0?"양쪽에 크게 열기":side==1?"왼쪽에 열기":"오른쪽에 열기",()->{
        Bundle b=new Bundle();b.putInt("side",side);b.putString("component",component);b.putInt("task",task);dismissAll();Bridge.async(task>=0?"task":"open",b,r->{if(side!=0)a.setTrackpad(side==2);});});dialog(label,c);}
    private JSONArray savedPairs(){try{return new JSONArray(a.prefs.getString("pairs","[]"));}catch(Exception e){return new JSONArray();}}
    private void pairs(){
        LinearLayout c=column();item(c,"지금 배치를 조합으로 저장",()->Bridge.async("status",null,state->{
            ArrayList<Bundle> tasks=state.getParcelableArrayList("tasks");String left=null,right=null;
            if(tasks!=null)for(Bundle t:tasks){if(t.getInt("task")==state.getInt("left"))left=t.getString("component");if(t.getInt("task")==state.getInt("right"))right=t.getString("component");}
            if(left==null||right==null){a.report("왼쪽과 오른쪽에 앱을 연 다음 저장해 주세요");return;}
            try{JSONArray list=savedPairs();JSONObject pair=new JSONObject();pair.put("left",left);pair.put("right",right);pair.put("name",name(ComponentName.unflattenFromString(left).getPackageName())+" + "+name(ComponentName.unflattenFromString(right).getPackageName()));list.put(pair);a.prefs.edit().putString("pairs",list.toString()).apply();a.report("조합을 저장했어요");}catch(Exception e){a.report("조합을 저장하지 못했어요");}
        }));
        JSONArray list=savedPairs();for(int i=0;i<list.length();i++){JSONObject pair=list.optJSONObject(i);if(pair==null)continue;final int index=i;
            item(c,pair.optString("name"),()->{dismissAll();Bundle l=new Bundle();l.putString("component",pair.optString("left"));l.putInt("side",1);Bridge.async("open",l,result->{Bundle r=new Bundle();r.putString("component",pair.optString("right"));r.putInt("side",2);Bridge.async("open",r,b->a.setTrackpad(true));});});
            item(c,"이 조합 삭제",()->{JSONArray updated=savedPairs();updated.remove(index);a.prefs.edit().putString("pairs",updated.toString()).apply();dismissAll();pairs();});
        }dialog("저장한 앱 조합",c);
    }
    private void dismissAll(){for(AlertDialog d:new ArrayList<>(dialogs))d.dismiss();}
    void destroy(){handler.removeCallbacksAndMessages(null);if(rollback!=null)rollback.run();dismissAll();}
}
