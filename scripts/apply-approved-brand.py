from pathlib import Path

p = Path("app/src/main/java/app/emurph/tv/MainActivity.java")
s = p.read_text(encoding="utf-8")

replacements = [
    (
        ".logo{font-family:cursive;font-style:italic;font-weight:900;font-size:43px;letter-spacing:-3px;text-shadow:0 0 18px rgba(60,125,255,.5)}.logo b{color:#ff315d;font-size:48px}.antenna{font-size:22px;position:relative;top:-19px;left:-18px}",
        "@import url('https://fonts.googleapis.com/css2?family=Knewave&display=swap');.logo,.brandmark{font-family:'Knewave','Arial Black',sans-serif;font-style:italic;font-weight:400;letter-spacing:-2px;text-transform:uppercase;line-height:.9;filter:drop-shadow(0 0 10px rgba(58,116,255,.22))}.logo{font-size:41px;white-space:nowrap}.logo .tv,.brandmark .tv{color:#ef173f;display:inline-block;transform:skew(-8deg) rotate(-2deg);margin-left:4px}.logo .em,.brandmark .em{color:#fff;display:inline-block;transform:skew(-7deg)}.tvbox{display:inline-block;position:relative;padding:2px 8px 0 5px;border-top:2px solid #fff;border-right:2px solid #fff;border-radius:8px 8px 0 0}.tvbox:before,.tvbox:after{content:'';position:absolute;top:-13px;width:2px;height:14px;background:#fff;transform-origin:bottom}.tvbox:before{left:43%;transform:rotate(-28deg)}.tvbox:after{left:58%;transform:rotate(28deg)}.brandbar{position:fixed;left:0;right:0;bottom:2px;height:96px;display:flex;flex-direction:column;align-items:center;justify-content:center;pointer-events:none;z-index:1}.brandmark{font-size:52px}.tagline{font:700 11px Arial,sans-serif;letter-spacing:6px;margin-top:8px;color:#fff}.pillars{font:700 9px Arial,sans-serif;letter-spacing:5px;margin-top:8px}.pillars .faith{color:#2781ff}.pillars .music{color:#b84cff}.pillars .ent{color:#ff315d}.wrap{padding-bottom:112px!important;grid-template-rows:78px 1fr 92px 48px!important}"
    ),
    (
        "<div class='top'><div class='logo'>EMurph <b>TV</b><span class='antenna'>⌁</span></div>",
        "<div class='top'><div class='logo'><span class='em'>EMURPH</span> <span class='tvbox'><span class='tv'>TV</span></span></div>"
    ),
    (
        "</div><div class='footer'><span>▣ &nbsp; Expiration: <strong>\"+exp+\"</strong></span><span>👤 &nbsp; Logged in: <strong>\"+user+\"</strong></span></div></div>",
        "</div><div class='footer'><span>▣ &nbsp; Expiration: <strong>\"+exp+\"</strong></span><span>👤 &nbsp; Logged in: <strong>\"+user+\"</strong></span></div></div><div class='brandbar'><div class='brandmark'><span class='em'>EMURPH</span> <span class='tvbox'><span class='tv'>TV</span></span></div><div class='tagline'>STREAM. LISTEN. INSPIRE.</div><div class='pillars'><span class='faith'>FAITH</span> • <span class='music'>MUSIC</span> • <span class='ent'>ENTERTAINMENT</span></div></div>"
    ),
]

changed = 0
for old, new in replacements:
    if old in s:
        s = s.replace(old, new, 1)
        changed += 1

p.write_text(s, encoding="utf-8")
print(f"Approved EMurph TV branding patch completed; replacements applied: {changed}")
