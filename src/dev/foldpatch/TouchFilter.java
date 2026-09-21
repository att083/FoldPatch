package dev.foldpatch;
import android.view.MotionEvent;

/** Admit pointers only where they START; a valid drag can leave the region without losing UP. */
final class TouchFilter {
    private int accepted;
    private final MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[32];
    private final MotionEvent.PointerCoords[] coordinates=new MotionEvent.PointerCoords[32];
    TouchFilter(){for(int i=0;i<32;i++){properties[i]=new MotionEvent.PointerProperties();coordinates[i]=new MotionEvent.PointerCoords();}}
    MotionEvent apply(MotionEvent e,int edge){
        int action=e.getActionMasked(),changed=e.getActionIndex(),changedId=e.getPointerId(changed);
        if(action==MotionEvent.ACTION_DOWN)accepted=0;
        if((action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN)&&e.getX(changed)>=0&&e.getX(changed)<edge)accepted|=1<<changedId;
        int count=0,newIndex=-1;
        for(int i=0;i<e.getPointerCount();i++)if((accepted&(1<<e.getPointerId(i)))!=0){
            if(i==changed)newIndex=count;e.getPointerProperties(i,properties[count]);e.getPointerCoords(i,coordinates[count]);count++;
        }
        int mappedAction=action;
        if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP){
            if(newIndex<0)mappedAction=MotionEvent.ACTION_MOVE;
            else if(count==1)mappedAction=action==MotionEvent.ACTION_POINTER_DOWN?MotionEvent.ACTION_DOWN:MotionEvent.ACTION_UP;
            else mappedAction=action|(newIndex<<MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        }
        MotionEvent result=count==0?null:(count==e.getPointerCount()&&mappedAction==e.getAction()?e:MotionEvent.obtain(e.getDownTime(),e.getEventTime(),mappedAction,count,properties,coordinates,e.getMetaState(),e.getButtonState(),e.getXPrecision(),e.getYPrecision(),e.getDeviceId(),e.getEdgeFlags(),e.getSource(),e.getFlags()));
        if(action==MotionEvent.ACTION_POINTER_UP)accepted&=~(1<<changedId);
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)accepted=0;
        return result;
    }
}
