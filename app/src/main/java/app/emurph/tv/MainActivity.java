package app.emurph.tv;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.text.InputType;
import android.util.Base64;
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
    private final int BG=Color.rgb(2,6,18), PANEL=Color.rgb(8,15,34), TEXT=Color.WHITE;
    private final int MUTED=Color.rgb(174,184,205), BLUE=Color.rgb(16,112,245), RED=Color.rgb(244,40,78), PURPLE=Color.rgb(109,52,210);
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
    private GradientDrawable box(int fill,int radius,int stroke,int sw){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(sw>0)g.setStroke(dp(sw),stroke);return g;}
    private GradientDrawable gradient(int[] colors,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,colors);g.setCornerRadius(dp(radius));return g;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(14),dp(8),dp(14),dp(8));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView action(String text,View.OnClickListener click){TextView b=label(text,17,TEXT,true);b.setGravity(Gravity.CENTER);b.setFocusable(true);b.setClickable(true);b.setBackground(box(Color.argb(70,8,15,34),12,Color.argb(120,120,145,190),1));b.setOnClickListener(click);b.setOnFocusChangeListener((v,f)->{v.animate().scaleX(f?1.05f:1f).scaleY(f?1.05f:1f).setDuration(120).start();v.setBackground(box(Color.argb(f?150:70,8,15,34),12,f?RED:Color.argb(120,120,145,190),f?3:1));});return b;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(17);e.setSingleLine(true);e.setPadding(dp(18),0,dp(18),0);e.setBackground(box(PANEL,10,Color.rgb(78,94,132),1));return e;}
    private void release(){if(player!=null){player.release();player=null;}}

    private void showHome(){
        release();
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(34),dp(18),dp(34),dp(18));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(2,6,18),Color.rgb(7,12,30),Color.rgb(20,5,27)}));

        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=label("EMURPH ",30,TEXT,true);header.addView(logo);
        TextView tv=label("TV",30,RED,true);header.addView(tv);
        TextView search=action("⌕  Master Search",v->showSearch());header.addView(search,new LinearLayout.LayoutParams(0,dp(58),1));
        Space hs=new Space(this);header.addView(hs,new LinearLayout.LayoutParams(dp(12),1));
        header.addView(action("🔔",v->toast("No new notifications")),new LinearLayout.LayoutParams(dp(60),dp(58)));
        Space hs2=new Space(this);header.addView(hs2,new LinearLayout.LayoutParams(dp(8),1));
        header.addView(action("⚙",v->showSettings()),new LinearLayout.LayoutParams(dp(60),dp(58)));
        Space hs3=new Space(this);header.addView(hs3,new LinearLayout.LayoutParams(dp(8),1));
        header.addView(action("Switch User",v->showUsers()),new LinearLayout.LayoutParams(dp(155),dp(58)));
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(68)));

        LinearLayout cards=new LinearLayout(this);cards.setOrientation(LinearLayout.HORIZONTAL);cards.setPadding(0,dp(16),0,dp(14));
        cards.addView(homeCard("▣","LIVE TV","Watch Live Channels",new int[]{Color.rgb(15,75,175),Color.rgb(4,28,75)},v->browse("live")),new LinearLayout.LayoutParams(0,-1,1));
        addGap(cards,12);
        cards.addView(homeCard("◉","MOVIES","Thousands of Movies",new int[]{Color.rgb(202,46,52),Color.rgb(82,8,24)},v->browse("movie")),new LinearLayout.LayoutParams(0,-1,1));
        addGap(cards,12);
        cards.addView(homeCard("▱","SERIES","Binge Worthy Shows",new int[]{Color.rgb(111,50,202),Color.rgb(38,13,85)},v->browse("series")),new LinearLayout.LayoutParams(0,-1,1));
        addGap(cards,12);
        cards.addView(homeCard("📻","EMURPH RADIO","Urban Contemporary Gospel",new int[]{Color.rgb(10,19,43),Color.rgb(25,4,35)},v->showRadio()),new LinearLayout.LayoutParams(0,-1,1.08f));
        root.addView(cards,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout features=new LinearLayout(this);features.setOrientation(LinearLayout.HORIZONTAL);
        features.addView(feature("▤","LIVE WITH EPG","See what's on now",v->browse("live")),new LinearLayout.LayoutParams(0,dp(78),1));
        addGap(features,12);
        features.addView(feature("▦","MULTI-SCREEN","Watch on multiple devices",v->toast("Multi-screen is staged for the next build.")),new LinearLayout.LayoutParams(0,dp(78),1));
        addGap(features,12);
        features.addView(feature("↶","CATCH UP","Never miss a moment",v->browse("live")),new LinearLayout.LayoutParams(0,dp(78),1));
        root.addView(features);

        LinearLayout footer=new LinearLayout(this);footer.setOrientation(LinearLayout.HORIZONTAL);footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setBackground(box(Color.argb(120,8,15,34),10,Color.rgb(42,65,110),1));
        footer.addView(label("▣  Expiration: "+expiration(),14,MUTED,false),new LinearLayout.LayoutParams(0,dp(48),1));
        TextView logged=label("●  Logged in: "+(activeProfile.isEmpty()?"No user":activeProfile),14,activeProfile.isEmpty()?MUTED:RED,true);
        logged.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);footer.addView(logged,new LinearLayout.LayoutParams(0,dp(48),1));
        root.addView(footer);
        setContentView(root);
    }

    private void addGap(LinearLayout row,int n){Space s=new Space(this);row.addView(s,new LinearLayout.LayoutParams(dp(n),1));}
    private LinearLayout homeCard(String icon,String title,String sub,int[] colors,View.OnClickListener click){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.setPadding(dp(16),dp(18),dp(16),dp(18));GradientDrawable normal=new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors);normal.setCornerRadius(dp(16));normal.setStroke(dp(1),Color.rgb(86,105,150));c.setBackground(normal);c.setFocusable(true);c.setClickable(true);c.setOnClickListener(click);TextView i=label(icon,46,TEXT,true);i.setGravity(Gravity.CENTER);c.addView(i,new LinearLayout.LayoutParams(-1,0,1));TextView t=label(title,21,TEXT,true);t.setGravity(Gravity.CENTER);c.addView(t);TextView st=label(sub,13,Color.rgb(222,229,245),false);st.setGravity(Gravity.CENTER);c.addView(st);if(title.equals("EMURPH RADIO")){TextView wave=label("▂▅▇▃▆▂▇▅▂",16,RED,true);wave.setGravity(Gravity.CENTER);c.addView(wave);}c.setOnFocusChangeListener((v,f)->{GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors);g.setCornerRadius(dp(16));g.setStroke(dp(f?3:1),f?RED:Color.rgb(86,105,150));v.setBackground(g);v.animate().scaleX(f?1.045f:1f).scaleY(f?1.045f:1f).setDuration(110).start();});return c;}
    private LinearLayout feature(String icon,String title,String sub,View.OnClickListener click){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(18),dp(8),dp(18),dp(8));c.setBackground(box(PANEL,12,Color.rgb(68,83,120),1));c.setFocusable(true);c.setClickable(true);c.setOnClickListener(click);TextView i=label(icon,29,TEXT,true);c.addView(i,new LinearLayout.LayoutParams(dp(58),-1));LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.addView(label(title,17,TEXT,true));words.addView(label(sub,12,MUTED,false));c.addView(words,new LinearLayout.LayoutParams(0,-1,1));c.setOnFocusChangeListener((v,f)->{v.setBackground(box(PANEL,12,f?BLUE:Color.rgb(68,83,120),f?3:1));v.animate().scaleX(f?1.025f:1f).scaleY(f?1.025f:1f).setDuration(100).start();});return c;}

    private LinearLayout screen(String title){release();LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(34),dp(22),dp(34),dp(22));root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(2,6,18),Color.rgb(7,12,30),Color.rgb(20,5,27)}));LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);TextView logo=label("EMURPH ",30,TEXT,true);top.addView(logo);TextView tv=label("TV",30,RED,true);top.addView(tv);TextView heading=label(title,24,TEXT,true);heading.setGravity(Gravity.CENTER);top.addView(heading,new LinearLayout.LayoutParams(0,dp(64),1));root.addView(top,new LinearLayout.LayoutParams(-1,dp(70)));setContentView(root);return root;}

    private void showUsers(){LinearLayout root=screen("LIST USERS");LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER_VERTICAL);bar.addView(label("Select a user profile to continue",15,MUTED,false),new LinearLayout.LayoutParams(0,dp(58),1));TextView add=action("+  ADD USER",v->showAddUser());add.setBackground(gradient(new int[]{BLUE,RED},12));bar.addView(add,new LinearLayout.LayoutParams(dp(190),dp(54)));root.addView(bar);LinearLayout cards=new LinearLayout(this);cards.setOrientation(LinearLayout.HORIZONTAL);cards.setGravity(Gravity.TOP);try{JSONArray users=new JSONArray(prefs.getString("profiles","[]"));if(users.length()==0){TextView none=label("No users saved yet. Select ADD USER to connect your service.",20,MUTED,false);none.setGravity(Gravity.CENTER);root.addView(none,new LinearLayout.LayoutParams(-1,0,1));return;}for(int i=0;i<users.length();i++){JSONObject p=users.getJSONObject(i);String name=p.optString("name");LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(24),dp(22),dp(24),dp(22));c.setBackground(box(PANEL,16,name.equals(activeProfile)?RED:Color.rgb(56,66,95),name.equals(activeProfile)?2:1));TextView avatar=label("●  "+name,24,TEXT,true);avatar.setTextColor(name.equals(activeProfile)?Color.rgb(123,101,255):TEXT);c.addView(avatar);c.addView(label("Username: "+p.optString("username"),14,MUTED,false));c.addView(label("Server: EMurph TV Main",14,MUTED,false));TextView sel=action(name.equals(activeProfile)?"SELECTED":"SELECT",v->{activeProfile=name;prefs.edit().putString("active_profile",name).apply();showHome();});c.addView(sel,new LinearLayout.LayoutParams(-1,dp(52)));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(235),1);cp.setMargins(dp(8),dp(14),dp(8),0);cards.addView(c,cp);}}catch(Exception ignored){}root.addView(cards,new LinearLayout.LayoutParams(-1,0,1));}

    private void showAddUser(){LinearLayout root=screen("Enter Your Login Details");LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.HORIZONTAL);content.setGravity(Gravity.CENTER);LinearLayout art=new LinearLayout(this);art.setOrientation(LinearLayout.VERTICAL);art.setGravity(Gravity.CENTER);TextView mic=label("◉\nEMURPH RADIO",36,TEXT,true);mic.setGravity(Gravity.CENTER);mic.setTextColor(Color.rgb(152,110,255));art.addView(mic);art.addView(label("Urban Contemporary Gospel",17,MUTED,false));content.addView(art,new LinearLayout.LayoutParams(0,-1,.8f));LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(20),dp(10),dp(20),dp(10));EditText n=input("Profile Name"),u=input("Username"),p=input("Password"),s=input("Server URL");p.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);for(EditText e:new EditText[]{n,u,p,s}){form.addView(e,new LinearLayout.LayoutParams(-1,dp(58)));Space sp=new Space(this);form.addView(sp,new LinearLayout.LayoutParams(1,dp(10)));}TextView add=action("ADD USER",v->addUser(n,u,p,s));add.setBackground(gradient(new int[]{BLUE,PURPLE,RED},12));form.addView(add,new LinearLayout.LayoutParams(-1,dp(60)));Space sp=new Space(this);form.addView(sp,new LinearLayout.LayoutParams(1,dp(12)));form.addView(action("LIST USERS",v->showUsers()),new LinearLayout.LayoutParams(-1,dp(54)));content.addView(form,new LinearLayout.LayoutParams(0,-1,1.25f));root.addView(content,new LinearLayout.LayoutParams(-1,0,1));}

    private void addUser(EditText n,EditText u,EditText p,EditText s){String name=n.getText().toString().trim(),user=u.getText().toString().trim(),pass=p.getText().toString(),server=s.getText().toString().trim();if(name.isEmpty()||user.isEmpty()||pass.isEmpty()||server.isEmpty()){toast("Complete all four fields.");return;}if(server.endsWith("/"))server=server.substring(0,server.length()-1);final String fs=server;toast("Checking login…");new Thread(()->{try{JSONObject response=getObject(fs+"/player_api.php?username="+enc(user)+"&password="+enc(pass));JSONObject info=response.optJSONObject("user_info");if(info==null)throw new Exception("No account data returned");JSONObject obj=new JSONObject();obj.put("name",name);obj.put("username",user);obj.put("password",pass);obj.put("server",fs);obj.put("exp",info.optString("exp_date",""));JSONArray arr=new JSONArray(prefs.getString("profiles","[]"));for(int i=arr.length()-1;i>=0;i--)if(arr.optJSONObject(i).optString("name").equals(name))arr.remove(i);arr.put(obj);prefs.edit().putString("profiles",arr.toString()).putString("active_profile",name).apply();activeProfile=name;runOnUiThread(this::showHome);}catch(Exception e){runOnUiThread(()->toast("Login failed: "+e.getMessage()));}}).start();}

    private void showSearch(){EditText e=input("Search channels, movies, or series");new AlertDialog.Builder(this).setTitle("Master Search").setView(e).setPositiveButton("Search",(d,w)->performSearch(e.getText().toString().trim())).setNegativeButton("Cancel",null).show();}
    private void performSearch(String q){if(q.isEmpty()){toast("Enter a search term");return;}if(!hasActiveUser()){toast("Add or select a user first.");showUsers();return;}new Thread(()->{JSONArray out=new JSONArray();try{JSONObject p=profile(activeProfile);for(String a:new String[]{"get_live_streams","get_vod_streams","get_series"}){JSONArray x=getArray(api(p)+"&action="+a);for(int i=0;i<x.length();i++){JSONObject o=x.optJSONObject(i);String n=o.optString("name",o.optString("title"));if(n.toLowerCase(Locale.US).contains(q.toLowerCase(Locale.US)))out.put(o);}}}catch(Exception ignored){}runOnUiThread(()->showResults("Search: "+q,out));}).start();}

    private void browse(String type){if(!hasActiveUser()){toast("Add or select an IPTV user first.");showUsers();return;}LinearLayout root=screen(type.toUpperCase(Locale.US));ProgressBar pb=new ProgressBar(this);root.addView(pb,new LinearLayout.LayoutParams(-1,0,1));new Thread(()->{JSONArray arr=new JSONArray();try{JSONObject p=profile(activeProfile);String action=type.equals("live")?"get_live_streams":type.equals("movie")?"get_vod_streams":"get_series";arr=getArray(api(p)+"&action="+action);}catch(Exception ignored){}JSONArray result=arr;runOnUiThread(()->showResults(type.toUpperCase(Locale.US),result));}).start();}
    private void showResults(String title,JSONArray arr){LinearLayout root=screen(title);ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);if(arr.length()==0){TextView n=label("No items returned.",18,MUTED,false);n.setGravity(Gravity.CENTER);list.addView(n,new LinearLayout.LayoutParams(-1,dp(180)));}for(int i=0;i<Math.min(arr.length(),250);i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;String name=o.optString("name",o.optString("title","Untitled"));list.addView(action(name,v->playItem(o)),new LinearLayout.LayoutParams(-1,dp(56)));Space s=new Space(this);list.addView(s,new LinearLayout.LayoutParams(1,dp(7)));}scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
    private void playItem(JSONObject o){try{JSONObject p=profile(activeProfile);if(o.has("series_id")){toast("Episode browsing is coming in the next build.");return;}String id=o.optString("stream_id"),ext=o.optString("container_extension","m3u8");boolean movie="movie".equals(o.optString("stream_type"))||o.has("rating_5based");String url=p.optString("server")+"/"+(movie?"movie":"live")+"/"+enc(p.optString("username"))+"/"+enc(p.optString("password"))+"/"+id+"."+ext;showPlayer(o.optString("name","Playing"),url);}catch(Exception e){toast("Unable to build stream URL.");}}
    private void showPlayer(String title,String url){LinearLayout root=screen(title);PlayerView view=new PlayerView(this);player=new ExoPlayer.Builder(this).build();view.setPlayer(player);player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));player.prepare();player.play();root.addView(view,new LinearLayout.LayoutParams(-1,0,1));}
    private void showRadio(){LinearLayout root=screen("EMURPH RADIO");LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.HORIZONTAL);body.setGravity(Gravity.CENTER);LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setGravity(Gravity.CENTER);TextView logo=label("◉\nEMURPH RADIO",38,TEXT,true);logo.setGravity(Gravity.CENTER);logo.setTextColor(Color.rgb(180,110,255));info.addView(logo);info.addView(label("Urban Contemporary Gospel",18,MUTED,false));TextView status=label("Ready",16,MUTED,false);status.setGravity(Gravity.CENTER);info.addView(status);body.addView(info,new LinearLayout.LayoutParams(0,-1,.8f));LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);PlayerView pv=new PlayerView(this);right.addView(pv,new LinearLayout.LayoutParams(-1,0,1));TextView play=action("▶  PLAY EMURPH RADIO",v->{release();player=new ExoPlayer.Builder(this).build();pv.setPlayer(player);player.setMediaItem(MediaItem.fromUri(Uri.parse(prefs.getString("radio_url",defaultRadioUrl))));player.prepare();player.play();status.setText("Now Playing");});play.setBackground(gradient(new int[]{BLUE,PURPLE,RED},12));right.addView(play,new LinearLayout.LayoutParams(-1,dp(62)));body.addView(right,new LinearLayout.LayoutParams(0,-1,1.3f));root.addView(body,new LinearLayout.LayoutParams(-1,0,1));}
    private void showSettings(){LinearLayout root=screen("SETTINGS");LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(150),dp(20),dp(150),dp(20));box.addView(action("Switch User",v->showUsers()),new LinearLayout.LayoutParams(-1,dp(60)));Space a=new Space(this);box.addView(a,new LinearLayout.LayoutParams(1,dp(14)));box.addView(action("Update EMurph Radio URL",v->editRadioUrl()),new LinearLayout.LayoutParams(-1,dp(60)));Space b=new Space(this);box.addView(b,new LinearLayout.LayoutParams(1,dp(14)));box.addView(action("Clear Saved Users",v->{prefs.edit().clear().apply();activeProfile="";showHome();}),new LinearLayout.LayoutParams(-1,dp(60)));box.addView(label("EMurph TV Branded Preview 0.2\nAndroid TV and Amazon Fire TV",16,MUTED,false));root.addView(box,new LinearLayout.LayoutParams(-1,0,1));}
    private void editRadioUrl(){EditText e=input("Radio URL");e.setText(prefs.getString("radio_url",defaultRadioUrl));new AlertDialog.Builder(this).setTitle("EMurph Radio URL").setView(e).setPositiveButton("Save",(d,w)->prefs.edit().putString("radio_url",e.getText().toString().trim()).apply()).setNegativeButton("Cancel",null).show();}

    private boolean hasActiveUser(){return !activeProfile.isEmpty();}
    private JSONObject profile(String name)throws Exception{JSONArray a=new JSONArray(prefs.getString("profiles","[]"));for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);if(name.equals(p.optString("name")))return p;}throw new Exception("Profile not found");}
    private String api(JSONObject p)throws Exception{return p.getString("server")+"/player_api.php?username="+enc(p.getString("username"))+"&password="+enc(p.getString("password"));}
    private String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private JSONObject getObject(String url)throws Exception{return new JSONObject(get(url));}
    private JSONArray getArray(String url)throws Exception{return new JSONArray(get(url));}
    private String get(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","EMurphTV/0.2");InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}
    private String expiration(){if(activeProfile.isEmpty())return "Not connected";try{String v=profile(activeProfile).optString("exp","");if(v.isEmpty()||"null".equals(v))return "Unknown";long epoch=Long.parseLong(v);return new SimpleDateFormat("MMMM d, yyyy",Locale.US).format(new Date(epoch*1000));}catch(Exception e){return "Unknown";}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){showHome();}
}
