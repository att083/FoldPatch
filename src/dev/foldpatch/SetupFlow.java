package dev.foldpatch;

/** Pure decision order: saved progress never substitutes for live connection/permission checks. */
final class SetupFlow {
    static void initialize(android.content.SharedPreferences prefs){
        if(prefs.contains("onboard_version"))return;
        boolean existing=prefs.getBoolean("setup_accepted",false);
        android.content.SharedPreferences.Editor e=prefs.edit().putInt("onboard_version",1);
        for(String key:new String[]{"side","intro","access","keyboard","range","practice","complete"})e.putBoolean("onboard_"+key,existing);
        e.commit();
    }
    enum Step { SIDE, INTRO, INSTALL, START, AUTHORIZE, CONNECT, OVERLAY, ACCESS, KEYBOARD, RANGE, PRACTICE, READY }
    static Step next(boolean side,boolean intro,boolean installed,boolean running,boolean authorized,
                     boolean connected,boolean overlay,boolean access,boolean keyboard,boolean range,boolean practice){
        if(!side)return Step.SIDE;
        if(!intro)return Step.INTRO;
        // A live provider can also be supplied by Sui; do not demand a manager in that case.
        if(!installed&&!running)return Step.INSTALL;
        if(!running)return Step.START;
        if(!authorized)return Step.AUTHORIZE;
        if(!connected)return Step.CONNECT;
        if(!overlay)return Step.OVERLAY;
        if(!access)return Step.ACCESS;
        if(!keyboard)return Step.KEYBOARD;
        if(!range)return Step.RANGE;
        if(!practice)return Step.PRACTICE;
        return Step.READY;
    }
}
