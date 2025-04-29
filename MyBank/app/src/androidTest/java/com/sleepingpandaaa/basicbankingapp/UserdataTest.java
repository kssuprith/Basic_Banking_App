package com.sleepingpandaaa.basicbankingapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class UserdataTest {

    private static final String TEST_ACCOUNT_NO = "1"; // Use Aditya Sharma for testing
    private static final String TEST_USER_NAME = "Aditya Sharma";
    private static final String TEST_USER_EMAIL = "aditya@gmail.com";
    private static final String TEST_USER_IFSC = "XXXX8569";
    private static final String TEST_USER_PHONE = "7854123698";
    private static final String TEST_INITIAL_BALANCE = "7895641238"; // Initial high balance

    private DatabaseHelper dbHelper;
    private Context targetContext;

    @Rule
    public ActivityScenarioRule<Userdata> activityRule =
            new ActivityScenarioRule<>(createIntentWithAccountNo(TEST_ACCOUNT_NO));

    @Before
    public void setUp() {
        targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbHelper = new DatabaseHelper(targetContext);
        // Ensure a clean state and initial balance for the test user
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.onUpgrade(db, 1, 2); // Recreate tables with initial data
        db.close();
    }

    private static Intent createIntentWithAccountNo(String accountNo) {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(targetContext, Userdata.class);
        intent.putExtra("account_no", accountNo);
        return intent;
    }

    @Test
    public void testUserDataDisplayedCorrectly() {
        // Verify user details are displayed
        onView(withId(R.id.username)).check(matches(withText(TEST_USER_NAME)));
        onView(withId(R.id.useremail)).check(matches(withText(TEST_USER_EMAIL)));
        onView(withId(R.id.userno)).check(matches(withText(TEST_USER_PHONE)));
        onView(withId(R.id.useraccount)).check(matches(withText(TEST_ACCOUNT_NO)));
        onView(withId(R.id.userifsc)).check(matches(withText(TEST_USER_IFSC)));
        onView(withId(R.id.userbalance)).check(matches(withText(TEST_INITIAL_BALANCE)));
    }

    @Test
    public void testTransferAmountValidation_EmptyAmount() {
        // Click transfer button
        onView(withId(R.id.transfer_button)).perform(click());

        // Check if dialog appears (assuming a dialog pops up)
        // This might need adjustment based on actual implementation (e.g., AlertDialog)
        onView(withText("Enter Amount")).check(matches(isDisplayed())); // Check for dialog title or a view inside it

        // Click SEND without entering amount
        onView(withText("SEND")).perform(click());

        // Check for error - Assuming a Toast is shown. This requires a custom Toast matcher or alternative error display check.
        // For now, we'll assume the dialog stays open or shows an inline error.
        // If it's a Toast: onView(withText("Amount cannot be empty")).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
        // If it's an error on the EditText:
        onView(withId(R.id.editTextAmount)).check(matches(hasErrorText("Amount cannot be empty"))); // Adjust ID and error text if needed
    }


    @Test
    public void testTransferAmountValidation_InsufficientBalance() {
         // Get the current balance text
         final String[] currentBalance = new String[1];
         activityRule.getScenario().onActivity(activity -> {
            android.widget.TextView balanceTextView = activity.findViewById(R.id.userbalance);
            currentBalance[0] = balanceTextView.getText().toString();
         });
         long balanceValue = Long.parseLong(currentBalance[0]);
         long excessiveAmount = balanceValue + 1000; // Amount greater than balance

        // Click transfer button
        onView(withId(R.id.transfer_button)).perform(click());
        onView(withText("Enter Amount")).check(matches(isDisplayed()));

        // Enter excessive amount
        onView(withId(R.id.editTextAmount)).perform(typeText(String.valueOf(excessiveAmount)), closeSoftKeyboard());

        // Click SEND
        onView(withText("SEND")).perform(click());

        // Check for insufficient balance error
        // If it's a Toast: onView(withText("Insufficient Balance")).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
        // If it's an error on the EditText:
         onView(withId(R.id.editTextAmount)).check(matches(hasErrorText("Insufficient Balance"))); // Adjust ID and error text
    }


    @Test
    public void testTransferAmountValidation_ValidAmount() {
        String validAmount = "100"; // A small valid amount

        // Click transfer button
        onView(withId(R.id.transfer_button)).perform(click());
        onView(withText("Enter Amount")).check(matches(isDisplayed()));

        // Enter valid amount
        onView(withId(R.id.editTextAmount)).perform(typeText(validAmount), closeSoftKeyboard());

        // Click SEND
        onView(withText("SEND")).perform(click());

        // Verify SendtoUser activity is launched (or dialog closes and navigates)
        // Check if an element unique to SendtoUser activity is displayed
        onView(withId(R.id.send_to_user_list)).check(matches(isDisplayed())); // Assuming SendtoUser has a RecyclerView with this ID
    }


    @Test
    public void testTransactionCancellationFlow() {
        String validAmount = "50";

        // 1. Initiate transfer
        onView(withId(R.id.transfer_button)).perform(click());
        onView(withId(R.id.editTextAmount)).perform(typeText(validAmount), closeSoftKeyboard());
        onView(withText("SEND")).perform(click());

        // 2. Verify navigation to SendtoUser activity
        onView(withId(R.id.send_to_user_list)).check(matches(isDisplayed()));

        // 3. Simulate cancellation (Press Back)
        Espresso.pressBack();

        // 4. Verify back in Userdata activity
        onView(withId(R.id.username)).check(matches(withText(TEST_USER_NAME))); // Check if user data is visible again

        // 5. Verify 'Failed' transaction record in DB
        Cursor cursor = dbHelper.readtransferdata();
        assertNotNull("Cursor should not be null", cursor);
        assertTrue("Cursor should contain results", cursor.moveToLast()); // Check the latest transaction

        String fromName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FROM_NAME));
        String amount = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT));
        String status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS));

        assertEquals("From name should match test user", TEST_USER_NAME, fromName);
        // Note: The amount in the failed transaction might be recorded differently, adjust if needed.
        // It depends on *when* the record is inserted during the flow. Assuming it's inserted before cancellation.
        assertEquals("Amount should match entered amount", validAmount, amount);
        assertEquals("Status should be Failure", "Failure", status); // **Crucial Check**

        cursor.close();
    }
}
