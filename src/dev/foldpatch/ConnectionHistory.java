package dev.foldpatch;
import android.content.Context;
import android.os.SystemClock;
import java.io.*;
/** Private bounded lifecycle history: fixed event names only, no UI/network/input contents. */
final class ConnectionHistory {
    private static Context app;
    static void init(Context c){app=c.getApplicationContext();}
    static synchronized void record(String event){if(app==null)return;try{
        File file=new File(app.getFilesDir(),"connection-history.log");
        if(file.length()>16384){File old=new File(app.getFilesDir(),"connection-history.previous.log");old.delete();file.renameTo(old);}
        try(FileWriter w=new FileWriter(file,true)){w.write(System.currentTimeMillis()+" "+SystemClock.elapsedRealtime()+" "+event+"\n");}
    }catch(IOException ignored){}}
}
