package app.emurph.tv;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private ExoPlayer player;
    private WebView web;
    private String activeProfile = "";
    private final String defaultRadioUrl = "http://34.26.99.249:8000/emurph";
    private final String defaultServerUrl = "http://limited-name.com:80";
    private final String remoteConfigUrl = "http://34.26.99.249:8080/tv_config.json";

    // ── Brush-style logo SVG (inline, no external font needed) ──────────────
    // EMURPH in bold white brush lettering + TV in red with antenna
    private static final String LOGO_SVG =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 340 72' width='340' height='72'>" +
        // EMURPH text – bold white brush-style via thick strokes
        "<text x='4' y='56' font-family='Arial Black,Impact,sans-serif' font-weight='900' " +
        "font-size='54' fill='white' letter-spacing='-1' " +
        "style='text-shadow:0 0 12px rgba(255,255,255,0.4)'>EMURPH</text>" +
        // TV box with antenna
        "<g transform='translate(228,4)'>" +
        // antenna lines
        "<line x1='32' y1='0' x2='22' y2='14' stroke='white' stroke-width='3' stroke-linecap='round'/>" +
        "<line x1='32' y1='0' x2='42' y2='14' stroke='white' stroke-width='3' stroke-linecap='round'/>" +
        "<circle cx='32' cy='0' r='3' fill='white'/>" +
        // TV text in red
        "<text x='0' y='58' font-family='Arial Black,Impact,sans-serif' font-weight='900' " +
        "font-size='54' fill='#ef1a3a' letter-spacing='-1'>TV</text>" +
        "</g>" +
        "</svg>";

    // Compact logo for screens (smaller)
    private static final String LOGO_SVG_SM =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 240 52' width='240' height='52'>" +
        "<text x='3' y='40' font-family='Arial Black,Impact,sans-serif' font-weight='900' " +
        "font-size='38' fill='white' letter-spacing='-1'>EMURPH</text>" +
        "<g transform='translate(162,2)'>" +
        "<line x1='22' y1='0' x2='15' y2='10' stroke='white' stroke-width='2.5' stroke-linecap='round'/>" +
        "<line x1='22' y1='0' x2='29' y2='10' stroke='white' stroke-width='2.5' stroke-linecap='round'/>" +
        "<circle cx='22' cy='0' r='2.5' fill='white'/>" +
        "<text x='0' y='40' font-family='Arial Black,Impact,sans-serif' font-weight='900' " +
        "font-size='38' fill='#ef1a3a' letter-spacing='-1'>TV</text>" +
        "</g>" +
        "</svg>";

    // Large brand lockup for footer section
    private static final String LOGO_SVG_LG =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 460 96' width='460' height='96'>" +
        "<text x='4' y='74' font-family='Arial Black,Impact,sans-serif' font-weight='900' " +
        "font-size='72' fill='white' letter-spacing='-2'>EMURPH</text>" +
        "<g transform='translate(308,4)'>" +
        "<line x1='42' y1='0' x2='28' y2='18' stroke='white' stroke-width='4' stroke-linecap='round'/>" +
        "<line x1='42' y1='0' x2='56' y2='18' stroke='white' stroke-width='4' stroke-linecap='round'/>" +
        "<circle cx='42' cy='0' r='4' fill='white'/>" +
        "<text x='0' y='74' font-family='Arial Black,Impact,sans-serif' font-weight='900' " +
        "font-size='72' fill='#ef1a3a' letter-spacing='-2'>TV</text>" +
        "</g>" +
        "</svg>";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs = getSharedPreferences("emurph_tv", MODE_PRIVATE);
        activeProfile = prefs.getString("active_profile", "");
        if (!prefs.contains("default_server_url")) prefs.edit().putString("default_server_url", defaultServerUrl).apply();
        if (!prefs.contains("radio_url")) prefs.edit().putString("radio_url", defaultRadioUrl).apply();
        showHome();
        new Thread(this::refreshRemoteConfig).start();
    }

    @Override protected void onDestroy() { releasePlayer(); super.onDestroy(); }

    private void showHome() {
        releasePlayer();
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(2,5,16));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "EMurph");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.loadDataWithBaseURL(null, homeHtml(), "text/html", "UTF-8", null);
        setContentView(web);
        web.requestFocus();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }

    private String homeHtml() {
        String user = activeProfile.isEmpty() ? "No user" : esc(activeProfile);
        String exp = esc(expiration());
        String announce = "";
        if (prefs.getBoolean("announcement_enabled", false)) {
            announce = "<div class='announce'><b>"+esc(prefs.getString("announcement_title",""))+"</b> "+esc(prefs.getString("announcement_message",""))+"</div>";
        }

        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>" +
        // ── Reset & base ──────────────────────────────────────────────────────
        "*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;color:#fff;font-family:'Arial Black',Arial,sans-serif;background:#020611}" +
        // ── Cinematic background ──────────────────────────────────────────────
        "body:before{content:'';position:fixed;inset:0;" +
        "background:radial-gradient(ellipse at 10% 80%,rgba(18,80,220,.32),transparent 38%)," +
        "radial-gradient(ellipse at 90% 72%,rgba(220,28,72,.22),transparent 32%)," +
        "radial-gradient(ellipse at 50% 50%,rgba(6,18,52,.9),transparent 70%)," +
        "linear-gradient(180deg,#030818 0%,#020510 100%);z-index:-4}" +
        // ── Microphone silhouette (right side, subtle) ────────────────────────
        ".micbg{position:fixed;right:-40px;top:20px;width:260px;height:580px;opacity:.13;z-index:-2}" +
        ".micbg:before{content:'';position:absolute;right:80px;top:0;width:88px;height:240px;" +
        "border:10px solid #ff315d;border-radius:48px}" +
        ".micbg:after{content:'';position:absolute;right:18px;top:195px;width:210px;height:290px;" +
        "border-right:11px solid #3978ff;border-bottom:11px solid #3978ff;border-radius:0 0 110px 0}" +
        // ── Equalizer bar at bottom ───────────────────────────────────────────
        ".eq{position:fixed;left:0;right:0;bottom:0;height:88px;opacity:.22;" +
        "background:repeating-linear-gradient(90deg,#216dff 0 3px,transparent 3px 10px,#ff315d 10px 13px,transparent 13px 20px);" +
        "mask:linear-gradient(transparent 0%,#000 60%)}" +
        // ── Main layout grid ──────────────────────────────────────────────────
        ".wrap{height:100%;padding:18px 36px 16px;display:grid;grid-template-rows:72px 1fr 80px 46px 100px;gap:12px}" +
        // ── Header ────────────────────────────────────────────────────────────
        ".top{display:grid;grid-template-columns:auto 1fr 54px 54px 54px 54px 80px;gap:10px;align-items:center}" +
        ".logo-wrap{display:flex;align-items:center;height:72px}" +
        ".logo-wrap svg{display:block}" +
        // ── Search bar ────────────────────────────────────────────────────────
        ".search{height:50px;border:1px solid #2a3d62;background:linear-gradient(180deg,#0e1a30,#080f1e);" +
        "border-radius:10px;color:#c8d6f0;display:flex;align-items:center;padding-left:18px;font-size:16px;gap:10px}" +
        ".search svg{opacity:.7;flex-shrink:0}" +
        // ── Icon buttons ──────────────────────────────────────────────────────
        ".icon{height:50px;border:1px solid #2a3d62;background:linear-gradient(180deg,#0e1a30,#080f1e);" +
        "border-radius:10px;color:#eaf0ff;display:flex;align-items:center;justify-content:center;font-size:19px;position:relative}" +
        ".icon .badge{position:absolute;top:6px;right:7px;background:#ef1a3a;border-radius:50%;width:16px;height:16px;" +
        "font-size:9px;display:flex;align-items:center;justify-content:center;font-weight:bold}" +
        ".switchuser{height:50px;border:1px solid #2a3d62;background:linear-gradient(180deg,#0e1a30,#080f1e);" +
        "border-radius:10px;color:#eaf0ff;display:flex;flex-direction:column;align-items:center;justify-content:center;" +
        "font-size:11px;gap:2px;padding:0 6px}" +
        ".switchuser svg{width:22px;height:22px}" +
        // ── Cards grid ────────────────────────────────────────────────────────
        ".cards{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;min-height:0}" +
        ".card{position:relative;overflow:hidden;border:1px solid #2e4470;border-radius:14px;" +
        "padding:16px;display:flex;flex-direction:column;justify-content:flex-end;" +
        "box-shadow:inset 0 0 30px rgba(255,255,255,.04),0 10px 26px rgba(0,0,0,.4)}" +
        ".card:after{content:'';position:absolute;inset:0;" +
        "background:linear-gradient(180deg,rgba(255,255,255,.05) 0%,rgba(0,0,0,.55) 100%);z-index:0}" +
        ".card>*{z-index:2;position:relative}" +
        // Card colour themes
        ".live{background:radial-gradient(circle at 50% 22%,rgba(60,140,255,.42),transparent 28%)," +
        "linear-gradient(155deg,#0a60e0,#082b6e 60%,#051120)}" +
        ".movies{background:radial-gradient(circle at 50% 22%,rgba(255,100,100,.32),transparent 26%)," +
        "linear-gradient(155deg,#d42c42,#6e1424 60%,#150710)}" +
        ".series{background:radial-gradient(circle at 50% 22%,rgba(170,80,255,.38),transparent 26%)," +
        "linear-gradient(155deg,#7e3ed0,#3a1878 60%,#100820)}" +
        ".radio{background:radial-gradient(circle at 50% 28%,rgba(0,100,255,.55),transparent 28%)," +
        "linear-gradient(155deg,#0f1c40,#060918);" +
        "border-color:#ff315d;box-shadow:0 0 20px #176bff,0 0 24px rgba(255,49,93,.7),inset 0 0 38px rgba(40,100,255,.22)}" +
        // Card scene overlays (subtle imagery)
        ".scene{position:absolute;left:0;right:0;bottom:0;height:120px;opacity:1;pointer-events:none}" +
        // LIVE TV: stage floor + audience rows silhouette
        ".live .scene{background:" +
        "linear-gradient(180deg,transparent 0%,rgba(2,15,40,.7) 55%,rgba(2,10,30,.95) 100%)}" +
        ".live .scene:before{content:'';position:absolute;left:0;right:0;bottom:0;height:52px;" +
        "background:" +
        "radial-gradient(ellipse 80% 12px at 50% 100%,rgba(50,117,255,.18) 0%,transparent 100%)," +
        "repeating-linear-gradient(0deg,transparent 0px,transparent 10px,rgba(30,70,160,.22) 10px,rgba(30,70,160,.22) 12px);" +
        "border-top:2px solid rgba(50,117,255,.35)}" +
        ".live .scene:after{content:'';position:absolute;left:20%;right:20%;bottom:52px;height:3px;" +
        "background:rgba(50,117,255,.5);border-radius:2px}" +
        // MOVIES: cinema seat rows
        ".movies .scene{background:linear-gradient(180deg,transparent 0%,rgba(30,5,12,.75) 55%,rgba(20,4,10,.97) 100%)}" +
        ".movies .scene:before{content:'';position:absolute;left:4%;right:4%;bottom:4px;height:48px;" +
        "background:" +
        "repeating-linear-gradient(90deg,#2a0a14 0 20px,#3d0f1c 20px 22px,#2a0a14 22px 42px,#3d0f1c 42px 44px);" +
        "border-radius:14px 14px 3px 3px;" +
        "box-shadow:inset 0 8px 12px rgba(0,0,0,.5)}" +
        ".movies .scene:after{content:'';position:absolute;left:8%;right:8%;bottom:52px;height:3px;" +
        "background:rgba(180,30,60,.3);border-radius:2px}" +
        // SERIES: sofa/couch silhouette
        ".series .scene{background:linear-gradient(180deg,transparent 0%,rgba(12,4,28,.75) 55%,rgba(8,3,20,.97) 100%)}" +
        ".series .scene:before{content:'';position:absolute;left:6%;right:6%;bottom:4px;height:44px;" +
        "background:#1a0b38;border-radius:22px 22px 4px 4px;" +
        "box-shadow:inset 0 -4px 0 rgba(100,60,200,.3),inset 0 8px 16px rgba(0,0,0,.4)}" +
        ".series .scene:after{content:'';position:absolute;left:10%;right:10%;bottom:44px;height:10px;" +
        "background:#2a1258;border-radius:6px 6px 0 0}" +
        // RADIO: equalizer bars silhouette
        ".radio .scene{background:linear-gradient(180deg,transparent 0%,rgba(2,6,20,.75) 55%,rgba(1,4,14,.97) 100%)}" +
        ".radio .scene:before{content:'';position:absolute;left:8%;right:8%;bottom:6px;height:42px;" +
        "background:" +
        "repeating-linear-gradient(90deg," +
        "#1a3a8a 0 4px,transparent 4px 7px," +
        "#8b1a3a 7px 11px,transparent 11px 14px," +
        "#1a3a8a 14px 18px,transparent 18px 21px," +
        "#8b1a3a 21px 25px,transparent 25px 28px," +
        "#1a3a8a 28px 32px,transparent 32px 35px," +
        "#8b1a3a 35px 39px,transparent 39px 42px" +
        ");" +
        "mask:linear-gradient(180deg,transparent 0%,#000 40%)}" +
        // Featured badge
        ".featured{position:absolute;left:12px;top:12px;background:#f02750;border-radius:4px;" +
        "padding:4px 9px;font-size:10px;font-weight:bold;letter-spacing:.5px}" +
        ".art{flex:1;display:flex;align-items:center;justify-content:center}" +
        ".art svg{width:100px;height:100px;filter:drop-shadow(0 0 10px rgba(255,255,255,.25))}" +
        ".card h2{margin:0 0 4px;font-size:24px;font-family:'Arial Black',Arial,sans-serif;font-weight:900;text-transform:uppercase;letter-spacing:.5px}" +
        ".card p{margin:0;color:#ccd8f0;font-size:12px;font-weight:normal}" +
        ".wave{height:16px;margin:6px 0;" +
        "background:repeating-linear-gradient(90deg,#2c78ff 0 3px,transparent 3px 7px,#ff315d 7px 10px,transparent 10px 14px);" +
        "mask:linear-gradient(transparent,#000 30%,#000 70%,transparent)}" +
        // ── Feature row ───────────────────────────────────────────────────────
        ".features{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}" +
        ".feature{border:1px solid #243556;border-radius:12px;" +
        "background:linear-gradient(180deg,#0d1728,#070c1a);" +
        "display:flex;align-items:center;padding:0 20px;gap:14px;" +
        "box-shadow:inset 3px 0 0 #176bff}" +
        ".feature:nth-child(2){box-shadow:inset 3px 0 0 #8844dd}" +
        ".feature:nth-child(3){box-shadow:inset -3px 0 0 #ff315d}" +
        ".feature .fi{font-size:30px;flex-shrink:0}" +
        ".feature b{display:block;font-size:15px;font-family:'Arial Black',Arial,sans-serif}" +
        ".feature small{color:#9fb0cc;font-size:11px}" +
        // ── Footer status bar ─────────────────────────────────────────────────
        ".footer{border:1px solid #1c2e50;border-radius:9px;" +
        "background:linear-gradient(180deg,#091122,#050a16);" +
        "display:flex;align-items:center;justify-content:space-between;" +
        "padding:0 18px;color:#a8b8d2;font-size:13px;" +
        "box-shadow:0 0 16px rgba(25,90,255,.14)}" +
        ".footer strong{color:#ff6a00}" +
        ".footer .sep{width:1px;height:24px;background:#1e3050}" +
        // ── Lower brand section ─────────────────────────────────────────────── ───────────────────────────────────────────────
        ".brandbar{display:flex;flex-direction:column;align-items:center;justify-content:center;" +
        "position:relative;overflow:hidden;border-radius:10px;" +
        "background:linear-gradient(90deg,#030c20,#060416,#030c20)}" +
        ".brandbar:before{content:'';position:absolute;inset:0;" +
        "background:repeating-linear-gradient(90deg,#216dff 0 2px,transparent 2px 12px,#ff315d 12px 14px,transparent 14px 26px);" +
        "opacity:.18;mask:linear-gradient(to right,transparent,#000 20%,#000 80%,transparent)}" +
        ".brandbar-logo{display:flex;align-items:center;justify-content:center;z-index:1}" +
        ".tagline{font-size:11px;letter-spacing:5px;color:#fff;margin-top:4px;z-index:1;font-weight:bold}" +
        ".pillars{font-size:9px;letter-spacing:4px;margin-top:4px;z-index:1;font-weight:bold}" +
        ".pillars .faith{color:#2781ff}.pillars .music{color:#b84cff}.pillars .ent{color:#ff315d}" +
        // ── Announcement banner ───────────────────────────────────────────────
        ".announce{position:fixed;left:36px;right:36px;bottom:62px;z-index:30;" +
        "background:linear-gradient(90deg,#174aa8,#a41442);border:1px solid #ff5579;" +
        "border-radius:9px;padding:10px 16px}" +
        // ── Focus / D-pad states ──────────────────────────────────────────────
        "[tabindex]{outline:none;transition:transform .1s,box-shadow .1s}" +
        "[tabindex]:focus{transform:scale(1.04);border-color:#fff!important;" +
        "box-shadow:0 0 0 3px #176bff,0 0 22px #ff315d!important;z-index:20}" +
        "</style></head><body>" +
        announce +
        "<div class='micbg'></div>" +
        "<div class='eq'></div>" +
        "<div class='wrap'>" +
        // ── Header ────────────────────────────────────────────────────────────
        "<div class='top'>" +
        "<div class='logo-wrap'>" + LOGO_SVG + "</div>" +
        "<div class='search' tabindex='1' onclick='EMurph.masterSearch()'>" +
        "<svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='#8aa0c8' stroke-width='2.5'>" +
        "<circle cx='11' cy='11' r='7'/><path d='M21 21l-4.35-4.35'/></svg>" +
        "Master Search</div>" +
        // Bell with badge
        "<div class='icon' tabindex='2' onclick='EMurph.message(\"No new notifications\")'>" +
        "<svg width='22' height='22' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'>" +
        "<path d='M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9'/>" +
        "<path d='M13.73 21a2 2 0 0 1-3.46 0'/></svg>" +
        "<span class='badge'>3</span></div>" +
        // Profile icon
        "<div class='icon' tabindex='3' onclick='EMurph.users()'>" +
        "<svg width='22' height='22' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'>" +
        "<circle cx='12' cy='8' r='4'/><path d='M4 20c0-4 3.6-7 8-7s8 3 8 7'/></svg></div>" +
        // REC button
        "<div class='icon' tabindex='4' onclick='EMurph.message(\"Recording controls coming soon\")'>" +
        "<svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'>" +
        "<circle cx='12' cy='12' r='8'/><circle cx='12' cy='12' r='3' fill='#ef1a3a' stroke='none'/></svg></div>" +
        // Settings gear
        "<div class='icon' tabindex='5' onclick='EMurph.settings()'>" +
        "<svg width='22' height='22' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'>" +
        "<circle cx='12' cy='12' r='3'/>" +
        "<path d='M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z'/>" +
        "</svg></div>" +
        // Switch User button
        "<div class='switchuser' tabindex='6' onclick='EMurph.users()'>" +
        "<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'>" +
        "<circle cx='12' cy='8' r='4'/><path d='M4 20c0-4 3.6-7 8-7s8 3 8 7'/></svg>" +
        "<span>Switch User</span></div>" +
        "</div>" + // end .top
        // ── Cards ─────────────────────────────────────────────────────────────
        "<div class='cards'>" +
        card("live","LIVE TV","Watch Live Channels","tv","7","EMurph.browse(\"live\")","")+
        card("movies","MOVIES","Thousands of Movies","movie","8","EMurph.browse(\"movie\")","")+
        card("series","SERIES","Binge Worthy Shows","series","9","EMurph.browse(\"series\")","")+
        card("radio","EMURPH <span style='color:#ff315d'>RADIO</span>","Urban Contemporary Gospel","radio","10","EMurph.radio()","<span class='featured'>FEATURED</span>")+
        "</div>" +
        // ── Feature row ───────────────────────────────────────────────────────
        "<div class='features'>" +
                "<div class='feature' tabindex='11' onclick='EMurph.browse(\"live\")'>"
        + "<span class='fi'><svg width='30' height='30' viewBox='0 0 24 24' fill='none' stroke='#6ab0ff' stroke-width='2'>"
        + "<rect x='2' y='3' width='20' height='14' rx='2'/><line x1='8' y1='21' x2='16' y2='21'/><line x1='12' y1='17' x2='12' y2='21'/></svg></span>"
        + "<div><b>LIVE WITH EPG</b><small>See what's on now</small></div></div>" +
        "<div class='feature' tabindex='12' onclick='EMurph.message(\"Multi-screen is staged\")'>" +
        "<span class='fi'><svg width='30' height='30' viewBox='0 0 24 24' fill='none' stroke='#aa66ff' stroke-width='2'>" +
        "<rect x='3' y='3' width='8' height='8' rx='1'/><rect x='13' y='3' width='8' height='8' rx='1'/>" +
        "<rect x='3' y='13' width='8' height='8' rx='1'/><rect x='13' y='13' width='8' height='8' rx='1'/></svg></span>" +
        "<div><b>MULTI-SCREEN</b><small>Watch on multiple devices</small></div></div>" +
                "<div class='feature' tabindex='13' onclick='EMurph.browse(\"live\")'>"
        + "<span class='fi'><svg width='30' height='30' viewBox='0 0 24 24' fill='none' stroke='#ff6688' stroke-width='2'>"
        + "<circle cx='12' cy='12' r='9'/><polyline points='12 7 12 12 15 15'/>"
        + "<path d='M3.05 11a9 9 0 0 1 .9-3.5' stroke-linecap='round'/><path d='M3 3l18 18' stroke='none'/>"
        + "<path d='M4 4.5 A9 9 0 0 0 3.05 11' stroke-linecap='round'/></svg></span>"
        + "<div><b>CATCH UP</b><small>Never miss a moment</small></div></div>" +
        "</div>" +
        // ── Footer status bar ─────────────────────────────────────────────────
        "<div class='footer'>" +
        "<span><svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='#8aa0c8' stroke-width='2' style='vertical-align:middle;margin-right:6px'>" +
        "<rect x='3' y='4' width='18' height='18' rx='2'/><line x1='16' y1='2' x2='16' y2='6'/>" +
        "<line x1='8' y1='2' x2='8' y2='6'/><line x1='3' y1='10' x2='21' y2='10'/></svg>" +
        "Expiration: <strong>"+exp+"</strong></span>" +
        "<div class='sep'></div>" +
        "<span><svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='#8aa0c8' stroke-width='2' style='vertical-align:middle;margin-right:6px'>" +
        "<circle cx='12' cy='8' r='4'/><path d='M4 20c0-4 3.6-7 8-7s8 3 8 7'/></svg>" +
        "Logged in: <strong>"+user+"</strong></span>" +
        "</div>" +
        // ── Lower brand section ───────────────────────────────────────────────
        "<div class='brandbar'>" +
        "<div class='brandbar-logo'>" + LOGO_SVG_LG + "</div>" +
        "<div class='tagline'>STREAM. LISTEN. INSPIRE.</div>" +
        "<div class='pillars'><span class='faith'>FAITH</span> &nbsp;•&nbsp; <span class='music'>MUSIC</span> &nbsp;•&nbsp; <span class='ent'>ENTERTAINMENT</span></div>" +
        "</div>" +
        "</div>" + // end .wrap
        // ── D-pad keyboard navigation ─────────────────────────────────────────
        "<script>" +
        "document.addEventListener('keydown',function(e){" +
        "var f=[].slice.call(document.querySelectorAll('[tabindex]'));" +
        "var i=f.indexOf(document.activeElement);if(i<0)i=0;" +
        "var k=e.key;" +
        "if(k==='ArrowRight')i=Math.min(f.length-1,i+1);" +
        "if(k==='ArrowLeft')i=Math.max(0,i-1);" +
        // Row-aware navigation: row1=cols 0-5 (6 items), row2=cols 6-9 (4 cards), row3=cols 10-12 (3 features), row4=footer
        "if(k==='ArrowDown'){if(i<6)i=Math.min(9,i+6);else if(i<10)i=Math.min(12,i+4);else if(i<13)i=i;}" +
        "if(k==='ArrowUp'){if(i>=10)i=Math.max(6,i-4);else if(i>=6)i=Math.max(0,i-6);}" +
        "if(k.startsWith('Arrow')){e.preventDefault();f[i].focus();}" +
        "});" +
        "setTimeout(function(){var el=document.querySelector('[tabindex]');if(el)el.focus();},250);" +
        "</script>" +
        "</body></html>";
    }

    private String card(String cls, String title, String sub, String icon, String tab, String click, String badge) {
        String svg;
        if (icon.equals("tv")) {
            // Television with play button
            svg = "<svg viewBox='0 0 120 120'><g fill='none' stroke='white' stroke-width='5'>" +
                  "<rect x='18' y='26' width='84' height='60' rx='9'/>" +
                  "<path d='M44 16l16 10 16-10' stroke-linecap='round'/>" +
                  "<path d='M38 94h44' stroke-linecap='round'/>" +
                  "</g>" +
                  "<polygon points='48,44 48,76 82,60' fill='white' opacity='.85'/>" +
                  "</svg>";
        } else if (icon.equals("movie")) {
            // Film reel
            svg = "<svg viewBox='0 0 120 120'><g fill='none' stroke='white' stroke-width='5'>" +
                  "<circle cx='60' cy='60' r='38'/>" +
                  "<circle cx='60' cy='60' r='12'/>" +
                  "</g>" +
                  "<g fill='white'>" +
                  "<circle cx='60' cy='30' r='7'/>" +
                  "<circle cx='83' cy='47' r='7'/>" +
                  "<circle cx='83' cy='73' r='7'/>" +
                  "<circle cx='60' cy='90' r='7'/>" +
                  "<circle cx='37' cy='73' r='7'/>" +
                  "<circle cx='37' cy='47' r='7'/>" +
                  "</g></svg>";
        } else if (icon.equals("series")) {
            // Clapperboard
            svg = "<svg viewBox='0 0 120 120'><g fill='none' stroke='white' stroke-width='5'>" +
                  "<rect x='20' y='44' width='80' height='54' rx='6'/>" +
                  "<rect x='20' y='28' width='80' height='20' rx='4'/>" +
                  "<line x1='38' y1='28' x2='32' y2='48'/>" +
                  "<line x1='56' y1='28' x2='50' y2='48'/>" +
                  "<line x1='74' y1='28' x2='68' y2='48'/>" +
                  "<line x1='92' y1='28' x2='86' y2='48'/>" +
                  "</g></svg>";
        } else {
            // Radio icon - clean simple design matching reference: radio body + WiFi arcs blue/red
            svg = "<svg viewBox='0 0 120 120'>" +
                  // Left broadcast arcs (blue)
                  "<g fill='none' stroke-linecap='round'>" +
                  "<path d='M38 50 a18 18 0 0 0 0 20' stroke='#3a8fff' stroke-width='4.5'/>" +
                  "<path d='M28 42 a30 30 0 0 0 0 36' stroke='#3a8fff' stroke-width='4' opacity='.6'/>" +
                  // Right broadcast arcs (red)
                  "<path d='M82 50 a18 18 0 0 1 0 20' stroke='#ff315d' stroke-width='4.5'/>" +
                  "<path d='M92 42 a30 30 0 0 1 0 36' stroke='#ff315d' stroke-width='4' opacity='.6'/>" +
                  "</g>" +
                  // Radio body (rounded rectangle)
                  "<rect x='40' y='46' width='40' height='28' rx='5' fill='none' stroke='#3a8fff' stroke-width='4'/>" +
                  // Speaker circle
                  "<circle cx='55' cy='60' r='8' fill='none' stroke='#3a8fff' stroke-width='3.5'/>" +
                  "<circle cx='55' cy='60' r='2.5' fill='#3a8fff'/>" +
                  // Tuner knob
                  "<circle cx='72' cy='57' r='5' fill='none' stroke='#ff315d' stroke-width='3'/>" +
                  "<circle cx='72' cy='57' r='2' fill='#ff315d'/>" +
                  // Antenna
                  "<line x1='68' y1='46' x2='60' y2='28' stroke='white' stroke-width='3.5' stroke-linecap='round'/>" +
                  "<circle cx='60' cy='28' r='3' fill='white'/>" +
                  "</svg>";
        }
        return "<div class='card " + cls + "' tabindex='" + tab + "' onclick='" + click + "'>" +
               badge +
               "<div class='art'>" + svg + "</div>" +
               "<div class='scene'></div>" +
               "<h2>" + title + "</h2>" +
               (icon.equals("radio") ? "<div class='wave'></div>" : "") +
               "<p>" + sub + "</p>" +
               "</div>";
    }

    public class Bridge {
        @JavascriptInterface public void masterSearch() { runOnUiThread(() -> showSearch()); }
        @JavascriptInterface public void users() { runOnUiThread(() -> showUsers()); }
        @JavascriptInterface public void settings() { runOnUiThread(() -> showSettings()); }
        @JavascriptInterface public void browse(String type) { runOnUiThread(() -> browseNative(type)); }
        @JavascriptInterface public void radio() { runOnUiThread(() -> showRadio()); }
        @JavascriptInterface public void message(String s) { runOnUiThread(() -> toast(s)); }
    }

    // ── User List Screen ──────────────────────────────────────────────────────
    private void showUsers() {
        releasePlayer();
        // Use WebView for the users screen to match the approved design
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(2,5,16));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.addJavascriptInterface(new UsersBridge(), "EMurph");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.loadDataWithBaseURL(null, usersHtml(), "text/html", "UTF-8", null);
        setContentView(web);
        web.requestFocus();
    }

    private String usersHtml() {
        StringBuilder cards = new StringBuilder();
        int tabIdx = 3;
        try {
            JSONArray users = new JSONArray(prefs.getString("profiles", "[]"));
            if (users.length() == 0) {
                cards.append("<div class='empty'>No users saved yet. Select <b>+ ADD USER</b> to connect your service.</div>");
            } else {
                for (int i = 0; i < users.length(); i++) {
                    JSONObject p = users.getJSONObject(i);
                    String name = esc(p.optString("name"));
                    String uname = esc(p.optString("username"));
                    String server = esc(p.optString("server"));
                    boolean active = p.optString("name").equals(activeProfile);
                    cards.append("<div class='ucard").append(active ? " active" : "").append("' tabindex='").append(tabIdx++).append("' onclick='EMurph.select(\"").append(name).append("\")'>");
                    cards.append("<div class='avatar'><svg viewBox='0 0 60 60' width='52' height='52'><circle cx='30' cy='22' r='12' fill='#4a6aaa'/><path d='M8 54c0-12 9-20 22-20s22 8 22 20' fill='#4a6aaa'/></svg></div>");
                    cards.append("<div class='uinfo'><div class='uname'>").append(name).append("</div>");
                    cards.append("<div class='udetail'>Username: ").append(uname).append("</div>");
                    cards.append("<div class='udetail'>Server: EMurph TV Main</div></div>");
                    if (active) cards.append("<div class='check'><svg viewBox='0 0 24 24' width='22' height='22' fill='none' stroke='#2196f3' stroke-width='3'><polyline points='20 6 9 17 4 12'/></svg></div>");
                    cards.append("</div>");
                }
            }
        } catch (Exception ignored) {}

        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>" +
        "*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;color:#fff;font-family:'Arial Black',Arial,sans-serif;background:#020611}" +
        "body:before{content:'';position:fixed;inset:0;background:radial-gradient(ellipse at 10% 80%,rgba(18,80,220,.28),transparent 38%),radial-gradient(ellipse at 90% 72%,rgba(220,28,72,.18),transparent 32%),linear-gradient(180deg,#030818,#020510);z-index:-1}" +
        ".wrap{height:100%;padding:22px 36px;display:flex;flex-direction:column;gap:18px}" +
        ".topbar{display:flex;align-items:center;gap:16px}" +
        ".topbar .logo-wrap{flex:1}" +
        ".title{flex:2;text-align:center;font-size:22px;font-weight:900;letter-spacing:2px;color:#fff}" +
        ".addbtn{background:linear-gradient(90deg,#1240b0,#8b1a3a);border:1px solid #4a6ad0;border-radius:9px;color:#fff;font-size:14px;font-weight:bold;padding:0 18px;height:46px;display:flex;align-items:center;gap:8px;cursor:pointer;white-space:nowrap}" +
        ".addbtn svg{width:18px;height:18px}" +
        ".cards-row{display:flex;gap:16px;flex:1;align-items:flex-start;padding-top:4px}" +
        ".ucard{display:flex;align-items:center;gap:14px;padding:16px 18px;border:1px solid #2a3d62;border-radius:12px;background:linear-gradient(180deg,#0a1428,#060c1c);min-width:280px;max-width:340px;cursor:pointer;position:relative}" +
        ".ucard.active{border-color:#2a5bd0;background:linear-gradient(180deg,#0f2a6a,#07133a);box-shadow:0 0 0 2px #2a5bd0,0 0 16px rgba(42,91,208,.4)}" +
        ".avatar{flex-shrink:0;background:#0e1e40;border-radius:50%;padding:4px}" +
        ".uinfo{flex:1}" +
        ".uname{font-size:17px;font-weight:bold;color:#fff}" +
        ".udetail{font-size:12px;color:#8aa0c8;margin-top:3px;font-weight:normal}" +
        ".check{position:absolute;top:12px;right:12px}" +
        ".hint{color:#8aa0c8;font-size:14px;margin-top:8px;display:flex;align-items:center;gap:8px}" +
        ".hint svg{opacity:.6}" +
        ".empty{color:#8aa0c8;font-size:16px;text-align:center;padding:40px;font-weight:normal}" +
        "[tabindex]{outline:none;transition:.1s}[tabindex]:focus{border-color:#fff!important;box-shadow:0 0 0 3px #176bff,0 0 20px #ff315d!important;transform:scale(1.03)}" +
        "</style></head><body><div class='wrap'>" +
        "<div class='topbar'>" +
        "<div class='logo-wrap'>" + LOGO_SVG_SM + "</div>" +
        "<div class='title'>LIST USERS</div>" +
        "<div class='addbtn' tabindex='1' onclick='EMurph.addUser()'>" +
        "<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.5'><line x1='12' y1='5' x2='12' y2='19'/><line x1='5' y1='12' x2='19' y2='12'/></svg>" +
        "+ ADD USER</div>" +
        "</div>" +
        "<div class='cards-row'>" + cards.toString() + "</div>" +
        "<div class='hint'>" +
        "<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><circle cx='12' cy='12' r='10'/><line x1='12' y1='8' x2='12' y2='12'/><line x1='12' y1='16' x2='12.01' y2='16'/></svg>" +
        "Select a user profile to continue</div>" +
        "</div>" +
        "<script>" +
        "document.addEventListener('keydown',function(e){" +
        "var f=[].slice.call(document.querySelectorAll('[tabindex]'));" +
        "var i=f.indexOf(document.activeElement);if(i<0)i=0;" +
        "if(e.key==='ArrowRight')i=Math.min(f.length-1,i+1);" +
        "if(e.key==='ArrowLeft')i=Math.max(0,i-1);" +
        "if(e.key==='ArrowDown')i=Math.min(f.length-1,i+1);" +
        "if(e.key==='ArrowUp')i=Math.max(0,i-1);" +
        "if(e.key.startsWith('Arrow')){e.preventDefault();f[i].focus();}" +
        "if(e.key==='Backspace'||e.key==='Back')EMurph.goHome();" +
        "});" +
        "setTimeout(function(){var el=document.querySelector('[tabindex]');if(el)el.focus();},200);" +
        "</script></body></html>";
    }

    public class UsersBridge {
        @JavascriptInterface public void select(String name) {
            runOnUiThread(() -> {
                activeProfile = name;
                prefs.edit().putString("active_profile", name).apply();
                showHome();
            });
        }
        @JavascriptInterface public void addUser() { runOnUiThread(() -> showAddUser()); }
        @JavascriptInterface public void goHome() { runOnUiThread(() -> showHome()); }
    }

    // ── Add User / Login Screen ───────────────────────────────────────────────
    private void showAddUser() {
        releasePlayer();
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(2,5,16));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.addJavascriptInterface(new AddUserBridge(), "EMurph");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        String defaultServer = esc(prefs.getString("default_server_url", defaultServerUrl));
        web.loadDataWithBaseURL(null, addUserHtml(defaultServer), "text/html", "UTF-8", null);
        setContentView(web);
        web.requestFocus();
    }

    private String addUserHtml(String defaultServer) {
        // Neon microphone SVG art (inline)
        String micArt =
            "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 220 320' width='200' height='290'>" +
            "<defs>" +
            "<radialGradient id='mg' cx='50%' cy='40%' r='55%'>" +
            "<stop offset='0%' stop-color='#5580ff' stop-opacity='.9'/>" +
            "<stop offset='100%' stop-color='#1a2a6c' stop-opacity='.3'/>" +
            "</radialGradient>" +
            "<filter id='glow'><feGaussianBlur stdDeviation='4' result='blur'/>" +
            "<feMerge><feMergeNode in='blur'/><feMergeNode in='SourceGraphic'/></feMerge></filter>" +
            "</defs>" +
            // Glow halo
            "<ellipse cx='110' cy='130' rx='80' ry='100' fill='url(#mg)' opacity='.35'/>" +
            // Mic body
            "<rect x='75' y='40' width='70' height='130' rx='35' fill='none' stroke='#4a7aff' stroke-width='6' filter='url(#glow)'/>" +
            // Mic grille lines
            "<line x1='75' y1='90' x2='145' y2='90' stroke='#4a7aff' stroke-width='2' opacity='.5'/>" +
            "<line x1='75' y1='110' x2='145' y2='110' stroke='#4a7aff' stroke-width='2' opacity='.5'/>" +
            "<line x1='75' y1='130' x2='145' y2='130' stroke='#4a7aff' stroke-width='2' opacity='.5'/>" +
            // Stand arm
            "<path d='M60 170 Q60 210 110 210 Q160 210 160 170' fill='none' stroke='#4a7aff' stroke-width='6' stroke-linecap='round' filter='url(#glow)'/>" +
            // Stand pole
            "<line x1='110' y1='210' x2='110' y2='250' stroke='#4a7aff' stroke-width='6' stroke-linecap='round'/>" +
            // Base
            "<line x1='75' y1='250' x2='145' y2='250' stroke='#4a7aff' stroke-width='6' stroke-linecap='round'/>" +
            // Red accent glow
            "<ellipse cx='110' cy='130' rx='40' ry='50' fill='none' stroke='#ff315d' stroke-width='2' opacity='.3'/>" +
            // Equalizer bars at bottom
            "<rect x='30' y='270' width='8' height='30' rx='3' fill='#2c78ff' opacity='.7'/>" +
            "<rect x='45' y='260' width='8' height='40' rx='3' fill='#4a90ff' opacity='.7'/>" +
            "<rect x='60' y='275' width='8' height='25' rx='3' fill='#2c78ff' opacity='.7'/>" +
            "<rect x='145' y='268' width='8' height='32' rx='3' fill='#ff315d' opacity='.7'/>" +
            "<rect x='160' y='258' width='8' height='42' rx='3' fill='#ff5577' opacity='.7'/>" +
            "<rect x='175' y='272' width='8' height='28' rx='3' fill='#ff315d' opacity='.7'/>" +
            "</svg>";

        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>" +
        "*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;color:#fff;font-family:'Arial Black',Arial,sans-serif;background:#020611}" +
        "body:before{content:'';position:fixed;inset:0;background:radial-gradient(ellipse at 10% 80%,rgba(18,80,220,.28),transparent 38%),radial-gradient(ellipse at 90% 72%,rgba(220,28,72,.18),transparent 32%),linear-gradient(180deg,#030818,#020510);z-index:-1}" +
        ".wrap{height:100%;padding:22px 36px;display:flex;gap:24px;align-items:stretch}" +
        // Left art panel
        ".art-panel{width:220px;display:flex;flex-direction:column;align-items:center;justify-content:center;flex-shrink:0}" +
        // Right form panel
        ".form-panel{flex:1;border:1px solid #1e3050;border-radius:14px;background:linear-gradient(180deg,#080e20,#050916);padding:24px 28px;display:flex;flex-direction:column;gap:0}" +
        ".panel-logo{margin-bottom:10px}" +
        ".panel-title{font-size:22px;font-weight:900;color:#fff;margin-bottom:18px;text-align:center;letter-spacing:.5px}" +
        ".field{position:relative;margin-bottom:10px}" +
        ".field svg.icon{position:absolute;left:14px;top:50%;transform:translateY(-50%);opacity:.5;pointer-events:none}" +
        ".field input{width:100%;height:50px;background:linear-gradient(180deg,#0a1428,#060c1c);border:1px solid #243050;border-radius:9px;color:#fff;font-size:15px;padding:0 44px 0 44px;font-family:Arial,sans-serif}" +
        ".field input::placeholder{color:#5a6e90}" +
        ".field input:focus{outline:none;border-color:#3a6aff;box-shadow:0 0 0 2px rgba(58,106,255,.3)}" +
        ".eye-btn{position:absolute;right:12px;top:50%;transform:translateY(-50%);background:none;border:none;color:#5a6e90;cursor:pointer;padding:4px}" +
        ".add-btn{width:100%;height:52px;background:linear-gradient(90deg,#1240b0,#8b1a3a);border:none;border-radius:9px;color:#fff;font-size:16px;font-weight:bold;cursor:pointer;margin-top:6px;letter-spacing:.5px}" +
        ".bottom-btns{display:flex;gap:10px;margin-top:10px}" +
        ".bottom-btns button{flex:1;height:46px;border-radius:9px;border:1px solid #2a3d62;background:linear-gradient(180deg,#0d1728,#070c1a);color:#fff;font-size:13px;font-weight:bold;cursor:pointer;display:flex;align-items:center;justify-content:center;gap:8px}" +
        "[tabindex]{outline:none;transition:.1s}[tabindex]:focus{border-color:#fff!important;box-shadow:0 0 0 3px #176bff,0 0 20px #ff315d!important}" +
        "button:focus{outline:none;border-color:#fff!important;box-shadow:0 0 0 3px #176bff,0 0 20px #ff315d!important}" +
        "</style></head><body><div class='wrap'>" +
        "<div class='art-panel'>" + micArt + "</div>" +
        "<div class='form-panel'>" +
        "<div class='panel-logo'>" + LOGO_SVG_SM + "</div>" +
        "<div class='panel-title'>Enter Your Login Details</div>" +
        // Profile Name
        "<div class='field'>" +
        "<svg class='icon' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><circle cx='12' cy='8' r='4'/><path d='M4 20c0-4 3.6-7 8-7s8 3 8 7'/></svg>" +
        "<input id='pname' type='text' placeholder='Profile Name' tabindex='1'/></div>" +
        // Username
        "<div class='field'>" +
        "<svg class='icon' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><circle cx='12' cy='8' r='4'/><path d='M4 20c0-4 3.6-7 8-7s8 3 8 7'/></svg>" +
        "<input id='uname' type='text' placeholder='Username' tabindex='2'/></div>" +
        // Password
        "<div class='field'>" +
        "<svg class='icon' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><rect x='3' y='11' width='18' height='11' rx='2'/><path d='M7 11V7a5 5 0 0 1 10 0v4'/></svg>" +
        "<input id='pass' type='password' placeholder='Password' tabindex='3'/>" +
        "<button class='eye-btn' onclick='togglePw()' tabindex='-1'>" +
        "<svg id='eyeicon' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><path d='M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z'/><circle cx='12' cy='12' r='3'/></svg>" +
        "</button></div>" +
        // Server URL
        "<div class='field'>" +
        "<svg class='icon' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><rect x='2' y='2' width='20' height='8' rx='2'/><rect x='2' y='14' width='20' height='8' rx='2'/><line x1='6' y1='6' x2='6.01' y2='6'/><line x1='6' y1='18' x2='6.01' y2='18'/></svg>" +
        "<input id='surl' type='text' placeholder='Server URL' value='" + defaultServer + "' tabindex='4'/></div>" +
        // ADD USER button
        "<button class='add-btn' tabindex='5' onclick='doAdd()'>ADD USER</button>" +
        // Bottom buttons
        "<div class='bottom-btns'>" +
        "<button tabindex='6' onclick='EMurph.listUsers()'>" +
        "<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><circle cx='12' cy='8' r='4'/><path d='M4 20c0-4 3.6-7 8-7s8 3 8 7'/></svg>" +
        "LIST USERS</button>" +
        "<button tabindex='7' onclick='EMurph.connectVpn()'>" +
        "<svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><path d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'/></svg>" +
        "CONNECT VPN</button>" +
        "</div>" +
        "</div>" + // end .form-panel
        "</div>" + // end .wrap
        "<script>" +
        "function togglePw(){var i=document.getElementById('pass');var e=document.getElementById('eyeicon');" +
        "if(i.type==='password'){i.type='text';e.style.stroke='#4a7aff';}else{i.type='password';e.style.stroke='currentColor';}}" +
        "function doAdd(){" +
        "var n=document.getElementById('pname').value.trim();" +
        "var u=document.getElementById('uname').value.trim();" +
        "var p=document.getElementById('pass').value;" +
        "var s=document.getElementById('surl').value.trim();" +
        "if(!n||!u||!p||!s){EMurph.showMsg('Complete all four fields.');return;}" +
        "EMurph.addUser(n,u,p,s);}" +
        "document.addEventListener('keydown',function(e){" +
        "var f=[].slice.call(document.querySelectorAll('[tabindex]'));" +
        "var i=f.indexOf(document.activeElement);if(i<0)i=0;" +
        "if(e.key==='ArrowDown')i=Math.min(f.length-1,i+1);" +
        "if(e.key==='ArrowUp')i=Math.max(0,i-1);" +
        "if(e.key.startsWith('Arrow')){e.preventDefault();f[i].focus();}" +
        "if(e.key==='Backspace'||e.key==='Back')EMurph.listUsers();" +
        "});" +
        "setTimeout(function(){var el=document.getElementById('pname');if(el)el.focus();},200);" +
        "</script></body></html>";
    }

    public class AddUserBridge {
        @JavascriptInterface public void addUser(String name, String user, String pass, String server) {
            runOnUiThread(() -> addUserFromWeb(name, user, pass, server));
        }
        @JavascriptInterface public void listUsers() { runOnUiThread(() -> showUsers()); }
        @JavascriptInterface public void connectVpn() { runOnUiThread(() -> toast("VPN integration coming soon.")); }
        @JavascriptInterface public void showMsg(String msg) { runOnUiThread(() -> toast(msg)); }
    }

    private void addUserFromWeb(String name, String user, String pass, String server) {
        String normalServer = normalizeServer(server);
        toast("Checking login…");
        new Thread(() -> {
            try {
                JSONObject r = getObject(normalServer + "/player_api.php?username=" + enc(user) + "&password=" + enc(pass));
                JSONObject info = r.optJSONObject("user_info");
                if (info == null) throw new Exception("No account data returned");
                JSONObject obj = new JSONObject();
                obj.put("name", name);
                obj.put("username", user);
                obj.put("password", pass);
                obj.put("server", normalServer);
                obj.put("exp", info.optString("exp_date", ""));
                JSONArray arr = new JSONArray(prefs.getString("profiles", "[]"));
                for (int i = arr.length() - 1; i >= 0; i--)
                    if (arr.optJSONObject(i).optString("name").equals(name)) arr.remove(i);
                arr.put(obj);
                prefs.edit().putString("profiles", arr.toString()).putString("active_profile", name).apply();
                activeProfile = name;
                runOnUiThread(this::showHome);
            } catch (Exception e) {
                runOnUiThread(() -> toast("Login failed: " + e.getMessage()));
            }
        }).start();
    }

    private void showSearch() {
        EditText e = input("Search channels, movies, or series");
        new AlertDialog.Builder(this).setTitle("Master Search").setView(e)
            .setPositiveButton("Search", (d, w) -> performSearch(e.getText().toString().trim()))
            .setNegativeButton("Cancel", null).show();
    }

    private void performSearch(String q) {
        if (q.isEmpty()) { toast("Enter a search term"); return; }
        if (!hasActiveUser()) { toast("Add or select a user first."); return; }
        new Thread(() -> {
            JSONArray out = new JSONArray();
            try {
                JSONObject p = profile(activeProfile);
                for (String a : new String[]{"get_live_streams", "get_vod_streams", "get_series"}) {
                    JSONArray x = getArray(api(p) + "&action=" + a);
                    for (int i = 0; i < x.length(); i++) {
                        JSONObject o = x.optJSONObject(i);
                        String n = o.optString("name", o.optString("title"));
                        if (n.toLowerCase(Locale.US).contains(q.toLowerCase(Locale.US))) out.put(o);
                    }
                }
            } catch (Exception ignored) {}
            runOnUiThread(() -> showResults("Search: " + q, out));
        }).start();
    }

    private void browseNative(String type) {
        if (!hasActiveUser()) { toast("Add or select an IPTV user first."); showUsers(); return; }
        LinearLayout root = new LinearLayout(this);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(2, 5, 16));
        root.addView(new ProgressBar(this));
        setContentView(root);
        new Thread(() -> {
            JSONArray arr = new JSONArray();
            try {
                JSONObject p = profile(activeProfile);
                String action = type.equals("live") ? "get_live_streams" : type.equals("movie") ? "get_vod_streams" : "get_series";
                arr = getArray(api(p) + "&action=" + action);
            } catch (Exception ignored) {}
            JSONArray r = arr;
            runOnUiThread(() -> showResults(type.toUpperCase(Locale.US), r));
        }).start();
    }

    private void showResults(String title, JSONArray arr) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(30), dp(20), dp(30), dp(20));
        root.setBackgroundColor(Color.rgb(2, 5, 16));
        LinearLayout top = new LinearLayout(this);
        top.addView(button("← HOME", Color.rgb(16, 31, 63), v -> showHome()), new LinearLayout.LayoutParams(dp(150), dp(52)));
        top.addView(label(title, 27, Color.WHITE, true), new LinearLayout.LayoutParams(0, dp(58), 1));
        root.addView(top);
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < Math.min(arr.length(), 250); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String name = o.optString("name", o.optString("title", "Untitled"));
            list.addView(button(name, Color.rgb(10, 20, 42), v -> playItem(o)), new LinearLayout.LayoutParams(-1, dp(58)));
            list.addView(space(1, dp(7)));
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void playItem(JSONObject o) {
        try {
            JSONObject p = profile(activeProfile);
            if (o.has("series_id")) { showSeriesEpisodes(o); return; }
            String id = o.optString("stream_id"), ext = o.optString("container_extension", "m3u8");
            boolean movie = "movie".equals(o.optString("stream_type")) || o.has("rating_5based");
            String url = profileServer(p) + "/" + (movie ? "movie" : "live") + "/" + enc(p.optString("username")) + "/" + enc(p.optString("password")) + "/" + id + "." + ext;
            showPlayer(o.optString("name", "Playing"), url);
        } catch (Exception e) { toast("Unable to build stream URL."); }
    }

    private void showSeriesEpisodes(JSONObject series) {
        String name = series.optString("name", "Series"), id = series.optString("series_id");
        LinearLayout l = new LinearLayout(this);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(Color.rgb(2, 5, 16));
        l.addView(new ProgressBar(this));
        setContentView(l);
        new Thread(() -> {
            JSONArray ep = new JSONArray();
            try {
                JSONObject p = profile(activeProfile);
                JSONObject info = getObject(api(p) + "&action=get_series_info&series_id=" + enc(id));
                JSONObject seasons = info.optJSONObject("episodes");
                if (seasons != null) {
                    Iterator<String> k = seasons.keys();
                    while (k.hasNext()) {
                        JSONArray a = seasons.optJSONArray(k.next());
                        if (a != null) for (int i = 0; i < a.length(); i++) ep.put(a.optJSONObject(i));
                    }
                }
            } catch (Exception ignored) {}
            runOnUiThread(() -> showResults(name, ep));
        }).start();
    }

    private void showPlayer(String title, String url) {
        releasePlayer();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.addView(button("← HOME • " + title, Color.rgb(10, 20, 42), v -> showHome()), new LinearLayout.LayoutParams(-1, dp(52)));
        PlayerView view = new PlayerView(this);
        player = new ExoPlayer.Builder(this).build();
        view.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
        root.addView(view, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showRadio() {
        releasePlayer();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(2, 5, 16));
        root.addView(label("📻", 88, Color.WHITE, false));
        root.addView(label("EMurph RADIO", 38, Color.WHITE, true));
        TextView status = label("Ready", 17, Color.LTGRAY, false);
        status.setGravity(Gravity.CENTER);
        root.addView(status);
        root.addView(button("▶ PLAY RADIO", Color.rgb(210, 31, 69), v -> {
            releasePlayer();
            player = new ExoPlayer.Builder(this).build();
            player.setMediaItem(MediaItem.fromUri(Uri.parse(prefs.getString("radio_url", defaultRadioUrl))));
            player.prepare();
            player.play();
            status.setText("Now Playing");
        }), new LinearLayout.LayoutParams(dp(440), dp(62)));
        root.addView(button("← HOME", Color.rgb(16, 31, 63), v -> showHome()), new LinearLayout.LayoutParams(dp(440), dp(54)));
        setContentView(root);
    }

    private void showSettings() {
        String[] o = {"Switch User", "Update EMurph Radio URL", "Clear Saved Users", "Cancel"};
        new AlertDialog.Builder(this).setTitle("EMurph TV Settings").setItems(o, (d, w) -> {
            if (w == 0) showUsers();
            else if (w == 1) editRadioUrl();
            else if (w == 2) { prefs.edit().remove("profiles").remove("active_profile").apply(); activeProfile = ""; showHome(); }
        }).show();
    }

    private void editRadioUrl() {
        EditText e = input("Radio URL");
        e.setText(prefs.getString("radio_url", defaultRadioUrl));
        new AlertDialog.Builder(this).setTitle("EMurph Radio URL").setView(e)
            .setPositiveButton("Save", (d, w) -> prefs.edit().putString("radio_url", e.getText().toString().trim()).apply())
            .setNegativeButton("Cancel", null).show();
    }

    // ── Utility helpers ───────────────────────────────────────────────────────
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(dp(14), dp(8), dp(14), dp(8));
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return t;
    }

    private TextView button(String text, int color, View.OnClickListener click) {
        TextView b = label(text, 18, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true); b.setClickable(true);
        b.setBackground(panel(color, Color.rgb(65, 82, 118), 12, 1));
        b.setOnClickListener(click);
        focusGlow(b);
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setHintTextColor(Color.rgb(140, 155, 184));
        e.setTextColor(Color.WHITE); e.setTextSize(18); e.setSingleLine(true);
        e.setPadding(dp(16), 0, dp(16), 0);
        e.setBackground(panel(Color.rgb(9, 17, 35), Color.rgb(58, 76, 112), 10, 1));
        return e;
    }

    private android.graphics.drawable.GradientDrawable panel(int color, int stroke, int radius, int width) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius)); g.setStroke(dp(width), stroke);
        return g;
    }

    private void focusGlow(View v) {
        v.setOnFocusChangeListener((view, f) -> {
            view.animate().scaleX(f ? 1.035f : 1f).scaleY(f ? 1.035f : 1f).setDuration(110).start();
            view.setAlpha(f ? 1f : .96f);
        });
    }

    private Space space(int w, int h) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        return s;
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private void releasePlayer() { if (player != null) { player.release(); player = null; } }
    private boolean hasActiveUser() { return !activeProfile.isEmpty(); }

    private JSONObject profile(String name) throws Exception {
        JSONArray a = new JSONArray(prefs.getString("profiles", "[]"));
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.getJSONObject(i);
            if (name.equals(p.optString("name"))) return p;
        }
        throw new Exception("Profile not found");
    }

    private String profileServer(JSONObject p) {
        return normalizeServer(p.optString("server", prefs.getString("default_server_url", defaultServerUrl)));
    }

    private String api(JSONObject p) throws Exception {
        return profileServer(p) + "/player_api.php?username=" + enc(p.getString("username")) + "&password=" + enc(p.getString("password"));
    }

    private String expiration() {
        if (activeProfile.isEmpty()) return "Not connected";
        try {
            String x = profile(activeProfile).optString("exp", "");
            if (x.isEmpty() || "null".equals(x)) return "Unknown";
            return new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(new Date(Long.parseLong(x) * 1000));
        } catch (Exception e) { return "Unknown"; }
    }

    private String enc(String s) throws Exception { return URLEncoder.encode(s, "UTF-8"); }

    private String normalizeServer(String s) {
        if (s == null) return "";
        s = s.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private void refreshRemoteConfig() {
        try {
            JSONObject cfg = getObject(remoteConfigUrl + "?t=" + System.currentTimeMillis());
            String server = cfg.optString("iptv_url", defaultServerUrl).trim();
            String radio = cfg.optString("radio_url", defaultRadioUrl).trim();
            JSONObject a = cfg.optJSONObject("announcement");
            SharedPreferences.Editor e = prefs.edit();
            if (server.startsWith("http://") || server.startsWith("https://")) e.putString("default_server_url", server);
            if (radio.startsWith("http://") || radio.startsWith("https://")) e.putString("radio_url", radio);
            if (a != null) {
                e.putBoolean("announcement_enabled", a.optBoolean("enabled", false));
                e.putString("announcement_title", a.optString("title", ""));
                e.putString("announcement_message", a.optString("message", ""));
            }
            e.apply();
            runOnUiThread(this::showHome);
        } catch (Exception ignored) {}
    }

    private JSONObject getObject(String url) throws Exception { return new JSONObject(read(url)); }
    private JSONArray getArray(String url) throws Exception { return new JSONArray(read(url)); }

    private String read(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(25000);
        c.setRequestProperty("User-Agent", "EMurphTV/1.1");
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0;) out.write(b, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } finally { c.disconnect(); }
    }
}
