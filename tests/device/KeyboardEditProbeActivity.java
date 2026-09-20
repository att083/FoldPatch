package dev.reachpad;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.lang.reflect.*;

/** Debug-only test through the live IME and an actual editor; restores the clipboard. */
public final class KeyboardEditProbeActivity extends Activity {
    private final Handler handler=new Handler();
    private EditText field;private TextView result;private ReachKeyboard keyboard;
    private ClipboardManager clipboard;private ClipData previousClip;private boolean clipSaved;
    private int attempts,step;private final String sample="가😀나 ABC";
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(32,250,32,0);
        result=new TextView(this);result.setText("Keyboard edit check");layout.addView(result);
        field=new EditText(this);field.setText(sample);field.setSingleLine(true);
        layout.addView(field,new LinearLayout.LayoutParams(-1,160));
        android.content.SharedPreferences p=getSharedPreferences("regions",0);
        FrameLayout root=new FrameLayout(this);int w=Math.round(getDisplay().getMode().getPhysicalWidth()*TouchSide.ratio(p));
        root.addView(layout,new FrameLayout.LayoutParams(w,-1,TouchSide.right(p)?Gravity.RIGHT:Gravity.LEFT));setContentView(root);
        field.requestFocus();getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        handler.postDelayed(this::awaitKeyboard,700);
    }
    private Object get(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void awaitKeyboard(){try{
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(field,InputMethodManager.SHOW_IMPLICIT);
        Field f=ReachKeyboard.class.getDeclaredField("instance");f.setAccessible(true);keyboard=(ReachKeyboard)f.get(null);
        if(keyboard==null||!ReachKeyboard.shown){if(++attempts<20){handler.postDelayed(this::awaitKeyboard,500);return;}throw new AssertionError("IME not ready");}
        clipboard=getSystemService(ClipboardManager.class);previousClip=clipboard.getPrimaryClip();clipSaved=true;
        runStep();
    }catch(Throwable e){fail(e);}}
    private Button find(View v,String label){if(v instanceof Button&&label.contentEquals(((Button)v).getText()))return (Button)v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button found=find(g.getChildAt(i),label);if(found!=null)return found;}}return null;}
    private void tap(String label)throws Exception{Button b=find((View)get(keyboard,"board"),label);if(b==null)throw new AssertionError("Missing button "+label);b.performClick();}
    private void tap(int id)throws Exception{tap(getString(id));}
    private void type(char c)throws Exception{Method m=ReachKeyboard.class.getDeclaredMethod("type",char.class);m.setAccessible(true);m.invoke(keyboard,c);}
    private void expect(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private void text(String expected){expect(expected.contentEquals(field.getText()),"Editor text mismatch at step "+step);}
    private void runStep(){try{
        switch(step++){
        case 0: tap(R.string.key_edit);break;
        case 1: tap(R.string.key_select_all);break;
        case 2: expect(Math.abs(field.getSelectionEnd()-field.getSelectionStart())==sample.length(),"Select all");tap(R.string.key_copy);break;
        case 3: expect(sample.contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"Copy");tap(R.string.key_select_all);break;
        case 4: tap(R.string.key_cut);break;
        case 5: text("");tap(R.string.key_paste);break;
        case 6: text(sample);field.setSelection(3);break;
        case 7: tap("←");break;
        case 8: expect(field.getSelectionEnd()==1,"Left across emoji");tap("→");break;
        case 9: expect(field.getSelectionEnd()==3,"Right across emoji");field.setSelection(sample.length());break;
        case 10: type('ㄱ');break;
        case 11: type('ㅏ');break;
        case 12: text(sample+"가");tap(R.string.key_select_all);break;
        case 13: tap(R.string.key_cut);break;
        case 14: text("");tap(R.string.key_paste);break;
        case 15: text(sample+"가");tap("←");break;
        case 16: type('x');break;
        case 17: text(sample+"x가");tap(R.string.key_edit);break;
        case 18: expect(!(Boolean)get(keyboard,"editing"),"Close edit tools");restoreClip();
            String message="PASS selection, copy, cut, paste, emoji arrows, Hangul composition and insertion";
            result.setText(message);android.util.Log.i("ReachPadEditTest",message);return;
        }
        handler.postDelayed(this::runStep,350);
    }catch(Throwable e){fail(e);}}
    private void restoreClip(){if(clipSaved){clipSaved=false;if(previousClip==null)clipboard.clearPrimaryClip();else clipboard.setPrimaryClip(previousClip);previousClip=null;}}
    private void fail(Throwable e){restoreClip();result.setText("FAIL: "+e);android.util.Log.e("ReachPadEditTest","FAIL step "+step,e);}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);restoreClip();super.onDestroy();}
}
