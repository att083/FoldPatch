package dev.foldpatch;
import java.io.*;
import java.util.concurrent.TimeUnit;
public final class ProcessShutdownTest {
    static final class Fake extends Process {
        boolean alive=true,grace,term,kill,terminated,killed,interrupt;
        public OutputStream getOutputStream(){return new ByteArrayOutputStream();}
        public InputStream getInputStream(){return new ByteArrayInputStream(new byte[0]);}
        public InputStream getErrorStream(){return getInputStream();}
        public int waitFor(){throw new AssertionError("Unbounded wait");}
        public boolean waitFor(long timeout,TimeUnit unit)throws InterruptedException {
            if(interrupt)throw new InterruptedException();
            if(grace||(terminated&&term)||(killed&&kill))alive=false;
            return !alive;
        }
        public int exitValue(){if(alive)throw new IllegalThreadStateException();return 0;}
        public boolean isAlive(){return alive;}
        public void destroy(){terminated=true;}
        public Process destroyForcibly(){killed=true;return this;}
    }
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args)throws Exception {
        Fake f=new Fake();f.grace=true;ProcessShutdown.finish(f,1,1,1,Process::destroyForcibly);check(!f.alive&&!f.terminated&&!f.killed);
        f=new Fake();f.term=true;ProcessShutdown.finish(f,1,1,1,Process::destroyForcibly);check(!f.alive&&f.terminated&&!f.killed);
        f=new Fake();f.kill=true;ProcessShutdown.finish(f,1,1,1,Process::destroyForcibly);check(!f.alive&&f.killed);
        f=new Fake();try{ProcessShutdown.finish(f,1,1,1,Process::destroyForcibly);throw new AssertionError("False cleanup success");}catch(IOException expected){check(f.alive);}
        f=new Fake();f.interrupt=true;try{ProcessShutdown.finish(f,1,1,1,Process::destroyForcibly);throw new AssertionError();}catch(InterruptedException expected){check(!f.terminated);}
        f=new Fake();f.alive=false;ProcessShutdown.finish(f,1,1,1,Process::destroyForcibly);check(!f.terminated);
        ProcessShutdown.finish(null,1,1,1,Process::destroyForcibly);
        System.out.println("ProcessShutdownTest passed: graceful, terminate, forced, timeout, interruption, already dead");
    }
}
