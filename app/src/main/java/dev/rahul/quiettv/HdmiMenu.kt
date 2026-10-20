package dev.rahul.quiettv

/** Fold HDMI-CEC child inputs into their physical port, without changing device state. */
object HdmiMenu {
    data class Input(val id:String,val parentId:String?,val label:String,val connected:Boolean)
    data class Entry(val label:String,val targetId:String,val connected:Boolean=false)
    fun entries(inputs:List<Input>):List<Entry> {
        val unique=inputs.distinctBy { it.id };val byId=unique.associateBy { it.id }
        return unique.groupBy { it.parentId?.takeIf(byId::containsKey) ?: it.id }.map { (id,items) ->
            val port=byId.getValue(id)
            val devices=items.filter { it.id!=id && it.connected }
            val names=devices.map { it.label.trim() }.filter { it.isNotEmpty() && !it.equals(port.label,true) }.distinctBy { it.lowercase() }
            Entry(port.label+if(names.isEmpty())"" else " · "+names.joinToString(" / "),devices.singleOrNull()?.id ?: port.id,port.connected || devices.isNotEmpty())
        }.sortedWith(compareByDescending<Entry> { it.connected }.thenBy { it.label })
    }
}
