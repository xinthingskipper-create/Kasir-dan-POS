package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import kotlinx.coroutines.flow.Flow

data class TopProductSummary(
    val productId: Long,
    val productName: String,
    val totalSold: Int,
    val totalRevenue: Double
)

data class PaymentSummary(
    val paymentMethod: String,
    val count: Int,
    val totalAmount: Double
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
    suspend fun getAllTransactionsList(): List<Transaction>

    @Query("SELECT * FROM transaction_items ORDER BY id ASC")
    suspend fun getAllTransactionItemsList(): List<TransactionItem>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: Long): Transaction?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    fun getItemsForTransactionFlow(transactionId: Long): Flow<List<TransactionItem>>

    @Query("""
        SELECT ti.productId, ti.productName, SUM(ti.quantity) as totalSold, SUM(ti.subtotal) as totalRevenue
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.timestamp >= :startTime AND t.timestamp <= :endTime
        GROUP BY ti.productId, ti.productName
        ORDER BY totalSold DESC
        LIMIT 10
    """)
    fun getTopSellingProducts(startTime: Long, endTime: Long): Flow<List<TopProductSummary>>

    @Query("""
        SELECT t.paymentMethod as paymentMethod, COUNT(t.id) as count, SUM(t.grandTotal) as totalAmount
        FROM transactions t
        WHERE t.timestamp >= :startTime AND t.timestamp <= :endTime
        GROUP BY t.paymentMethod
    """)
    fun getPaymentMethodBreakdown(startTime: Long, endTime: Long): Flow<List<PaymentSummary>>

    @Query("""
        SELECT SUM(ti.subtotal) - SUM(ti.quantity * ti.costPrice)
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.timestamp >= :startTime AND t.timestamp <= :endTime
    """)
    fun getTotalProfitBetween(startTime: Long, endTime: Long): Flow<Double?>

    // --- Pelacakan Sinkronisasi Database Otomatis ---
    @Query("SELECT * FROM transactions WHERE isSynced = 0 ORDER BY timestamp DESC")
    fun getUnsyncedTransactions(): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE isSynced = 0")
    fun getUnsyncedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE isSynced = 1")
    fun getSyncedCountFlow(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedTransactionsList(): List<Transaction>

    @Query("UPDATE transactions SET isSynced = :isSynced, syncedAt = :syncedAt, syncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, isSynced: Boolean, syncedAt: Long?, error: String?)

    @Query("UPDATE transactions SET isSynced = 1, syncedAt = :syncedAt, syncError = NULL WHERE invoiceNumber = :invoiceNumber")
    suspend fun markAsSynced(invoiceNumber: String, syncedAt: Long = System.currentTimeMillis())
}
