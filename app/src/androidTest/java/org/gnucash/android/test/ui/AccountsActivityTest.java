/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gnucash.android.test.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.robotium.solo.Solo;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.account.AccountsListFragment;

import java.util.Currency;
import java.util.List;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountsActivityTest extends ActivityInstrumentationTestCase2<AccountsActivity> {
	private static final String DUMMY_ACCOUNT_CURRENCY_CODE = "USD";
	private static final String DUMMY_ACCOUNT_NAME = "Dummy account";
    public static final String  DUMMY_ACCOUNT_UID   = "dummy-account";
	private Solo mSolo;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;

    public AccountsActivityTest() {
		super(AccountsActivity.class);
	}

	protected void setUp() throws Exception {
		Context context = getInstrumentation().getTargetContext();
        preventFirstRunDialogs(context);

        mDbHelper = new DatabaseHelper(context);
        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(getClass().getName(), "Error getting database: " + e.getMessage());
            mDb = mDbHelper.getReadableDatabase();
        }
        mSplitsDbAdapter = new SplitsDbAdapter(mDb);
        mTransactionsDbAdapter = new TransactionsDbAdapter(mDb, mSplitsDbAdapter);
        mAccountsDbAdapter = new AccountsDbAdapter(mDb, mTransactionsDbAdapter);

		mSolo = new Solo(getInstrumentation(), getActivity());
		
		Account account = new Account(DUMMY_ACCOUNT_NAME);
        account.setUID(DUMMY_ACCOUNT_UID);
		account.setCurrency(Currency.getInstance(DUMMY_ACCOUNT_CURRENCY_CODE));
		mAccountsDbAdapter.addAccount(account);
	}

    public static void preventFirstRunDialogs(Context context) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        //do not show first run dialog
        editor.putBoolean(context.getString(R.string.key_first_run), false);
        editor.putInt(AccountsActivity.LAST_OPEN_TAB_INDEX, AccountsActivity.INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT);

        //do not show "What's new" dialog
        String minorVersion = context.getString(R.string.app_minor_version);
        int currentMinor = Integer.parseInt(minorVersion);
        editor.putInt(context.getString(R.string.key_previous_minor_version), currentMinor);
        editor.commit();
    }

    /*
        public void testDisplayAccountsList(){
            final int NUMBER_OF_ACCOUNTS = 15;
            for (int i = 0; i < NUMBER_OF_ACCOUNTS; i++) {
                Account account = new Account("Acct " + i);
                mAccountsDbAdapter.addAccount(account);
            }

            //there should exist a listview of accounts
            refreshAccountsList();
            mSolo.waitForText("Acct");
            mSolo.scrollToBottom();

            ListView accountsListView = (ListView) mSolo.getView(android.R.id.list);
            assertNotNull(accountsListView);
            assertEquals(NUMBER_OF_ACCOUNTS + 1, accountsListView.getCount());
        }
    */
    public void testSearchAccounts(){
        String SEARCH_ACCOUNT_NAME = "Search Account";

        Account account = new Account(SEARCH_ACCOUNT_NAME);
        account.setParentUID(DUMMY_ACCOUNT_UID);
        mAccountsDbAdapter.addAccount(account);

        refreshAccountsList();

        //enter search query
//        ActionBarUtils.clickSherlockActionBarItem(mSolo, R.id.menu_search);
        mSolo.clickOnActionBarItem(R.id.menu_search);
        mSolo.sleep(2000);
        mSolo.enterText(0, "Se");
        mSolo.sleep(3000);
        boolean accountFound = mSolo.waitForText(SEARCH_ACCOUNT_NAME, 1, 2000);
        assertTrue(accountFound);

        mSolo.clearEditText(0);

        mSolo.sleep(2000);
        //the child account should be hidden again
        accountFound = mSolo.waitForText(SEARCH_ACCOUNT_NAME, 1, 2000);
        assertFalse(accountFound);
    }

    /**
     * Tests that an account can be created successfully and that the account list is sorted alphabetically.
     */
	public void testCreateAccount(){
        mSolo.clickOnActionBarItem(R.id.menu_add_account);
		mSolo.waitForText(getActivity().getString(R.string.title_add_account));

        View checkbox = mSolo.getCurrentActivity().findViewById(R.id.checkbox_parent_account);
        //there already exists one eligible parent account in the system
        assertThat(checkbox).isVisible();

        mSolo.clickOnCheckBox(0);

        EditText inputAccountName = (EditText) mSolo.getCurrentActivity().findViewById(R.id.edit_text_account_name);
        String NEW_ACCOUNT_NAME = "A New Account";
        mSolo.enterText(inputAccountName, NEW_ACCOUNT_NAME);
        mSolo.clickOnActionBarItem(R.id.menu_save);

        mSolo.waitForText(NEW_ACCOUNT_NAME);
        mSolo.sleep(3000);

		List<Account> accounts = mAccountsDbAdapter.getAllAccounts();
        assertThat(accounts).isNotNull();
        assertThat(accounts).hasSize(2);
		Account newestAccount = accounts.get(0); //because of alphabetical sorting

		assertThat(newestAccount.getName()).isEqualTo(NEW_ACCOUNT_NAME);
		assertThat(newestAccount.getCurrency().getCurrencyCode()).isEqualTo(Money.DEFAULT_CURRENCY_CODE);
        assertThat(newestAccount.isPlaceholderAccount()).isTrue();
	}

    public void testChangeParentAccount(){
        final String accountName = "Euro Account";
        Account account = new Account(accountName, Currency.getInstance("EUR"));
        mAccountsDbAdapter.addAccount(account);

        refreshAccountsList();
        mSolo.waitForText(accountName);

        mSolo.clickLongOnText(accountName);
        mSolo.clickOnView(mSolo.getView(R.id.context_menu_edit_accounts));
        mSolo.waitForView(EditText.class);

        mSolo.clickOnCheckBox(1);
        mSolo.sleep(2000);

        mSolo.clickOnActionBarItem(R.id.menu_save);
        mSolo.sleep(1000);
        mSolo.waitForText(getActivity().getString(R.string.title_accounts));
        Account editedAccount = mAccountsDbAdapter.getAccount(account.getUID());
        String parentUID = editedAccount.getParentUID();

        assertThat(parentUID).isNotNull();
        assertThat(DUMMY_ACCOUNT_UID).isEqualTo(parentUID);
    }

	public void testEditAccount(){
        refreshAccountsList();
        mSolo.sleep(2000);
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		String editedAccountName = "Edited Account";
				
		mSolo.clickLongOnText(DUMMY_ACCOUNT_NAME);

        clickSherlockActionBarItem(R.id.context_menu_edit_accounts);

        mSolo.waitForView(EditText.class);

		mSolo.clearEditText(0);
		mSolo.enterText(0, editedAccountName);

        clickSherlockActionBarItem(R.id.menu_save);

		mSolo.waitForDialogToClose();
        mSolo.waitForText("Accounts");

		List<Account> accounts = mAccountsDbAdapter.getAllAccounts();
		Account latest = accounts.get(0);  //will be the first due to alphabetical sorting
		
		assertEquals("Edited Account", latest.getName());
		assertEquals(DUMMY_ACCOUNT_CURRENCY_CODE, latest.getCurrency().getCurrencyCode());	
	}

    //TODO: Add test for moving content of accounts before deleting it
	public void testDeleteAccount(){
        final String accountNameToDelete = "TO BE DELETED";
        final String accountUidToDelete = "to-be-deleted";

        Account acc = new Account(accountNameToDelete);
        acc.setUID(accountUidToDelete);

        Transaction transaction = new Transaction("hats");
        transaction.addSplit(new Split(Money.getZeroInstance(), accountUidToDelete));
        acc.addTransaction(transaction);
        mAccountsDbAdapter.addAccount(acc);

        Fragment fragment = getActivity().getCurrentAccountListFragment();
        assertNotNull(fragment);

        ((AccountsListFragment) fragment).refresh();

        mSolo.clickLongOnText(accountNameToDelete);

        clickSherlockActionBarItem(R.id.context_menu_delete);

        mSolo.waitForDialogToOpen();
        mSolo.clickOnRadioButton(0);
        mSolo.clickOnView(mSolo.getView(R.id.btn_save));

        mSolo.waitForDialogToClose();
        mSolo.waitForText("Accounts");

        Exception expectedException = null;
        try {
            mAccountsDbAdapter.getID(accountUidToDelete);
        } catch (IllegalArgumentException e){
            expectedException = e;
        }
        assertNotNull(expectedException);

        List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(accountUidToDelete);
        assertEquals(0, transactions.size());
    }

	//TODO: Test import of account file
    //TODO: test settings activity

	public void testIntentAccountCreation(){
		Intent intent = new Intent(Intent.ACTION_INSERT);
		intent.putExtra(Intent.EXTRA_TITLE, "Intent Account");
		intent.putExtra(Intent.EXTRA_UID, "intent-account");
		intent.putExtra(Account.EXTRA_CURRENCY_CODE, "EUR");
		intent.setType(Account.MIME_TYPE);
		getActivity().sendBroadcast(intent);
		
		//give time for the account to be created
		synchronized (mSolo) {
			try {
				mSolo.wait(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
				
		Account account = mAccountsDbAdapter.getAccount("intent-account");
		assertNotNull(account);
		assertEquals("Intent Account", account.getName());
		assertEquals("intent-account", account.getUID());
		assertEquals("EUR", account.getCurrency().getCurrencyCode());
	}
	
	
	protected void tearDown() throws Exception {
        mSolo.finishOpenedActivities();
        mSolo.waitForEmptyActivityStack(20000);
        mSolo.sleep(5000);
        mAccountsDbAdapter.deleteAllRecords();

		super.tearDown();
	}

    /**
     * Finds a view in the action bar and clicks it, since the native methods are not supported by ActionBarSherlock
     * @param id
     */
    private void clickSherlockActionBarItem(int id){
        View view = mSolo.getView(id);
        mSolo.clickOnView(view);
    }

    /**
     * Refresh the account list fragment
     */
    private void refreshAccountsList(){
        Fragment fragment = getActivity().getCurrentAccountListFragment();
        ((AccountsListFragment)fragment).refresh();
    }
}
