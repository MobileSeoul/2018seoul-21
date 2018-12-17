package org.seoro.seoro.model;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONObject;

public class GroupImageMemo {
    private String mId;
    private String mImage;
    private String mThumbnail;
    private Location mLocation;
    private int mCount;

    public GroupImageMemo(JSONObject jsonObject) {
        mId = jsonObject.optString("_id", null);
        mImage = jsonObject.optString("img", null);
        mThumbnail = jsonObject.optString("thumbnail", null);
        JSONObject locationJSONObject = jsonObject.optJSONObject("loc");
        JSONArray coordinateJSONArray = locationJSONObject.optJSONArray("coordinates");

        double longitude = coordinateJSONArray.optDouble(0, 0.0);
        double latitude = coordinateJSONArray.optDouble(1, 0.0);

        mLocation = new Location(mId);
        mLocation.setLongitude(longitude);
        mLocation.setLatitude(latitude);

        mCount = jsonObject.optInt("count", 0);
    }

    public String getId() {
        return mId;
    }

    public String getImage() {
        return mImage;
    }

    public String getThumbnail() {
        return mThumbnail;
    }

    public Location getLocation() {
        return mLocation;
    }

    public int getCount() {
        return mCount;
    }
}
