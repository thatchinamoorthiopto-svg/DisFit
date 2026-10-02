package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PatientRecord): Long

    @Update
    suspend fun updateRecord(record: PatientRecord)

    @Delete
    suspend fun deleteRecord(record: PatientRecord)

    @Query("SELECT * FROM patient_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<PatientRecord>>

    @Query("SELECT * FROM patient_records WHERE id = :id")
    fun getRecordById(id: Long): Flow<PatientRecord?>

    @Query("SELECT * FROM patient_records WHERE id = :id")
    suspend fun getRecordByIdDirect(id: Long): PatientRecord?

    @Query("SELECT * FROM patient_records WHERE name LIKE '%' || :query || '%' OR patientId LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchRecords(query: String): Flow<List<PatientRecord>>
}
