package dev.reachpad;

import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Same compact controls are rendered in the overlay and its live settings preview. */
final class FloatingControls {
    interface Action {void run(String id);}
    private final ReachUi ui;
    private final int cell,stride,gap,maxColumns,target;
    private final float scale;
    static int sizePercent(SharedPreferences prefs){return Math.max(60,Math.min(160,prefs.getInt("native_control_percent",prefs.getBoolean("native_large_controls",false)?117:100)));}
    static final int COLLAPSED=0, PINNED=1, FULL=2;
    static int nextLevel(int level){return (level+1)%3;}
    private final boolean vertical;
    private final int level;
    private final Action action;
    private final View.OnTouchListener drag;
    private final Set<String> pinned;
    final LinearLayout view;
    final int width;
    FloatingControls(Context c,SharedPreferences prefs,int target,int level,int maxWidth,Action action,View.OnTouchListener drag){
        ui=new ReachUi(c);scale=sizePercent(prefs)/100f;cell=ui.dp(48*scale);gap=ui.dp(4*scale);stride=cell+gap;
        maxColumns=Math.max(1,Math.min(5,(maxWidth-gap)/stride));
        this.target=target;this.level=level;this.action=action;this.drag=drag;
        vertical=prefs.getBoolean("native_vertical",false);
        pinned=prefs.getStringSet("native_pinned",new HashSet<>(Arrays.asList("mode","back")));
        ArrayList<String> main=new ArrayList<>();main.add("more");ArrayList<String> extra=new ArrayList<>();
        for(String id:new String[]{"mode","back","home","recent"}){
            ArrayList<String> list=pinned.contains(id)?main:extra;
            if(id.equals("mode"))Collections.addAll(list,"left","right","whole");else list.add(id);
        }
        extra.add("settings");
        if(level==COLLAPSED){main.clear();main.add("more");}
        view=ui.column();view.setPadding(gap,gap,0,0);view.setBackground(ui.shape(ReachUi.BG,Math.round(10*scale),true));
        if(vertical){
            int cols=level==FULL?Math.min(2,maxColumns):1;
            if(level==FULL)main.addAll(extra);
            width=gap+stride*cols;view.addView(grid(main,cols));
        }else{
            if(level==FULL)main.addAll(extra);
            int rows=(main.size()+maxColumns-1)/maxColumns;
            int cols=(main.size()+rows-1)/rows;width=gap+stride*cols;view.addView(grid(main,cols));
        }
    }
    private LinearLayout grid(List<String> ids,int cols){
        LinearLayout out=ui.column(),row=null;
        for(int i=0;i<ids.size();i++){
            if(i%cols==0){row=ui.row();out.addView(row);}
            View b=button(ids.get(i));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(cell,cell);p.setMargins(0,0,gap,gap);row.addView(b,p);
        }return out;
    }
    private View button(String id){
        boolean collapsedHandle=id.equals("more")&&level==COLLAPSED;
        boolean selected=collapsedHandle||id.equals("left")&&target==0||id.equals("right")&&target==1||id.equals("whole")&&target==2;
        String label=id.equals("left")?ui.c.getString(R.string.left):id.equals("right")?ui.c.getString(R.string.right):id.equals("whole")?ui.c.getString(R.string.whole):id.equals("settings")?ui.c.getString(R.string.settings):"";
        if(collapsedHandle)label=ui.c.getString(target==0?R.string.left:target==1?R.string.right:R.string.whole);
        View v;
        if(!label.isEmpty()){
            Button b=ui.button(label,selected,()->action.run(id));b.setAutoSizeTextTypeUniformWithConfiguration(Math.max(5,Math.round(8*scale)),Math.max(6,Math.round(12*scale)),1,android.util.TypedValue.COMPLEX_UNIT_SP);b.setPadding(0,0,0,0);b.setMaxLines(2);b.setIncludeFontPadding(false);b.setLineSpacing(0,1);b.setBackground(ui.background(selected?ReachUi.ACCENT:ReachUi.SURFACE,Math.round(8*scale),!selected));v=b;
        }else{
            String icon=id.equals("more")?(level==FULL?"close":"more_horiz"):id.equals("back")?"arrow_back":id.equals("home")?"home":"filter_none";
            String desc=id.equals("more")?(level==FULL?ui.c.getString(R.string.collapse_drag):ui.c.getString(level==COLLAPSED?R.string.unfold_drag:R.string.more_drag)):id.equals("back")?ui.c.getString(R.string.back):id.equals("home")?ui.c.getString(R.string.home):ui.c.getString(R.string.recent_apps);
            ImageButton b=ui.iconButton(icon,desc,()->action.run(id));b.setBackground(ui.background(ReachUi.SURFACE,Math.round(8*scale),true));b.setScaleType(ImageView.ScaleType.FIT_CENTER);int inset=ui.dp(12*scale);b.setPadding(inset,inset,inset,inset);v=b;
        }
        if(id.equals("more")&&drag!=null)v.setOnTouchListener(drag);
        if(collapsedHandle){v.setContentDescription(ui.c.getString(R.string.control_area,label)+", "+ui.c.getString(R.string.unfold_drag));v.setStateDescription(label);}
        if(id.equals("left")||id.equals("right")||id.equals("whole")){v.setContentDescription(ui.c.getString(R.string.control_area,label));v.setSelected(selected);v.setStateDescription(selected?ui.c.getString(R.string.selected):ui.c.getString(R.string.not_selected));}
        return v;
    }
}
