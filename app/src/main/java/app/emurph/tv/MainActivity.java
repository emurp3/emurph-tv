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
    private final String defaultRadioUrl = "http://34.26.99.249:8080/";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs = getSharedPreferences("emurph_tv", MODE_PRIVATE);
        activeProfile = prefs.getString("active_profile", "");
        showHome();
    }

    @Override protected void onDestroy() {
        if (player != null) player.release();
        super.onDestroy();
    }

    private void showHome() {
        releasePlayer();
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(2, 5, 16));
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
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String homeHtml() {
        String user = activeProfile.isEmpty() ? "No user" : esc(activeProfile);
        String expiration = esc(expiration());
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
        "<style>" +
        "*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#020510;color:#fff;font-family:Arial,Helvetica,sans-serif}" +
        "body:before{content:'';position:fixed;inset:0;background:radial-gradient(circle at 15% 70%,rgba(0,93,255,.16),transparent 32%),radial-gradient(circle at 86% 72%,rgba(255,35,82,.13),transparent 30%),linear-gradient(180deg,#030817 0%,#020510 75%);z-index:-3}" +
        ".studio{position:fixed;right:-50px;bottom:-25px;width:260px;height:520px;opacity:.17;filter:drop-shadow(0 0 30px #ff244e);z-index:-2}.studio:before{content:'';position:absolute;right:75px;top:0;width:70px;height:210px;border:8px solid #ff315d;border-radius:42px}.studio:after{content:'';position:absolute;right:20px;top:175px;width:180px;height:250px;border-right:9px solid #467dff;border-bottom:9px solid #467dff;border-radius:0 0 90px 0}" +
        ".wrap{height:100%;padding:26px 34px 22px;display:grid;grid-template-rows:72px 1fr 92px 46px;gap:16px}" +
        ".top{display:grid;grid-template-columns:300px 1fr 54px 54px 54px 54px;gap:12px;align-items:center}.logo{font-family:cursive;font-style:italic;font-weight:900;font-size:39px;letter-spacing:-2px;text-shadow:0 0 18px rgba(70,125,255,.35)}.logo b{color:#ff315d;font-size:44px}.search,.icon{height:52px;border:1px solid #33405d;background:linear-gradient(180deg,#0d1529,#080f21);border-radius:12px;color:#dce6ff;display:flex;align-items:center;justify-content:center;font-size:17px}.search{justify-content:flex-start;padding-left:22px}.icon{font-size:22px}" +
        ".cards{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;min-height:0}.card{position:relative;border:1px solid #31405f;border-radius:14px;overflow:hidden;padding:20px 18px;display:flex;flex-direction:column;justify-content:flex-end;background:#101a35;box-shadow:inset 0 0 35px rgba(255,255,255,.025)}" +
        ".card:before{content:'';position:absolute;inset:0;background:linear-gradient(180deg,rgba(255,255,255,.03),rgba(2,5,16,.35));z-index:0}.card>*{position:relative}.live{background:linear-gradient(160deg,#075bd8 0%,#082758 67%,#071329)}.movies{background:linear-gradient(160deg,#d32f3d 0%,#621724 66%,#140812)}.series{background:linear-gradient(160deg,#7738d8 0%,#381772 66%,#10091d)}.radio{background:radial-gradient(circle at 50% 37%,rgba(0,117,255,.5),transparent 23%),linear-gradient(160deg,#101b3a,#080b1a);border-color:#ff315d;box-shadow:0 0 18px rgba(255,49,93,.18),inset 0 0 30px rgba(52,111,255,.12)}" +
        ".art{flex:1;display:flex;align-items:center;justify-content:center;font-size:76px;text-shadow:0 0 20px rgba(255,255,255,.28)}.radio .art{font-size:68px}.featured{position:absolute;left:15px;top:14px;background:#ef234a;border-radius:4px;padding:4px 8px;font-size:11px;font-weight:bold}.card h2{margin:0 0 7px;font-size:24px}.card p{margin:0;color:#d6def0;font-size:14px}.wave{height:18px;margin:9px 0;background:repeating-linear-gradient(90deg,#2c78ff 0 3px,transparent 3px 7px,#ff315d 7px 10px,transparent 10px 14px);mask:linear-gradient(180deg,transparent 0,#000 35%,#000 65%,transparent 100%)}" +
        ".features{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.feature{border:1px solid #263653;border-radius:12px;background:linear-gradient(180deg,#0c1428,#070d1c);display:flex;align-items:center;padding:0 24px;gap:16px}.feature .fi{font-size:34px}.feature b{display:block;font-size:18px}.feature small{color:#aebbd2;font-size:13px}" +
        ".footer{border:1px solid #1d2b48;border-radius:10px;background:#080e1d;display:flex;align-items:center;justify-content:space-between;padding:0 18px;color:#b8c3d8;font-size:14px;box-shadow:0 8px 30px rgba(0,0,0,.25)}.footer strong{color:#ff4569}" +
        "[tabindex]{outline:none;transition:.12s transform,.12s box-shadow,.12s border-color}[tabindex]:focus{transform:scale(1.035);border-color:#eaf2ff!important;box-shadow:0 0 0 3px #1b71ff,0 0 22px #ff315d!important;z-index:10}" +
        "</style></head><body><div class='studio'></div><div class='wrap'>" +
        "<div class='top'><div class='logo'>EMurph <b>TV</b></div><div class='search' tabindex='1' onclick='EMurph.masterSearch()'>⌕ &nbsp; Master Search</div><div class='icon' tabindex='2' onclick='EMurph.message(\"No new notifications\")'>🔔</div><div class='icon' tabindex='3' onclick='EMurph.users()'>👤</div><div class='icon' tabindex='4' onclick='EMurph.message(\"Recording controls are being finalized\")'>REC</div><div class='icon' tabindex='5' onclick='EMurph.settings()'>⚙</div></div>" +
        "<div class='cards'><div class='card live' tabindex='6' onclick='EMurph.browse(\"live\")'><div class='art'>▣</div><h2>LIVE TV</h2><p>Watch Live Channels</p></div>" +
        "<div class='card movies' tabindex='7' onclick='EMurph.browse(\"movie\")'><div class='art'>◉</div><h2>MOVIES</h2><p>Thousands of Movies</p></div>" +
        "<div class='card series' tabindex='8' onclick='EMurph.browse(\"series\")'><div class='art'>▰</div><h2>SERIES</h2><p>Binge Worthy Shows</p></div>" +
        "<div class='card radio' tabindex='9' onclick='EMurph.radio()'><span class='featured'>FEATURED</span><div class='art'>📻</div><h2>EMURPH <span style='color:#ff315d'>RADIO</span></h2><div class='wave'></div><p>Urban Contemporary Gospel</p></div></div>" +
        "<div class='features'><div class='feature' tabindex='10' onclick='EMurph.browse(\"live\")'><span class='fi'>▤</span><div><b>LIVE WITH EPG</b><small>See what’s on now</small></div></div><div class='feature' tabindex='11' onclick='EMurph.message(\"Multi-screen is staged for the next build\")'><span class='fi'>▦</span><div><b>MULTI-SCREEN</b><small>Watch on multiple devices</small></div></div><div class='feature' tabindex='12' onclick='EMurph.browse(\"live\")'><span class='fi'>◴</span><div><b>CATCH UP</b><small>Never miss a moment</small></div></div></div>" +
        "<div class='footer'><span>▣ &nbsp; Expiration: &nbsp;<strong>"+expiration+"</strong></span><span>👤 &nbsp; Logged in: &nbsp;<strong>"+user+"</strong></span></div></div>" +
        "<script>document.addEventListener('keydown',e=>{const f=[...document.querySelectorAll('[tabindex]')];let i=f.indexOf(document.activeElement);if(i<0)i=0;if(e.key==='ArrowRight')i=Math.min(f.length-1,i+1);if(e.key==='ArrowLeft')i=Math.max(0,i-1);if(e.key==='ArrowDown')i=Math.min(f.length-1,i+(i<6?6:(i<10?4:3)));if(e.key==='ArrowUp')i=Math.max(0,i-(i<6?1:(i<10?6:4)));if(['ArrowRight','ArrowLeft','ArrowDown','ArrowUp'].includes(e.key)){e.preventDefault();f[i].focus()}});setTimeout(()=>document.querySelector('[tabindex]').focus(),250);</script></body></html>";
    }

    public class Bridge {
        @JavascriptInterface public void masterSearch() { runOnUiThread(() -> showSearch()); }
        @JavascriptInterface public void users() { runOnUiThread(() -> showUsers()); }
        @JavascriptInterface public void settings() { runOnUiThread(() -> showSettings()); }
        @JavascriptInterface public void browse(String type) { runOnUiThread(() -> browseNative(type)); }
        @JavascriptInterface public void radio() { runOnUiThread(() -> showRadio()); }
        @JavascriptInterface public void message(String s) { runOnUiThread(() -> toast(s)); }
    }

    private void showUsers() {
        releasePlayer();
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(38),dp(25),dp(38),dp(25)); root.setBackgroundColor(Color.rgb(2,5,16));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(label("EMurph TV",32,Color.WHITE,true), new LinearLayout.LayoutParams(0,dp(62),1));
        top.addView(label("LIST USERS",22,Color.WHITE,true), new LinearLayout.LayoutParams(0,dp(62),1));
        TextView add = button("＋ ADD USER", Color.rgb(18,76,180), v -> showAddUser()); top.addView(add,new LinearLayout.LayoutParams(dp(190),dp(54)));
        root.addView(top);
        LinearLayout cards = new LinearLayout(this); cards.setOrientation(LinearLayout.HORIZONTAL); cards.setPadding(0,dp(28),0,0); cards.setGravity(Gravity.TOP);
        try {
            JSONArray users = new JSONArray(prefs.getString("profiles","[]"));
            if (users.length()==0) {
                TextView empty=label("No users saved yet. Select ADD USER to connect your service.",20,Color.rgb(170,184,210),false); empty.setGravity(Gravity.CENTER); root.addView(empty,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root); return;
            }
            for(int i=0;i<users.length();i++){
                JSONObject p=users.getJSONObject(i); String name=p.optString("name"); boolean active=name.equals(activeProfile);
                LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(22),dp(20),dp(22),dp(20));
                card.setBackground(panel(active?Color.rgb(26,92,210):Color.rgb(13,22,43),active?Color.rgb(255,49,93):Color.rgb(55,69,99),14,active?3:1)); card.setFocusable(true);
                card.addView(label("◉  "+name,24,Color.WHITE,true)); card.addView(label("Username: "+p.optString("username"),15,Color.rgb(184,198,222),false)); card.addView(label("Server: EMurph TV Main",15,Color.rgb(184,198,222),false));
                card.setOnClickListener(v->{activeProfile=name;prefs.edit().putString("active_profile",name).apply();showHome();}); focusGlow(card);
                cards.addView(card,new LinearLayout.LayoutParams(0,dp(220),1)); if(i<users.length()-1) cards.addView(space(dp(18),1));
            }
        } catch(Exception ignored) {}
        root.addView(cards,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }

    private void showAddUser(){
        releasePlayer();
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.HORIZONTAL);root.setPadding(dp(48),dp(34),dp(48),dp(34));root.setBackgroundColor(Color.rgb(2,5,16));
        LinearLayout art=new LinearLayout(this);art.setOrientation(LinearLayout.VERTICAL);art.setGravity(Gravity.CENTER);art.addView(label("🎙",84,Color.rgb(76,126,255),false));art.addView(label("EMurph RADIO",30,Color.WHITE,true));art.addView(label("STREAM • LISTEN • INSPIRE",14,Color.rgb(255,77,111),true));root.addView(art,new LinearLayout.LayoutParams(0,-1,.9f));
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(25),dp(10),dp(25),dp(10));form.setBackground(panel(Color.rgb(8,14,29),Color.rgb(43,61,96),16,1));
        TextView title=label("Enter Your Login Details",28,Color.WHITE,true);title.setGravity(Gravity.CENTER);form.addView(title,new LinearLayout.LayoutParams(-1,dp(66)));
        EditText n=input("Profile Name"),u=input("Username"),p=input("Password"),s=input("Server URL");p.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        for(EditText e:new EditText[]{n,u,p,s}){form.addView(e,new LinearLayout.LayoutParams(-1,dp(58)));form.addView(space(1,dp(11)));}
        form.addView(button("ADD USER",Color.rgb(210,31,69),v->addUser(n,u,p,s)),new LinearLayout.LayoutParams(-1,dp(58)));form.addView(space(1,dp(12)));form.addView(button("LIST USERS",Color.rgb(16,31,63),v->showUsers()),new LinearLayout.LayoutParams(-1,dp(52)));
        root.addView(form,new LinearLayout.LayoutParams(0,-1,1.15f));setContentView(root);
    }

    private void showSearch(){ EditText e=input("Search channels, movies, or series"); new AlertDialog.Builder(this).setTitle("Master Search").setView(e).setPositiveButton("Search",(d,w)->performSearch(e.getText().toString().trim())).setNegativeButton("Cancel",null).show(); }
    private void performSearch(String q){ if(q.isEmpty()){toast("Enter a search term");return;} if(!hasActiveUser()){toast("Add or select a user first.");return;} new Thread(()->{JSONArray out=new JSONArray();try{JSONObject p=profile(activeProfile);for(String a:new String[]{"get_live_streams","get_vod_streams","get_series"}){JSONArray x=getArray(api(p)+"&action="+a);for(int i=0;i<x.length();i++){JSONObject o=x.optJSONObject(i);String n=o.optString("name",o.optString("title"));if(n.toLowerCase(Locale.US).contains(q.toLowerCase(Locale.US)))out.put(o);}}}catch(Exception ignored){}runOnUiThread(()->showResults("Search: "+q,out));}).start(); }

    private void browseNative(String type){ if(!hasActiveUser()){toast("Add or select an IPTV user first.");showUsers();return;} LinearLayout root=new LinearLayout(this);root.setGravity(Gravity.CENTER);root.setBackgroundColor(Color.rgb(2,5,16));ProgressBar pb=new ProgressBar(this);root.addView(pb);setContentView(root);new Thread(()->{JSONArray arr=new JSONArray();try{JSONObject p=profile(activeProfile);String action=type.equals("live")?"get_live_streams":type.equals("movie")?"get_vod_streams":"get_series";arr=getArray(api(p)+"&action="+action);}catch(Exception ignored){}JSONArray result=arr;runOnUiThread(()->showResults(type.toUpperCase(Locale.US),result));}).start(); }
    private void showResults(String title,JSONArray arr){ LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(30),dp(20),dp(30),dp(20));root.setBackgroundColor(Color.rgb(2,5,16));LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.addView(button("← HOME",Color.rgb(16,31,63),v->showHome()),new LinearLayout.LayoutParams(dp(150),dp(52)));top.addView(label(title,27,Color.WHITE,true),new LinearLayout.LayoutParams(0,dp(58),1));root.addView(top);ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);if(arr.length()==0){TextView n=label("No items returned.",18,Color.LTGRAY,false);n.setGravity(Gravity.CENTER);list.addView(n,new LinearLayout.LayoutParams(-1,dp(180)));}for(int i=0;i<Math.min(arr.length(),250);i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;String name=o.optString("name",o.optString("title","Untitled"));list.addView(button(name,Color.rgb(10,20,42),v->playItem(o)),new LinearLayout.LayoutParams(-1,dp(58)));list.addView(space(1,dp(7)));}scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root); }
    private void playItem(JSONObject o){try{JSONObject p=profile(activeProfile);if(o.has("series_id")){toast("Episode browsing is coming in the next build.");return;}String id=o.optString("stream_id"),ext=o.optString("container_extension","m3u8");boolean movie="movie".equals(o.optString("stream_type"))||o.has("rating_5based");String url=p.optString("server")+"/"+(movie?"movie":"live")+"/"+enc(p.optString("username"))+"/"+enc(p.optString("password"))+"/"+id+"."+ext;showPlayer(o.optString("name","Playing"),url);}catch(Exception e){toast("Unable to build stream URL.");}}
    private void showPlayer(String title,String url){releasePlayer();LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.BLACK);TextView back=button("← HOME  •  "+title,Color.rgb(10,20,42),v->showHome());root.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));PlayerView view=new PlayerView(this);player=new ExoPlayer.Builder(this).build();view.setPlayer(player);player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));player.prepare();player.play();root.addView(view,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private void showRadio(){releasePlayer();LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(34),dp(25),dp(34),dp(25));root.setGravity(Gravity.CENTER);root.setBackgroundColor(Color.rgb(2,5,16));root.addView(label("📻",88,Color.WHITE,false));root.addView(label("EMurph RADIO",38,Color.WHITE,true));root.addView(label("Urban Contemporary Gospel",19,Color.rgb(180,196,222),false));TextView status=label("Ready",16,Color.rgb(180,196,222),false);status.setGravity(Gravity.CENTER);root.addView(status);root.addView(space(1,dp(22)));root.addView(button("▶ PLAY RADIO",Color.rgb(210,31,69),v->{releasePlayer();player=new ExoPlayer.Builder(this).build();player.setMediaItem(MediaItem.fromUri(Uri.parse(prefs.getString("radio_url",defaultRadioUrl))));player.prepare();player.play();status.setText("Now Playing");}),new LinearLayout.LayoutParams(dp(440),dp(62)));root.addView(space(1,dp(14)));root.addView(button("← HOME",Color.rgb(16,31,63),v->showHome()),new LinearLayout.LayoutParams(dp(440),dp(54)));setContentView(root);}
    private void showSettings(){String[] options={"Switch User","Update EMurph Radio URL","Clear Saved Users","Cancel"};new AlertDialog.Builder(this).setTitle("EMurph TV Settings").setItems(options,(d,w)->{if(w==0)showUsers();else if(w==1)editRadioUrl();else if(w==2){prefs.edit().clear().apply();activeProfile="";showHome();}}).show();}
    private void editRadioUrl(){EditText e=input("Radio URL");e.setText(prefs.getString("radio_url",defaultRadioUrl));new AlertDialog.Builder(this).setTitle("EMurph Radio URL").setView(e).setPositiveButton("Save",(d,w)->prefs.edit().putString("radio_url",e.getText().toString().trim()).apply()).setNegativeButton("Cancel",null).show();}

    private void addUser(EditText n,EditText u,EditText p,EditText s){String name=n.getText().toString().trim(),user=u.getText().toString().trim(),pass=p.getText().toString(),server=s.getText().toString().trim();if(name.isEmpty()||user.isEmpty()||pass.isEmpty()||server.isEmpty()){toast("Complete all four fields.");return;}if(server.endsWith("/"))server=server.substring(0,server.length()-1);String fs=server;toast("Checking login…");new Thread(()->{try{JSONObject response=getObject(fs+"/player_api.php?username="+enc(user)+"&password="+enc(pass));JSONObject info=response.optJSONObject("user_info");if(info==null)throw new Exception("No account data returned");JSONObject obj=new JSONObject();obj.put("name",name);obj.put("username",user);obj.put("password",pass);obj.put("server",fs);obj.put("exp",info.optString("exp_date",""));JSONArray arr=new JSONArray(prefs.getString("profiles","[]"));for(int i=arr.length()-1;i>=0;i--)if(arr.optJSONObject(i).optString("name").equals(name))arr.remove(i);arr.put(obj);prefs.edit().putString("profiles",arr.toString()).putString("active_profile",name).apply();activeProfile=name;runOnUiThread(this::showHome);}catch(Exception e){runOnUiThread(()->toast("Login failed: "+e.getMessage()));}}).start();}

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
    private String api(JSONObject p)throws Exception{return p.getString("server")+"/player_api.php?username="+enc(p.getString("username"))+"&password="+enc(p.getString("password"));}
    private String expiration(){if(activeProfile.isEmpty())return "Not connected";try{String x=profile(activeProfile).optString("exp","");if(x.isEmpty()||"null".equals(x))return "Unknown";long sec=Long.parseLong(x);return new SimpleDateFormat("MMMM d, yyyy",Locale.US).format(new Date(sec*1000));}catch(Exception e){return "Unknown";}}
    private String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private JSONObject getObject(String url)throws Exception{return new JSONObject(read(url));}
    private JSONArray getArray(String url)throws Exception{return new JSONArray(read(url));}
    private String read(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(25000);c.setRequestProperty("User-Agent","EMurphTV/0.2");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];for(int n;(n=in.read(b))>0;)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}finally{c.disconnect();}}
}
