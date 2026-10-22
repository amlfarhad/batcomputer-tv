package io.github.amlfarhad.batcomputertv;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.*;
import android.view.animation.DecelerateInterpolator;
import java.text.SimpleDateFormat;
import java.util.*;

final class TerminalView extends View {
    static final int BG=0xff09090a,INK=0xffd3c5b3,MUTED=0xff89807a,LINE=0xff2d2a2d,RED=0xffcc4b53;
    final MainActivity host;
    final Typeface font;
    final Paint paint=new Paint(3);
    final TerminalTexture texture=new TerminalTexture();
    final ArrayList<Hit> hits=new ArrayList<>();
    final RectF moving=new RectF();
    ValueAnimator animator;
    int lastPage=-1,lastLayout=-1,accessFocus=-1;
    String lastScreen="";
    boolean wokeTouch;
    static final class Hit {
        int id,index,kind;String text;RectF rect;boolean selected;
        Hit(int id,int kind,int index,String text,RectF rect,boolean selected){this.id=id;this.kind=kind;this.index=index;this.text=text;this.rect=rect;this.selected=selected;}
    }
    TerminalView(MainActivity context){super(context);host=context;font=Typeface.createFromAsset(context.getAssets(),"fonts/JetBrainsMono-Regular.ttf");setFocusable(true);setFocusableInTouchMode(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    float scale(){return Math.min(getWidth()/1920f,getHeight()/1080f);}
    float offsetX(){return (getWidth()-1920*scale())/2;}
    float offsetY(){return (getHeight()-1080*scale())/2;}
    boolean search(){return host.screen.equals("search");}
    boolean settings(){return host.screen.equals("settings");}
    boolean directory(){return LauncherLogic.sidebar(host.layout);}
    boolean console(){return host.layout==2;}
    boolean grid(){return console()&&!search()&&!settings();}
    int visible(){return LauncherLogic.visibleRows(host.layout,host.screen);}
    int first(){return LauncherLogic.pageStart(host.selected,visible(),grid());}
    RectF rowRect(int index){
        int relative=index-first();
        if(directory()){float top=(search()?270:190)+relative*80;return new RectF(390,top,1860,top+76);}
        if(console()&&settings())return new RectF(110,330+relative*105,1810,414+relative*105);
        if(console()&&search())return new RectF(110,435+relative*90,1810,515+relative*90);
        if(settings())return new RectF(72,265+relative*100,1848,347+relative*100);
        if(search())return new RectF(80,354+relative*100,1840,436+relative*100);
        if(grid()){float x=110+(relative%2)*950,y=365+(relative/2)*180;return new RectF(x,y,x+790,y+110);}
        return new RectF(72,265+relative*100,1224,347+relative*100);
    }
    float labelWidth(String label){paint.setTypeface(font);paint.setTextSize(31);return paint.measureText(label);}
    RectF navRect(int index){if(directory())return new RectF(64,148+index*86,316,210+index*86);String[] labels=host.navLabels();float[] widths=new float[labels.length];for(int n=0;n<labels.length;n++)widths[n]=labelWidth(labels[n]);float x=LauncherLogic.tabLeft(widths,index,68);return new RectF(x,962,x+widths[index]+30,1030);}
    RectF target(){return host.nav>=0?navRect(host.nav):rowRect(host.selected);}
    void selectionChanged(boolean animate){
        RectF target=target();int page=first();
        if(animator!=null)animator.cancel();
        if(animate&&page==lastPage&&host.layout==lastLayout&&host.screen.equals(lastScreen)&&!moving.isEmpty()&&host.nav<0){
            final RectF from=new RectF(moving),to=new RectF(target);animator=ValueAnimator.ofFloat(0,1);animator.setDuration(95);animator.setInterpolator(new DecelerateInterpolator(1.4f));
            animator.addUpdateListener(a->{float t=(Float)a.getAnimatedValue();moving.set(from.left+(to.left-from.left)*t,from.top+(to.top-from.top)*t,from.right+(to.right-from.right)*t,from.bottom+(to.bottom-from.bottom)*t);invalidate();});animator.start();
        }else moving.set(target);
        lastPage=page;lastLayout=host.layout;lastScreen=host.screen;invalidate();
    }
    void text(Canvas c,String value,float x,float y,float size,int color){text(c,value,x,y,size,color,Paint.Align.LEFT);}
    void text(Canvas c,String value,float x,float y,float size,int color,Paint.Align align){paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setTypeface(font);paint.setTextSize(size);paint.setTextAlign(align);TerminalTypography.draw(c,paint,value,x,y,color,host.prefs.getBoolean("texture",true));}
    void cursor(Canvas c,String label,float x,float baseline,float size){paint.setTypeface(font);paint.setTextSize(size);float edge=x+paint.measureText(label)+17;rect(c,new RectF(edge,baseline-size*.72f,edge+7,baseline+3),0xffb84a50);}
    void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width){paint.setShader(null);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);c.drawLine(x1,y1,x2,y2,paint);paint.setStyle(Paint.Style.FILL);}
    void rect(Canvas c,RectF r,int color){paint.setShader(null);paint.setColor(color);paint.setStyle(Paint.Style.FILL);c.drawRect(r,paint);}
    String fit(String value,float size,float width){paint.setTypeface(font);paint.setTextSize(size);if(paint.measureText(value)<=width)return value;String s=value;while(s.length()>1&&paint.measureText(s+"…")>width)s=s.substring(0,s.length()-1);return s+"…";}
    void wrapped(Canvas c,String value,float x,float y,float size,int color,float width){
        paint.setTypeface(font);paint.setTextSize(size);StringBuilder row=new StringBuilder();int line=0;
        for(String word:value.split(" ")){String next=row.length()==0?word:row+" "+word;if(paint.measureText(next)>width&&row.length()>0){text(c,row.toString(),x,y+line*size*1.45f,size,color);row=new StringBuilder(word);line++;}else row=new StringBuilder(next);if(line>=2)break;}
        if(line<3)text(c,fit(row.toString(),size,width),x,y+line*size*1.45f,size,color);
    }
    String description(AppCatalog.Item item){
        return item.category;
    }
    void focus(Canvas c){
        if(host.nav>=0||(!settings()&&host.shown.isEmpty()))return;
        RectF r=moving.isEmpty()?target():moving;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(r.left,0,r.right,0,new int[]{0xff431318,0xff230d11},null,Shader.TileMode.CLAMP));c.drawRect(r,paint);paint.setShader(null);
        if(console()){
            line(c,r.left,r.top,r.left,r.bottom,RED,2);line(c,r.right,r.top,r.right,r.bottom,RED,2);
            line(c,r.left,r.top,r.left+11,r.top,RED,2);line(c,r.right-11,r.top,r.right,r.top,RED,2);
            line(c,r.left,r.bottom,r.left+11,r.bottom,RED,2);line(c,r.right-11,r.bottom,r.right,r.bottom,RED,2);
        }else rect(c,new RectF(r.left,r.top,r.left+9,r.bottom),RED);
    }
    void addRowHit(int index,String label,RectF bounds){hits.add(new Hit(settings()?500+index:100+index,settings()?1:0,index,label,new RectF(bounds),host.nav<0&&host.selected==index));}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);c.drawColor(BG);if(getWidth()==0)return;
        hits.clear();c.save();c.translate(offsetX(),offsetY());c.scale(scale(),scale());
        if(moving.isEmpty())moving.set(target());
        drawHeader(c);
        if(settings())drawSettings(c);else if(directory()&&search())drawDirectorySearch(c);else if(directory())drawDirectory(c);else if(grid())drawGrid(c);else drawList(c);
        drawNavigation(c);c.restore();if(host.prefs.getBoolean("texture",true))texture.draw(c);
    }
    void drawHeader(Canvas c){
        Date now=new Date();String clock=new SimpleDateFormat(host.layout==0?"h:mm a":"HH:mm",Locale.getDefault()).format(now);
        String path=settings()?"~/settings":search()?"~/search":host.favoritesOnly?"~/favorites":"~/applications";
        text(c,path,72,66,23,MUTED);
        text(c,clock,1848,console()?136:66,console()?52:25,0xffbcb8b6,Paint.Align.RIGHT);
        if(directory()){line(c,60,88,1860,88,LINE,1);return;}
        String heading=settings()?"Settings":search()?"Search":host.favoritesOnly?"Favorites":grid()?"Home":"Applications";
        text(c,heading,72,190,64,INK);
        if(console())line(c,72,265,1848,265,LINE,1.2f);
    }
    void drawList(Canvas c){
        focus(c);int first=first(),end=Math.min(host.shown.size(),first+visible());
        for(int i=first;i<end;i++){
            AppCatalog.Item item=host.shown.get(i);RectF r=rowRect(i);boolean active=host.nav<0&&i==host.selected;
            if(search()){text(c,fit(item.label,37,1250),r.left+48,r.top+54,37,active?INK:0xffbbb7b5);text(c,item.category,r.right-35,r.top+54,27,MUTED,Paint.Align.RIGHT);}
            else{
                text(c,String.format(Locale.ROOT,"%02d",i+1),r.left+58,r.top+54,35,active?0xffb45156:MUTED);
                String label=fit(item.label,42,800);text(c,label,r.left+173,r.top+55,42,INK);if(active)cursor(c,label,r.left+173,r.top+55,42);
                float x=r.right-63,y=r.centerY();line(c,x-7,y-12,x+5,y,active?RED:0xffaaa5a5,2.2f);line(c,x+5,y,x-7,y+12,active?RED:0xffaaa5a5,2.2f);
            }
            addRowHit(i,item.label,r);
        }
        if(!search()){
            line(c,1270,265,1270,864,LINE,1.2f);
            if(!host.shown.isEmpty()){
                AppCatalog.Item item=host.shown.get(host.selected);wrapped(c,item.label,1350,348,51,INK,480);wrapped(c,description(item),1350,476,32,MUTED,470);
            }
        }
        if(host.shown.isEmpty()&&!host.scanning)text(c,host.favoritesOnly?"No favorites":"No matching apps",search()?rowRect(0).left+48:132,search()?rowRect(0).top+54:330,34,MUTED);
        scroll(c,search()?1870:1242,search()?(console()?435:355):265,search()?(console()?875:850):865,visible());
    }
    void directoryFrame(Canvas c,String left,String right,float labelY,float ruleY){
        line(c,352,119,352,885,LINE,1);text(c,left,425,labelY,23,MUTED);text(c,right,1810,labelY,23,MUTED,Paint.Align.RIGHT);line(c,390,ruleY,1860,ruleY,LINE,1);
    }
    void drawDirectorySearch(Canvas c){
        directoryFrame(c,"APPLICATION","CATEGORY",242,260);focus(c);
        int first=first(),end=Math.min(host.shown.size(),first+visible());
        for(int i=first;i<end;i++){
            AppCatalog.Item item=host.shown.get(i);RectF r=rowRect(i);String label=fit(item.label,32,1080);
            text(c,label,425,r.top+51,32,INK);if(host.nav<0&&host.selected==i)cursor(c,label,425,r.top+51,32);
            text(c,item.category,1810,r.top+51,28,MUTED,Paint.Align.RIGHT);addRowHit(i,item.label,r);
        }
        if(host.shown.isEmpty())text(c,"No matching apps",425,322,32,MUTED);
        scroll(c,1878,270,826,visible());
    }
    void drawDirectory(Canvas c){
        text(c,"APPLICATION",425,156,23,MUTED);text(c,"CATEGORY",1810,156,23,MUTED,Paint.Align.RIGHT);
        line(c,352,119,352,885,LINE,1);line(c,390,178,1860,178,LINE,1);
        focus(c);int first=first(),end=Math.min(host.shown.size(),first+6);
        for(int i=first;i<end;i++){
            AppCatalog.Item item=host.shown.get(i);RectF r=rowRect(i);
            String label=fit(item.label,32,1080);text(c,label,425,r.top+51,32,INK);if(host.nav<0&&host.selected==i)cursor(c,label,425,r.top+51,32);text(c,item.category,1810,r.top+51,28,MUTED,Paint.Align.RIGHT);
            if(i!=host.selected)line(c,405,r.bottom,1860,r.bottom,LINE,1);addRowHit(i,item.label,r);
        }
        line(c,390,729,1860,729,LINE,1);
        if(!host.shown.isEmpty()){
            AppCatalog.Item item=host.shown.get(host.selected);text(c,fit(item.label,48,1400),425,828,48,INK);text(c,fit(description(item),29,1400),425,882,29,MUTED);
        }else text(c,host.favoritesOnly?"No favorites":"No matching apps",425,250,32,MUTED);
        scroll(c,1878,190,670,6);
    }
    void drawGrid(Canvas c){
        focus(c);int first=first(),end=Math.min(host.shown.size(),first+6);
        for(int i=first;i<end;i++){
            AppCatalog.Item item=host.shown.get(i);RectF r=rowRect(i);boolean active=host.nav<0&&host.selected==i;
            text(c,"/ "+String.format(Locale.ROOT,"%02d",i+1),r.left+3,r.top+65,28,MUTED);
            String label=fit(item.label,50,570);text(c,label,r.left+174,r.top+68,50,INK);if(active)cursor(c,label,r.left+174,r.top+68,50);
            if(active){paint.setTextSize(50);line(c,r.left+174,r.bottom-7,r.left+174+Math.min(230,paint.measureText(item.label)),r.bottom-7,RED,2);}
            addRowHit(i,item.label,r);
        }
        if(host.shown.isEmpty())text(c,host.favoritesOnly?"No favorites":"No matching apps",110,430,34,MUTED);
        scroll(c,1875,365,835,6);
    }
    void drawSettings(Canvas c){
        if(directory())directoryFrame(c,"SETTING","VALUE",156,178);
        focus(c);int first=first(),end=Math.min(MainActivity.SETTING_NAMES.length,first+visible());
        for(int i=first;i<end;i++){
            RectF r=rowRect(i);float baseline=r.top+(directory()?51:55);
            text(c,MainActivity.SETTING_NAMES[i],r.left+(directory()?35:49),baseline,directory()?32:35,INK);
            text(c,host.settingValue(i),r.right-49,baseline,30,MUTED,Paint.Align.RIGHT);
            addRowHit(i,MainActivity.SETTING_NAMES[i]+(host.settingValue(i).isEmpty()?"":", "+host.settingValue(i)),r);
        }
        RectF firstRow=rowRect(first),lastRow=rowRect(first+visible()-1);scroll(c,1878,firstRow.top,lastRow.bottom,visible());
    }
    void drawNavigation(Canvas c){
        String[] labels=host.navLabels();
        if(directory()){
            for(int i=0;i<labels.length;i++){
                RectF r=navRect(i);boolean current=host.nav==i||(host.nav<0&&i==LauncherLogic.activeTab(host.screen,host.favoritesOnly));
                text(c,labels[i],64,r.top+43,30,current?INK:MUTED);
                if(current)line(c,64,r.bottom-1,64+Math.min(215,labels[i].length()*19),r.bottom-1,RED,2.5f);
                hits.add(new Hit(10+i,2,i,labels[i],r,host.nav==i));
            }
            line(c,60,956,1860,956,LINE,1);return;
        }
        line(c,72,934,1848,934,LINE,1);
        int active=LauncherLogic.activeTab(host.screen,host.favoritesOnly);
        for(int i=0;i<labels.length;i++){
            RectF r=navRect(i);text(c,labels[i],r.left,1007,31,(host.nav==i||(host.nav<0&&i==active))?RED:MUTED);
            if(host.nav==i)line(c,r.left,1024,r.left+labelWidth(labels[i]),1024,RED,2.2f);
            hits.add(new Hit(10+i,2,i,labels[i],r,host.nav==i));
        }
    }
    void scroll(Canvas c,float x,float top,float bottom,int visible){
        int count=settings()?MainActivity.SETTING_NAMES.length:host.shown.size();if(count<=visible)return;float height=bottom-top;
        line(c,x,top,x,bottom,0xff211f21,1.5f);
        float thumb=Math.max(30,height*visible/count),position=(height-thumb)*first()/Math.max(1,count-visible);
        line(c,x,top+position,x,top+position+thumb,0xff693038,2);
    }
    void select(Hit hit,boolean activate){
        host.wake();if(hit.kind==2)host.nav=hit.index;else{host.nav=-1;host.selected=hit.index;}
        host.update(true);if(activate)host.activate();
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        if(e.getAction()==android.view.MotionEvent.ACTION_DOWN){wokeTouch=host.idle;if(wokeTouch)host.wake();return true;}
        if(e.getAction()==android.view.MotionEvent.ACTION_UP){
            if(wokeTouch){wokeTouch=false;return true;}
            float x=(e.getX()-offsetX())/scale(),y=(e.getY()-offsetY())/scale();
            for(Hit hit:new ArrayList<>(hits))if(hit.rect.contains(x,y)){select(hit,true);return true;}
        }return true;
    }
    Rect screenBounds(RectF design){int[] location=new int[2];getLocationOnScreen(location);float s=scale();return new Rect(Math.round(location[0]+offsetX()+design.left*s),Math.round(location[1]+offsetY()+design.top*s),Math.round(location[0]+offsetX()+design.right*s),Math.round(location[1]+offsetY()+design.bottom*s));}
    final AccessibilityNodeProvider nodes=new AccessibilityNodeProvider(){
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id){
            AccessibilityNodeInfo info=AccessibilityNodeInfo.obtain();info.setPackageName(host.getPackageName());info.setEnabled(true);info.setVisibleToUser(true);
            if(id==View.NO_ID){
                info.setSource(TerminalView.this);info.setClassName("android.widget.ListView");info.setContentDescription(getContentDescription());info.setBoundsInScreen(screenBounds(new RectF(0,0,1920,1080)));
                if(getParent() instanceof View)info.setParent((View)getParent());for(Hit hit:hits)info.addChild(TerminalView.this,hit.id);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);return info;
            }
            for(Hit hit:hits)if(hit.id==id){
                info.setSource(TerminalView.this,id);info.setParent(TerminalView.this);info.setClassName("android.widget.Button");info.setText(hit.text);info.setContentDescription(hit.text);info.setClickable(true);info.setFocusable(true);info.setSelected(hit.selected);info.setFocused(hit.selected);info.setAccessibilityFocused(accessFocus==id);
                info.setBoundsInScreen(screenBounds(hit.rect));float s=scale();info.setBoundsInParent(new Rect(Math.round(offsetX()+hit.rect.left*s),Math.round(offsetY()+hit.rect.top*s),Math.round(offsetX()+hit.rect.right*s),Math.round(offsetY()+hit.rect.bottom*s)));
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_FOCUS);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);return info;
            }info.recycle();return null;
        }
        @Override public boolean performAction(int id,int action,Bundle args){
            if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)return host.key(KeyEvent.KEYCODE_DPAD_DOWN);
            if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)return host.key(KeyEvent.KEYCODE_DPAD_UP);
            for(Hit hit:new ArrayList<>(hits))if(hit.id==id){
                if(action==AccessibilityNodeInfo.ACTION_CLICK){select(hit,true);return true;}
                if(action==AccessibilityNodeInfo.ACTION_FOCUS){select(hit,false);return true;}
                if(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS){accessFocus=id;AccessibilityEvent event=AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);event.setSource(TerminalView.this,id);event.setPackageName(host.getPackageName());event.setClassName("android.widget.Button");event.getText().add(hit.text);getParent().requestSendAccessibilityEvent(TerminalView.this,event);return true;}
                if(action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS){accessFocus=-1;return true;}
            }return false;
        }
    };
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider(){return nodes;}
}
