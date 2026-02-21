package com.oop.logistics.preprocessing;

import com.oop.logistics.database.DatabaseManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("DatabasePreprocessor Tests")
class TestDatabasePreprocessor {

    @Test
    @DisplayName("Should process rows, detect duplicates, and execute batch SQL")
    void testPreprocessDisasterData() throws Exception {
        // 1. Arrange Mocks
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockSelectStmt = mock(PreparedStatement.class);
        PreparedStatement mockUpdateStmt = mock(PreparedStatement.class);
        PreparedStatement mockDeleteStmt = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        // Define Connection behavior
        when(mockConn.prepareStatement(contains("SELECT"))).thenReturn(mockSelectStmt);
        when(mockConn.prepareStatement(contains("UPDATE"))).thenReturn(mockUpdateStmt);
        when(mockConn.prepareStatement(contains("DELETE"))).thenReturn(mockDeleteStmt);
        
        // Simulate a ResultSet with 2 rows: one original, one duplicate text
        when(mockSelectStmt.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, true, false); // 2 rows, then stop
        
        when(mockRs.getInt("id")).thenReturn(1, 2);
        when(mockRs.getString("content")).thenReturn("Cây đổ ở Hà Nội", "Cây đổ ở Hà Nội");
        when(mockRs.getString("published_date")).thenReturn("2024-09-07T15:23:00Z");

        // 2. Act
        try (MockedStatic<DatabaseManager> mockedDb = mockStatic(DatabaseManager.class)) {
            mockedDb.when(DatabaseManager::getConnection).thenReturn(mockConn);

            DatabasePreprocessor.preprocessDisasterData("Yagi", "News");

            // 3. Assert
            // It should update the first row (adding to batch)
            verify(mockUpdateStmt, times(1)).addBatch();
            
            // It should detect the second row as a duplicate and delete it
            verify(mockDeleteStmt, times(1)).addBatch();

            // Finally, both batches must be executed
            verify(mockUpdateStmt, times(1)).executeBatch();
            verify(mockDeleteStmt, times(1)).executeBatch();
        }
    }
}