package io.github.amlfarhad.batcomputertv;

import java.util.*;

public final class LauncherLogicTest {
    private static int assertions;
    private static void equal(Object expected, Object actual, String message) {
        assertions++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) {
        equal(1, LauncherLogic.move(0, 1, 21, false), "Down selects the next app");
        equal(20, LauncherLogic.move(20, 1, 21, false), "Down clamps at last app");
        equal(0, LauncherLogic.move(0, -1, 21, false), "Up clamps at first app");
        equal(2, LauncherLogic.move(0, 1, 21, true), "Grid Down moves one row");
        equal(20, LauncherLogic.move(19, 1, 21, true), "Odd final grid row remains reachable");
        equal(0, LauncherLogic.move(0, 1, 0, true), "Empty catalog has safe index");
        equal(1, LauncherLogic.horizontal(0, 1, 7), "Grid Right moves to adjacent column");
        equal(1, LauncherLogic.horizontal(1, 1, 7), "Grid Right stays in row");
        equal(6, LauncherLogic.horizontal(6, 1, 7), "Odd last grid tile stays reachable");
        equal(0, LauncherLogic.pageStart(5, 6, false), "First six rows stay visible");
        equal(1, LauncherLogic.pageStart(6, 6, false), "Seventh app scrolls into view");
        equal(2, LauncherLogic.pageStart(6, 6, true), "Grid scrolls by full row");
        equal(16, LauncherLogic.pageStart(20, 6, true), "Last app is visible in grid");
        equal(true, LauncherLogic.matches("Example Music", "org.example.music", "music"), "Search matches app label");
        equal(true, LauncherLogic.matches("Example Music", "org.example.music", "EXAMPLE"), "Search matches package case-insensitively");
        equal(false, LauncherLogic.matches("Example Music", "org.example.music", "netflix"), "Search excludes unrelated apps");
        equal(true, LauncherLogic.matches("Example Music", "org.example.music", "  "), "Empty query shows every app");
        equal(100, LauncherLogic.priority("org.smarttube.stable"), "Installed apps have no preset priority");
        equal(100, LauncherLogic.priority("org.example.music"), "Favorites are selected by the user");
        equal(100, LauncherLogic.priority("other.package"), "Other apps sort after favorites");
        equal(false, LauncherLogic.canIdle(true, false, false), "Do not idle while media is playing");
        equal(false, LauncherLogic.canIdle(false, true, false), "Do not idle with keyboard open");
        equal(false, LauncherLogic.canIdle(false, false, true), "Do not idle while a dialog is open");
        equal(true, LauncherLogic.canIdle(false, false, false), "Quiet launcher may enter idle");
        equal(3, LauncherLogic.selectionAfterRefresh(true,3,8,20), "Catalog reorder cannot change a settings row into an app index");
        equal(7, LauncherLogic.selectionAfterRefresh(true,7,8,2), "Settings selection survives app removal");
        equal(20, LauncherLogic.selectionAfterRefresh(false,3,25,20), "App selection follows package identity after reorder");
        equal(2, LauncherLogic.selectionAfterRefresh(false,20,3,-1), "App removal clamps selection to remaining catalog");
        equal(0, LauncherLogic.selectionAfterRefresh(false,20,0,-1), "Empty catalog refresh remains safe");
        equal(false, LauncherLogic.isFavorite("", "com.netflix.ninja"), "Favorites start empty");
        equal(true, LauncherLogic.isFavorite("com.netflix.ninja|org.example.music", "org.example.music"), "Favorites match exact package identity");
        equal(false, LauncherLogic.isFavorite("org.smarttube.beta", "org.smarttube"), "Favorite matching never uses package prefixes");
        equal("org.example.music", LauncherLogic.toggleFavorite("", "org.example.music"), "First favorite is stored");
        equal("org.example.music|com.netflix.ninja", LauncherLogic.toggleFavorite("org.example.music", "com.netflix.ninja"), "New favorite preserves existing choices");
        equal("com.netflix.ninja", LauncherLogic.toggleFavorite("org.example.music|com.netflix.ninja", "org.example.music"), "Removing favorite preserves others");
        equal("", LauncherLogic.toggleFavorite("org.example.music", "org.example.music"), "Last favorite can be removed");
        equal(true, LauncherLogic.visibleInCollection(true,"org.example.music","org.example.music"), "Favorite collection includes selected favorite");
        equal(false, LauncherLogic.visibleInCollection(true,"","com.stremio.one"), "Default preferred apps are not implicitly favorites");
        equal(true, LauncherLogic.visibleInCollection(false,"","com.stremio.one"), "All apps includes nonfavorites");
        equal(72f, LauncherLogic.tabLeft(new float[]{167,74,112,149,93},0,68), "First tab aligns with page margin");
        equal(307f, LauncherLogic.tabLeft(new float[]{167,74,112,149,93},1,68), "Second tab follows measured label and fixed gap");
        equal(846f, LauncherLogic.tabLeft(new float[]{167,74,112,149,93},4,68), "Store follows the same spacing rhythm");
        equal(false, LauncherLogic.hasHomeRoleApi(26), "Android 8 cannot call Android 10 HOME role API");
        equal(true, LauncherLogic.hasHomeRoleApi(29), "Android 10 supports HOME role API");
        equal(78, LauncherLogic.scanlineAlpha(3), "CRT dark line is visible at TV distance");
        equal(24, LauncherLogic.scanlineAlpha(2), "CRT line has a softer neighboring row");
        equal(0, LauncherLogic.scanlineAlpha(1), "CRT leaves clear rows for readable glyphs");
        equal(78, LauncherLogic.scanlineAlpha(7), "CRT tile repeats without a seam");
        equal(330f, LauncherLogic.scaledHeight(876,876,330), "Source crop preserves its exact aspect ratio at original size");
        equal(165f, LauncherLogic.scaledHeight(438,876,330), "Logo scales uniformly in both dimensions");
        equal(true, LauncherLogic.sidebar(1), "File Browser keeps its sidebar on every screen");
        equal(false, LauncherLogic.sidebar(0), "Command Line uses bottom tabs");
        equal(8, LauncherLogic.visibleRows(1,"settings"), "File Browser settings use a compact table");
        equal(7, LauncherLogic.visibleRows(1,"search"), "File Browser search fits its own table");
        equal(5, LauncherLogic.visibleRows(2,"settings"), "Quiet Console settings retain spacious rows");
        equal(-1, LauncherLogic.horizontalTarget(0,"settings",0,10,1), "Right on any setting focuses tabs without editing values");
        equal(-1, LauncherLogic.horizontalTarget(0,"apps",16,27,1), "Right opens tabs from any list row");
        equal(-1, LauncherLogic.horizontalTarget(1,"search",4,12,-1), "Left opens the sidebar from search results");
        equal(1, LauncherLogic.horizontalTarget(2,"apps",0,8,1), "Console app grid retains adjacent-column navigation");
        equal(-1, LauncherLogic.horizontalTarget(2,"apps",1,8,1), "Console grid edge opens tabs");
        equal(-1, LauncherLogic.horizontalTarget(2,"settings",2,10,-1), "Console setting Left opens tabs instead of changing value");
        equal(3, LauncherLogic.activeTab("settings",false), "Settings navigation focuses Settings tab");
        equal(2, LauncherLogic.activeTab("search",false), "Search navigation focuses Search tab");
        equal(0, LauncherLogic.activeTab("apps",true), "Favorites navigation focuses Favorites tab");
        equal(1, LauncherLogic.activeTab("apps",false), "All apps navigation focuses Apps tab");
        System.out.println(assertions + " launcher logic assertions passed");
    }
}
