package io.github.amlfarhad.batcomputertv;

import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    static final String GOOGLE_DREAM="com.google.android.apps.tv.dreamx/.service.Backdrop";
    static final String TERMINAL_DREAM="io.github.amlfarhad.batcomputertv/.TerminalDream";
    static final String[] LAYOUTS={"Command Line","File Browser","Quiet Console"};
    static final int[] IDLE_MINUTES={0,1,3,5,10};
    static final String[] SETTING_NAMES={"Layout","Home idle after","Idle screen","Idle preview","App order","System settings","Terminal texture"};
    final ArrayList<AppCatalog.Item> all=new ArrayList<>(),shown=new ArrayList<>();
    final Handler handler=new Handler();
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    SharedPreferences prefs;
    FrameLayout root;
    TerminalView terminal;
    EditText query;
    IdleView idleView;
    String screen="apps";
    String lastAppPkg;
    int layout,selected,nav=-1;
    boolean favoritesOnly,dialogOpen,scanning,rescanPending,active,keyboardOpen,idle,held,okDown;
    int wakeKey=-1;
    long lastScan;
    final Runnable longPress=new Runnable(){public void run(){held=true;if(screen.equals("apps")&&nav<0)appMenu();}};
    final Runnable clockTick=new Runnable(){public void run(){if(active){terminal.invalidate();handler.postDelayed(this,60000-System.currentTimeMillis()%60000);}}};
    final Runnable enterIdle=new Runnable(){public void run(){
        AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        if(active&&prefs.getBoolean("terminalIdle",true)&&LauncherLogic.canIdle(audio.isMusicActive(),keyboardOpen,dialogOpen)) showIdle();
        else if(active) scheduleIdle();
    }};
    final BroadcastReceiver packages=new BroadcastReceiver(){public void onReceive(Context c,Intent i){refreshCatalog();}};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("terminal",MODE_PRIVATE);layout=prefs.getInt("layout",0);favoritesOnly=prefs.getBoolean("favoritesTab",false);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(9,9,10));
        terminal=new TerminalView(this);root.addView(terminal,new FrameLayout.LayoutParams(-1,-1));
        query=new EditText(this);query.setSingleLine(true);query.setTypeface(terminal.font);query.setTextColor(0xffd3c5b3);
        query.setHintTextColor(0xff747172);query.setHint("Search apps");query.setPadding(0,0,0,0);query.setBackgroundColor(0xff121012);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH|EditorInfo.IME_FLAG_NO_EXTRACT_UI);query.setVisibility(View.GONE);
        root.addView(query);setContentView(root);terminal.requestFocus();
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->positionQuery());
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){selected=0;filter(null);}public void afterTextChanged(Editable e){}});
        query.setOnEditorActionListener((v,action,event)->{hideKeyboard();terminal.requestFocus();return true;});
        query.setOnKeyListener((v,key,event)->{
            if(event.getAction()==KeyEvent.ACTION_DOWN&&(key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_BACK)) {hideKeyboard();terminal.requestFocus();return true;}return false;
        });
        all.addAll(AppCatalog.read(prefs.getString("catalog","[]")));sortCatalog();
        filter(prefs.getString("selectedPkg",null));
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_PACKAGE_ADDED);filter.addAction(Intent.ACTION_PACKAGE_REMOVED);filter.addAction(Intent.ACTION_PACKAGE_CHANGED);filter.addDataScheme("package");
        if(Build.VERSION.SDK_INT>=33)registerReceiver(packages,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(packages,filter);
        refreshCatalog();
    }
    @Override protected void onResume(){super.onResume();active=true;wake();getWindow().getDecorView().setSystemUiVisibility(5894);handler.removeCallbacks(clockTick);handler.post(clockTick);applyScreensaver();scheduleIdle();if(SystemClock.uptimeMillis()-lastScan>60000)refreshCatalog();}
    @Override protected void onPause(){active=false;okDown=false;wake();handler.removeCallbacks(enterIdle);handler.removeCallbacks(clockTick);handler.removeCallbacks(longPress);saveSelection();super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);worker.shutdownNow();try{unregisterReceiver(packages);}catch(Exception ignored){}super.onDestroy();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);showApps();applyScreensaver();}
    @Override public void onBackPressed(){key(KeyEvent.KEYCODE_BACK);}
    void saveSelection(){String pkg=currentPackage();if(pkg!=null)prefs.edit().putString("selectedPkg",pkg).apply();}
    String currentPackage(){return screen.equals("settings")?lastAppPkg:selected>=0&&selected<shown.size()?shown.get(selected).pkg:lastAppPkg;}
    void refreshCatalog(){
        if(scanning){rescanPending=true;return;}scanning=true;
        worker.execute(()->{ArrayList<AppCatalog.Item> result=AppCatalog.scan(this);runOnUiThread(()->{
            if(isFinishing()||isDestroyed())return;
            String preserve=currentPackage();all.clear();all.addAll(result);sortCatalog();
            prefs.edit().putString("catalog",AppCatalog.cache(all)).apply();scanning=false;lastScan=SystemClock.uptimeMillis();filter(preserve);if(rescanPending){rescanPending=false;refreshCatalog();}
        });});
    }
    int rank(AppCatalog.Item item){
        String[] pinned=prefs.getString("pinned","").split("\\|");
        for(int n=0;n<pinned.length;n++)if(pinned[n].equals(item.pkg))return n-100;
        return LauncherLogic.priority(item.pkg);
    }
    void sortCatalog(){Collections.sort(all,(a,b)->{int order=prefs.getBoolean("alphabetical",false)?0:Integer.compare(rank(a),rank(b));return order!=0?order:a.label.compareToIgnoreCase(b.label);});}
    void filter(String preserve){
        shown.clear();String search=screen.equals("search")?query.getText().toString():"";
        String favorites=prefs.getString("favorites","");
        for(AppCatalog.Item item:all)if(LauncherLogic.visibleInCollection(favoritesOnly,favorites,item.pkg)&&LauncherLogic.matches(item.label,item.pkg,search))shown.add(item);
        int match=-1;if(preserve!=null)for(int n=0;n<shown.size();n++)if(shown.get(n).pkg.equals(preserve)){match=n;break;}
        selected=LauncherLogic.selectionAfterRefresh(screen.equals("settings"),selected,screen.equals("settings")?SETTING_NAMES.length:shown.size(),match);
        update(false);
    }
    void positionQuery(){
        if(query==null||terminal==null)return;float s=terminal.scale();
        float x=layout==1?425:layout==2?110:80,y=layout==1?125:layout==2?315:230,width=layout==1?1395:layout==2?1700:1760;
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(Math.round(width*s),Math.round(70*s));
        p.leftMargin=Math.round(terminal.offsetX()+x*s);p.topMargin=Math.round(terminal.offsetY()+y*s);
        query.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,36*s);query.setLayoutParams(p);
    }
    void showSearch(){wake();screen="search";favoritesOnly=false;nav=-1;selected=0;query.setVisibility(View.VISIBLE);positionQuery();query.requestFocus();keyboardOpen=true;filter(null);query.post(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(query,InputMethodManager.SHOW_IMPLICIT));scheduleIdle();}
    void hideKeyboard(){keyboardOpen=false;((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(),0);if(!screen.equals("search"))query.setVisibility(View.GONE);}
    void showApps(){showCollection(prefs.getBoolean("favoritesTab",false));}
    void showCollection(boolean favorites){String preserve=currentPackage();wake();screen="apps";favoritesOnly=favorites;prefs.edit().putBoolean("favoritesTab",favorites).apply();nav=-1;hideKeyboard();query.setVisibility(View.GONE);filter(preserve);terminal.requestFocus();}
    void showSettings(){wake();screen="settings";nav=-1;selected=0;hideKeyboard();query.setVisibility(View.GONE);terminal.requestFocus();update(false);}
    void scheduleIdle(){handler.removeCallbacks(enterIdle);int minutes=prefs.getInt("idleMinutes",3);if(active&&minutes>0&&!idle)handler.postDelayed(enterIdle,minutes*60000L);}
    void showIdle(){
        if(idle)return;hideKeyboard();query.setVisibility(View.GONE);idle=true;handler.removeCallbacks(enterIdle);
        terminal.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        idleView=new IdleView(this);root.addView(idleView,new FrameLayout.LayoutParams(-1,-1));idleView.requestFocus();
    }
    void wake(){if(idle){idle=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(idleView!=null)root.removeView(idleView);idleView=null;terminal.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);terminal.requestFocus();if(screen.equals("search"))query.setVisibility(View.VISIBLE);}scheduleIdle();}
    void update(boolean animate){if(!screen.equals("settings")&&selected<shown.size())lastAppPkg=shown.get(selected).pkg;terminal.selectionChanged(animate);terminal.setContentDescription(screen+", "+selectionLabel());terminal.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);}
    String selectionLabel(){if(nav>=0)return navLabels()[Math.min(nav,navLabels().length-1)];if(screen.equals("settings"))return SETTING_NAMES[selected]+", "+settingValue(selected);return shown.isEmpty()?"No matching apps":shown.get(selected).label;}
    String[] navLabels(){return layout==1?new String[]{"Favorites","All apps","Search","Settings","Store"}:new String[]{"Favorites","Apps","Search","Settings","Store"};}
    void navigate(int index){
        if(index==0||index==1)showCollection(index==0);else if(index==2)showSearch();else if(index==3)showSettings();else openStore();
    }
    void openStore(){for(AppCatalog.Item item:all)if(item.pkg.equals("com.android.vending")){launch(item);return;}Toast.makeText(this,"Play Store is unavailable",Toast.LENGTH_SHORT).show();}
    boolean key(int key){
        scheduleIdle();
        if(key==KeyEvent.KEYCODE_BACK){if(!screen.equals("apps")||nav>=0){showApps();}else {selected=0;update(true);}return true;}
        if(key==KeyEvent.KEYCODE_SEARCH){showSearch();return true;}
        if(key==KeyEvent.KEYCODE_MENU){if(screen.equals("apps"))appMenu();return true;}
        boolean up=key==KeyEvent.KEYCODE_DPAD_UP,down=key==KeyEvent.KEYCODE_DPAD_DOWN,left=key==KeyEvent.KEYCODE_DPAD_LEFT,right=key==KeyEvent.KEYCODE_DPAD_RIGHT;
        if(!(up||down||left||right))return false;
        boolean sidebar=LauncherLogic.sidebar(layout);
        if(nav>=0){
            if(sidebar){if(right){nav=-1;}else if(up)nav=Math.max(0,nav-1);else if(down)nav=Math.min(navLabels().length-1,nav+1);}
            else {if(up)nav=-1;else if(left)nav=Math.max(0,nav-1);else if(right)nav=Math.min(navLabels().length-1,nav+1);}
            update(true);return true;
        }
        if(screen.equals("settings")){
            if(up)selected=Math.max(0,selected-1);
            if(down){if(selected==SETTING_NAMES.length-1)nav=3;else selected++;}
            if(left||right)nav=LauncherLogic.activeTab(screen,favoritesOnly);
        }else{
            boolean grid=layout==2&&!screen.equals("search");
            if(left||right){int target=LauncherLogic.horizontalTarget(layout,screen,selected,shown.size(),left?-1:1);if(target<0)nav=LauncherLogic.activeTab(screen,favoritesOnly);else selected=target;}
            if(up)selected=LauncherLogic.move(selected,-1,shown.size(),grid);
            if(down){int next=LauncherLogic.move(selected,1,shown.size(),grid);if(next==selected)nav=LauncherLogic.activeTab(screen,favoritesOnly);else selected=next;}
        }
        update(true);return true;
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        int key=event.getKeyCode();
        if(key==KeyEvent.KEYCODE_VOLUME_UP||key==KeyEvent.KEYCODE_VOLUME_DOWN||key==KeyEvent.KEYCODE_VOLUME_MUTE||key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE||key==KeyEvent.KEYCODE_MEDIA_PLAY||key==KeyEvent.KEYCODE_MEDIA_PAUSE||key==KeyEvent.KEYCODE_MEDIA_NEXT||key==KeyEvent.KEYCODE_MEDIA_PREVIOUS||key==KeyEvent.KEYCODE_MEDIA_STOP||key==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD||key==KeyEvent.KEYCODE_MEDIA_REWIND||key==KeyEvent.KEYCODE_HEADSETHOOK)return super.dispatchKeyEvent(event);
        if(idle){if(event.getAction()==KeyEvent.ACTION_DOWN){wakeKey=key;wake();}return true;}
        if(wakeKey==key){if(event.getAction()==KeyEvent.ACTION_UP)wakeKey=-1;return true;}
        if(query.hasFocus())return super.dispatchKeyEvent(event);
        boolean ok=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER;
        if(ok){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){okDown=true;held=false;handler.postDelayed(longPress,600);}
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()>0&&okDown&&!held){handler.removeCallbacks(longPress);longPress.run();}
            if(event.getAction()==KeyEvent.ACTION_UP){handler.removeCallbacks(longPress);boolean activate=okDown&&!held;okDown=false;if(activate)activate();}
            return true;
        }
        if(event.getAction()==KeyEvent.ACTION_DOWN&&key(key))return true;
        return super.dispatchKeyEvent(event);
    }
    void activate(){scheduleIdle();if(nav>=0){navigate(nav);return;}if(screen.equals("settings")){changeSetting(1);return;}if(!shown.isEmpty())launch(shown.get(selected));}
    void launch(AppCatalog.Item item){
        try{saveSelection();Intent intent=Intent.makeMainActivity(ComponentName.unflattenFromString(item.component));intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);startActivity(intent);}catch(Exception e){Toast.makeText(this,"App is unavailable",Toast.LENGTH_SHORT).show();refreshCatalog();}
    }
    String settingValue(int index) {
        switch(index) {
            case 0:return LAYOUTS[layout];
            case 1:int minutes=prefs.getInt("idleMinutes",3);return minutes==0?"Off":minutes+" min";
            case 2:return prefs.getBoolean("terminalIdle",true)?"Terminal":"Google Ambient";
            case 4:return prefs.getBoolean("alphabetical",false)?"Alphabetical":"Pinned first";
            case 6:return prefs.getBoolean("texture",true)?"On":"Off";
            default:return "";
        }
    }
    void changeSetting(int direction) {
        if(selected==0){layout=(layout+direction+3)%3;prefs.edit().putInt("layout",layout).apply();}
        else if(selected==1){
            int minutes=prefs.getInt("idleMinutes",3),index=0;
            for(int n=0;n<IDLE_MINUTES.length;n++)if(IDLE_MINUTES[n]==minutes)index=n;
            index=(index+direction+IDLE_MINUTES.length)%IDLE_MINUTES.length;
            prefs.edit().putInt("idleMinutes",IDLE_MINUTES[index]).apply();scheduleIdle();
        }
        else if(selected==2){boolean terminalMode=!prefs.getBoolean("terminalIdle",true);prefs.edit().putBoolean("terminalIdle",terminalMode).apply();applyScreensaver();scheduleIdle();}
        else if(selected==3){showIdle();return;}
        else if(selected==4){prefs.edit().putBoolean("alphabetical",!prefs.getBoolean("alphabetical",false)).apply();sortCatalog();}
        else if(selected==5){startActivity(new Intent(Settings.ACTION_SETTINGS));}
        else if(selected==6){prefs.edit().putBoolean("texture",!prefs.getBoolean("texture",true)).apply();}
        positionQuery();update(false);
    }
    boolean setDream(String component){try{return Settings.Secure.putString(getContentResolver(),"screensaver_components",component);}catch(SecurityException e){Toast.makeText(this,"Screensaver setting is unavailable",Toast.LENGTH_SHORT).show();return false;}}
    void applyScreensaver(){
        if(checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")!=PackageManager.PERMISSION_GRANTED)return;
        boolean home=false;
        if(LauncherLogic.hasHomeRoleApi(Build.VERSION.SDK_INT)){RoleManager roles=(RoleManager)getSystemService(ROLE_SERVICE);home=roles!=null&&roles.isRoleHeld(RoleManager.ROLE_HOME);}
        else {android.content.pm.ResolveInfo result=getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);home=result!=null&&result.activityInfo!=null&&getPackageName().equals(result.activityInfo.packageName);}
        if(home)setDream(prefs.getBoolean("terminalIdle",true)?TERMINAL_DREAM:GOOGLE_DREAM);
    }
    void appMenu(){
        if(shown.isEmpty()||!screen.equals("apps"))return;
        final AppCatalog.Item item=shown.get(selected);dialogOpen=true;
        final boolean pinned=Arrays.asList(prefs.getString("pinned","").split("\\|")).contains(item.pkg);
        final boolean favorite=LauncherLogic.isFavorite(prefs.getString("favorites",""),item.pkg);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(item.label).setItems(new String[]{favorite?"Remove from favorites":"Add to favorites",pinned?"Unpin":"Pin first","App info"},(d,which)->{
            if(which==0){prefs.edit().putString("favorites",LauncherLogic.toggleFavorite(prefs.getString("favorites",""),item.pkg)).apply();filter(item.pkg);}
            else if(which==1){ArrayList<String> pins=new ArrayList<>(Arrays.asList(prefs.getString("pinned","").split("\\|")));pins.remove("");pins.remove(item.pkg);if(!pinned)pins.add(0,item.pkg);prefs.edit().putString("pinned",TextUtils.join("|",pins)).apply();sortCatalog();filter(item.pkg);}
            else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+item.pkg)));
        }).create();dialog.setOnDismissListener(d->{dialogOpen=false;scheduleIdle();});dialog.show();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xff151214));
    }
}
