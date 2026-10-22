package io.github.amlfarhad.batcomputertv;

import android.service.dreams.DreamService;

public final class TerminalDream extends DreamService {
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();setInteractive(false);setFullscreen(true);setScreenBright(false);
        setContentView(new IdleView(this));
    }
}
