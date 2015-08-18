package org.gnucash.android.model;


import android.support.annotation.NonNull;

/**
 * A split amount in a transaction.
 * Every transaction is made up of at least two splits (representing a double entry transaction)
 * <p>The split amount is always stored in the database as the absolute value alongside its transaction type of CREDIT/DEBIT<br/>
 * This is independent of the negative values which are shown in the UI (for user convenience).
 * The actual movement of the balance in the account depends on the type of normal balance of the account and the
 * transaction type of the split.</p>
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class Split extends BaseModel{
    /**
     * Amount value of this split which is in the currency of the transaction
     */
    private Money mValue;

    /**
     * Amount of the split in the currency of the account to which the split belongs
     */
    private Money mQuantity;

    /**
     * Transaction UID which this split belongs to
     */
    private String mTransactionUID = "";

    /**
     * Account UID which this split belongs to
     */
    private String mAccountUID;

    /**
     * The type of this transaction, credit or debit
     */
    private TransactionType mSplitType = TransactionType.CREDIT;

    /**
     * Memo associated with this split
     */
    private String mMemo;

    /**
     * Initialize split with a value amount and account
     * @param value Money value amount of this split
     * @param accountUID String UID of transfer account
     */
    public Split(@NonNull Money value, String accountUID){
        setValue(value);
        setAccountUID(accountUID);
        //NOTE: This is a rather simplististic approach to the split type.
        //It typically also depends on the account type of the account. But we do not want to access
        //the database everytime a split is created. So we keep it simple here. Set the type you want explicity.
        mSplitType = value.isNegative() ? TransactionType.DEBIT : TransactionType.CREDIT;
    }

    /**
     * Clones the <code>sourceSplit</code> to create a new instance with same fields
     * @param sourceSplit Split to be cloned
     * @param generateUID Determines if the clone should have a new UID or should maintain the one from source
     */
    public Split(Split sourceSplit, boolean generateUID){
        this.mMemo          = sourceSplit.mMemo;
        this.mAccountUID    = sourceSplit.mAccountUID;
        this.mSplitType     = sourceSplit.mSplitType;
        this.mTransactionUID = sourceSplit.mTransactionUID;
        this.mValue = sourceSplit.mValue.absolute();

        if (generateUID){
            generateUID();
        } else {
            setUID(sourceSplit.getUID());
        }
    }

    /**
     * Returns the value amount of the split
     * @return Money amount of the split with the currency of the transaction
     * @see #getQuantity()
     */
    public Money getValue() {
        return mValue;
    }

    /**
     * Sets the value amount of the split.<br>
     * The value is in the currency of the containing transaction
     * <p>If the quantity of the split is null, it will be set to the {@code amount}</p>
     * @param amount Money value of this split
     * @see #setQuantity(Money)
     */
    public void setValue(Money amount) {
        this.mValue = amount;
        if (mQuantity == null){
            mQuantity = amount;
        }
    }

    /**
     * Returns the quantity amount of the split.
     * <p>The quantity is in the currency of the account to which the split is associated</p>
     * @return Money quantity amount
     * @see #getValue()
     */
    public Money getQuantity() {
        return mQuantity;
    }

    /**
     * Sets the quantity value of the split
     * @param quantity Money quantity amount
     * @see #setValue(Money)
     */
    public void setQuantity(Money quantity) {
        this.mQuantity = quantity;
    }

    /**
     * Returns transaction GUID to which the split belongs
     * @return String GUID of the transaction
     */
    public String getTransactionUID() {
        return mTransactionUID;
    }

    /**
     * Sets the transaction to which the split belongs
     * @param transactionUID GUID of transaction
     */
    public void setTransactionUID(String transactionUID) {
        this.mTransactionUID = transactionUID;
    }

    /**
     * Returns the account GUID of this split
     * @return GUID of the account
     */
    public String getAccountUID() {
        return mAccountUID;
    }

    /**
     * Sets the GUID of the account of this split
     * @param accountUID GUID of account
     */
    public void setAccountUID(String accountUID) {
        this.mAccountUID = accountUID;
    }

    /**
     * Returns the type of the split
     * @return {@link TransactionType} of the split
     */
    public TransactionType getType() {
        return mSplitType;
    }

    /**
     * Sets the type of this split
     * @param splitType Type of the split
     */
    public void setType(TransactionType splitType) {
        this.mSplitType = splitType;
    }

    /**
     * Returns the memo of this split
     * @return String memo of this split
     */
    public String getMemo() {
        return mMemo;
    }

    /**
     * Sets this split memo
     * @param memo String memo of this split
     */
    public void setMemo(String memo) {
        this.mMemo = memo;
    }

    /**
     * Creates a split which is a pair of this instance.
     * A pair split has all the same attributes except that the SplitType is inverted and it belongs
     * to another account.
     * @param accountUID GUID of account
     * @return New split pair of current split
     * @see TransactionType#invert()
     */
    public Split createPair(String accountUID){
        Split pair = new Split(mValue.absolute(), accountUID);
        pair.setType(mSplitType.invert());
        pair.setMemo(mMemo);
        pair.setTransactionUID(mTransactionUID);

        return pair;
    }

    /**
     * Clones this split and returns an exact copy.
     * @return New instance of a split which is a copy of the current one
     */
    protected Split clone() throws CloneNotSupportedException {
        super.clone();
        Split split = new Split(mValue, mAccountUID);
        split.setUID(getUID());
        split.setType(mSplitType);
        split.setMemo(mMemo);
        split.setTransactionUID(mTransactionUID);
        return split;
    }

    /**
     * Checks is this <code>other</code> is a pair split of this.
     * <p>Two splits are considered a pair if they have the same amount and opposite split types</p>
     * @param other the other split of the pair to be tested
     * @return whether the two splits are a pair
     */
    public boolean isPairOf(Split other) {
        return mValue.absolute().equals(other.mValue.absolute())
                && mSplitType.invert().equals(other.mSplitType);
    }

    @Override
    public String toString() {
        return mSplitType.name() + " of " + mValue.toString() + " in account: " + mAccountUID;
    }

    /**
     * Returns a string representation of the split which can be parsed again using {@link org.gnucash.android.model.Split#parseSplit(String)}
     * <p>The string is formatted as:<br/>
     * "<valueNum>;<valueDenom>;<valueCurrencyCode>;<quantityNum>;<quantityDenom>;<quantityCurrencyCode>;<transaction_uid>;<account_uid>;<type>;<memo>"</p>
     * @return the converted CSV string of this split
     */
    public String toCsv(){
        String sep = ";";
        int valueFractionDigits = mValue.getCurrency().getDefaultFractionDigits();
        int qtyFractionDigits = mQuantity.getCurrency().getDefaultFractionDigits();
        String valueDenom = String.valueOf((int) Math.pow(10, valueFractionDigits));
        String valueNum = String.valueOf(mValue.multiply(Integer.parseInt(valueDenom)).intValue());
        String qtyDenom = String.valueOf((int)Math.pow(10, qtyFractionDigits));
        String qtyNum   = String.valueOf(mQuantity.multiply(Integer.parseInt(qtyDenom)).intValue());

        String splitString = valueNum + sep + valueDenom + sep + mValue.getCurrency().getCurrencyCode()
                + qtyNum + sep + qtyDenom + sep + mQuantity.getCurrency().getCurrencyCode()
                + sep + mTransactionUID + sep + mAccountUID + sep + mSplitType.name();
        if (mMemo != null){
            splitString = splitString + sep + mMemo;
        }
        return splitString;
    }

    /**
     * Parses a split which is in the format:<br/>
     * "<valueNum>;<valueDenom>;<currency_code>;<quantityNum>;<quantityDenom>;<currency_code>;<transaction_uid>;<account_uid>;<type>;<memo>".
     * <p>Also supports parsing of the deprecated format "<amount>;<currency_code>;<transaction_uid>;<account_uid>;<type>;<memo>".
     * The split input string is the same produced by the {@link Split#toCsv()} method
     *</p>
     * @param splitString String containing formatted split
     * @return Split instance parsed from the string
     */
    public static Split parseSplit(String splitString) {
        String[] tokens = splitString.split(";");
        if (tokens.length < 8) {
            Money amount = new Money(tokens[0], tokens[1]);
            Split split = new Split(amount, tokens[2]);
            split.setTransactionUID(tokens[3]);
            split.setType(TransactionType.valueOf(tokens[4]));
            if (tokens.length == 6) {
                split.setMemo(tokens[5]);
            }
            return split;
        } else {
            int valueNum = Integer.parseInt(tokens[0]);
            int valueDenom = Integer.parseInt(tokens[1]);
            String valueCurrencyCode = tokens[2];
            int quantityNum = Integer.parseInt(tokens[3]);
            int quantityDenom = Integer.parseInt(tokens[4]);
            String qtyCurrencyCode = tokens[5];

            Money value = new Money(valueNum, valueDenom, valueCurrencyCode);
            Money quantity = new Money(quantityNum, quantityDenom, qtyCurrencyCode);

            Split split = new Split(value, tokens[7]);
            split.setQuantity(quantity);
            split.setTransactionUID(tokens[6]);
            split.setType(TransactionType.valueOf(tokens[8]));
            if (tokens.length == 9) {
                split.setMemo(tokens[9]);
            }
            return split;
        }
    }
}
