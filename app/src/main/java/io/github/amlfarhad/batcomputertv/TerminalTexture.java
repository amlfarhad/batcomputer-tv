package io.github.amlfarhad.batcomputertv;

import android.graphics.*;

/** Static, tiny repeating texture: one GPU fill, no timer or animated noise. */
final class TerminalTexture {
    private static final Bitmap TILE=tile();
    private final Paint paint=new Paint();
    TerminalTexture(){paint.setShader(new BitmapShader(TILE,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT));}
    private static Bitmap tile(){
        int side=128,seed=0x426174;
        int[] pixels=new int[side*side];
        for(int y=0;y<side;y++)for(int x=0;x<side;x++){
            seed=1664525*seed+1013904223;
            int shade=LauncherLogic.scanlineAlpha(y);
            pixels[y*side+x]=shade>0?Color.argb(shade,0,0,0):Color.argb((seed>>>28)%5,230,220,213);
        }
        return Bitmap.createBitmap(pixels,side,side,Bitmap.Config.ARGB_8888);
    }
    void draw(Canvas canvas){canvas.drawPaint(paint);}
}
