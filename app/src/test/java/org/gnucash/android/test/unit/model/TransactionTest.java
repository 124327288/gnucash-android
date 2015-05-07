package org.gnucash.android.test.unit.model;

import junit.framework.TestCase;

import org.gnucash.android.model.Money;
import org.gnucash.android.model.Transaction;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionTest extends TestCase {

	public TransactionTest(String name) {
		super(name);
	}

	public void testCloningTransaction(){
		Transaction transaction = new Transaction("Bobba Fett");
		assertThat(transaction.getUID()).isNotNull();
		assertThat(transaction.getCurrencyCode()).isEqualTo(Money.DEFAULT_CURRENCY_CODE);

		Transaction clone1 = new Transaction(transaction, false);
		assertThat(transaction.getUID()).isEqualTo(clone1.getUID());
		assertEquals(transaction, clone1);

		Transaction clone2 = new Transaction(transaction, true);
		assertThat(transaction.getUID()).isNotEqualTo(clone2.getUID());
		assertThat(transaction.getCurrencyCode()).isEqualTo(clone2.getCurrencyCode());
		assertThat(transaction.getDescription()).isEqualTo(clone2.getDescription());
		assertThat(transaction.getNote()).isEqualTo(clone2.getNote());
		assertThat(transaction.getTimeMillis()).isEqualTo(clone2.getTimeMillis());
		//TODO: Clone the created_at and modified_at times?

	}
}
