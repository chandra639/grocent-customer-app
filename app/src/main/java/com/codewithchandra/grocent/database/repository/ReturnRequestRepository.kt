package com.codewithchandra.grocent.database.repository

import com.codewithchandra.grocent.database.dao.ReturnItemDao
import com.codewithchandra.grocent.database.dao.ReturnRequestDao
import com.codewithchandra.grocent.database.entities.ReturnItemEntity
import com.codewithchandra.grocent.database.entities.ReturnRequestEntity
import com.codewithchandra.grocent.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReturnRequestRepository(
    private val returnRequestDao: ReturnRequestDao,
    private val returnItemDao: ReturnItemDao
) {
    // Convert ReturnRequestEntity to ReturnRequest
    private suspend fun ReturnRequestEntity.toReturnRequest(): ReturnRequest {
        val items = returnItemDao.getReturnItemsByRequestId(this.id).map { it.toReturnItem() }
        return ReturnRequest(
            id = id,
            orderId = orderId,
            userId = userId,
            items = items,
            reason = ReturnReason.valueOf(reason),
            description = description,
            status = status,
            requestedAt = requestedAt,
            reviewedBy = reviewedBy,
            reviewedAt = reviewedAt,
            pickupScheduledAt = pickupScheduledAt,
            pickedUpAt = pickedUpAt,
            verifiedAt = verifiedAt,
            refundStatus = refundStatus,
            refundAmount = refundAmount,
            adminComment = adminComment
        )
    }

    // Convert ReturnItemEntity to ReturnItem
    private fun ReturnItemEntity.toReturnItem(): ReturnItem {
        return ReturnItem(
            productId = productId,
            productName = productName,
            quantity = quantity,
            returnReason = returnReason
        )
    }

    // Convert ReturnRequest to ReturnRequestEntity
    private fun ReturnRequest.toReturnRequestEntity(): ReturnRequestEntity {
        return ReturnRequestEntity(
            id = id,
            orderId = orderId,
            userId = userId,
            reason = reason.name,
            description = description,
            status = status,
            requestedAt = requestedAt,
            reviewedBy = reviewedBy,
            reviewedAt = reviewedAt,
            pickupScheduledAt = pickupScheduledAt,
            pickedUpAt = pickedUpAt,
            verifiedAt = verifiedAt,
            refundStatus = refundStatus,
            refundAmount = refundAmount,
            adminComment = adminComment
        )
    }

    // Convert ReturnItem to ReturnItemEntity
    private fun ReturnItem.toReturnItemEntity(returnRequestId: String): ReturnItemEntity {
        return ReturnItemEntity(
            returnRequestId = returnRequestId,
            productId = productId,
            productName = productName,
            quantity = quantity,
            returnReason = returnReason
        )
    }

    // Get all return requests
    fun getAllReturnRequests(): Flow<List<ReturnRequest>> {
        return returnRequestDao.getAllReturnRequests().map { entities ->
            kotlinx.coroutines.runBlocking {
                entities.map { entity ->
                    val items = returnItemDao.getReturnItemsByRequestId(entity.id)
                    ReturnRequest(
                        id = entity.id,
                        orderId = entity.orderId,
                        userId = entity.userId,
                        items = items.map { it.toReturnItem() },
                        reason = ReturnReason.valueOf(entity.reason),
                        description = entity.description,
                        status = entity.status,
                        requestedAt = entity.requestedAt,
                        reviewedBy = entity.reviewedBy,
                        reviewedAt = entity.reviewedAt,
                        pickupScheduledAt = entity.pickupScheduledAt,
                        pickedUpAt = entity.pickedUpAt,
                        verifiedAt = entity.verifiedAt,
                        refundStatus = entity.refundStatus,
                        refundAmount = entity.refundAmount,
                        adminComment = entity.adminComment
                    )
                }
            }
        }
    }

    // Get return request by ID
    suspend fun getReturnRequestById(returnRequestId: String): ReturnRequest? {
        val entity = returnRequestDao.getReturnRequestById(returnRequestId) ?: return null
        return entity.toReturnRequest()
    }

    // Get return request by order ID
    suspend fun getReturnRequestByOrderId(orderId: String): ReturnRequest? {
        val entity = returnRequestDao.getReturnRequestByOrderId(orderId) ?: return null
        return entity.toReturnRequest()
    }

    // Get return requests by user ID
    fun getReturnRequestsByUserId(userId: String): Flow<List<ReturnRequest>> {
        return returnRequestDao.getReturnRequestsByUserId(userId).map { entities ->
            kotlinx.coroutines.runBlocking {
                entities.map { entity ->
                    val items = returnItemDao.getReturnItemsByRequestId(entity.id)
                    ReturnRequest(
                        id = entity.id,
                        orderId = entity.orderId,
                        userId = entity.userId,
                        items = items.map { it.toReturnItem() },
                        reason = ReturnReason.valueOf(entity.reason),
                        description = entity.description,
                        status = entity.status,
                        requestedAt = entity.requestedAt,
                        reviewedBy = entity.reviewedBy,
                        reviewedAt = entity.reviewedAt,
                        pickupScheduledAt = entity.pickupScheduledAt,
                        pickedUpAt = entity.pickedUpAt,
                        verifiedAt = entity.verifiedAt,
                        refundStatus = entity.refundStatus,
                        refundAmount = entity.refundAmount,
                        adminComment = entity.adminComment
                    )
                }
            }
        }
    }

    // Get return requests by status
    fun getReturnRequestsByStatus(status: ReturnRequestStatus): Flow<List<ReturnRequest>> {
        return returnRequestDao.getReturnRequestsByStatus(status).map { entities ->
            kotlinx.coroutines.runBlocking {
                entities.map { entity ->
                    val items = returnItemDao.getReturnItemsByRequestId(entity.id)
                    ReturnRequest(
                        id = entity.id,
                        orderId = entity.orderId,
                        userId = entity.userId,
                        items = items.map { it.toReturnItem() },
                        reason = ReturnReason.valueOf(entity.reason),
                        description = entity.description,
                        status = entity.status,
                        requestedAt = entity.requestedAt,
                        reviewedBy = entity.reviewedBy,
                        reviewedAt = entity.reviewedAt,
                        pickupScheduledAt = entity.pickupScheduledAt,
                        pickedUpAt = entity.pickedUpAt,
                        verifiedAt = entity.verifiedAt,
                        refundStatus = entity.refundStatus,
                        refundAmount = entity.refundAmount,
                        adminComment = entity.adminComment
                    )
                }
            }
        }
    }

    // Insert return request
    suspend fun insertReturnRequest(returnRequest: ReturnRequest) {
        val entity = returnRequest.toReturnRequestEntity()
        returnRequestDao.insertReturnRequest(entity)
        
        // Insert return items
        val itemEntities = returnRequest.items.map { it.toReturnItemEntity(returnRequest.id) }
        returnItemDao.insertReturnItems(itemEntities)
    }

    // Update return request
    suspend fun updateReturnRequest(returnRequest: ReturnRequest) {
        val entity = returnRequest.toReturnRequestEntity()
        returnRequestDao.updateReturnRequest(entity)
        
        // Update return items (delete old, insert new)
        returnItemDao.deleteReturnItemsByRequestId(returnRequest.id)
        val itemEntities = returnRequest.items.map { it.toReturnItemEntity(returnRequest.id) }
        returnItemDao.insertReturnItems(itemEntities)
    }

    // Update return request status
    suspend fun updateReturnRequestStatus(
        returnRequestId: String,
        status: ReturnRequestStatus,
        reviewedBy: String?,
        reviewedAt: Long,
        adminComment: String?
    ) {
        returnRequestDao.updateReturnRequestStatus(
            returnRequestId = returnRequestId,
            status = status,
            reviewedBy = reviewedBy,
            reviewedAt = reviewedAt,
            adminComment = adminComment
        )
    }

    // Delete return request
    suspend fun deleteReturnRequest(returnRequest: ReturnRequest) {
        returnItemDao.deleteReturnItemsByRequestId(returnRequest.id)
        returnRequestDao.deleteReturnRequest(returnRequest.toReturnRequestEntity())
    }
}

