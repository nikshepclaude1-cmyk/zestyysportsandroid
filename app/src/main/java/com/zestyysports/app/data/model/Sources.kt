package com.zestyysports.app.data.model

data class PlaylistSource(
    val id: String,
    val name: String,
    val url: String
)

object Sources {

    val INDIA = listOf(
        PlaylistSource("premium",       "Premium Playlist",  "https://raw.githubusercontent.com/nikkexe0-del/alexplaylist/refs/heads/main/premium.m3u"),
        PlaylistSource("sportspremimum","Sports Premium",    "https://raw.githubusercontent.com/nikkexe0-del/alexplaylist/refs/heads/main/sportspremimum.m3u"),
        PlaylistSource("ixp",           "IXP (Default)",     "https://m3u-tvb.pages.dev/ixp.m3u"),
        PlaylistSource("sonur",         "Sports (Sonur)",    "https://raw.githubusercontent.com/Tarangg5/sports/c95399510f59c764e1ef903c69b40e627495a2f7/sonur.m3u"),
        PlaylistSource("layasync",      "LayaSync",          "https://raw.githubusercontent.com/layasync/LayaSync.github.io/0dcc38e4281761de08a946619e51f48a34e0bed3/playlist%20(10).m3u")
    )

    val USA = listOf(
        PlaylistSource("zestyycustom",  "Zestyy Custom",    "https://raw.githubusercontent.com/nikkexe0-del/alexplaylist/refs/heads/main/zestyyxtream_live_custom.m3u"),
        PlaylistSource("usa",           "USA Streams",       "https://raw.githubusercontent.com/nikkexe0-del/alexplaylist/refs/heads/main/4klive.m3u")
    )

    val BLOCKED_GROUPS = setOf("bangla", "bangladeshi", "bd channels", "xxx", "adult")
}
