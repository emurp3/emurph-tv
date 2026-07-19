package app.emurph.tv;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.view.*;
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
    private final int BG=Color.rgb(3,7,18), PANEL=Color.rgb(9,16,34), TEXT=Color.WHITE;
    private final int MUTED=Color.rgb(174,184,205), BLUE=Color.rgb(18,115,234), RED=Color.rgb(255,49,90), PURPLE=Color.rgb(105,48,190);
    private LinearLayout root;
    private SharedPreferences prefs;
    private ExoPlayer player;
    private String activeProfile="";
    private final String defaultRadioUrl="http://34.26.99.249:8080/";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs=getSharedPreferences("emurph_tv",MODE_PRIVATE);
        activeProfile=prefs.getString("active_profile","");
        showHome();
    }

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h,float weight){return new LinearLayout.LayoutParams(w,h,weight);}
    private Space gap(int n){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(dp(n),1));return s;}
    private Space vgap(int n){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(n)));return s;}
    private GradientDrawable shape(int color,int radius,int stroke,int sw){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));if(sw>0)g.setStroke(dp(sw),stroke);return g;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(14),dp(8),dp(14),dp(8));t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView button(String label,int color,View.OnClickListener click){TextView b=text(label,18,TEXT,true);b.setGravity(Gravity.CENTER);b.setFocusable(true);b.setClickable(true);b.setBackground(shape(color,12,Color.argb(130,255,255,255),1));b.setOnClickListener(click);b.setOnFocusChangeListener((v,f)->{v.animate().scaleX(f?1.045f:1f).scaleY(f?1.045f:1f).setDuration(110).start();v.setBackground(shape(color,12,f?Color.WHITE:Color.argb(130,255,255,255),f?3:1));});return b;}
    private LinearLayout tile(String title,String sub,int color,View.OnClickListener click){LinearLayout c=col();c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setGravity(Gravity.CENTER);c.setFocusable(true);c.setClickable(true);c.setBackground(shape(color,16,Color.argb(130,255,255,255),1));TextView i=text(title.equals("EMURPH RADIO")?"♫":"▣",42,TEXT,true);i.setGravity(Gravity.CENTER);c.addView(i,lp(-1,0,1));TextView a=text(title,22,TEXT,true);a.setGravity(Gravity.CENTER);c.addView(a);TextView s=text(sub,13,Color.rgb(225,231,245),false);s.setGravity(Gravity.CENTER);c.addView(s);c.setOnClickListener(click);c.setOnFocusChangeListener((v,f)->{v.animate().scaleX(f?1.045f:1f).scaleY(f?1.045f:1f).setDuration(110).start();v.setBackground(shape(color,16,f?Color.WHITE:Color.argb(130,255,255,255),f?3:1));});return c;}

    private void base(String title){if(player!=null){player.release();player=null;}root=col();root.setPadding(dp(28),dp(18),dp(28),dp(18));root.setBackgroundColor(BG);setContentView(root);LinearLayout top=row();top.addView(text("EMURPH",28,TEXT,true));top.addView(text("TV",28,RED,true));top.addView(text(title,22,TEXT,true),lp(0,dp(58),1));root.addView(top,new LinearLayout.LayoutParams(-1,dp(62)));}

    private void showHome(){
        base("");
        LinearLayout tools=row();tools.addView(button("⌕  Master Search",PANEL,v->showSearch()),lp(0,dp(55),1));tools.addView(gap(12));tools.addView(button("⚙",PANEL,v->showSettings()),lp(dp(64),dp(55),0));tools.addView(gap(8));tools.addView(button("Switch User",PANEL,v->showUsers()),lp(dp(165),dp(55),0));root.addView(tools);
        LinearLayout cards=row();cards.setPadding(0,dp(20),0,dp(16));cards.addView(tile("LIVE TV","Watch Live Channels",Color.rgb(15,74,160),v->browse("live")),lp(0,-1,1));cards.addView(gap(14));cards.addView(tile("MOVIES","Thousands of Movies",Color.rgb(167,38,48),v->browse("movie")),lp(0,-1,1));cards.addView(gap(14));cards.addView(tile("SERIES","Binge Worthy Shows",Color.rgb(75,35,142),v->browse("series")),lp(0,-1,1));cards.addView(gap(14));cards.addView(tile("EMURPH RADIO","Urban Contemporary Gospel",Color.rgb(15,25,51),v->showRadio()),lp(0,-1,1));root.addView(cards,lp(-1,0,1));
        LinearLayout features=row();features.addView(button("LIVE WITH EPG",PANEL,v->browse("live")),lp(0,dp(72),1));features.addView(gap(14));features.addView(button("MULTI-SCREEN",PANEL,v->toast("Multi-screen is staged for the next build.")),lp(0,dp(72),1));features.addView(gap(14));features.addView(button("CATCH UP",PANEL,v->browse("live")),lp(0,dp(72),1));root.addView(features);
        LinearLayout footer=row();footer.addView(text("Expiration: "+expiration(),15,MUTED,false),lp(0,dp(48),1));footer.addView(text("Logged in: "+(activeProfile.isEmpty()?"No user":activeProfile),15,activeProfile.isEmpty()?MUTED:RED,true));root.addView(footer);
    }

    private void showSearch(){EditText e=input("Search channels, movies, or series");new AlertDialog.Builder(this).setTitle("Master Search").setView(e).setPositiveButton("Search",(d,w)->performSearch(e.getText().toString().trim())).setNegativeButton("Cancel",null).show();}
    private void performSearch(String q){if(q.isEmpty()){toast("Enter a search term");return;}if(!hasActiveUser()){toast("Add or select a user first.");return;}new Thread(()->{JSONArray out=new JSONArray();try{JSONObject p=profile(activeProfile);for(String a:new String[]{"get_live_streams","get_vod_streams","get_series"}){JSONArray x=getArray(api(p)+"&action="+a);for(int i=0;i<x.length();i++){JSONObject o=x.optJSONObject(i);String n=o.optString("name",o.optString("title"));if(n.toLowerCase(Locale.US).contains(q.toLowerCase(Locale.US)))out.put(o);}}}catch(Exception ignored){}runOnUiThread(()->showResults("Search: "+q,out));}).start();}

    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(18);e.setSingleLine(true);e.setPadding(dp(16),0,dp(16),0);e.setBackground(shape(PANEL,10,Color.rgb(100,118,150),1));return e;}
    private void showUsers(){base("LIST USERS");LinearLayout bar=row();bar.addView(text("Select a profile to continue",16,MUTED,false),lp(0,dp(60),1));bar.addView(button("＋ ADD USER",RED,v->showAddUser()),lp(dp(190),dp(54),0));root.addView(bar);LinearLayout cards=row();cards.setGravity(Gravity.TOP);try{JSONArray users=new JSONArray(prefs.getString("profiles","[]"));if(users.length()==0){TextView n=text("No users saved yet. Select ADD USER to connect your service.",20,MUTED,false);n.setGravity(Gravity.CENTER);root.addView(n,lp(-1,0,1));return;}for(int i=0;i<users.length();i++){JSONObject p=users.getJSONObject(i);String name=p.optString("name");LinearLayout c=col();c.setPadding(dp(20),dp(18),dp(20),dp(18));c.setBackground(shape(PANEL,14,name.equals(activeProfile)?BLUE:MUTED,name.equals(activeProfile)?2:1));c.addView(text("◉  "+name,22,TEXT,true));c.addView(text("Username: "+p.optString("username"),14,MUTED,false));c.addView(text("Server: EMurph TV Main",14,MUTED,false));c.addView(button("SELECT",name.equals(activeProfile)?BLUE:PURPLE,v->{activeProfile=name;prefs.edit().putString("active_profile",name).apply();showHome();}),new LinearLayout.LayoutParams(-1,dp(48)));cards.addView(c,lp(0,dp(220),1));if(i<users.length()-1)cards.addView(gap(16));}}catch(Exception ignored){}root.addView(cards,lp(-1,0,1));}
    private void showAddUser(){base("Enter Your Login Details");LinearLayout content=row();LinearLayout art=col();art.setGravity(Gravity.CENTER);TextView m=text("♫\nEMURPH RADIO",34,TEXT,true);m.setGravity(Gravity.CENTER);art.addView(m);content.addView(art,lp(0,-1,.8f));LinearLayout form=col();EditText n=input("Profile Name"),u=input("Username"),p=input("Password"),s=input("Server URL (http://host:port)");p.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);for(EditText e:new EditText[]{n,u,p,s}){form.addView(e,new LinearLayout.LayoutParams(-1,dp(58)));form.addView(vgap(10));}form.addView(button("ADD USER",RED,v->addUser(n,u,p,s)),new LinearLayout.LayoutParams(-1,dp(58)));form.addView(vgap(12));form.addView(button("LIST USERS",PANEL,v->showUsers()),new LinearLayout.LayoutParams(-1,dp(52)));content.addView(form,lp(0,-1,1.3f));root.addView(content,lp(-1,0,1));}
    private void addUser(EditText n,EditText u,EditText p,EditText s){String name=n.getText().toString().trim(),user=u.getText().toString().trim(),pass=p.getText().toString(),server=s.getText().toString().trim();if(name.isEmpty()||user.isEmpty()||pass.isEmpty()||server.isEmpty()){toast("Complete all four fields.");return;}if(server.endsWith("/"))server=server.substring(0,server.length()-1);String fs=server;toast("Checking login…");new Thread(()->{try{JSONObject response=getObject(fs+"/player_api.php?username="+enc(user)+"&password="+enc(pass));JSONObject info=response.optJSONObject("user_info");if(info==null)throw new Exception("No account data returned");JSONObject obj=new JSONObject();obj.put("name",name);obj.put("username",user);obj.put("password",pass);obj.put("server",fs);obj.put("exp",info.optString("exp_date",""));JSONArray arr=new JSONArray(prefs.getString("profiles","[]"));for(int i=arr.length()-1;i>=0;i--)if(arr.optJSONObject(i).optString("name").equals(name))arr.remove(i);arr.put(obj);prefs.edit().putString("profiles",arr.toString()).putString("active_profile",name).apply();activeProfile=name;runOnUiThread(this::showHome);}catch(Exception e){runOnUiThread(()->toast("Login failed: "+e.getMessage()));}}).start();}

    private void browse(String type){if(!hasActiveUser()){toast("Add or select an IPTV user first.");showUsers();return;}base(type.toUpperCase(Locale.US));ProgressBar pb=new ProgressBar(this);root.addView(pb,lp(-1,0,1));new Thread(()->{JSONArray arr=new JSONArray();try{JSONObject p=profile(activeProfile);String action=type.equals("live")?"get_live_streams":type.equals("movie")?"get_vod_streams":"get_series";arr=getArray(api(p)+"&action="+action);}catch(Exception ignored){}JSONArray result=arr;runOnUiThread(()->showResults(type.toUpperCase(Locale.US),result));}).start();}
    private void showResults(String title,JSONArray arr){base(title);ScrollView scroll=new ScrollView(this);LinearLayout list=col();if(arr.length()==0){TextView n=text("No items returned.",18,MUTED,false);n.setGravity(Gravity.CENTER);list.addView(n,new LinearLayout.LayoutParams(-1,dp(180)));}for(int i=0;i<Math.min(arr.length(),250);i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;String name=o.optString("name",o.optString("title","Untitled"));list.addView(button(name,PANEL,v->playItem(o)),new LinearLayout.LayoutParams(-1,dp(58)));list.addView(vgap(7));}scroll.addView(list);root.addView(scroll,lp(-1,0,1));}
    private void playItem(JSONObject o){try{JSONObject p=profile(activeProfile);if(o.has("series_id")){toast("Episode browsing is coming in the next build.");return;}String id=o.optString("stream_id"),ext=o.optString("container_extension","m3u8");boolean movie="movie".equals(o.optString("stream_type"))||o.has("rating_5based");String url=p.optString("server")+"/"+(movie?"movie":"live")+"/"+enc(p.optString("username"))+"/"+enc(p.optString("password"))+"/"+id+"."+ext;showPlayer(o.optString("name","Playing"),url);}catch(Exception e){toast("Unable to build stream URL.");}}
    private void showPlayer(String title,String url){base(title);PlayerView view=new PlayerView(this);player=new ExoPlayer.Builder(this).build();view.setPlayer(player);player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));player.prepare();player.play();root.addView(view,lp(-1,0,1));}
    private void showRadio(){base("EMURPH RADIO");LinearLayout body=row();LinearLayout info=col();info.setGravity(Gravity.CENTER);TextView logo=text("♫\nEMURPH RADIO",38,TEXT,true);logo.setGravity(Gravity.CENTER);info.addView(logo);info.addView(text("Urban Contemporary Gospel",18,MUTED,false));TextView status=text("Ready",16,MUTED,false);status.setGravity(Gravity.CENTER);info.addView(status);body.addView(info,lp(0,-1,.8f));LinearLayout right=col();PlayerView pv=new PlayerView(this);right.addView(pv,lp(-1,0,1));right.addView(button("▶ PLAY RADIO",RED,v->{if(player!=null)player.release();player=new ExoPlayer.Builder(this).build();pv.setPlayer(player);player.setMediaItem(MediaItem.fromUri(Uri.parse(prefs.getString("radio_url",defaultRadioUrl))));player.prepare();player.play();status.setText("Now Playing");}),new LinearLayout.LayoutParams(-1,dp(58)));body.addView(right,lp(0,-1,1.3f));root.addView(body,lp(-1,0,1));}
    private void showSettings(){base("SETTINGS");LinearLayout box=col();box.setPadding(dp(120),dp(20),dp(120),dp(20));box.addView(button("Switch User",PANEL,v->showUsers()),new LinearLayout.LayoutParams(-1,dp(60)));box.addView(vgap(14));box.addView(button("Update EMurph Radio URL",PANEL,v->editRadioUrl()),new LinearLayout.LayoutParams(-1,dp(60)));box.addView(vgap(14));box.addView(button("Clear Saved Users",RED,v->{prefs.edit().clear().apply();activeProfile="";showHome();}),new LinearLayout.LayoutParams(-1,dp(60)));box.addView(vgap(14));box.addView(text("EMurph TV Nightly MVP 0.1\nAndroid TV and Amazon Fire TV",16,MUTED,false));root.addView(box,lp(-1,0,1));}
    private void editRadioUrl(){EditText e=input("Radio URL");e.setText(prefs.getString("radio_url",defaultRadioUrl));new AlertDialog.Builder(this).setTitle("EMurph Radio URL").setView(e).setPositiveButton("Save",(d,w)->prefs.edit().putString("radio_url",e.getText().toString().trim()).apply()).setNegativeButton("Cancel",null).show();}

    private boolean hasActiveUser(){return !activeProfile.isEmpty()&&profile(activeProfile)!=null;}
    private JSONObject profile(String name){try{JSONArray a=new JSONArray(prefs.getString("profiles","[]"));for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);if(name.equals(p.optString("name")))return p;}}catch(Exception ignored){}return null;}
    private String expiration(){JSONObject p=profile(activeProfile);if(p==null)return "Not connected";String exp=p.optString("exp","");if(exp.isEmpty()||"null".equals(exp))return "Not provided";try{return new SimpleDateFormat("MMMM d, yyyy",Locale.US).format(new Date(Long.parseLong(exp)*1000L));}catch(Exception e){return exp;}}
    private String api(JSONObject p)throws Exception{return p.getString("server")+"/player_api.php?username="+enc(p.getString("username"))+"&password="+enc(p.getString("password"));}
    private String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private JSONObject getObject(String u)throws Exception{return new JSONObject(read(u));}
    private JSONArray getArray(String u)throws Exception{return new JSONArray(read(u));}
    private String read(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","EMurphTV/0.1");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}finally{c.disconnect();}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(player!=null){player.release();player=null;}showHome();}
    @Override protected void onStop(){super.onStop();if(player!=null)player.pause();}
}
