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
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>"+
        "*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;color:#fff;font-family:Arial,sans-serif;background:#020611}"+
        "body:before{content:'';position:fixed;inset:0;background:radial-gradient(circle at 13% 75%,rgba(24,96,255,.24),transparent 34%),radial-gradient(circle at 88% 70%,rgba(255,35,85,.18),transparent 30%),linear-gradient(180deg,#020817,#02040c);z-index:-4}"+
        ".micbg{position:fixed;right:-70px;top:40px;width:270px;height:600px;opacity:.16;filter:drop-shadow(0 0 28px #ff315d);z-index:-2}.micbg:before{content:'';position:absolute;right:90px;top:0;width:82px;height:250px;border:9px solid #ff315d;border-radius:45px}.micbg:after{content:'';position:absolute;right:25px;top:205px;width:200px;height:280px;border-right:10px solid #3978ff;border-bottom:10px solid #3978ff;border-radius:0 0 105px 0}"+
        ".eq{position:fixed;left:0;right:0;bottom:0;height:80px;opacity:.25;background:repeating-linear-gradient(90deg,#216dff 0 3px,transparent 3px 9px,#ff315d 9px 12px,transparent 12px 18px);mask:linear-gradient(transparent,#000)}"+
        ".wrap{height:100%;padding:25px 38px 20px;display:grid;grid-template-rows:78px 1fr 92px 48px;gap:15px}"+
        ".top{display:grid;grid-template-columns:310px 1fr 54px 54px 54px 54px;gap:12px;align-items:center}"+
        ".logo{font-family:cursive;font-style:italic;font-weight:900;font-size:43px;letter-spacing:-3px;text-shadow:0 0 18px rgba(60,125,255,.5)}.logo b{color:#ff315d;font-size:48px}.antenna{font-size:22px;position:relative;top:-19px;left:-18px}"+
        ".search,.icon{height:53px;border:1px solid #314365;background:linear-gradient(180deg,#101a31,#080f20);border-radius:11px;color:#eaf0ff;display:flex;align-items:center;justify-content:center}.search{justify-content:flex-start;padding-left:22px;font-size:17px}.icon{font-size:21px}"+
        ".cards{display:grid;grid-template-columns:repeat(4,1fr);gap:15px;min-height:0}.card{position:relative;overflow:hidden;border:1px solid #31476d;border-radius:13px;padding:17px;display:flex;flex-direction:column;justify-content:flex-end;box-shadow:inset 0 0 32px rgba(255,255,255,.04),0 12px 28px rgba(0,0,0,.35)}"+
        ".card:after{content:'';position:absolute;inset:0;background:linear-gradient(180deg,rgba(255,255,255,.06),rgba(0,0,0,.48));z-index:0}.card>*{z-index:2;position:relative}.live{background:radial-gradient(circle at 50% 28%,rgba(73,155,255,.38),transparent 26%),linear-gradient(160deg,#0a65ea,#082e73 62%,#061226)}.movies{background:radial-gradient(circle at 50% 28%,rgba(255,118,118,.28),transparent 25%),linear-gradient(160deg,#e52f45,#761627 62%,#170812)}.series{background:radial-gradient(circle at 50% 28%,rgba(177,92,255,.35),transparent 25%),linear-gradient(160deg,#8744dc,#421c86 62%,#12091f)}.radio{background:radial-gradient(circle at 50% 30%,rgba(0,114,255,.55),transparent 26%),linear-gradient(160deg,#111d41,#070a18);border-color:#ff315d;box-shadow:0 0 18px #176bff,0 0 22px rgba(255,49,93,.65),inset 0 0 35px rgba(51,109,255,.2)}"+
        ".art{flex:1;display:flex;align-items:center;justify-content:center}.art svg{width:108px;height:108px;filter:drop-shadow(0 0 10px rgba(255,255,255,.22))}.scene{position:absolute;left:0;right:0;bottom:58px;height:76px;opacity:.33;background:linear-gradient(180deg,transparent,rgba(0,0,0,.95))}.movies .scene:before{content:'';position:absolute;left:8%;right:8%;bottom:4px;height:34px;border-radius:18px 18px 3px 3px;background:repeating-linear-gradient(90deg,#34101a 0 22px,#681526 22px 46px)}.series .scene:before{content:'';position:absolute;left:10%;right:10%;bottom:6px;height:33px;border-radius:16px 16px 4px 4px;background:#27124a}.live .scene:before{content:'';position:absolute;left:12%;right:12%;bottom:5px;height:25px;background:#071326;border-top:4px solid #3275ff}"+
        ".featured{position:absolute;left:14px;top:13px;background:#f02750;border-radius:4px;padding:4px 8px;font-size:10px;font-weight:bold}.card h2{margin:0 0 5px;font-size:23px}.card p{margin:0;color:#d7e0f2;font-size:13px}.wave{height:17px;margin:8px 0;background:repeating-linear-gradient(90deg,#2c78ff 0 3px,transparent 3px 7px,#ff315d 7px 10px,transparent 10px 14px);mask:linear-gradient(transparent,#000 35%,#000 65%,transparent)}"+
        ".features{display:grid;grid-template-columns:repeat(3,1fr);gap:15px}.feature{border:1px solid #283a5b;border-radius:11px;background:linear-gradient(180deg,#0e1830,#070d1c);display:flex;align-items:center;padding:0 22px;gap:15px;box-shadow:inset 3px 0 #176bff}.feature:nth-child(3){box-shadow:inset -3px 0 #ff315d}.feature .fi{font-size:33px}.feature b{display:block;font-size:17px}.feature small{color:#aebbd2;font-size:12px}"+
        ".footer{border:1px solid #1e3152;border-radius:9px;background:linear-gradient(180deg,#0a1224,#060b17);display:flex;align-items:center;justify-content:space-between;padding:0 18px;color:#b9c5da;font-size:14px;box-shadow:0 0 18px rgba(29,100,255,.12)}.footer strong{color:#ff466c}.announce{position:fixed;left:38px;right:38px;bottom:65px;z-index:30;background:linear-gradient(90deg,#174aa8,#a41442);border:1px solid #ff5579;border-radius:9px;padding:10px 16px}"+
        "[tabindex]{outline:none;transition:.12s}[tabindex]:focus{transform:scale(1.035);border-color:#fff!important;box-shadow:0 0 0 3px #176bff,0 0 24px #ff315d!important;z-index:20}"+
        "</style></head><body>"+announce+"<div class='micbg'></div><div class='eq'></div><div class='wrap'>"+
        "<div class='top'><div class='logo'>EMurph <b>TV</b><span class='antenna'>⌁</span></div><div class='search' tabindex='1' onclick='EMurph.masterSearch()'>⌕ &nbsp; Master Search</div><div class='icon' tabindex='2' onclick='EMurph.message(\"No new notifications\")'>🔔</div><div class='icon' tabindex='3' onclick='EMurph.users()'>👤</div><div class='icon' tabindex='4' onclick='EMurph.message(\"Recording controls coming soon\")'>REC</div><div class='icon' tabindex='5' onclick='EMurph.settings()'>⚙</div></div>"+
        "<div class='cards'>"+
        card("live","LIVE TV","Watch Live Channels","tv","6","EMurph.browse(\"live\")","")+
        card("movies","MOVIES","Thousands of Movies","movie","7","EMurph.browse(\"movie\")","")+
        card("series","SERIES","Binge Worthy Shows","series","8","EMurph.browse(\"series\")","")+
        card("radio","EMURPH <span style='color:#ff315d'>RADIO</span>","Urban Contemporary Gospel","radio","9","EMurph.radio()","<span class='featured'>FEATURED</span>")+
        "</div><div class='features'><div class='feature' tabindex='10' onclick='EMurph.browse(\"live\")'><span class='fi'>▤</span><div><b>LIVE WITH EPG</b><small>See what’s on now</small></div></div><div class='feature' tabindex='11' onclick='EMurph.message(\"Multi-screen is staged\")'><span class='fi'>▦</span><div><b>MULTI-SCREEN</b><small>Watch on multiple devices</small></div></div><div class='feature' tabindex='12' onclick='EMurph.browse(\"live\")'><span class='fi'>◴</span><div><b>CATCH UP</b><small>Never miss a moment</small></div></div></div>"+
        "<div class='footer'><span>▣ &nbsp; Expiration: <strong>"+exp+"</strong></span><span>👤 &nbsp; Logged in: <strong>"+user+"</strong></span></div></div>"+
        "<script>document.addEventListener('keydown',e=>{const f=[...document.querySelectorAll('[tabindex]')];let i=f.indexOf(document.activeElement);if(i<0)i=0;const k=e.key;if(k==='ArrowRight')i=Math.min(f.length-1,i+1);if(k==='ArrowLeft')i=Math.max(0,i-1);if(k==='ArrowDown')i=Math.min(f.length-1,i+(i<6?6:(i<10?4:3)));if(k==='ArrowUp')i=Math.max(0,i-(i<6?1:(i<10?6:4)));if(k.startsWith('Arrow')){e.preventDefault();f[i].focus()}});setTimeout(()=>document.querySelector('[tabindex]').focus(),250);</script></body></html>";
    }

    private String card(String cls,String title,String sub,String icon,String tab,String click,String badge){
        String svg;
        if(icon.equals("tv")) svg="<svg viewBox='0 0 120 120'><g fill='none' stroke='white' stroke-width='5'><rect x='20' y='28' width='80' height='62' rx='9'/><path d='M45 18l15 10 15-10M48 45l30 15-30 15zM40 98h40'/></g></svg>";
        else if(icon.equals("movie")) svg="<svg viewBox='0 0 120 120'><circle cx='60' cy='58' r='39' fill='none' stroke='white' stroke-width='5'/><g fill='white'><circle cx='60' cy='35' r='8'/><circle cx='82' cy='54' r='8'/><circle cx='73' cy='79' r='8'/><circle cx='47' cy='79' r='8'/><circle cx='38' cy='54' r='8'/></g></svg>";
        else if(icon.equals("series")) svg="<svg viewBox='0 0 120 120'><g fill='none' stroke='white' stroke-width='5'><rect x='22' y='42' width='76' height='52' rx='6'/><path d='M22 42l12-22h18L40 42m18 0l12-22h18L76 42M22 58h76'/></g></svg>";
        else svg="<svg viewBox='0 0 120 120'><defs><linearGradient id='g' x1='0' x2='1'><stop stop-color='#2478ff'/><stop offset='1' stop-color='#ff315d'/></linearGradient></defs><g fill='none' stroke='url(#g)' stroke-width='5'><rect x='25' y='48' width='70' height='45' rx='8'/><path d='M38 48l42-26M39 63h25M39 76h18'/><circle cx='78' cy='70' r='8'/><path d='M98 42q15 15 0 30M105 32q26 25 0 50'/></g></svg>";
        return "<div class='card "+cls+"' tabindex='"+tab+"' onclick='"+click+"'>"+badge+"<div class='art'>"+svg+"</div><div class='scene'></div><h2>"+title+"</h2>"+(icon.equals("radio")?"<div class='wave'></div>":"")+"<p>"+sub+"</p></div>";
    }

    public class Bridge {
        @JavascriptInterface public void masterSearch(){runOnUiThread(()->showSearch());}
        @JavascriptInterface public void users(){runOnUiThread(()->showUsers());}
        @JavascriptInterface public void settings(){runOnUiThread(()->showSettings());}
        @JavascriptInterface public void browse(String type){runOnUiThread(()->browseNative(type));}
        @JavascriptInterface public void radio(){runOnUiThread(()->showRadio());}
        @JavascriptInterface public void message(String s){runOnUiThread(()->toast(s));}
    }

    private void showUsers(){
        releasePlayer();
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(38),dp(25),dp(38),dp(25));root.setBackgroundColor(Color.rgb(2,5,16));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(label("EMurph TV",32,Color.WHITE,true),new LinearLayout.LayoutParams(0,dp(62),1));
        top.addView(label("LIST USERS",22,Color.WHITE,true),new LinearLayout.LayoutParams(0,dp(62),1));
        top.addView(button("＋ ADD USER",Color.rgb(18,76,180),v->showAddUser()),new LinearLayout.LayoutParams(dp(190),dp(54)));
        root.addView(top);
        LinearLayout cards=new LinearLayout(this);cards.setOrientation(LinearLayout.HORIZONTAL);cards.setPadding(0,dp(28),0,0);
        try{
            JSONArray users=new JSONArray(prefs.getString("profiles","[]"));
            if(users.length()==0){TextView e=label("No users saved yet. Select ADD USER to connect your service.",20,Color.rgb(170,184,210),false);e.setGravity(Gravity.CENTER);root.addView(e,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);return;}
            for(int i=0;i<users.length();i++){
                JSONObject p=users.getJSONObject(i);String name=p.optString("name");boolean active=name.equals(activeProfile);
                LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(22),dp(20),dp(22),dp(20));c.setFocusable(true);
                c.setBackground(panel(active?Color.rgb(17,58,145):Color.rgb(10,18,36),active?Color.rgb(255,49,93):Color.rgb(54,75,112),14,active?3:1));
                c.addView(label("◉  "+name,24,Color.WHITE,true));c.addView(label("Username: "+p.optString("username"),15,Color.rgb(184,198,222),false));c.addView(label("Server: "+p.optString("server"),14,Color.rgb(184,198,222),false));
                c.setOnClickListener(v->{activeProfile=name;prefs.edit().putString("active_profile",name).apply();showHome();});focusGlow(c);
                cards.addView(c,new LinearLayout.LayoutParams(0,dp(220),1));if(i<users.length()-1)cards.addView(space(dp(18),1));
            }
        }catch(Exception ignored){}
        root.addView(cards,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void showAddUser(){
        releasePlayer();
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.HORIZONTAL);root.setPadding(dp(48),dp(34),dp(48),dp(34));root.setBackgroundColor(Color.rgb(2,5,16));
        LinearLayout art=new LinearLayout(this);art.setOrientation(LinearLayout.VERTICAL);art.setGravity(Gravity.CENTER);art.addView(label("🎙",84,Color.rgb(76,126,255),false));art.addView(label("EMurph RADIO",30,Color.WHITE,true));art.addView(label("STREAM • LISTEN • INSPIRE",14,Color.rgb(255,77,111),true));root.addView(art,new LinearLayout.LayoutParams(0,-1,.9f));
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(25),dp(10),dp(25),dp(10));form.setBackground(panel(Color.rgb(8,14,29),Color.rgb(43,61,96),16,1));
        TextView title=label("Enter Your Login Details",28,Color.WHITE,true);title.setGravity(Gravity.CENTER);form.addView(title,new LinearLayout.LayoutParams(-1,dp(60)));
        EditText n=input("Profile Name"),u=input("Username"),p=input("Password"),s=input("Server URL");
        p.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        s.setText(prefs.getString("default_server_url",defaultServerUrl));
        for(EditText e:new EditText[]{n,u,p,s}){form.addView(e,new LinearLayout.LayoutParams(-1,dp(54)));form.addView(space(1,dp(8)));}
        form.addView(button("ADD USER",Color.rgb(210,31,69),v->addUser(n,u,p,s)),new LinearLayout.LayoutParams(-1,dp(56)));form.addView(space(1,dp(10)));form.addView(button("LIST USERS",Color.rgb(16,31,63),v->showUsers()),new LinearLayout.LayoutParams(-1,dp(50)));
        root.addView(form,new LinearLayout.LayoutParams(0,-1,1.15f));setContentView(root);
    }

    private void showSearch(){EditText e=input("Search channels, movies, or series");new AlertDialog.Builder(this).setTitle("Master Search").setView(e).setPositiveButton("Search",(d,w)->performSearch(e.getText().toString().trim())).setNegativeButton("Cancel",null).show();}
    private void performSearch(String q){if(q.isEmpty()){toast("Enter a search term");return;}if(!hasActiveUser()){toast("Add or select a user first.");return;}new Thread(()->{JSONArray out=new JSONArray();try{JSONObject p=profile(activeProfile);for(String a:new String[]{"get_live_streams","get_vod_streams","get_series"}){JSONArray x=getArray(api(p)+"&action="+a);for(int i=0;i<x.length();i++){JSONObject o=x.optJSONObject(i);String n=o.optString("name",o.optString("title"));if(n.toLowerCase(Locale.US).contains(q.toLowerCase(Locale.US)))out.put(o);}}}catch(Exception ignored){}runOnUiThread(()->showResults("Search: "+q,out));}).start();}
    private void browseNative(String type){if(!hasActiveUser()){toast("Add or select an IPTV user first.");showUsers();return;}LinearLayout root=new LinearLayout(this);root.setGravity(Gravity.CENTER);root.setBackgroundColor(Color.rgb(2,5,16));root.addView(new ProgressBar(this));setContentView(root);new Thread(()->{JSONArray arr=new JSONArray();try{JSONObject p=profile(activeProfile);String action=type.equals("live")?"get_live_streams":type.equals("movie")?"get_vod_streams":"get_series";arr=getArray(api(p)+"&action="+action);}catch(Exception ignored){}JSONArray r=arr;runOnUiThread(()->showResults(type.toUpperCase(Locale.US),r));}).start();}
    private void showResults(String title,JSONArray arr){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(30),dp(20),dp(30),dp(20));root.setBackgroundColor(Color.rgb(2,5,16));LinearLayout top=new LinearLayout(this);top.addView(button("← HOME",Color.rgb(16,31,63),v->showHome()),new LinearLayout.LayoutParams(dp(150),dp(52)));top.addView(label(title,27,Color.WHITE,true),new LinearLayout.LayoutParams(0,dp(58),1));root.addView(top);ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);for(int i=0;i<Math.min(arr.length(),250);i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;String name=o.optString("name",o.optString("title","Untitled"));list.addView(button(name,Color.rgb(10,20,42),v->playItem(o)),new LinearLayout.LayoutParams(-1,dp(58)));list.addView(space(1,dp(7)));}scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private void playItem(JSONObject o){try{JSONObject p=profile(activeProfile);if(o.has("series_id")){showSeriesEpisodes(o);return;}String id=o.optString("stream_id"),ext=o.optString("container_extension","m3u8");boolean movie="movie".equals(o.optString("stream_type"))||o.has("rating_5based");String url=profileServer(p)+"/"+(movie?"movie":"live")+"/"+enc(p.optString("username"))+"/"+enc(p.optString("password"))+"/"+id+"."+ext;showPlayer(o.optString("name","Playing"),url);}catch(Exception e){toast("Unable to build stream URL.");}}
    private void showSeriesEpisodes(JSONObject series){String name=series.optString("name","Series"),id=series.optString("series_id");LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER);l.setBackgroundColor(Color.rgb(2,5,16));l.addView(new ProgressBar(this));setContentView(l);new Thread(()->{JSONArray ep=new JSONArray();try{JSONObject p=profile(activeProfile);JSONObject info=getObject(api(p)+"&action=get_series_info&series_id="+enc(id));JSONObject seasons=info.optJSONObject("episodes");if(seasons!=null){Iterator<String> k=seasons.keys();while(k.hasNext()){JSONArray a=seasons.optJSONArray(k.next());if(a!=null)for(int i=0;i<a.length();i++)ep.put(a.optJSONObject(i));}}}catch(Exception ignored){}runOnUiThread(()->showEpisodeResults(name,ep));}).start();}
    private void showEpisodeResults(String title,JSONArray e){showResults(title,e);}
    private void playEpisode(JSONObject e){try{JSONObject p=profile(activeProfile);String url=profileServer(p)+"/series/"+enc(p.optString("username"))+"/"+enc(p.optString("password"))+"/"+e.optString("id")+"."+e.optString("container_extension","mp4");showPlayer(e.optString("title","Episode"),url);}catch(Exception x){toast("Unable to play episode.");}}
    private void showPlayer(String title,String url){releasePlayer();LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.BLACK);root.addView(button("← HOME • "+title,Color.rgb(10,20,42),v->showHome()),new LinearLayout.LayoutParams(-1,dp(52)));PlayerView view=new PlayerView(this);player=new ExoPlayer.Builder(this).build();view.setPlayer(player);player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));player.prepare();player.play();root.addView(view,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private void showRadio(){releasePlayer();LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setBackgroundColor(Color.rgb(2,5,16));root.addView(label("📻",88,Color.WHITE,false));root.addView(label("EMurph RADIO",38,Color.WHITE,true));TextView status=label("Ready",17,Color.LTGRAY,false);status.setGravity(Gravity.CENTER);root.addView(status);root.addView(button("▶ PLAY RADIO",Color.rgb(210,31,69),v->{releasePlayer();player=new ExoPlayer.Builder(this).build();player.setMediaItem(MediaItem.fromUri(Uri.parse(prefs.getString("radio_url",defaultRadioUrl))));player.prepare();player.play();status.setText("Now Playing");}),new LinearLayout.LayoutParams(dp(440),dp(62)));root.addView(button("← HOME",Color.rgb(16,31,63),v->showHome()),new LinearLayout.LayoutParams(dp(440),dp(54)));setContentView(root);}
    private void showSettings(){String[] o={"Switch User","Update EMurph Radio URL","Clear Saved Users","Cancel"};new AlertDialog.Builder(this).setTitle("EMurph TV Settings").setItems(o,(d,w)->{if(w==0)showUsers();else if(w==1)editRadioUrl();else if(w==2){prefs.edit().remove("profiles").remove("active_profile").apply();activeProfile="";showHome();}}).show();}
    private void editRadioUrl(){EditText e=input("Radio URL");e.setText(prefs.getString("radio_url",defaultRadioUrl));new AlertDialog.Builder(this).setTitle("EMurph Radio URL").setView(e).setPositiveButton("Save",(d,w)->prefs.edit().putString("radio_url",e.getText().toString().trim()).apply()).setNegativeButton("Cancel",null).show();}
    private void addUser(EditText n,EditText u,EditText p,EditText s){String name=n.getText().toString().trim(),user=u.getText().toString().trim(),pass=p.getText().toString(),server=normalizeServer(s.getText().toString());if(name.isEmpty()||user.isEmpty()||pass.isEmpty()||server.isEmpty()){toast("Complete all four fields.");return;}toast("Checking login…");new Thread(()->{try{JSONObject r=getObject(server+"/player_api.php?username="+enc(user)+"&password="+enc(pass));JSONObject info=r.optJSONObject("user_info");if(info==null)throw new Exception("No account data returned");JSONObject obj=new JSONObject();obj.put("name",name);obj.put("username",user);obj.put("password",pass);obj.put("server",server);obj.put("exp",info.optString("exp_date",""));JSONArray arr=new JSONArray(prefs.getString("profiles","[]"));for(int i=arr.length()-1;i>=0;i--)if(arr.optJSONObject(i).optString("name").equals(name))arr.remove(i);arr.put(obj);prefs.edit().putString("profiles",arr.toString()).putString("active_profile",name).apply();activeProfile=name;runOnUiThread(this::showHome);}catch(Exception e){runOnUiThread(()->toast("Login failed: "+e.getMessage()));}}).start();}

    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(14),dp(8),dp(14),dp(8));if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);return t;}
    private TextView button(String text,int color,View.OnClickListener click){TextView b=label(text,18,Color.WHITE,true);b.setGravity(Gravity.CENTER);b.setFocusable(true);b.setClickable(true);b.setBackground(panel(color,Color.rgb(65,82,118),12,1));b.setOnClickListener(click);focusGlow(b);return b;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(140,155,184));e.setTextColor(Color.WHITE);e.setTextSize(18);e.setSingleLine(true);e.setPadding(dp(16),0,dp(16),0);e.setBackground(panel(Color.rgb(9,17,35),Color.rgb(58,76,112),10,1));return e;}
    private android.graphics.drawable.GradientDrawable panel(int color,int stroke,int radius,int width){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));g.setStroke(dp(width),stroke);return g;}
    private void focusGlow(View v){v.setOnFocusChangeListener((view,f)->{view.animate().scaleX(f?1.035f:1f).scaleY(f?1.035f:1f).setDuration(110).start();view.setAlpha(f?1f:.96f);});}
    private Space space(int w,int h){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(w,h));return s;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void releasePlayer(){if(player!=null){player.release();player=null;}}
    private boolean hasActiveUser(){return !activeProfile.isEmpty();}
    private JSONObject profile(String name)throws Exception{JSONArray a=new JSONArray(prefs.getString("profiles","[]"));for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);if(name.equals(p.optString("name")))return p;}throw new Exception("Profile not found");}
    private String profileServer(JSONObject p){return normalizeServer(p.optString("server",prefs.getString("default_server_url",defaultServerUrl)));}
    private String api(JSONObject p)throws Exception{return profileServer(p)+"/player_api.php?username="+enc(p.getString("username"))+"&password="+enc(p.getString("password"));}
    private String expiration(){if(activeProfile.isEmpty())return "Not connected";try{String x=profile(activeProfile).optString("exp","");if(x.isEmpty()||"null".equals(x))return "Unknown";return new SimpleDateFormat("MMMM d, yyyy",Locale.US).format(new Date(Long.parseLong(x)*1000));}catch(Exception e){return "Unknown";}}
    private String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private String normalizeServer(String s){if(s==null)return "";s=s.trim();while(s.endsWith("/"))s=s.substring(0,s.length()-1);return s;}
    private void refreshRemoteConfig(){try{JSONObject cfg=getObject(remoteConfigUrl+"?t="+System.currentTimeMillis());String server=cfg.optString("iptv_url",defaultServerUrl).trim(),radio=cfg.optString("radio_url",defaultRadioUrl).trim();JSONObject a=cfg.optJSONObject("announcement");SharedPreferences.Editor e=prefs.edit();if(server.startsWith("http://")||server.startsWith("https://"))e.putString("default_server_url",server);if(radio.startsWith("http://")||radio.startsWith("https://"))e.putString("radio_url",radio);if(a!=null){e.putBoolean("announcement_enabled",a.optBoolean("enabled",false));e.putString("announcement_title",a.optString("title",""));e.putString("announcement_message",a.optString("message",""));}e.apply();runOnUiThread(this::showHome);}catch(Exception ignored){}}
    private JSONObject getObject(String url)throws Exception{return new JSONObject(read(url));}
    private JSONArray getArray(String url)throws Exception{return new JSONArray(read(url));}
    private String read(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(25000);c.setRequestProperty("User-Agent","EMurphTV/0.3");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];for(int n;(n=in.read(b))>0;)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}finally{c.disconnect();}}
}
