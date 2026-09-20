package dev.reachpad;

/** Coordinates follow the usable screen, independent of app/task boundaries. */
final class PointerGeometry {
    static float speed(float value){return Float.isNaN(value)||Float.isInfinite(value)?1f:Math.max(.5f,Math.min(3f,value));}
    static float move(float physicalX,float delta,int left,int right,int width,boolean whole){
        if(!whole)return Math.max(right,Math.min(width-1,physicalX+delta));
        int gap=right-left;
        float x=physicalX<left?physicalX:Math.max(left,physicalX-gap);
        x=Math.max(0,Math.min(width-gap-1,x+delta));
        return x<left?x:x+gap;
    }
    static float moveTarget(float x,float delta,int left,int right,int width,int target){
        if(target==0)return Math.max(0,Math.min(left-1,x+delta));
        return move(x,delta,left,right,width,target==2);
    }

}
