package io.github.amlfarhad.batcomputertv;

import java.util.Locale;

final class LauncherLogic {
    static boolean sidebar(int layout) { return layout==1; }
    static int visibleRows(int layout,String screen) {
        if(screen.equals("settings"))return layout==1?8:layout==2?5:6;
        if(screen.equals("search"))return layout==1?7:5;
        return 6;
    }
    static int horizontalTarget(int layout,String screen,int selected,int count,int direction) {
        if(layout==2&&screen.equals("apps")){
            int target=horizontal(selected,direction,count);if(target!=selected)return target;
        }
        return -1;
    }
    static int activeTab(String screen,boolean favorites) { return screen.equals("settings")?3:screen.equals("search")?2:favorites?0:1; }
    static float scaledHeight(float width,int sourceWidth,int sourceHeight) { return width*sourceHeight/sourceWidth; }
    static boolean hasHomeRoleApi(int api) { return api>=29; }
    static int scanlineAlpha(int row) { return row%4==3?78:row%4==2?24:0; }
    static float tabLeft(float[] widths,int index,float gap) { float x=72;for(int n=0;n<Math.min(index,widths.length);n++)x+=widths[n]+gap;return x; }
    static boolean isFavorite(String stored,String packageName) {
        for(String member:stored.split("\\|"))if(member.equals(packageName))return true;
        return false;
    }
    static String toggleFavorite(String stored,String packageName) {
        java.util.LinkedHashSet<String> members=new java.util.LinkedHashSet<>();
        for(String member:stored.split("\\|"))if(!member.isEmpty())members.add(member);
        if(!members.remove(packageName))members.add(packageName);
        return String.join("|",members);
    }
    static boolean visibleInCollection(boolean favoritesOnly,String stored,String packageName) {
        return !favoritesOnly||isFavorite(stored,packageName);
    }
    static int move(int index, int direction, int count, boolean grid) {
        return count == 0 ? 0 : Math.max(0, Math.min(count - 1, index + direction * (grid ? 2 : 1)));
    }
    static int horizontal(int index, int direction, int count) {
        if (count == 0) return 0;
        if (direction > 0 && index % 2 == 0 && index + 1 < count) return index + 1;
        if (direction < 0 && index % 2 == 1) return index - 1;
        return index;
    }
    static int pageStart(int index, int visible, boolean grid) {
        return grid ? Math.max(0, index / 2 - visible / 2 + 1) * 2 : Math.max(0, index - visible + 1);
    }
    static boolean matches(String label, String packageName, String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return label.toLowerCase(Locale.ROOT).contains(needle) || packageName.toLowerCase(Locale.ROOT).contains(needle);
    }
    static int priority(String packageName) {
        return 100;
    }
    static boolean canIdle(boolean playing, boolean keyboard, boolean dialog) {
        return !playing && !keyboard && !dialog;
    }
    static int selectionAfterRefresh(boolean settings, int selected, int count, int matchingAppIndex) {
        int desired=settings||matchingAppIndex<0?selected:matchingAppIndex;
        return Math.max(0,Math.min(Math.max(0,count-1),desired));
    }
}
