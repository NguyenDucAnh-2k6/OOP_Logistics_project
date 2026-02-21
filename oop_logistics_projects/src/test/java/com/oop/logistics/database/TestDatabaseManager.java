package com.oop.logistics.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("DataRepository Tests")
class TestDataRepository {

    private static final Logger logger = LoggerFactory.getLogger(TestDataRepository.class);
    private final DataRepository repository = new DataRepository();

    @Test
    @DisplayName("Should successfully get or create a disaster and return ID")
    void testGetOrCreateDisaster() throws Exception {
        // 1. Arrange: Create mock JDBC objects
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockInsertStmt = mock(PreparedStatement.class);
        PreparedStatement mockSelectStmt = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        // 2. Arrange: Define the behavior of our mocks
        when(mockConn.prepareStatement(contains("INSERT"))).thenReturn(mockInsertStmt);
        when(mockConn.prepareStatement(contains("SELECT"))).thenReturn(mockSelectStmt);
        
        // Simulate the select statement finding ID '99'
        when(mockSelectStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true); 
        when(mockResultSet.getInt("id")).thenReturn(99);

        // 3. Act: We must mock the static DatabaseManager.getConnection() 
        // inside a try-with-resources block so it closes properly after the test.
        try (MockedStatic<DatabaseManager> mockedDb = mockStatic(DatabaseManager.class)) {
            mockedDb.when(DatabaseManager::getConnection).thenReturn(mockConn);
            
            logger.info("Testing getOrCreateDisaster with mocked database connection");
            int disasterId = repository.getOrCreateDisaster("Typhoon Yagi");
            
            // 4. Assert
            assertEquals(99, disasterId, "Should return the ID retrieved from the database");
            
            // Verify our statements were actually called with the right data
            verify(mockInsertStmt).setString(1, "Typhoon Yagi");
            verify(mockInsertStmt).executeUpdate();
            verify(mockSelectStmt).setString(1, "Typhoon Yagi");
        }
    }
}