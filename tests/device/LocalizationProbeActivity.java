package dev.foldpatch;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.*;

/** Debug-only resource resolution and compact-control layout checks on the Android runtime. */
public final class LocalizationProbeActivity extends Activity {
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        TextView result=new TextView(this);result.setTextColor(0xfff3f6f4);result.setBackgroundColor(0xff161c1e);result.setPadding(40,300,40,40);setContentView(result);
        try {
            String[][] cases={{"en-US","Left"},{"en,ko-KR","Left"},{"ko-KR","왼쪽"},{"ja-JP","左"},{"zh-CN","左侧"},{"zh-SG","左侧"},{"zh-TW","左側"},{"zh-HK","左側"},{"zh-Hans","左侧"},{"zh-Hant","左側"},{"fr-FR","Left"}};
            int strings=0,layouts=0;
            for(String[] test:cases){
                Configuration cfg=new Configuration(getResources().getConfiguration());cfg.setLocales(LocaleList.forLanguageTags(test[0]));
                Context c=createConfigurationContext(cfg);
                if(!test[1].equals(c.getString(R.string.left)))throw new AssertionError("Locale resolution: "+test[0]+" = "+c.getString(R.string.left));
                for(Field f:R.string.class.getFields()){
                    int id=f.getInt(null);String template=c.getString(id);Matcher m=Pattern.compile("%(\\d+)\\$[.\\d]*([sdf])").matcher(template);Object[] args=new Object[3];int count=0;
                    while(m.find()){int n=Integer.parseInt(m.group(1));count=Math.max(count,n);args[n-1]=m.group(2).equals("d")?Long.valueOf(12):m.group(2).equals("f")?Float.valueOf(1.5f):"44.5%";}
                    if(count>0)c.getString(id,Arrays.copyOf(args,count));strings++;
                }
                SharedPreferences prefs=c.getSharedPreferences("localization_probe",0);
                for(boolean vertical:new boolean[]{false,true})for(int percent:new int[]{60,75,100,160})for(int level:new int[]{FloatingControls.COLLAPSED,FloatingControls.PINNED,FloatingControls.FULL}){
                    prefs.edit().putBoolean("native_vertical",vertical).putInt("native_control_percent",percent).putStringSet("native_pinned",new HashSet<>(Arrays.asList("mode","back","home","recent"))).commit();
                    int width=Math.round(getDisplay().getMode().getPhysicalWidth()*.20f);
                    FloatingControls controls=new FloatingControls(c,prefs,2,level,width,id->{},null);
                    controls.view.measure(View.MeasureSpec.makeMeasureSpec(controls.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                    controls.view.layout(0,0,controls.width,controls.view.getMeasuredHeight());
                    int expected=level==FloatingControls.COLLAPSED?1:level==FloatingControls.PINNED?7:8;
                    if(buttonCount(controls.view)!=expected)throw new AssertionError("Toolbar stage button count: "+level);
                    if(controls.width>width)throw new AssertionError("Toolbar exceeds working area");
                    checkText(controls.view,test[0]+" "+percent+"% "+vertical);layouts++;
                }
                c.deleteSharedPreferences("localization_probe");
            }
            String message="PASS: "+cases.length+" locale cases, "+strings+" strings, "+layouts+" toolbar layouts";
            android.util.Log.i("FoldPatchLocaleTest",message);result.setText(message);
        }catch(Throwable e){android.util.Log.e("FoldPatchLocaleTest","FAIL",e);result.setText("FAIL: "+e);}
    }
    private int buttonCount(View view){
        if(view instanceof Button||view instanceof ImageButton)return 1;
        int count=0;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)count+=buttonCount(group.getChildAt(i));}return count;
    }
    private void checkText(View view,String test){
        if(view instanceof TextView){TextView v=(TextView)view;android.text.Layout l=v.getLayout();
            if(l!=null&&v.getText().length()>0){int line=l.getLineCount()-1;
                if(l.getEllipsisCount(line)>0||l.getLineEnd(line)<v.getText().length()||l.getHeight()>v.getHeight()-v.getCompoundPaddingTop()-v.getCompoundPaddingBottom()+1)
                    throw new AssertionError("Clipped text "+test+": "+v.getText());
                for(int i=0;i<=line;i++)if(l.getLineWidth(i)>v.getWidth()-v.getCompoundPaddingLeft()-v.getCompoundPaddingRight()+1)throw new AssertionError("Wide text "+test+": "+v.getText());
            }
        }
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)checkText(g.getChildAt(i),test);}
    }
}
