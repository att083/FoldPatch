package dev.foldpatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.Display;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class MainActivity extends Activity {
    static MainActivity current;
    private ControlPanel panel;
    private boolean returning,waitingNotice,redirected;
    private SplitView split;
    private CursorView cursor;
    private TextView status;
    private Button modeButton;
    int physicalWidth, physicalHeight, logicalWidth, leftEdge, rightEdge, displayId = -1;
    float leftFraction, rightFraction, cursorX, cursorY;
    boolean trackpad, closing, cursorMode, lowLatency, benchmark;
    private float targetRefreshRate = 60f;
    private Surface targetSurface;
    SharedPreferences prefs;
    private IBinder activeHelper;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(!getIntent().getBooleanExtra("benchmark",false)){redirected=true;startActivity(new Intent(this,NativeActivity.class));finish();return;}
        current=this;
        lowLatency = getIntent().getBooleanExtra("low_latency", true);
        benchmark = getIntent().getBooleanExtra("benchmark", false);
        if(!benchmark)Bridge.init(this);
        if (lowLatency) {
            Display display = getWindowManager().getDefaultDisplay();
            Display.Mode current = display.getMode();
            for (Display.Mode mode : display.getSupportedModes()) {
                if (mode.getPhysicalWidth()==current.getPhysicalWidth() && mode.getPhysicalHeight()==current.getPhysicalHeight()) {
                    targetRefreshRate = Math.min(120f, Math.max(targetRefreshRate, mode.getRefreshRate()));
                }
            }
            WindowManager.LayoutParams attributes=getWindow().getAttributes();
            attributes.preferredRefreshRate=targetRefreshRate;getWindow().setAttributes(attributes);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        prefs = getSharedPreferences("regions", MODE_PRIVATE);
        leftFraction = prefs.getFloat("left", .4f); rightFraction = prefs.getFloat("right", .6f);
        if(prefs.getBoolean("pending_regions",false)){leftFraction=prefs.getFloat("previous_left",.45f);rightFraction=prefs.getFloat("previous_right",.55f);prefs.edit().putFloat("left",leftFraction).putFloat("right",rightFraction).putBoolean("pending_regions",false).apply();}
        cursorMode = prefs.getBoolean("cursor_mode", false);
        trackpad=prefs.getBoolean("trackpad",false);
        if (benchmark) { trackpad=true; cursorMode=false; }
        FrameLayout root = new FrameLayout(this);
        split = new SplitView(); root.addView(split);
        cursor = new CursorView(); root.addView(cursor);
        status=new TextView(this);
        panel=new ControlPanel(this,root,status);setContentView(root);
        if(!benchmark){startForegroundService(new Intent(this,ReachService.class));if(BridgeProvider.helper!=null)Bridge.async("capture",null);}
        BridgeProvider.listener = () -> runOnUiThread(this::connectDisplay);
        updateStatus("연결 준비 중");
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private Button button(String text, Runnable action) {
        Button b = new Button(this); b.setText(text); b.setTextSize(12); b.setAllCaps(false);
        b.setPadding(0,0,0,0); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(40)));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void updateStatus(String message) { status.setText(message); }
    void report(String message){updateStatus(message);android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_SHORT).show();}
    private void geometry(int width, int height) {
        boolean changed = physicalWidth != width || physicalHeight != height;
        physicalWidth = width; physicalHeight = height;
        leftEdge = Math.round(width * leftFraction); rightEdge = Math.round(width * rightFraction);
        logicalWidth = leftEdge + width - rightEdge;
        if (cursorX == 0 || changed) { cursorX = leftEdge + (width - rightEdge) * .5f; cursorY = height * .5f; }
        prefs.edit().putInt("keyboard_width",leftEdge).apply();
        if(panel!=null)panel.layout();
        if (targetSurface != null) connectDisplay();
    }
    private void connectDisplay() {
        if (closing || returning || targetSurface == null || physicalWidth == 0) return;
        if(!benchmark&&!ReachService.visible)return;
        if(!benchmark&&!ReachService.inner(this)){updateStatus("내부 화면을 펼쳐 주세요");return;}
        IBinder helper = BridgeProvider.helper;
        if (helper == null) { updateStatus("설정에서 사용 준비를 해 주세요");if(!waitingNotice){waitingNotice=true;Bridge.ui.postDelayed(()->{if(!closing&&BridgeProvider.helper==null&&panel!=null)panel.open();},1500);}return; }
        waitingNotice=false;

        try {
            if(!benchmark&&displayId<0)Bridge.command("capture",null);
            activeHelper = helper;
            Parcel data = data(), reply = Parcel.obtain();
            try {
                targetSurface.writeToParcel(data, 0); data.writeInt(logicalWidth); data.writeInt(physicalHeight); data.writeInt(420); data.writeFloat(targetRefreshRate);
                helper.transact(1, data, reply, 0); reply.readException(); displayId = reply.readInt();
            } finally { data.recycle(); reply.recycle(); }
            updateStatus("화면 연결됨 · " + logicalWidth + "px");
            android.util.Log.i("FoldPatchPerf", "config fast="+lowLatency+" benchmark="+benchmark+" requestedHz="+targetRefreshRate);
            if(benchmark)launch("latency");
            else{
                Bundle layout=new Bundle();layout.putInt("left",leftEdge);
                Bridge.async("adopt",layout,b->{Bridge.async("layout",layout);enableKeyboard();});
            }
        } catch (Exception e) { updateStatus("연결 오류: " + e.getMessage()); android.util.Log.e("FoldPatch", "create display", e); }
    }
    private Parcel data() { Parcel p = Parcel.obtain(); p.writeInterfaceToken("dev.foldpatch.control"); return p; }
    private void call(int code, Parcel data) {
        Parcel reply = Parcel.obtain();
        try {
            IBinder helper = BridgeProvider.helper;
            if (helper == null) throw new IllegalStateException("연결 끊김");
            helper.transact(code, data, reply, 0); reply.readException();
        } catch (Exception e) { updateStatus(e.getMessage()); android.util.Log.e("FoldPatch", "command " + code, e); }
        finally { data.recycle(); reply.recycle(); }
    }
    void launch(String app) { if (displayId < 0) return; Parcel p = data(); p.writeString(app); call(5, p); }
    private void inject(int action, float x, float y, long downTime) {
        if (displayId < 0) return;
        MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                Math.max(0, Math.min(logicalWidth-1, x)), Math.max(0, Math.min(physicalHeight-1, y)), 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        sendMotion(event); event.recycle();
    }
    private void sendMotion(MotionEvent event) {
        long start = benchmark ? System.nanoTime() : 0;
        Parcel p = data(); event.writeToParcel(p,0);
        if (lowLatency) {
            try {
                IBinder helper=BridgeProvider.helper;
                if(helper==null || !helper.transact(6,p,null,IBinder.FLAG_ONEWAY)) throw new IllegalStateException("입력 연결 끊김");
            } catch(Exception e) { updateStatus(e.getMessage()); android.util.Log.e("FoldPatch","input",e); }
            finally { p.recycle(); }
        } else call(2,p);
        if(benchmark) android.util.Log.i("FoldPatchPerf","bridge input="+event.getEventTime()+" callUs="+(System.nanoTime()-start)/1000);
    }
    private MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[0];
    private MotionEvent.PointerCoords[] pointerCoordinates = new MotionEvent.PointerCoords[0];
    /** Preserve the original gesture, including hold duration and multiple fingers. */
    private void injectMapped(MotionEvent original, boolean toRight) {
        if (displayId < 0) return;
        int count = original.getPointerCount();
        if (pointerProperties.length<count || !lowLatency) {
            pointerProperties=new MotionEvent.PointerProperties[count];pointerCoordinates=new MotionEvent.PointerCoords[count];
            for(int i=0;i<count;i++){pointerProperties[i]=new MotionEvent.PointerProperties();pointerCoordinates[i]=new MotionEvent.PointerCoords();}
        }
        MotionEvent.PointerProperties[] properties = pointerProperties;
        MotionEvent.PointerCoords[] coordinates = pointerCoordinates;
        float scale = toRight ? (logicalWidth-leftEdge-1f)/Math.max(1,leftEdge-1) : 1f;
        for (int i=0;i<count;i++) {
            original.getPointerProperties(i,properties[i]);
            original.getPointerCoords(i,coordinates[i]);
            coordinates[i].x = Math.max(0,Math.min(leftEdge-1,coordinates[i].x))*scale+(toRight?leftEdge:0);
            coordinates[i].y = Math.max(0,Math.min(physicalHeight-1,coordinates[i].y));
        }
        MotionEvent mapped = MotionEvent.obtain(original.getDownTime(), original.getEventTime(), original.getAction(), count,
            properties, coordinates, original.getMetaState(), original.getButtonState(), original.getXPrecision(),
            original.getYPrecision(), original.getDeviceId(), original.getEdgeFlags(), InputDevice.SOURCE_TOUCHSCREEN, original.getFlags());
        if (toRight) { cursorX=coordinates[0].x; cursorY=coordinates[0].y; cursor.postInvalidateOnAnimation(); }
        if(split.lastMapped!=null)split.lastMapped.recycle();split.lastMapped=MotionEvent.obtain(mapped);
        sendMotion(mapped);mapped.recycle();
    }
    void enableKeyboard(){if(!prefs.getBoolean("left_keyboard",true)||displayId<0)return;Bundle b=new Bundle();b.putBoolean("enabled",true);b.putString("previous",prefs.getString("previous_ime",""));Bridge.async("ime",b,r->{String old=r.getString("previous","");if(!old.isEmpty())prefs.edit().putString("previous_ime",old).apply();});}
    void setTrackpad(boolean value){split.cancelTouch();trackpad=value;prefs.edit().putBoolean("trackpad",value).apply();cursorX=Math.max(leftEdge+1,Math.min(logicalWidth-1,cursorX));cursor.invalidate();panel.rebuild();}
    void keyboardChanged(){if(panel!=null)panel.layout();}
    void back(){Bundle b=new Bundle();b.putBoolean("right",trackpad);b.putBoolean("ime",ReachKeyboard.shown);Bridge.async("back",b);}
    void applyGeometry(){
        leftFraction=prefs.getFloat("left",.45f);rightFraction=prefs.getFloat("right",.55f);cursorMode=prefs.getBoolean("cursor_mode",false);
        split.cancelTouch();
        split.queueEvent(()->{if(split.texture!=null)split.texture.setDefaultBufferSize(Math.round(physicalWidth*leftFraction)+physicalWidth-Math.round(physicalWidth*rightFraction),physicalHeight);});
        geometry(physicalWidth,physicalHeight);cursor.invalidate();panel.rebuild();
    }
    void leave(){ReachService.paused=true;returning=true;Bridge.async("leave",null,b->finish());}
    void finishForFold(){returning=true;finish();}
    @Override protected void onResume(){super.onResume();if(redirected)return;ReachService.visible=true;ReachService.paused=false;ReachService.changed();if(split!=null)split.onResume();if(targetSurface!=null)connectDisplay();}
    @Override protected void onPause(){super.onPause();if(redirected)return;ReachService.visible=false;ReachService.changed();if(split!=null){split.cancelTouch();split.onPause();}if(!benchmark&&BridgeProvider.helper!=null){try{Bridge.command("detach",null);}catch(Exception ignored){}Bundle b=new Bundle();b.putBoolean("enabled",false);Bridge.async("ime",b);}}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(targetSurface!=null)connectDisplay();}
    @Override public void onBackPressed(){if(redirected){finish();return;}if(panel.expanded)panel.close();else back();}
    @Override protected void onDestroy(){
        if(redirected){super.onDestroy();return;}
        closing=true;if(current==this)current=null;BridgeProvider.listener=null;
        if(panel!=null)panel.destroy();
        if(benchmark&&displayId>=0&&BridgeProvider.helper!=null)call(3,data());
        else if(!returning&&BridgeProvider.helper!=null)Bridge.async("detach",null);
        if(targetSurface!=null)targetSurface.release();if(split.frameCallbacks!=null)split.frameCallbacks.quitSafely();
        super.onDestroy();ReachService.changed();
    }

    private final class CursorView extends View {
        final Paint paint = new Paint(3);
        CursorView() { super(MainActivity.this); setClickable(false); }
        @Override protected void onDraw(Canvas c) {
            if (!trackpad) return;
            paint.setColor(0x183fcdb0); c.drawRect(0,0,leftEdge,getHeight(),paint);
            float physicalX = cursorX < leftEdge ? cursorX : cursorX + rightEdge-leftEdge;
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(Color.BLACK);
            c.drawCircle(physicalX,cursorY,dp(10),paint); paint.setColor(Color.YELLOW);
            c.drawCircle(physicalX,cursorY,dp(8),paint); paint.setStyle(Paint.Style.FILL);
        }
    }
    private final class SplitView extends GLSurfaceView implements GLSurfaceView.Renderer {
        final FloatBuffer vertices = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
        final AtomicBoolean pending = new AtomicBoolean();
        final float[] matrix = new float[16];
        final ByteBuffer marker = ByteBuffer.allocateDirect(4);
        HandlerThread frameCallbacks;
        int positionLocation,imageLocation,transformLocation,leftLocation,rightLocation;
        int lastMarker=-1;
        SurfaceTexture texture;
        int program, textureId, viewportWidth, viewportHeight;
        boolean ready, moved, scrolling, touching, keyboardGesture;
        MotionEvent lastMapped;
        float lastX,lastY,startX,startY,scrollX,scrollY;
        long down;
        SplitView() {
            super(MainActivity.this); setEGLContextClientVersion(2); setPreserveEGLContextOnPause(true);
            vertices.put(new float[]{-1,-1, 1,-1, -1,1, 1,1}).position(0);
            setRenderer(this); setRenderMode(RENDERMODE_WHEN_DIRTY);
        }
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig config) {
            String vs = "attribute vec2 p; varying vec2 uv; void main(){gl_Position=vec4(p,0.,1.);uv=vec2((p.x+1.)*.5,(1.-p.y)*.5);}";
            String fs = "#extension GL_OES_EGL_image_external : require\nprecision highp float; varying vec2 uv; uniform samplerExternalOES image; uniform mat4 transform; uniform float leftEdge; uniform float rightEdge; void main(){if(uv.x>=leftEdge&&uv.x<rightEdge){gl_FragColor=vec4(0.,0.,0.,1.);return;} float x=uv.x<leftEdge?uv.x:uv.x-(rightEdge-leftEdge); x/=(1.-rightEdge+leftEdge); vec4 q=transform*vec4(x,1.-uv.y,0.,1.); gl_FragColor=texture2D(image,q.xy);}";
            program = GLES20.glCreateProgram(); GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,vs)); GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,fs)); GLES20.glLinkProgram(program);
            positionLocation=GLES20.glGetAttribLocation(program,"p");imageLocation=GLES20.glGetUniformLocation(program,"image");
            transformLocation=GLES20.glGetUniformLocation(program,"transform");leftLocation=GLES20.glGetUniformLocation(program,"leftEdge");rightLocation=GLES20.glGetUniformLocation(program,"rightEdge");
            int[] ids = new int[1]; GLES20.glGenTextures(1,ids,0); textureId=ids[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            texture=new SurfaceTexture(textureId);
            if(lowLatency){
                frameCallbacks=new HandlerThread("FoldPatchFrames",android.os.Process.THREAD_PRIORITY_DISPLAY);frameCallbacks.start();
                texture.setOnFrameAvailableListener(t->{pending.set(true);requestRender();},new Handler(frameCallbacks.getLooper()));
            }else texture.setOnFrameAvailableListener(t->{pending.set(true);requestRender();});
        }
        @Override public void onSurfaceChanged(GL10 gl,int w,int h) {
            viewportWidth=w;viewportHeight=h; GLES20.glViewport(0,0,w,h);
            if(lowLatency)getHolder().getSurface().setFrameRate(targetRefreshRate,Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
            texture.setDefaultBufferSize(Math.round(w*leftFraction)+w-Math.round(w*rightFraction),h);
            Surface surface = new Surface(texture);
            runOnUiThread(()->{ if(targetSurface!=null)targetSurface.release(); targetSurface=surface; geometry(w,h); });
        }
        @Override public void onDrawFrame(GL10 gl) {
            GLES20.glClearColor(0,0,0,1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            if(pending.getAndSet(false)){texture.updateTexImage();texture.getTransformMatrix(matrix);ready=true;}
            if(!ready)return;
            GLES20.glUseProgram(program);
            int p=lowLatency?positionLocation:GLES20.glGetAttribLocation(program,"p"); GLES20.glEnableVertexAttribArray(p); GLES20.glVertexAttribPointer(p,2,GLES20.GL_FLOAT,false,0,vertices);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);
            GLES20.glUniform1i(lowLatency?imageLocation:GLES20.glGetUniformLocation(program,"image"),0);
            GLES20.glUniformMatrix4fv(lowLatency?transformLocation:GLES20.glGetUniformLocation(program,"transform"),1,false,matrix,0);
            GLES20.glUniform1f(lowLatency?leftLocation:GLES20.glGetUniformLocation(program,"leftEdge"),Math.round(viewportWidth*leftFraction)/(float)viewportWidth);
            GLES20.glUniform1f(lowLatency?rightLocation:GLES20.glGetUniformLocation(program,"rightEdge"),Math.round(viewportWidth*rightFraction)/(float)viewportWidth);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
            if(benchmark) {
                marker.position(0);
                GLES20.glReadPixels(viewportWidth-16,viewportHeight-16,1,1,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,marker);
                int encoded=((marker.get(0)&255)<<16)|((marker.get(1)&255)<<8)|(marker.get(2)&255);
                long age=((SystemClock.uptimeMillis()&0xffffff)-encoded)&0xffffff;
                if(encoded!=lastMarker&&encoded!=0&&age<2000){
                    android.util.Log.i("FoldPatchPerf","frame marker="+encoded+" ageMs="+age);lastMarker=encoded;
                }
            }
        }
        int shader(int type,String source){int id=GLES20.glCreateShader(type);GLES20.glShaderSource(id,source);GLES20.glCompileShader(id);int[]ok=new int[1];GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(id));return id;}
        void cancelTouch(){if(touching&&lastMapped!=null){MotionEvent cancel=MotionEvent.obtain(lastMapped);cancel.setAction(MotionEvent.ACTION_CANCEL);sendMotion(cancel);cancel.recycle();}touching=false;scrolling=false;keyboardGesture=false;}
        final TouchFilter filter=new TouchFilter();
        @Override public boolean onTouchEvent(MotionEvent original){
            MotionEvent filtered=filter.apply(original,leftEdge);if(filtered==null)return true;
            try{return handleTouch(filtered);}finally{if(filtered!=original)filtered.recycle();}
        }
        private boolean handleTouch(MotionEvent e) {
            int action=e.getActionMasked();float x=e.getX(),y=e.getY();
            if(lowLatency&&action==MotionEvent.ACTION_DOWN)requestUnbufferedDispatch(e);
            if(benchmark){
                int source=Math.round(e.getAxisValue(MotionEvent.AXIS_GENERIC_1));
                android.util.Log.i("FoldPatchPerf","host input="+source+" ageMs="+(((SystemClock.uptimeMillis()&0xffffff)-source)&0xffffff));
            }
            if(action==MotionEvent.ACTION_DOWN)keyboardGesture=ReachKeyboard.shown&&ReachKeyboard.onDisplay==displayId&&y>=ReachKeyboard.top&&x<leftEdge;
            if (!cursorMode||keyboardGesture) {
                if(action==MotionEvent.ACTION_DOWN) { if(x>=leftEdge)return false; touching=true; }
                if(!touching)return false;
                injectMapped(e,trackpad&&!keyboardGesture);
                if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)touching=false;
                return true;
            }
            if(action==MotionEvent.ACTION_DOWN){
                if(x>=leftEdge)return false;
                touching=true;down=e.getEventTime();startX=lastX=x;startY=lastY=y;moved=false;scrolling=false;
                if(!trackpad)inject(MotionEvent.ACTION_DOWN,x,y,down);
                return true;
            }
            if(!touching)return false;
            if(!trackpad){
                if(action==MotionEvent.ACTION_MOVE)inject(action,Math.min(x,leftEdge-1),y,down);
                else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){inject(action,Math.min(x,leftEdge-1),y,down);touching=false;}
                return true;
            }
            if(action==MotionEvent.ACTION_POINTER_DOWN && e.getPointerCount()==2){
                scrolling=true;moved=true;scrollX=cursorX;scrollY=cursorY;lastY=(e.getY(0)+e.getY(1))/2;
                inject(MotionEvent.ACTION_DOWN,scrollX,scrollY,down);
            }else if(action==MotionEvent.ACTION_MOVE){
                if(scrolling && e.getPointerCount()>=2){float cy=(e.getY(0)+e.getY(1))/2;scrollY+=cy-lastY;lastY=cy;inject(MotionEvent.ACTION_MOVE,scrollX,scrollY,down);}
                else if(!scrolling){
                    cursorX=Math.max(leftEdge,Math.min(logicalWidth-1,cursorX+(x-lastX)*1.5f));
                    cursorY=Math.max(0,Math.min(physicalHeight-1,cursorY+(y-lastY)*1.5f));lastX=x;lastY=y;
                    if(Math.hypot(x-startX,y-startY)>dp(5))moved=true;cursor.invalidate();
                }
            }else if(action==MotionEvent.ACTION_POINTER_UP && scrolling){inject(MotionEvent.ACTION_UP,scrollX,scrollY,down);scrolling=false;lastX=x;lastY=y;}
            else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                if(scrolling)inject(MotionEvent.ACTION_CANCEL,scrollX,scrollY,down);
                else if(!moved&&action==MotionEvent.ACTION_UP){long now=SystemClock.uptimeMillis();inject(MotionEvent.ACTION_DOWN,cursorX,cursorY,now);inject(MotionEvent.ACTION_UP,cursorX,cursorY,now);}
                touching=false;scrolling=false;
            }
            return true;
        }
    }
}
