package dev.reachpad;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Warm ivory / orange visual system shared by settings, overlays and the IME. */
final class ReachUi {
    static final int BG=0xfffffaf4, SURFACE=0xfff5ebe0, LINE=0xffe5d7ca;
    static final int TEXT=0xff32231d, MUTED=0xff79675b, ACCENT=0xffbd471d, INK=0xffffffff;
    static final int PRESSED=0xffead3bf, KEY=0xfffffdf9;
    final Context c;
    ReachUi(Context c){this.c=c;}
    int dp(float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    TextView text(String s,int size,int color,boolean bold){
        TextView v=new TextView(c);v.setText(s);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        v.setIncludeFontPadding(false);v.setLineSpacing(dp(3),1);return v;
    }
    LinearLayout column(){LinearLayout l=new LinearLayout(c);l.setOrientation(1);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(c);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    GradientDrawable shape(int color,int radius,boolean border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border)d.setStroke(dp(1),LINE);return d;}
    android.graphics.drawable.Drawable background(int color,int radius,boolean border){return new RippleDrawable(ColorStateList.valueOf(0x2679675b),shape(color,radius,border),shape(0xffffffff,radius,false));}
    Button button(String label,boolean primary,Runnable action){
        Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTypeface(Typeface.create("sans-serif-medium",0));
        b.setTextColor(primary?INK:TEXT);b.setPadding(dp(8),0,dp(8),0);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);
        b.setMaxLines(2);b.setAutoSizeTextTypeUniformWithConfiguration(10,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        b.setBackground(background(primary?ACCENT:SURFACE,14,false));b.setStateListAnimator(null);b.setElevation(0);b.setOnClickListener(v->action.run());return b;
    }
    android.graphics.drawable.Drawable icon(String name,int tint){int id=c.getResources().getIdentifier("ic_"+name,"drawable",c.getPackageName());android.graphics.drawable.Drawable d=c.getDrawable(id).mutate();d.setTint(tint);return d;}
    ImageButton iconButton(String name,String label,Runnable action){ImageButton b=new ImageButton(c);b.setImageDrawable(icon(name,TEXT));b.setScaleType(ImageView.ScaleType.CENTER);b.setContentDescription(label);b.setBackground(background(0x00000000,8,false));b.setOnClickListener(v->action.run());return b;}
    Switch toggle(boolean checked){Switch s=new Switch(c);s.setShowText(false);s.setChecked(checked);s.setMinHeight(dp(48));s.setMinimumWidth(dp(48));
        s.setThumbTintList(ColorStateList.valueOf(KEY));
        StateListDrawable tracks=new StateListDrawable();
        GradientDrawable on=shape(ACCENT,20,false),off=shape(0xff927a69,20,false);on.setSize(dp(44),dp(24));off.setSize(dp(44),dp(24));
        tracks.addState(new int[]{android.R.attr.state_checked},on);tracks.addState(new int[]{},off);s.setTrackTintList(null);s.setTrackDrawable(tracks);return s;}
    void gap(LinearLayout l,int h){View v=new View(c);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    void line(LinearLayout l){View v=new View(c);v.setBackgroundColor(LINE);l.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    LinearLayout.LayoutParams fill(int h){return new LinearLayout.LayoutParams(-1,h<0?h:dp(h));}
    void enabled(View v,boolean on){v.setEnabled(on);v.setAlpha(on?1f:.42f);}
}
