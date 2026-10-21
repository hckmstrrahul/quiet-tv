package dev.rahul.quiettv

object WallpaperCatalog {
    const val shuffleIntervalMs = 20_000L
    const val crossfadeDurationMs = 1_200L
    data class Picture(val id:String,val title:String)
    val pictures=listOf("Ember","Dusk","Deep ocean","Fern","Starlight","Prism","Aurora","Silk","Graphite")
        .mapIndexed { i,title->Picture("wallpaper_%02d".format(java.util.Locale.US,i+1),title) }
    val defaultId=pictures.first().id
    fun valid(id:String?)=pictures.any {it.id==id}
}

/** Visit every picture once per round, without repeating across round boundaries. */
class WallpaperShuffle(private val ids:List<String>,private val random:kotlin.random.Random=kotlin.random.Random.Default) {
    private val remaining=mutableListOf<String>()
    private var firstRound=true
    fun next(current:String):String {
        if(ids.size<2)return ids.firstOrNull() ?: current
        if(remaining.isEmpty()) {
            remaining.addAll((if(firstRound)ids.filter {it!=current} else ids).shuffled(random))
            firstRound=false
        }
        if(remaining.first()==current && remaining.size>1)java.util.Collections.swap(remaining,0,1)
        return remaining.removeAt(0)
    }
}
