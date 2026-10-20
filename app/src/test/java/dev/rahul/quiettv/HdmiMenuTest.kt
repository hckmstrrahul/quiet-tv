package dev.rahul.quiettv

import org.junit.Assert.*
import org.junit.Test

class HdmiMenuTest {
    @Test fun childIsOneNamedPortAndKeepsLaunchTarget() {
        val rows=HdmiMenu.entries(listOf(
            HdmiMenu.Input("p1",null,"HDMI 1",false),HdmiMenu.Input("p2",null,"HDMI 2",true),
            HdmiMenu.Input("apple","p2","Apple TV",true)))
        assertEquals(listOf("HDMI 2 · Apple TV","HDMI 1"),rows.map { it.label })
        assertEquals("apple",rows[0].targetId)
    }
    @Test fun disconnectedChildDoesNotBecomeAnotherRow() {
        val rows=HdmiMenu.entries(listOf(HdmiMenu.Input("p",null,"HDMI 3",false),HdmiMenu.Input("c","p","Player",false)))
        assertEquals(listOf(HdmiMenu.Entry("HDMI 3","p")),rows)
    }
    @Test fun multipleChildrenKeepPhysicalPortTargetAndUniqueNames() {
        val rows=HdmiMenu.entries(listOf(HdmiMenu.Input("p",null,"HDMI 2",true),
            HdmiMenu.Input("a","p","Player",true),HdmiMenu.Input("b","p","Player",true)))
        assertEquals(listOf(HdmiMenu.Entry("HDMI 2 · Player","p",true)),rows)
    }
    @Test fun connectedChildElevatesStandbyParentAheadOfEmptyPorts() {
        val rows=HdmiMenu.entries(listOf(HdmiMenu.Input("p1",null,"HDMI 1",false),
            HdmiMenu.Input("p3",null,"HDMI 3",false),HdmiMenu.Input("c","p3","Console",true),
            HdmiMenu.Input("p2",null,"HDMI 2",true)))
        assertEquals(listOf("HDMI 2","HDMI 3 · Console","HDMI 1"),rows.map {it.label})
        assertEquals(listOf(true,true,false),rows.map {it.connected})
    }
    @Test fun missingParentDoesNotHideTheInput() {
        assertEquals(listOf(HdmiMenu.Entry("Console","child",true)),HdmiMenu.entries(listOf(HdmiMenu.Input("child","missing","Console",true))))
    }
}
