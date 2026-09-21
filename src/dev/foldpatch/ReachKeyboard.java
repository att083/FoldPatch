package dev.foldpatch;

import android.inputmethodservice.InputMethodService;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.content.*;

public final class ReachKeyboard extends InputMethodService {
    static volatile boolean shown;
    static volatile int top=Integer.MAX_VALUE;
    static volatile int onDisplay=-1;
    private final HangulComposer composer=new HangulComposer();
    private boolean korean=true,shift,symbols,editing;
    private LinearLayout board;private FrameLayout wrapper;
    private Context labelContext;
    private String composition="";
    private int width,height;
    private static ReachKeyboard instance;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener=(p,key)->{if("touch_right".equals(key)||"keyboard_width".equals(key)||"keyboard_source_width".equals(key))rebuild();};
    @Override public void onCreate(){super.onCreate();instance=this;getSharedPreferences("regions",0).registerOnSharedPreferenceChangeListener(preferenceListener);}
    @Override public void onDestroy(){getSharedPreferences("regions",0).unregisterOnSharedPreferenceChangeListener(preferenceListener);instance=null;super.onDestroy();}
    static void settingsChanged(){if(instance!=null)instance.rebuild();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public boolean onEvaluateFullscreenMode(){return false;}
    @Override public View onCreateInputView(){
        wrapper=new FrameLayout(this);wrapper.setBackgroundColor(Color.TRANSPARENT);
        board=new LinearLayout(this);board.setOrientation(1);board.setPadding(dp(4),dp(3),dp(4),dp(5));board.setBackgroundColor(ReachUi.BG);
        wrapper.addView(board);rebuild();return wrapper;
    }
    private String uiString(int id){return (labelContext==null?this:labelContext).getString(id);}
    private void rebuild(){
        if(board==null)return;
        // IME window contexts may use system locales even when the app has an override.
        android.os.LocaleList locales=getSystemService(android.app.LocaleManager.class).getApplicationLocales();
        labelContext=this;
        if(!locales.isEmpty()){
            android.content.res.Configuration config=new android.content.res.Configuration(getResources().getConfiguration());
            config.setLocales(locales);labelContext=createConfigurationContext(config);
        }
        int total=getResources().getDisplayMetrics().widthPixels;
        SharedPreferences p=getSharedPreferences("regions",0);
        int configured=p.getInt("keyboard_width",total);
        int did=getDisplay()==null?0:getDisplay().getDisplayId();
        width=did==0&&!ReachService.inner(this)?total:Math.min(total,configured);
        boolean right=did==0&&ReachService.inner(this)&&TouchSide.right(p);
        FrameLayout.LayoutParams bounds=new FrameLayout.LayoutParams(width,-2,Gravity.LEFT|Gravity.BOTTOM);
        // IME may have a full physical-width window even while app content is reflowed.
        int source=NativeService.isActive()?Math.min(total,p.getInt("keyboard_source_width",total)):total;
        bounds.leftMargin=right?Math.max(0,source-width):0;
        board.setLayoutParams(bounds);
        board.removeAllViews();
        LinearLayout tools=row();key(tools,korean?uiString(R.string.key_korean):uiString(R.string.key_english),()->{finish();korean=!korean;symbols=false;rebuild();},1.3f);
        key(tools,symbols?(korean?"가나다":"ABC"):"123",()->{finish();symbols=!symbols;rebuild();},1);
        Button edit=key(tools,uiString(R.string.key_edit),()->{finish();editing=!editing;rebuild();},1.2f);edit.setSelected(editing);edit.setStateDescription(uiString(editing?R.string.selected:R.string.not_selected));
        key(tools,uiString(R.string.key_hide),()->{finish();requestHideSelf(0);},1.4f);
        if(did==0&&!ReachService.inner(this))key(tools,uiString(R.string.key_other),()->{finish();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();},1.4f);
        if(editing){
            LinearLayout editRow=row();
            key(editRow,uiString(R.string.key_cut),()->edit(android.R.id.cut),1);
            key(editRow,uiString(R.string.key_copy),()->edit(android.R.id.copy),1);
            key(editRow,uiString(R.string.key_paste),()->edit(android.R.id.paste),1);
            LinearLayout cursorRow=row();
            key(cursorRow,uiString(R.string.key_select_all),()->edit(android.R.id.selectAll),1.5f);
            key(cursorRow,"←",()->moveCursor(KeyEvent.KEYCODE_DPAD_LEFT),1).setContentDescription(uiString(R.string.key_cursor_left));
            key(cursorRow,"→",()->moveCursor(KeyEvent.KEYCODE_DPAD_RIGHT),1).setContentDescription(uiString(R.string.key_cursor_right));
        }
        String[] rows=symbols?new String[]{"1234567890","@#%&*()!?","-_/+:;,."}:korean?
            new String[]{shift?"ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ":"ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ","ㅁㄴㅇㄹㅎㅗㅓㅏㅣ","ㅋㅌㅊㅍㅠㅜㅡ"}:
            new String[]{shift?"QWERTYUIOP":"qwertyuiop",shift?"ASDFGHJKL":"asdfghjkl",shift?"ZXCVBNM":"zxcvbnm"};
        for(int r=0;r<rows.length;r++){
            LinearLayout line=row();if(r==2)key(line,"⇧",()->{shift=!shift;rebuild();},1.25f);
            for(char c:rows[r].toCharArray())key(line,""+c,()->type(c),1);
            if(r==2)key(line,"⌫",this::erase,1.4f);
        }
        LinearLayout bottom=row();key(bottom,",",()->type(','),1);key(bottom,uiString(R.string.key_space),()->{finish();commit(" ");},3.5f);
        key(bottom,".",()->type('.'),1);key(bottom,actionLabel(),this::enter,1.8f);
        board.post(()->{height=board.getHeight();int[] position=new int[2];board.getLocationOnScreen(position);if(position[1]>0)top=position[1];notifyHost();});
    }
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(0);board.addView(r,new LinearLayout.LayoutParams(-1,dp(45)));return r;}
    private Button key(LinearLayout row,String label,Runnable action,float weight){
        Button b=new Button(this);b.setText(label);b.setTextSize(label.length()>2?11:18);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);
        ReachUi ui=new ReachUi(this);boolean enter=label.equals(actionLabel());
        StateListDrawable background=new StateListDrawable();background.addState(new int[]{android.R.attr.state_pressed},ui.shape(enter?ReachUi.ACCENT:ReachUi.PRESSED,7,false));background.addState(new int[]{android.R.attr.state_selected},ui.shape(enter?ReachUi.ACCENT:ReachUi.PRESSED,7,false));background.addState(new int[]{},ui.shape(enter?ReachUi.ACCENT:ReachUi.KEY,7,true));b.setBackground(background);b.setTextColor(enter?ReachUi.INK:ReachUi.TEXT);
        b.setMaxLines(1);b.setAutoSizeTextTypeUniformWithConfiguration(8,label.length()>2?11:18,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        b.setHapticFeedbackEnabled(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,weight);lp.setMargins(dp(1),dp(2),dp(1),dp(2));row.addView(b,lp);b.setOnTouchListener((v,event)->{
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
            return false;
        });
        b.setOnClickListener(v->action.run());return b;
    }
    private void edit(int action){
        finish();InputConnection ic=getCurrentInputConnection();if(ic==null)return;
        // Let the focused editor handle its own selection, clipboard and restrictions.
        if(!ic.performContextMenuAction(action))Toast.makeText(this,uiString(R.string.key_edit_unavailable),Toast.LENGTH_SHORT).show();
    }
    private void moveCursor(int keyCode){
        finish();sendDownUpKeyEvents(keyCode);
    }
    private void commit(String text){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(text,1);}
    private void type(char c){
        InputConnection ic=getCurrentInputConnection();if(ic==null)return;
        if(korean&&!symbols&&(HangulComposer.INITIAL.indexOf(c)>=0||HangulComposer.VOWEL.indexOf(c)>=0)){
            ic.beginBatchEdit();
            try{String done=composer.add(c);if(!done.isEmpty())ic.commitText(done,1);
                composition=composer.text();ic.setComposingText(composition,1);}
            finally{ic.endBatchEdit();}
        }else{finish();commit(""+c);}
        if(shift){shift=false;rebuild();}
    }
    private void erase(){InputConnection ic=getCurrentInputConnection();if(ic==null)return;
        if(composer.backspace()){composition=composer.text();ic.setComposingText(composition,1);if(composition.isEmpty())ic.finishComposingText();}
        else{CharSequence selected=ic.getSelectedText(0);if(selected!=null&&selected.length()>0)ic.commitText("",1);else ic.deleteSurroundingTextInCodePoints(1,0);}}
    private void finish(){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.finishComposingText();composer.clear();composition="";}
    private String actionLabel(){EditorInfo e=getCurrentInputEditorInfo();int a=e==null?0:e.imeOptions&EditorInfo.IME_MASK_ACTION;
        if(a==EditorInfo.IME_ACTION_SEARCH)return uiString(R.string.key_search);if(a==EditorInfo.IME_ACTION_SEND)return uiString(R.string.key_send);if(a==EditorInfo.IME_ACTION_NEXT)return uiString(R.string.key_next);if(a==EditorInfo.IME_ACTION_GO)return uiString(R.string.key_go);return uiString(R.string.key_enter);}
    private void enter(){finish();InputConnection ic=getCurrentInputConnection();if(ic==null)return;EditorInfo e=getCurrentInputEditorInfo();int a=e.imeOptions&EditorInfo.IME_MASK_ACTION;
        if(a!=EditorInfo.IME_ACTION_NONE&&a!=EditorInfo.IME_ACTION_UNSPECIFIED)ic.performEditorAction(a);else ic.commitText("\n",1);}
    @Override public void onStartInput(EditorInfo e,boolean restarting){super.onStartInput(e,restarting);composer.clear();composition="";shift=false;editing=false;symbols=(e.inputType&15)==android.text.InputType.TYPE_CLASS_NUMBER||(e.inputType&15)==android.text.InputType.TYPE_CLASS_PHONE;rebuild();}
    @Override public void onStartInputView(EditorInfo e,boolean restarting){super.onStartInputView(e,restarting);shown=true;onDisplay=getDisplay()==null?0:getDisplay().getDisplayId();rebuild();android.util.Log.i("FoldPatchKeyboard","shown display="+onDisplay+" width="+width+" top="+top);}
    @Override public void onFinishInputView(boolean finishing){shown=false;top=Integer.MAX_VALUE;notifyHost();android.util.Log.i("FoldPatchKeyboard","hidden finishing="+finishing);super.onFinishInputView(finishing);}
    @Override public void onFinishInput(){shown=false;composer.clear();composition="";super.onFinishInput();}
    @Override public void onUpdateSelection(int a,int b,int c,int d,int start,int end){super.onUpdateSelection(a,b,c,d,start,end);
        if(!composition.isEmpty()&&(c!=end||d!=end)){composer.clear();composition="";InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.finishComposingText();}}
    private void notifyHost(){MainActivity a=MainActivity.current;if(a!=null)a.keyboardChanged();NativeService.keyboardChanged();}
    @Override public void onComputeInsets(Insets out){
        super.onComputeInsets(out);if(board==null)return;
        int[] location=new int[2];board.getLocationInWindow(location);
        out.contentTopInsets=location[1];out.visibleTopInsets=location[1];out.touchableInsets=Insets.TOUCHABLE_INSETS_REGION;
        out.touchableRegion.set(location[0],location[1],location[0]+width,location[1]+board.getHeight());
        int[] screenPosition=new int[2];board.getLocationOnScreen(screenPosition);if(screenPosition[1]>0)top=screenPosition[1];notifyHost();
    }
}
