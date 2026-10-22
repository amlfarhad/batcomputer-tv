package io.github.amlfarhad.batcomputertv;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;

final class IdleView extends View {
    final Paint paint = new Paint(3);
    final Typeface font;
    final Bitmap emblem;
    final Handler timer = new Handler();
    final TerminalTexture texture = new TerminalTexture();
    private final Runnable tick = new Runnable() {
        public void run() { invalidate(); timer.postDelayed(this, 60000 - System.currentTimeMillis() % 60000); }
    };
    IdleView(Context context) {
        super(context);
        font = Typeface.createFromAsset(context.getAssets(), "fonts/JetBrainsMono-Regular.ttf");
        try (java.io.InputStream input=context.getAssets().open("bat-emblem.png")) {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
            emblem=BitmapFactory.decodeStream(input,null,options);
        } catch(java.io.IOException error) { throw new IllegalStateException("Missing source logo crop",error); }
        setFocusable(true);
        setContentDescription("Idle");
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); timer.post(tick); }
    @Override protected void onDetachedFromWindow() { timer.removeCallbacksAndMessages(null); super.onDetachedFromWindow(); }
    private void text(Canvas c, String text, float x, float y, float size, int color, boolean center) {
        paint.setShader(null);paint.setColor(color);paint.setTextSize(size);paint.setTypeface(font);
        paint.setTextAlign(center ? Paint.Align.CENTER : Paint.Align.LEFT);TerminalTypography.draw(c,paint,text,x,y,color,getContext().getSharedPreferences("terminal",Context.MODE_PRIVATE).getBoolean("texture",true));
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(9,9,10));
        float scale=Math.min(getWidth()/1920f,getHeight()/1080f);
        canvas.save();canvas.translate((getWidth()-1920*scale)/2,(getHeight()-1080*scale)/2);canvas.scale(scale,scale);
        paint.setShader(null);paint.setStrokeWidth(1.2f);paint.setColor(0xff303032);
        canvas.drawLine(60,80,1860,80,paint);canvas.drawLine(60,986,1860,986,paint);
        int minute=(int)(System.currentTimeMillis()/60000);
        float dx=(minute%7-3)*1.2f,dy=((minute/7)%5-2)*1.2f;
        if(emblem!=null) {
            float width=840,height=LauncherLogic.scaledHeight(width,emblem.getWidth(),emblem.getHeight());
            paint.setStyle(Paint.Style.FILL);paint.setShader(null);paint.setColor(Color.WHITE);
            canvas.drawBitmap(emblem,null,new RectF(960-width/2+dx,320+dy,960+width/2+dx,320+height+dy),paint);
        }
        Date now=new Date();
        String clock=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(now);
        text(canvas,clock,960+dx,702+dy,80,0xffd3c5b3,true);
        text(canvas,new SimpleDateFormat("EEE dd MMM",Locale.getDefault()).format(now).toUpperCase(Locale.getDefault()),960+dx,752+dy,27,0xffb9b5b3,true);
        text(canvas,"display: idle",60,922,25,0xffa6a2a1,false);
        text(canvas,">",60,961,25,0xffa1323b,false);
        text(canvas,"awaiting input_",92,961,25,0xffbcb8b6,false);
        paint.setColor(0xff80212a);canvas.drawRect(60,1010,112,1015,paint);
        canvas.restore();if(getContext().getSharedPreferences("terminal",Context.MODE_PRIVATE).getBoolean("texture",true))texture.draw(canvas);
    }
}
