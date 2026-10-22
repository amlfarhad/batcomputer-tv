package io.github.amlfarhad.batcomputertv;

import android.content.*;
import android.content.pm.*;
import org.json.*;
import java.util.*;

final class AppCatalog {
    static final class Item {
        final String label, pkg, component, category;
        Item(String label,String pkg,String component,String category) {this.label=label;this.pkg=pkg;this.component=component;this.category=category;}
    }
    static ArrayList<Item> scan(Context context) {
        PackageManager pm=context.getPackageManager();LinkedHashMap<String,Item> found=new LinkedHashMap<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
            Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(category);
            for(ResolveInfo info:pm.queryIntentActivities(intent,0)) {
                ActivityInfo a=info.activityInfo;
                if(a==null||!a.enabled||!a.applicationInfo.enabled||a.packageName.equals(context.getPackageName())||found.containsKey(a.packageName))continue;
                String label=String.valueOf(info.loadLabel(pm));
                found.put(a.packageName,new Item(label,a.packageName,new ComponentName(a.packageName,a.name).flattenToString(),category(a.packageName,a.applicationInfo.category)));
            }
        }
        return new ArrayList<>(found.values());
    }
    static String category(String pkg,int category) {
        if(pkg.contains("browser"))return "Browser";
        if(pkg.contains("explorer"))return "Files";
        if(pkg.equals("com.android.vending"))return "Store";
        if(pkg.contains("settings"))return "System";
        switch(category) {
            case ApplicationInfo.CATEGORY_AUDIO:return "Music";
            case ApplicationInfo.CATEGORY_VIDEO:return "Video";
            case ApplicationInfo.CATEGORY_GAME:return "Games";
            case ApplicationInfo.CATEGORY_IMAGE:return "Photos";
            case ApplicationInfo.CATEGORY_NEWS:return "News";
            case ApplicationInfo.CATEGORY_SOCIAL:return "Social";
            case ApplicationInfo.CATEGORY_MAPS:return "Maps";
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:return "Tools";
            default:return "App";
        }
    }
    static String cache(List<Item> items) {
        JSONArray array=new JSONArray();
        for(Item i:items) {JSONArray row=new JSONArray();row.put(i.label);row.put(i.pkg);row.put(i.component);row.put(i.category);array.put(row);}
        return array.toString();
    }
    static ArrayList<Item> read(String cached) {
        ArrayList<Item> items=new ArrayList<>();
        try {JSONArray array=new JSONArray(cached);for(int n=0;n<array.length();n++){JSONArray row=array.getJSONArray(n);items.add(new Item(row.getString(0),row.getString(1),row.getString(2),row.getString(3)));}}catch(Exception ignored){}
        return items;
    }
}
