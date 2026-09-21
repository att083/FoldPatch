package dev.foldpatch;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Bounded shutdown; a timeout must never be reported as successful cleanup. */
final class ProcessShutdown {
    interface ForceKill { void kill(Process process)throws IOException; }
    static void finish(Process process,long graceMs,long terminateMs,long killMs,ForceKill forceKill)throws IOException,InterruptedException {
        if(process==null||!process.isAlive())return;
        if(process.waitFor(graceMs,TimeUnit.MILLISECONDS))return;
        process.destroy();
        if(process.waitFor(terminateMs,TimeUnit.MILLISECONDS))return;
        // SIGTERM alone cannot stop a suspended process until it resumes.
        forceKill.kill(process);
        if(!process.waitFor(killMs,TimeUnit.MILLISECONDS)||process.isAlive())
            throw new IOException("Process cleanup did not finish");
    }
}
