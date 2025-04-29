package com.sleepingpandaaa.basicbankingapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class DatabaseHelperTest {

    private DatabaseHelper dbHelper;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbHelper = new DatabaseHelper(context);
        // Ensure the database is clean before each test
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.onUpgrade(db, 1, 2); // Recreate tables
        db.close();
    }

    @Test
    public void testReadalldata() {
        Cursor cursor = dbHelper.readalldata();
        assertNotNull(cursor);
        // Assuming 15 initial users based on DatabaseHelper structure
        assertEquals(15, cursor.getCount());
        cursor.close();
    }

    @Test
    public void testReadparticulardata() {
        Cursor cursor = dbHelper.readparticulardata("1"); // Test for user with account number 1
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("Aditya Sharma", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_NAME)));
        assertEquals("7895641238", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ACCOUNT_BALANCE)));
        assertEquals("aditya@gmail.com", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL)));
        assertEquals("7854123698", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PHONE_NO)));
        assertEquals("1234", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ACCOUNT_NO)));
        assertEquals("XXXX8569", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_IFSC_CODE)));
        cursor.close();

        // Test for a non-existent user
        cursor = dbHelper.readparticulardata("99");
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void testReadselectuserdata() {
        Cursor cursor = dbHelper.readselectuserdata("1"); // Exclude user with account number 1
        assertNotNull(cursor);
        // Should return 14 users (15 initial - 1 excluded)
        assertEquals(14, cursor.getCount());

        // Check that the excluded user is not present
        while (cursor.moveToNext()) {
            assertNotEquals("1", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ACCOUNT_NO)));
        }
        cursor.close();
    }

    @Test
    public void testUpdateAmount() {
        // Update balance for user with account number 1
        boolean success = dbHelper.updateAmount("1", "5000");
        assertTrue(success);

        // Verify the updated balance
        Cursor cursor = dbHelper.readparticulardata("1");
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("5000", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ACCOUNT_BALANCE)));
        cursor.close();

        // Test updating a non-existent user
        success = dbHelper.updateAmount("99", "1000");
        assertFalse(success); // Should fail as user doesn't exist
    }

    @Test
    public void testInsertAndReadTransferData_Success() {
        // Insert a successful transaction record
        boolean insertSuccess = dbHelper.insertTransferData("Aditya Sharma", "Rohan Gupta", "1000", "Success");
        assertTrue(insertSuccess);

        // Read transfer data
        Cursor cursor = dbHelper.readtransferdata();
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());

        // Verify the inserted data
        assertEquals("Aditya Sharma", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FROM_NAME)));
        assertEquals("Rohan Gupta", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TO_NAME)));
        assertEquals("1000", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT)));
        assertEquals("Success", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS)));
        cursor.close();
    }

     @Test
    public void testInsertAndReadTransferData_Failure() {
        // Insert a failed transaction record
        boolean insertSuccess = dbHelper.insertTransferData("Suresh Kumar", "Mohit Jain", "500", "Failure");
        assertTrue(insertSuccess);

        // Read transfer data
        Cursor cursor = dbHelper.readtransferdata();
        assertNotNull(cursor);
        assertTrue(cursor.moveToLast()); // Check the last inserted record

        // Verify the inserted data
        assertEquals("Suresh Kumar", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FROM_NAME)));
        assertEquals("Mohit Jain", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TO_NAME)));
        assertEquals("500", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT)));
        assertEquals("Failure", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS)));
        cursor.close();
    }
}
