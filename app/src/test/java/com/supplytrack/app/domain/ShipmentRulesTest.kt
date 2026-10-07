package com.supplytrack.app.domain

import com.supplytrack.app.data.ShipmentStatus.CANCELLED
import com.supplytrack.app.data.ShipmentStatus.DELIVERED
import com.supplytrack.app.data.ShipmentStatus.IN_TRANSIT
import com.supplytrack.app.data.ShipmentStatus.PENDING
import com.supplytrack.app.data.ShipmentType.INBOUND
import com.supplytrack.app.data.ShipmentType.OUTBOUND
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShipmentRulesTest {

    @Test
    fun pendingCanBeDispatchedOrCancelled() {
        assertEquals(listOf(IN_TRANSIT, CANCELLED), ShipmentRules.nextStatuses(PENDING))
    }

    @Test
    fun inTransitCanOnlyBeDelivered() {
        assertEquals(listOf(DELIVERED), ShipmentRules.nextStatuses(IN_TRANSIT))
        assertFalse(ShipmentRules.canTransition(IN_TRANSIT, CANCELLED))
    }

    @Test
    fun terminalStatusesHaveNoTransitions() {
        assertTrue(ShipmentRules.nextStatuses(DELIVERED).isEmpty())
        assertTrue(ShipmentRules.nextStatuses(CANCELLED).isEmpty())
        assertFalse(ShipmentRules.canTransition(DELIVERED, PENDING))
    }

    @Test
    fun outboundDispatchRemovesStock() {
        assertEquals(-25, ShipmentRules.stockDelta(OUTBOUND, PENDING, IN_TRANSIT, 25))
        assertEquals(0, ShipmentRules.stockDelta(OUTBOUND, IN_TRANSIT, DELIVERED, 25))
    }

    @Test
    fun inboundDeliveryAddsStock() {
        assertEquals(0, ShipmentRules.stockDelta(INBOUND, PENDING, IN_TRANSIT, 40))
        assertEquals(40, ShipmentRules.stockDelta(INBOUND, IN_TRANSIT, DELIVERED, 40))
    }

    @Test
    fun cancellingNeverMovesStock() {
        assertEquals(0, ShipmentRules.stockDelta(INBOUND, PENDING, CANCELLED, 10))
        assertEquals(0, ShipmentRules.stockDelta(OUTBOUND, PENDING, CANCELLED, 10))
    }
}
