package com.example.ryan.wheretobuy.model;


import java.io.Serializable;

public class ProductPrice implements Serializable {
    private String mID;
    private float mLowestPrice;
    private float mHighestPrice;
    private String mWhichIsLowest;
    private float mCMWPrice;
    private float mPLPrice;
    private float mFLPrice;
    private float mTWPrice;
    private float mHWPrice;
    private String mLastUpdateDateString;


    public ProductPrice(String id, float lowestPrice, float highestPrice, String whichIsLowest, float CMWPrice,
                        float PLPrice, float FLPrice, float TWPrice, float HWPrice, String lastUpdateDateString) {
        mID = id;
        mLowestPrice = lowestPrice;
        mHighestPrice = highestPrice;
        mWhichIsLowest = whichIsLowest;
        mCMWPrice = CMWPrice;
        mPLPrice = PLPrice;
        mFLPrice = FLPrice;
        mTWPrice = TWPrice;
        mHWPrice = HWPrice;
        mLastUpdateDateString = lastUpdateDateString;
    }

    public String getID() {
        return mID;
    }

    public void setID(String ID) {
        mID = ID;
    }

    public float getLowestPrice() {
        return mLowestPrice;
    }

    public void setLowestPrice(float lowestPrice) {
        mLowestPrice = lowestPrice;
    }

    public float getHighestPrice() {
        return mHighestPrice;
    }

    public void setHighestPrice(float highestPrice) {
        mHighestPrice = highestPrice;
    }

    public String getWhichIsLowest() {
        return mWhichIsLowest;
    }

    public void setWhichIsLowest(String whichIsLowest) {
        mWhichIsLowest = whichIsLowest;
    }

    public float getCMWPrice() {
        return mCMWPrice;
    }

    public void setCMWPrice(float CMWPrice) {
        mCMWPrice = CMWPrice;
    }

    public float getPLPrice() {
        return mPLPrice;
    }

    public void setPLPrice(float PLPrice) {
        mPLPrice = PLPrice;
    }

    public float getFLPrice() {
        return mFLPrice;
    }

    public void setFLPrice(float FLPrice) {
        mFLPrice = FLPrice;
    }

    public float getTWPrice() {
        return mTWPrice;
    }

    public void setTWPrice(float TWPrice) {
        mTWPrice = TWPrice;
    }

    public float getHWPrice() {
        return mHWPrice;
    }

    public void setHWPrice(float HWPrice) {
        mHWPrice = HWPrice;
    }

    public String getLastUpdateDateString() {
        return mLastUpdateDateString;
    }

    public void setLastUpdateDateString(String lastUpdateDateString) {
        mLastUpdateDateString = lastUpdateDateString;
    }
}