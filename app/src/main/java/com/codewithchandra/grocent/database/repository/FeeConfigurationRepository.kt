package com.codewithchandra.grocent.database.repository

import com.codewithchandra.grocent.database.dao.FeeConfigurationDao
import com.codewithchandra.grocent.database.entities.FeeConfigurationEntity
import com.codewithchandra.grocent.model.FeeConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeeConfigurationRepository(
    private val feeConfigurationDao: FeeConfigurationDao
) {
    // Convert Entity to Model
    private fun FeeConfigurationEntity.toFeeConfiguration(): FeeConfiguration {
        return FeeConfiguration(
            id = id,
            handlingFeeEnabled = handlingFeeEnabled,
            handlingFeeAmount = handlingFeeAmount,
            handlingFeeFree = handlingFeeFree,
            deliveryFeeEnabled = deliveryFeeEnabled,
            deliveryFeeAmount = deliveryFeeAmount,
            deliveryFeeFree = deliveryFeeFree,
            minimumOrderForFreeDelivery = minimumOrderForFreeDelivery,
            taxEnabled = taxEnabled,
            taxPercentage = taxPercentage,
            rainFeeEnabled = rainFeeEnabled,
            rainFeeAmount = rainFeeAmount,
            isRaining = isRaining,
            updatedAt = updatedAt
        )
    }
    
    // Convert Model to Entity
    private fun FeeConfiguration.toFeeConfigurationEntity(): FeeConfigurationEntity {
        return FeeConfigurationEntity(
            id = id,
            handlingFeeEnabled = handlingFeeEnabled,
            handlingFeeAmount = handlingFeeAmount,
            handlingFeeFree = handlingFeeFree,
            deliveryFeeEnabled = deliveryFeeEnabled,
            deliveryFeeAmount = deliveryFeeAmount,
            deliveryFeeFree = deliveryFeeFree,
            minimumOrderForFreeDelivery = minimumOrderForFreeDelivery,
            taxEnabled = taxEnabled,
            taxPercentage = taxPercentage,
            rainFeeEnabled = rainFeeEnabled,
            rainFeeAmount = rainFeeAmount,
            isRaining = isRaining,
            updatedAt = updatedAt
        )
    }
    
    suspend fun getFeeConfiguration(): FeeConfiguration? {
        return feeConfigurationDao.getFeeConfiguration()?.toFeeConfiguration()
    }
    
    fun getFeeConfigurationFlow(): Flow<FeeConfiguration?> {
        return feeConfigurationDao.getFeeConfigurationFlow().map { it?.toFeeConfiguration() }
    }
    
    suspend fun saveFeeConfiguration(config: FeeConfiguration) {
        feeConfigurationDao.insertOrUpdateFeeConfiguration(config.toFeeConfigurationEntity())
    }
    
    suspend fun deleteFeeConfiguration() {
        feeConfigurationDao.deleteFeeConfiguration()
    }
}
































