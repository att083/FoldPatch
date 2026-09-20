package dev.reachpad;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import android.text.*;
public final class InputProbeActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout row=new LinearLayout(this);row.setBackgroundColor(0xff263542);
        for(int i=0;i<2;i++){final String side=i==0?"left":"right";LinearLayout col=new LinearLayout(this);col.setOrientation(1);col.setPadding(24,100,24,24);
            TextView title=new TextView(this);title.setText(i==0?"왼쪽 입력 시험":"오른쪽 입력 시험");title.setTextSize(22);title.setTextColor(Color.WHITE);col.addView(title);
            EditText input=new EditText(this);input.setId(300+i);input.setSingleLine();input.setHint("여기에 입력");input.setTextSize(22);input.setTextColor(Color.WHITE);input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);col.addView(input);
            TextView value=new TextView(this);value.setTextSize(22);value.setTextColor(Color.WHITE);col.addView(value);
            input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int start,int before,int count){value.setText(s);android.util.Log.i("ReachPadInput",side+"="+s);}public void afterTextChanged(Editable e){}});
            input.setOnEditorActionListener((v,action,event)->{android.util.Log.i("ReachPadInput",side+" action="+action);return false;});
            row.addView(col,new LinearLayout.LayoutParams(0,-1,1));
        }setContentView(row);row.setFocusableInTouchMode(true);row.requestFocus();}
}
