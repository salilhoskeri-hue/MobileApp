package com.supplytrack.app.domain

import com.supplytrack.app.data.ShipmentStatus
import com.supplytrack.app.data.ShipmentStatus.CANCELLED
import com.supplytrack.app.data.ShipmentStatus.DELIVERED
import com.supplytrack.app.data.ShipmentStatus.IN_TRANSIT
import com.supplytrack.app.data.ShipmentStatus.PENDING
import com.supplytrack.app.data.ShipmentType

/**
 * Shipment lifecycle: PENDING -> IN_TRANSIT -> DELIVERED, with cancellation allowed only
 * before the goods leave. Stock moves when goods physically leave (outbound dispatch)
 * or arrive (inbound delivery).
 */
object ShipmentRules {

    fun nextStatuses(current: ShipmentStatus): List<ShipmentStatus> = when (current) {
        PENDING -> listOf(IN_TRANSIT, CANCELLED)
        IN_TRANSIT -> listOf(DELIVERED)
        DELIVERED, CANCELLED -> emptyList()
    }

    fun canTransition(from: ShipmentStatus, to: ShipmentStatus): Boolean = to in nextStatuses(from)

    fun isActive(status: ShipmentStatus): Boolean = status == PENDING || status == IN_TRANSIT

    /** Change to on-hand stock caused by moving a shipment of [quantity] from [from] to [to]. */
    fun stockDelta(type: ShipmentType, from: ShipmentStatus, to: ShipmentStatus, quantity: Int): Int = when {
        type == ShipmentType.OUTBOUND && from == PENDING && to == IN_TRANSIT -> -quantity
        type == ShipmentType.INBOUND && from == IN_TRANSIT && to == DELIVERED -> quantity
        else -> 0
    }
}
