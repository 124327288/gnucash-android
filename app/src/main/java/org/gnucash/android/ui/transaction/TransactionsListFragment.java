/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.CursorRecyclerAdapter;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.ui.FormActivity;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.util.AccountBalanceTask;
import org.gnucash.android.ui.util.OnTransactionClickedListener;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;
import org.ocpsoft.prettytime.PrettyTime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * List Fragment for displaying list of transactions for an account
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class TransactionsListFragment extends Fragment implements
        Refreshable, LoaderCallbacks<Cursor>{

	/**
	 * Logging tag
	 */
	protected static final String LOG_TAG = "TransactionListFragment";

    private TransactionsDbAdapter mTransactionsDbAdapter;
    private String mAccountUID;


	private TransactionRecyclerAdapter mTransactionRecyclerAdapter;
	@Bind(R.id.transaction_recycler_view) RecyclerView mRecyclerView;
	@Bind(R.id.fab_create_transaction) FloatingActionButton createTransactionFAB;
	/**
	 * Callback listener for editing transactions
	 */
	private OnTransactionClickedListener mTransactionEditListener;

	@Override
 	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		Bundle args = getArguments();
		mAccountUID = args.getString(UxArgument.SELECTED_ACCOUNT_UID);

		mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_transactions_list, container, false);
		ButterKnife.bind(this, view);

		mRecyclerView.setHasFixedSize(true);
		LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
		mRecyclerView.setLayoutManager(mLayoutManager);

		createTransactionFAB = (FloatingActionButton) view.findViewById(R.id.fab_create_transaction);
		createTransactionFAB.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mTransactionEditListener.createNewTransaction(mAccountUID);
			}
		});
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		
		ActionBar aBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		aBar.setDisplayShowTitleEnabled(false);
		aBar.setDisplayHomeAsUpEnabled(true);

		mTransactionRecyclerAdapter = new TransactionRecyclerAdapter(null);
		mRecyclerView.setAdapter(mTransactionRecyclerAdapter);

		setHasOptionsMenu(true);		
	}

    /**
     * Refresh the list with transactions from account with ID <code>accountId</code>
     * @param accountUID GUID of account to load transactions from
     */
    @Override
	public void refresh(String accountUID){
		mAccountUID = accountUID;
		refresh();
	}

    /**
     * Reload the list of transactions and recompute account balances
     */
    @Override
	public void refresh(){
		getLoaderManager().restartLoader(0, null, this);

        /*
	  Text view displaying the sum of the accounts
	 */
        TextView mSumTextView = (TextView) getView().findViewById(R.id.transactions_sum);
        new AccountBalanceTask(mSumTextView).execute(mAccountUID);

	}
			
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			 mTransactionEditListener = (OnTransactionClickedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnAccountSelectedListener");
		}	
	}
	
	@Override
	public void onResume() {
		super.onResume();
		((TransactionsActivity)getActivity()).updateNavigationSelection();
		refresh();
	}

	public void onListItemClick(long id) {
		Intent intent = new Intent(getActivity(), TransactionInfoActivity.class);
		intent.putExtra(UxArgument.SELECTED_TRANSACTION_UID, mTransactionsDbAdapter.getUID(id));
		intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
		startActivity(intent);
//		mTransactionEditListener.editTransaction(mTransactionsDbAdapter.getUID(id));
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.transactions_list_actions, menu);	
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Log.d(LOG_TAG, "Creating transactions loader");
		return new TransactionsCursorLoader(getActivity(), mAccountUID);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.d(LOG_TAG, "Transactions loader finished. Swapping in cursor");
		mTransactionRecyclerAdapter.swapCursor(cursor);
		mTransactionRecyclerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(LOG_TAG, "Resetting transactions loader");
		mTransactionRecyclerAdapter.swapCursor(null);
	}

	
	/**
	 * {@link DatabaseCursorLoader} for loading transactions asynchronously from the database
	 * @author Ngewi Fet <ngewif@gmail.com>
	 */
	protected static class TransactionsCursorLoader extends DatabaseCursorLoader {
		private String accountUID;
		
		public TransactionsCursorLoader(Context context, String accountUID) {
			super(context);			
			this.accountUID = accountUID;
		}
		
		@Override
		public Cursor loadInBackground() {
			mDatabaseAdapter = TransactionsDbAdapter.getInstance();
			Cursor c = ((TransactionsDbAdapter) mDatabaseAdapter).fetchAllTransactionsForAccount(accountUID);
			if (c != null)
				registerContentObserver(c);
			return c;
		}		
	}

	public class TransactionRecyclerAdapter extends CursorRecyclerAdapter<TransactionRecyclerAdapter.ViewHolder>{
		private int VIEW_TYPE_HEADER = 0x10;
		private int VIEW_TYPE_CONTENT = 0x11;

		private final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("MMMM yyyy", Locale.US);
		private final PrettyTime prettyTime = new PrettyTime();
		public TransactionRecyclerAdapter(Cursor cursor) {
			super(cursor);
		}

		/**
		 * Checks if two timestamps have the same calendar month
		 * @param timeMillis1 Timestamp in milliseconds
		 * @param timeMillis2 Timestamp in milliseconds
		 * @return <code>true</code> if both timestamps are on same day, <code>false</code> otherwise
		 */
		private boolean isSameMonth(long timeMillis1, long timeMillis2){
			Date date1 = new Date(timeMillis1);
			Date date2 = new Date(timeMillis2);

			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMM", Locale.US);
			return fmt.format(date1).equals(fmt.format(date2));
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.cardview_transaction, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public int getItemViewType(int position) {
			if (position == 0){
				return VIEW_TYPE_HEADER;
			} else {
				Cursor cursor = getCursor();
				long transactionTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP));
				cursor.moveToPosition(position - 1);
				long previousTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP));
				cursor.moveToPosition(position);
				//has header if two consecutive transactions were not in same month
				return isSameMonth(previousTimestamp, transactionTime) ? VIEW_TYPE_CONTENT : VIEW_TYPE_HEADER;
			}
		}

		@Override
		public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
			holder.transactionId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry._ID));

			String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION));
			holder.transactionDescription.setText(description);

			final String transactionUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_UID));
			Money amount = mTransactionsDbAdapter.getBalance(transactionUID, mAccountUID);
			TransactionsActivity.displayBalance(holder.transactionAmount, amount);

			List<Split> splits = SplitsDbAdapter.getInstance().getSplitsForTransaction(transactionUID);
			String text = "";

			if (splits.size() == 2 && splits.get(0).isPairOf(splits.get(1))){
				for (Split split : splits) {
					if (!split.getAccountUID().equals(mAccountUID)){
						text = AccountsDbAdapter.getInstance().getFullyQualifiedAccountName(split.getAccountUID());
						break;
					}
				}
			}

			if (splits.size() > 2){
				text = splits.size() + " splits";
			}
			holder.transactionNote.setText(text);

			long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP));
			holder.transactionDate.setText(prettyTime.format(new Date(dateMillis)));

			final long id = holder.transactionId;
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onListItemClick(id);
				}
			});

			holder.editTransaction.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getActivity(), FormActivity.class);
					intent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.TRANSACTION_FORM.name());
					intent.putExtra(UxArgument.SELECTED_TRANSACTION_UID, transactionUID);
					intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
					startActivity(intent);
				}
			});

		}


		public class ViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener{
			@Bind(R.id.primary_text) 		public TextView transactionDescription;
			@Bind(R.id.secondary_text) 		public TextView transactionNote;
			@Bind(R.id.transaction_amount)	public TextView transactionAmount;
			@Bind(R.id.transaction_date)	public TextView transactionDate;
			@Bind(R.id.edit_transaction)	public ImageView editTransaction;
			@Bind(R.id.options_menu)		public ImageView optionsMenu;

			long transactionId;

			public ViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);

				optionsMenu.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						PopupMenu popup = new PopupMenu(getActivity(), v);
						popup.setOnMenuItemClickListener(ViewHolder.this);
						MenuInflater inflater = popup.getMenuInflater();
						inflater.inflate(R.menu.transactions_context_menu, popup.getMenu());
						popup.show();
					}
				});
			}

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
					case R.id.context_menu_delete:
						mTransactionsDbAdapter.deleteRecord(transactionId);
						WidgetConfigurationActivity.updateAllWidgets(getActivity());
						refresh();
						return true;

					default:
						return false;

				}
			}
		}
	}
}
