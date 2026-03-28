package com.dologan.humblebrowser.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dologan.humblebrowser.data.db.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE orderId = :orderId ORDER BY humanName ASC")
    fun observeByBundle(orderId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE orderId = :orderId ORDER BY humanName ASC")
    suspend fun getByBundle(orderId: String): List<ProductEntity>

    @Query("SELECT * FROM products ORDER BY humanName ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY humanName ASC")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE orderId = :orderId")
    suspend fun deleteByBundle(orderId: String)
}
