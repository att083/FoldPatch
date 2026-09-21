package dev.foldpatch;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.Locale;

/** Dedicated physical ruler, then a reversible preview. Controls stay on the working touch side. */
public final class NativeRangeActivity extends Activity {
    static boolean visible,measuring;
    static int controlWidth;
    private ReachUi ui;
    private SharedPreferences prefs;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private LinearLayout panel,body,footer;
    private ScrollView scroll;
    private View plotSpace;
    private Pattern pattern;
    private SeekBar leftSlider,rightSlider;
    private TextView message,leftValue,rightValue;
    private Button primary;
    private boolean preview,closing,lastReady;
    private int physical,entryWidth;
    private float left,right;
    private final Runnable tick=new Runnable(){public void run(){check();if(!closing)handler.postDelayed(this,250);}};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);ui=new ReachUi(this);prefs=getSharedPreferences("regions",0);
        RangePreview.rollback(prefs);
        physical=getDisplay().getMode().getPhysicalWidth();left=prefs.getFloat("left",.45f);right=1-prefs.getFloat("right",.55f);
        entryWidth=Math.round(physical*(TouchSide.right(prefs)?right:left));controlWidth=entryWidth;measuring=true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().getDecorView().setSystemUiVisibility(5894);render();
    }
    @Override protected void onResume(){super.onResume();visible=true;measuring=!preview;NativeService.settingsChanged();handler.post(tick);}
    @Override protected void onPause(){
        visible=false;measuring=false;handler.removeCallbacks(tick);RangePreview.rollback(prefs);NativeService.settingsChanged();super.onPause();
        if(!isFinishing()){closing=true;finish();}
    }
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);resize();pattern.invalidate();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
    private void render(){
        root=new FrameLayout(this);root.setBackgroundColor(ReachUi.BG);pattern=new Pattern();root.addView(pattern,new FrameLayout.LayoutParams(-1,-1));
        panel=ui.column();root.addView(panel,new FrameLayout.LayoutParams(controlWidth,-1,TouchSide.right(prefs)?Gravity.RIGHT:Gravity.LEFT));
        scroll=new ScrollView(this);scroll.setFillViewport(true);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=ui.column();body.setPadding(ui.dp(14),ui.dp(22),ui.dp(14),ui.dp(8));scroll.addView(body);
        TextView title=ui.text(preview?getString(R.string.range_preview_title):getString(R.string.range_title),23,ReachUi.TEXT,true);body.addView(title);ui.gap(body,10);
        body.addView(ui.text(preview?getString(R.string.range_preview_description):getString(R.string.range_adjust_description),14,ReachUi.MUTED,false));
        plotSpace=new View(this);body.addView(plotSpace,new LinearLayout.LayoutParams(1,ui.dp(preview?130:166)));
        LinearLayout form=ui.column();form.setBackgroundColor(ReachUi.BG);body.addView(form,ui.fill(-2));
        leftSlider=null;rightSlider=null;leftValue=null;rightValue=null;
        form.addView(ui.text(getString(R.string.range_usable),19,ReachUi.TEXT,true));ui.gap(form,12);
        if(preview){
            form.addView(ui.text(getString(R.string.regions_summary,NativeActivity.percent(left),NativeActivity.percent(right)),18,ReachUi.ACCENT,true));ui.gap(form,16);
            form.addView(ui.text(getString(R.string.range_save_help),14,ReachUi.MUTED,false));ui.gap(form,10);
            form.addView(ui.text(getString(R.string.range_readjust_help),14,ReachUi.MUTED,false));
        }else{
            leftValue=stepper(form,getString(R.string.left_width),true);ui.gap(form,14);ui.line(form);ui.gap(form,14);rightValue=stepper(form,getString(R.string.right_width),false);
            ui.gap(form,10);form.addView(ui.text(getString(R.string.range_sliders_help),12,ReachUi.MUTED,false));
        }
        message=ui.text("",preview?16:13,preview?ReachUi.ACCENT:ReachUi.MUTED,preview);message.setMinHeight(ui.dp(30));message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);ui.gap(form,10);form.addView(message,ui.fill(-2));
        footer=ui.column();footer.setBackgroundColor(ReachUi.BG);footer.setPadding(ui.dp(14),ui.dp(6),ui.dp(14),ui.dp(14));panel.addView(footer,ui.fill(-2));
        primary=ui.button(preview?getString(R.string.save_range):getString(R.string.preview),true,()->{if(preview)save();else showPreview();});footer.addView(primary,ui.fill(50));ui.gap(footer,8);
        if(preview){footer.addView(ui.button(getString(R.string.readjust),false,this::backToInput),ui.fill(48));ui.gap(footer,8);}
        footer.addView(ui.button(getString(R.string.cancel),false,this::cancel),ui.fill(48));
        setContentView(root);resize();updateValues();
        body.setFocusableInTouchMode(true);body.requestFocus();
    }
    private TextView stepper(LinearLayout form,String label,boolean isLeft){
        form.addView(ui.text(label,15,ReachUi.TEXT,true));ui.gap(form,4);TextView value=ui.text("",25,ReachUi.ACCENT,true);form.addView(value);
        SeekBar slider=new SeekBar(this);slider.setMax(60);slider.setProgress(Math.round(((isLeft?left:right)-.20f)*200));slider.setMinHeight(ui.dp(48));
        slider.setProgressTintList(ColorStateList.valueOf(ReachUi.ACCENT));slider.setThumbTintList(ColorStateList.valueOf(ReachUi.ACCENT));slider.setProgressBackgroundTintList(ColorStateList.valueOf(ReachUi.LINE));
        slider.setPadding(ui.dp(10),0,ui.dp(10),0);slider.setContentDescription(label);form.addView(slider,ui.fill(48));
        if(isLeft)leftSlider=slider;else rightSlider=slider;
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){renew();}
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){if(!fromUser)return;if(isLeft)left=.20f+progress*.005f;else right=.20f+progress*.005f;renew();updateValues();pattern.invalidate();}
            public void onStopTrackingTouch(SeekBar b){applySelection();}
        });
        LinearLayout row=ui.row();Button minus=ui.button("− 0.5%",false,()->adjust(isLeft,-.005f));minus.setContentDescription(getString(R.string.width_decrease,label));
        Button plus=ui.button("+ 0.5%",false,()->adjust(isLeft,.005f));plus.setContentDescription(getString(R.string.width_increase,label));
        row.addView(minus,new LinearLayout.LayoutParams(0,ui.dp(48),1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,ui.dp(48),1);p.leftMargin=ui.dp(8);row.addView(plus,p);form.addView(row);return value;
    }
    private void renew(){if(preview&&RangePreview.pending(prefs))prefs.edit().putLong("native_regions_expires",System.currentTimeMillis()+RangePreview.TIMEOUT).apply();}
    private void showPreview(){preview=true;measuring=false;RangePreview.apply(prefs,left,right);resize();NativeService.settingsChanged();render();}
    private void applySelection(){if(preview){RangePreview.apply(prefs,left,right);resize();NativeService.settingsChanged();}updateValues();pattern.invalidate();}
    private void adjust(boolean isLeft,float amount){
        float before=isLeft?left:right,after=RangePreview.clamp(before+amount);if(before==after)return;
        if(isLeft)left=after;else right=after;
        applySelection();
    }
    private void save(){
        if(RangePreview.expire(prefs)||!RangePreview.pending(prefs)||!NativeService.matches(left,1-right)){check();return;}
        RangePreview.save(prefs);prefs.edit().putBoolean("onboard_range",true).commit();Toast.makeText(this,getString(R.string.range_saved),Toast.LENGTH_SHORT).show();close();
    }
    private void backToInput(){hideKeyboard();RangePreview.rollback(prefs);preview=false;measuring=true;NativeService.settingsChanged();render();}
    private void cancel(){RangePreview.rollback(prefs);close();}
    private void close(){closing=true;hideKeyboard();visible=false;measuring=false;NativeService.settingsChanged();finish();}
    private void hideKeyboard(){getSystemService(InputMethodManager.class).hideSoftInputFromWindow(root.getWindowToken(),0);}
    private void updateValues(){
        if(leftValue!=null)leftValue.setText(String.format(Locale.US,"%.1f%%",left*100));if(rightValue!=null)rightValue.setText(String.format(Locale.US,"%.1f%%",right*100));
        if(leftSlider!=null)leftSlider.setProgress(Math.round((left-.20f)*200));if(rightSlider!=null)rightSlider.setProgress(Math.round((right-.20f)*200));
        if(leftSlider!=null)leftSlider.setStateDescription(NativeActivity.percent(left));if(rightSlider!=null)rightSlider.setStateDescription(NativeActivity.percent(right));
    }
    private void resize(){
        if(!ReachService.inner(this)||getDisplay().getRotation()!=Surface.ROTATION_0){cancel();return;}
        int current=Math.round(physical*(preview?(TouchSide.right(prefs)?right:left):.50f));
        controlWidth=Math.min(entryWidth,current);
        if(panel!=null){FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)panel.getLayoutParams();p.width=Math.min(controlWidth,getResources().getDisplayMetrics().widthPixels);panel.setLayoutParams(p);}
    }
    private boolean ready(){return preview?NativeService.matches(left,1-right):!NativeService.isActive()&&getResources().getDisplayMetrics().widthPixels==physical;}
    private void check(){
        if(closing)return;
        if(!NativeService.requested(this)){cancel();return;}
        if(!ReachService.inner(this)||getDisplay().getRotation()!=Surface.ROTATION_0){cancel();return;}
        boolean expired=RangePreview.expire(prefs);
        if(preview&&(expired||!RangePreview.pending(prefs))){preview=false;measuring=true;NativeService.settingsChanged();render();message.setText(getString(R.string.range_preview_cancelled));return;}
        boolean ready=ready();
        ui.enabled(primary,ready);
        if(preview){long seconds=Math.max(0,(prefs.getLong("native_regions_expires",0)-System.currentTimeMillis()+999)/1000);message.setText(ready?getString(R.string.range_countdown,seconds):getString(R.string.range_applying));}
        else if(!ready)message.setText(getString(R.string.range_preparing));
        else if(message.getText().length()==0||message.getText().toString().equals(getString(R.string.range_preparing)))message.setText(getString(R.string.ruler_legend));
        if(lastReady!=ready){lastReady=ready;pattern.invalidate();}
    }
    @Override public void onBackPressed(){if(ReachKeyboard.shown)hideKeyboard();else cancel();}

    /** Test geometry, not decorative artwork: rulers are tied to physical pixel positions. */
    final class Pattern extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Pattern(){super(NativeRangeActivity.this);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void text(Canvas c,String s,float x,float y,int size,int color,Paint.Align align){p.setColor(color);p.setTextSize(ui.dp(size));p.setTextAlign(align);p.setStyle(Paint.Style.FILL);c.drawText(s,x,y,p);}
        @Override protected void onDraw(Canvas c){
            c.drawColor(ReachUi.BG);boolean ready=ready();if(!ready)return;
            float w=getWidth(),cut=preview?Math.round(physical*left):physical/2f,top=ui.dp(111),bottom=ui.dp(preview?197:232);
            int expected=preview?Math.round(physical*(left+right)):physical;if(Math.abs(w-expected)>3)return;
            text(c,preview?(TouchSide.right(prefs)?getString(R.string.check_left):getString(R.string.check_right)):(TouchSide.right(prefs)?getString(R.string.left_zero):getString(R.string.right_zero)),TouchSide.right(prefs)?ui.dp(14):w-ui.dp(14),ui.dp(46),17,ReachUi.ACCENT,TouchSide.right(prefs)?Paint.Align.LEFT:Paint.Align.RIGHT);
            if(!preview){
                float leftEdge=physical*left,rightEdge=w-physical*right;
                p.setColor(ReachUi.SURFACE);
                c.drawRect(0,top,leftEdge,bottom,p);c.drawRect(rightEdge,top,w,bottom,p);
                p.setColor(ReachUi.MUTED);p.setStrokeWidth(ui.dp(1));c.drawLine(0,top,w,top,p);
                for(int percent=0;percent<=50;percent++){
                    float distance=physical*percent/100f;boolean major=percent%5==0;
                    p.setColor(major?ReachUi.MUTED:ReachUi.MUTED);p.setStrokeWidth(ui.dp(major?1:.5f));
                    float tickBottom=top+ui.dp(major?22:10);
                    c.drawLine(distance,top,distance,tickBottom,p);c.drawLine(w-distance,top,w-distance,tickBottom,p);
                    if(major){
                        float y=top-ui.dp(9);
                        text(c,String.valueOf(percent),percent==0?ui.dp(3):distance,y,13,ReachUi.TEXT,percent==0?Paint.Align.LEFT:Paint.Align.CENTER);
                        if(percent<50)text(c,String.valueOf(percent),percent==0?w-ui.dp(3):w-distance,y,13,ReachUi.TEXT,percent==0?Paint.Align.RIGHT:Paint.Align.CENTER);
                    }
                }
                text(c,getString(R.string.left_percent,NativeActivity.percent(left)),leftEdge/2,ui.dp(184),21,ReachUi.ACCENT,Paint.Align.CENTER);
                text(c,getString(R.string.right_percent,NativeActivity.percent(right)),rightEdge+(w-rightEdge)/2,ui.dp(184),21,ReachUi.ACCENT,Paint.Align.CENTER);
                text(c,getString(R.string.use_to_line),leftEdge/2,ui.dp(212),13,ReachUi.MUTED,Paint.Align.CENTER);
                text(c,getString(R.string.use_to_line),rightEdge+(w-rightEdge)/2,ui.dp(212),13,ReachUi.MUTED,Paint.Align.CENTER);
            }else{
                for(int side=0;side<2;side++){
                    float start=side==0?0:cut,step=(side==0?cut:w-cut)/8;
                    for(int i=0;i<=8;i++){p.setColor(ReachUi.LINE);c.drawLine(start+i*step,top,start+i*step,bottom,p);}
                    for(int i=0;i<8;i++){int number=side*8+i+1;p.setColor(Color.HSVToColor(new float[]{(number-1)*22.5f,.42f,.88f}));c.drawRect(start+i*step,ui.dp(167),start+(i+1)*step,ui.dp(197),p);text(c,String.valueOf(number),start+(i+.5f)*step,ui.dp(192),15,ReachUi.TEXT,Paint.Align.CENTER);}
                }
                p.setColor(ReachUi.ACCENT);c.drawCircle(cut/2,ui.dp(138),ui.dp(23),p);c.drawCircle(cut+(w-cut)/2,ui.dp(138),ui.dp(23),p);
            }
            if(!preview){p.setColor(ReachUi.ACCENT);p.setStrokeWidth(ui.dp(2));c.drawLine(physical*left,top,physical*left,bottom,p);c.drawLine(w-physical*right,top,w-physical*right,bottom,p);}
            if(preview){p.setColor(ReachUi.ACCENT);p.setStrokeWidth(ui.dp(1.5f));c.drawLine(cut-ui.dp(2),top,cut-ui.dp(2),getHeight(),p);c.drawLine(cut+ui.dp(2),top,cut+ui.dp(2),getHeight(),p);}
        }
    }
}
