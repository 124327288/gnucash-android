package org.gnucash.android.model;

import java.sql.Timestamp;

/**
 * Model for commodity prices
 */
public class Price extends BaseModel {

    private String mCommodityUID;
    private String mCurrencyUID;
    private Timestamp mDate;
    private String mSource;
    private String mType;
    private int mValueNum;
    private int mValueDenom;

    /**
     * String indicating that the price was provided by the user
     */
    public static final String SOURCE_USER = "user:xfer-dialog";

    public Price(){
        mDate = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Create new instance with the GUIDs of the commodities
     * @param commodityUID GUID of the origin commodity
     * @param currencyUID GUID of the target commodity
     */
    public Price(String commodityUID, String currencyUID){
        this.mCommodityUID = commodityUID;
        this.mCurrencyUID = currencyUID;
        mDate = new Timestamp(System.currentTimeMillis());
    }

    public String getCommodityUID() {
        return mCommodityUID;
    }

    public void setCommodityUID(String mCommodityUID) {
        this.mCommodityUID = mCommodityUID;
    }

    public String getCurrencyUID() {
        return mCurrencyUID;
    }

    public void setCurrencyUID(String currencyUID) {
        this.mCurrencyUID = currencyUID;
    }

    public Timestamp getDate() {
        return mDate;
    }

    public void setDate(Timestamp date) {
        this.mDate = date;
    }

    public String getSource() {
        return mSource;
    }

    public void setSource(String source) {
        this.mSource = source;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public int getValueNum() {
        return mValueNum;
    }

    public void setValueNum(int valueNum) {
        this.mValueNum = valueNum;
    }

    public int getValueDenom() {
        return mValueDenom;
    }

    public void setValueDenom(int valueDenom) {
        this.mValueDenom = valueDenom;
    }
}
