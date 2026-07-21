from pathlib import Path
import re

SOURCE = Path("app/src/main/java/app/emurph/tv/MainActivity.java")
text = SOURCE.read_text(encoding="utf-8")
original = text


def require_replace(old: str, new: str, label: str, count: int = 1) -> None:
    global text
    found = text.count(old)
    if found != count:
        raise SystemExit(f"{label}: expected {count} occurrence(s), found {found}")
    text = text.replace(old, new, count)
    print(f"Applied: {label}")


# ---------------------------------------------------------------------------
# Native custom-scheme router. This removes Fire TV's unreliable dependency on
# synthetic WebView click events reaching @JavascriptInterface methods.
# ---------------------------------------------------------------------------
router_marker = "    private String homeHtml() {"
if "private WebViewClient appWebViewClient()" not in text:
    if router_marker not in text:
        raise SystemExit("Could not locate homeHtml insertion point")
    router = r'''
    private static final String APP_BASE_URL = "https://app.emurph.tv/";

    private WebViewClient appWebViewClient() {
        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return request != null && handleAppUri(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return url != null && handleAppUri(Uri.parse(url));
            }
        };
    }

    private boolean handleAppUri(Uri uri) {
        if (uri == null || !"emurph".equalsIgnoreCase(uri.getScheme())) return false;

        final String action = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.US);
        final String profileName = uri.getQueryParameter("profile");
        final String name = uri.getQueryParameter("name");
        final String username = uri.getQueryParameter("user");
        final String password = uri.getQueryParameter("pass");
        final String server = uri.getQueryParameter("server");

        runOnUiThread(() -> {
            switch (action) {
                case "search":
                    showSearch();
                    break;
                case "users":
                case "profile":
                    showUsers();
                    break;
                case "add-user-screen":
                    showAddUser();
                    break;
                case "settings":
                    showSettings();
                    break;
                case "live":
                case "epg":
                    browseNative("live");
                    break;
                case "movies":
                    browseNative("movie");
                    break;
                case "series":
                    browseNative("series");
                    break;
                case "radio":
                    showRadio();
                    break;
                case "select-user":
                    if (profileName != null && !profileName.trim().isEmpty()) {
                        activeProfile = profileName.trim();
                        prefs.edit().putString("active_profile", activeProfile).apply();
                        showHome();
                    } else {
                        toast("Unable to select that profile.");
                    }
                    break;
                case "add-user":
                    addUserFromWeb(name, username, password, server);
                    break;
                case "vpn":
                    toast("VPN integration coming soon.");
                    break;
                case "multiscreen":
                    toast("Multi-screen support is being prepared.");
                    break;
                case "catchup":
                    toast("Opening archive-enabled channels.");
                    browseCatchUp();
                    break;
                case "notifications":
                    toast("No new notifications.");
                    break;
                case "rec":
                    toast("Recording availability depends on your TV provider.");
                    break;
                case "home":
                    showHome();
                    break;
                default:
                    toast("That control is not available yet.");
                    break;
            }
        });
        return true;
    }

    private void browseCatchUp() {
        if (!hasActiveUser()) {
            toast("Add or select an IPTV user first.");
            showUsers();
            return;
        }
        LinearLayout root = new LinearLayout(this);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(2, 5, 16));
        root.addView(new ProgressBar(this));
        setContentView(root);
        new Thread(() -> {
            JSONArray archive = new JSONArray();
            try {
                JSONObject p = profile(activeProfile);
                JSONArray all = getArray(api(p) + "&action=get_live_streams");
                for (int i = 0; i < all.length(); i++) {
                    JSONObject item = all.optJSONObject(i);
                    if (item != null && (item.optInt("tv_archive", 0) == 1 || item.optInt("tv_archive_duration", 0) > 0)) {
                        archive.put(item);
                    }
                }
            } catch (Exception ignored) {}
            runOnUiThread(() -> showResults("CATCH UP", archive));
        }).start();
    }

'''
    text = text.replace(router_marker, router + router_marker, 1)
    print("Applied: native custom-scheme action router")
else:
    print("Router already present")

# Make every app WebView use the native router and a trusted non-null origin.
count_clients = text.count("web.setWebViewClient(new WebViewClient());")
if count_clients:
    text = text.replace("web.setWebViewClient(new WebViewClient());", "web.setWebViewClient(appWebViewClient());")
    print(f"Applied: routed {count_clients} app WebView client(s)")
else:
    print("App WebView clients already routed")

count_bases = text.count("web.loadDataWithBaseURL(null,")
if count_bases:
    text = text.replace("web.loadDataWithBaseURL(null,", "web.loadDataWithBaseURL(APP_BASE_URL,")
    print(f"Applied: non-null base URL to {count_bases} WebView load(s)")
else:
    print("WebView base URLs already patched")

# ---------------------------------------------------------------------------
# Add native action metadata without altering Manus's approved visuals.
# Existing inline onclick handlers remain only as fallback for touch devices.
# ---------------------------------------------------------------------------
action_replacements = [
    ("<div class=\\'search\\' tabindex=\\'1\\'>", "<div class=\\'search\\' tabindex=\\'1\\' data-action=\\'search\\'>", "Master Search"),
    ("<div class=\\'hbtn\\' tabindex=\\'2\\'>", "<div class=\\'hbtn\\' tabindex=\\'2\\' data-action=\\'notifications\\'>", "notifications"),
    ("<div class=\\'hbtn\\' tabindex=\\'3\\'>", "<div class=\\'hbtn\\' tabindex=\\'3\\' data-action=\\'profile\\'>", "profile control"),
    ("<div class=\\'hbtn\\' tabindex=\\'4\\'", "<div class=\\'hbtn\\' tabindex=\\'4\\' data-action=\\'rec\\'", "REC control"),
    ("<div class=\\'hbtn\\' tabindex=\\'5\\' onclick=\\'android.settings();\\'>", "<div class=\\'hbtn\\' tabindex=\\'5\\' data-action=\\'settings\\' onclick=\\'android.settings();\\'>", "settings"),
    ("<div class=\\'sw\\' tabindex=\\'6\\' onclick=\\'android.showUsers();\\'>", "<div class=\\'sw\\' tabindex=\\'6\\' data-action=\\'users\\' onclick=\\'android.showUsers();\\'>", "switch user"),
    ("<div class=\\'card live\\' tabindex=\\'7\\' onclick=\\'android.loadLiveTV();\\'>", "<div class=\\'card live\\' tabindex=\\'7\\' data-action=\\'live\\' onclick=\\'android.loadLiveTV();\\'>", "Live TV"),
    ("<div class=\\'card movies\\' tabindex=\\'8\\' onclick=\\'android.loadMovies();\\'>", "<div class=\\'card movies\\' tabindex=\\'8\\' data-action=\\'movies\\' onclick=\\'android.loadMovies();\\'>", "Movies"),
    ("<div class=\\'card series\\' tabindex=\\'9\\' onclick=\\'android.loadSeries();\\'>", "<div class=\\'card series\\' tabindex=\\'9\\' data-action=\\'series\\' onclick=\\'android.loadSeries();\\'>", "Series"),
    ("<div class=\\'card radio\\' tabindex=\\'10\\' onclick=\\'android.loadRadio();\\'>", "<div class=\\'card radio\\' tabindex=\\'10\\' data-action=\\'radio\\' onclick=\\'android.loadRadio();\\'>", "EMurph Radio"),
    ("<div class=\\'feat\\' tabindex=\\'11\\'>", "<div class=\\'feat\\' tabindex=\\'11\\' data-action=\\'epg\\'>", "EPG panel"),
    ("<div class=\\'feat\\' tabindex=\\'12\\'>", "<div class=\\'feat\\' tabindex=\\'12\\' data-action=\\'multiscreen\\'>", "multi-screen panel"),
    ("<div class=\\'feat\\' tabindex=\\'13\\'>", "<div class=\\'feat\\' tabindex=\\'13\\' data-action=\\'catchup\\'>", "catch-up panel"),
    ("<button class=\\'add-btn\\' tabindex=\\'1\\' onclick=\\'android.showAddUser();\\'>", "<button class=\\'add-btn\\' tabindex=\\'1\\' data-action=\\'add-user-screen\\' onclick=\\'android.showAddUser();\\'>", "Add User navigation"),
    ("<button class=\\'submit\\' tabindex=\\'6\\' onclick=\\'android.addUser(", "<button class=\\'submit\\' tabindex=\\'6\\' data-action=\\'add-user\\' onclick=\\'android.addUser(", "Add User submission"),
    ("<button class=\\'btn2\\' tabindex=\\'7\\' onclick=\\'android.showUsers();\\'>", "<button class=\\'btn2\\' tabindex=\\'7\\' data-action=\\'users\\' onclick=\\'android.showUsers();\\'>", "List Users navigation"),
    ("<button class=\\'btn2\\' tabindex=\\'8\\' onclick=\\'android.connectVPN();\\'>", "<button class=\\'btn2\\' tabindex=\\'8\\' data-action=\\'vpn\\' onclick=\\'android.connectVPN();\\'>", "VPN control"),
]
for old, new, label in action_replacements:
    if old in text and "data-action" not in old:
        text = text.replace(old, new, 1)
        print(f"Applied action metadata: {label}")
    elif new in text:
        print(f"Action metadata already present: {label}")
    else:
        raise SystemExit(f"Could not locate control for action metadata: {label}")

# Profile identity must always be the saved profile name. Older code mixed username
# and profile name, which made profile() fail after switching users.
old_name = '                    String name = esc(p.optString("name","Profile "+(i+1)));'
new_name = '                    String rawName = p.optString("name","Profile "+(i+1));\n                    String name = esc(rawName);'
if old_name in text:
    text = text.replace(old_name, new_name, 1)
    print("Applied: preserve raw profile name")
elif new_name not in text:
    raise SystemExit("Could not patch raw profile name")

old_active = '                    boolean active = p.optString("username","").equals(activeProfile);'
new_active = '                    boolean active = rawName.equals(activeProfile) || p.optString("username","").equals(activeProfile);'
if old_active in text:
    text = text.replace(old_active, new_active, 1)
    print("Applied: correct active-profile comparison")
elif new_active not in text:
    raise SystemExit("Could not patch active profile comparison")

old_card = '                    cards.append("<div class=\\\'ucard"+(active?" active":"")+"\\\' tabindex=\\\'"+tabIdx+"\\\' onclick=\\\'android.selectUser(\\\""+uname+"\\\");\\\'>");'
new_card = '                    cards.append("<div class=\\\'ucard"+(active?" active":"")+"\\\' tabindex=\\\'"+tabIdx+"\\\' data-action=\\\'select-user\\\' data-profile=\\\'"+name+"\\\' onclick=\\\'android.selectUser(\\\""+name+"\\\");\\\'>");'
if old_card in text:
    text = text.replace(old_card, new_card, 1)
    print("Applied: profile selection routes by profile name")
elif new_card not in text:
    raise SystemExit("Could not patch user card selection")

# Replace the old el.click() D-pad shim with deterministic action routing.
old_script = "<script>document.addEventListener('keydown',function(e){if(e.keyCode===13||e.keyCode===23||e.keyCode===32){var el=document.activeElement;if(el&&el!==document.body){e.preventDefault();el.click();}}});</script>"
new_script = "<script>(function(){var last=0;function field(id){var n=document.getElementById(id);return n?n.value:'';}function go(el){if(!el)return false;var a=el.getAttribute('data-action');if(!a)return false;var now=Date.now();if(now-last<350)return false;last=now;var u='emurph://'+a;if(a==='add-user'){u+='?name='+encodeURIComponent(field('pname'))+'&user='+encodeURIComponent(field('uname'))+'&pass='+encodeURIComponent(field('pass'))+'&server='+encodeURIComponent(field('surl'));}else if(a==='select-user'){u+='?profile='+encodeURIComponent(el.getAttribute('data-profile')||'');}window.location.href=u;return false;}window.EMurphActivateFocused=function(){var el=document.activeElement;var tag=el&&el.tagName?el.tagName.toUpperCase():'';if(tag==='INPUT'||tag==='TEXTAREA'||tag==='SELECT')return false;return go(el);};document.addEventListener('click',function(e){var el=e.target;while(el&&el!==document.body&&!el.getAttribute('data-action'))el=el.parentElement;if(el&&el.getAttribute('data-action')){e.preventDefault();e.stopPropagation();go(el);}},true);document.addEventListener('keydown',function(e){var k=e.keyCode||e.which;if(k===13||k===23||k===32){var el=document.activeElement;var tag=el&&el.tagName?el.tagName.toUpperCase():'';if(tag==='INPUT'||tag==='TEXTAREA'||tag==='SELECT')return;if(el&&el.getAttribute('data-action')){e.preventDefault();e.stopPropagation();go(el);}}});})();</script>"
script_count = text.count(old_script)
if script_count:
    text = text.replace(old_script, new_script)
    print(f"Applied: deterministic Fire TV D-pad routing on {script_count} screen(s)")
elif new_script not in text:
    raise SystemExit("Could not locate legacy D-pad scripts")

# Fix Movies incorrectly requesting the Series endpoint.
old_action = 'String action = type.equals("live") ? "get_live_streams" : type.equals("movie") ? "get_vod_streams" : "get_series";'
new_action = 'String action = type.equals("live") ? "get_live_streams" : (type.equals("movie") || type.equals("movies")) ? "get_vod_streams" : "get_series";'
if old_action in text:
    text = text.replace(old_action, new_action, 1)
    print("Applied: Movies API endpoint")
elif new_action not in text:
    raise SystemExit("Could not patch Movies endpoint")

text = text.replace('browseNative("movies")', 'browseNative("movie")')

# Replace Add User persistence/login code with validated, profile-name-consistent logic.
add_user_pattern = re.compile(r'    private void addUserFromWeb\(String name, String user, String pass, String server\) \{.*?\n    \}\n\n    private void showSearch\(\)', re.S)
add_user_replacement = r'''    private void addUserFromWeb(String name, String user, String pass, String server) {
        final String username = user == null ? "" : user.trim();
        final String password = pass == null ? "" : pass.trim();
        String requestedName = name == null ? "" : name.trim();
        final String profileName = requestedName.isEmpty() ? username : requestedName;
        String requestedServer = normalizeServer(server);
        final String normalServer = requestedServer.isEmpty()
            ? normalizeServer(prefs.getString("default_server_url", defaultServerUrl))
            : requestedServer;

        if (profileName.isEmpty() || username.isEmpty() || password.isEmpty() || normalServer.isEmpty()) {
            toast("Enter a profile name, username, password, and server URL.");
            return;
        }
        if (!normalServer.startsWith("http://") && !normalServer.startsWith("https://")) {
            toast("Server URL must begin with http:// or https://");
            return;
        }

        toast("Checking login…");
        new Thread(() -> {
            try {
                JSONObject r = getObject(normalServer + "/player_api.php?username=" + enc(username) + "&password=" + enc(password));
                JSONObject info = r.optJSONObject("user_info");
                if (info == null || (info.has("auth") && info.optInt("auth", 0) != 1)) {
                    throw new Exception("The provider rejected those login details");
                }

                JSONObject obj = new JSONObject();
                obj.put("name", profileName);
                obj.put("username", username);
                obj.put("password", password);
                obj.put("server", normalServer);
                obj.put("exp", info.optString("exp_date", ""));

                JSONArray arr = new JSONArray(prefs.getString("profiles", "[]"));
                for (int i = arr.length() - 1; i >= 0; i--) {
                    JSONObject saved = arr.optJSONObject(i);
                    if (saved != null && (profileName.equals(saved.optString("name")) ||
                        (username.equals(saved.optString("username")) && normalServer.equals(normalizeServer(saved.optString("server")))))) {
                        arr.remove(i);
                    }
                }
                arr.put(obj);
                prefs.edit()
                    .putString("profiles", arr.toString())
                    .putString("active_profile", profileName)
                    .apply();
                activeProfile = profileName;
                runOnUiThread(() -> {
                    toast("Profile connected.");
                    showHome();
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast("Login failed: " + e.getMessage()));
            }
        }).start();
    }

    private void showSearch()'''
text, add_count = add_user_pattern.subn(add_user_replacement, text, count=1)
if add_count != 1:
    if "The provider rejected those login details" not in text:
        raise SystemExit(f"Could not replace addUserFromWeb; replacements={add_count}")
else:
    print("Applied: validated Add User/login backend")

# Correct stream URL construction for live, VOD, and series episodes.
play_pattern = re.compile(r'    private void playItem\(JSONObject o\) \{.*?\n    \}\n\n    private void showSeriesEpisodes', re.S)
play_replacement = r'''    private void playItem(JSONObject o) {
        try {
            JSONObject p = profile(activeProfile);
            if (o.has("series_id") && !o.has("id") && !o.has("stream_id")) {
                showSeriesEpisodes(o);
                return;
            }

            String id = o.optString("stream_id", o.optString("id", ""));
            if (id.isEmpty()) throw new Exception("Missing stream ID");
            String ext = o.optString("container_extension", "m3u8");
            String streamType = o.optString("stream_type", "");
            boolean episode = o.has("episode_num") || o.has("season") || "series".equalsIgnoreCase(streamType);
            boolean movie = "movie".equalsIgnoreCase(streamType) || "vod".equalsIgnoreCase(streamType) || o.has("rating_5based");
            String bucket = episode ? "series" : movie ? "movie" : "live";
            String url = profileServer(p) + "/" + bucket + "/" + enc(p.optString("username")) + "/" + enc(p.optString("password")) + "/" + id + "." + ext;
            String title = o.optString("name", o.optString("title", "Playing"));
            showPlayer(title, url);
        } catch (Exception e) {
            toast("Unable to play this item: " + e.getMessage());
        }
    }

    private void showSeriesEpisodes'''
text, play_count = play_pattern.subn(play_replacement, text, count=1)
if play_count != 1:
    if "String bucket = episode ? \"series\"" not in text:
        raise SystemExit(f"Could not replace playItem; replacements={play_count}")
else:
    print("Applied: live/movie/episode playback URL routing")

# Use the configured direct radio stream in ExoPlayer so selecting the card starts
# audio immediately. This is more reliable than auto-clicking a web page button.
radio_pattern = re.compile(r'\s+private void showRadio\(\) \{.*?\n    \}\n\n    private void showSettings\(\)', re.S)
radio_replacement = r'''
    private void showRadio() {
        String url = prefs.getString("radio_url", defaultRadioUrl);
        url = url == null ? "" : url.trim();
        if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            url = defaultRadioUrl;
        }
        toast("Starting EMurph Radio…");
        showPlayer("EMurph Radio", url);
    }

    private void showSettings()'''
text, radio_count = radio_pattern.subn(radio_replacement, text, count=1)
if radio_count != 1:
    if "Starting EMurph Radio" not in text:
        raise SystemExit(f"Could not replace showRadio; replacements={radio_count}")
else:
    print("Applied: direct EMurph Radio autoplay")

# Let users' manually selected radio URL survive future remote-config refreshes.
old_radio_save = 'prefs.edit().putString("radio_url", e.getText().toString().trim()).apply()'
new_radio_save = 'prefs.edit().putString("radio_url", e.getText().toString().trim()).putBoolean("radio_url_custom", true).apply()'
if old_radio_save in text:
    text = text.replace(old_radio_save, new_radio_save, 1)
    print("Applied: preserve custom radio URL")

old_remote_radio = 'if (radio.startsWith("http://") || radio.startsWith("https://")) e.putString("radio_url", radio);'
new_remote_radio = 'if (!prefs.getBoolean("radio_url_custom", false) && (radio.startsWith("http://") || radio.startsWith("https://"))) e.putString("radio_url", radio);'
if old_remote_radio in text:
    text = text.replace(old_remote_radio, new_remote_radio, 1)
    print("Applied: remote config respects custom radio URL")

# Backward compatibility: old broken builds sometimes stored username as active_profile.
old_profile_match = '            if (name.equals(p.optString("name"))) return p;'
new_profile_match = '            if (name.equals(p.optString("name")) || name.equals(p.optString("username"))) return p;'
if old_profile_match in text:
    text = text.replace(old_profile_match, new_profile_match, 1)
    print("Applied: legacy active-profile migration compatibility")

if text == original:
    raise SystemExit("No backend changes were applied")

SOURCE.write_text(text, encoding="utf-8")
print("EMurph TV Fire TV backend patch completed successfully.")
