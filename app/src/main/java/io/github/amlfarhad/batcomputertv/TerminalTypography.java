package io.github.amlfarhad.batcomputertv;

import android.graphics.*;

/** Phosphor edge drawn with cached glyphs; no blur, layer bitmap or animation. */
final class TerminalTypography {
    static void draw(Canvas canvas,Paint paint,String value,float x,float y,int color,boolean finish){
        if(finish){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.4f);
            paint.setColor((color&0x00ffffff)|0x36000000);canvas.drawText(value,x,y,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor((color&0x00ffffff)|0x3c000000);
            canvas.drawText(value,x+.8f,y+.4f,paint);
        }
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);canvas.drawText(value,x,y,paint);
    }
}
