package io.github.amlfarhad.batcomputertv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Entry point for the Android TV app list. */
public final class EntryActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
