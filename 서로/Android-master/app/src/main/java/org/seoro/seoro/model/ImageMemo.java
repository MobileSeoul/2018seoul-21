package org.seoro.seoro.model;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ImageMemo implements Parcelable {
    public static final String LOCATION_PROVIDER = "ImageMemo";
    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private String mId;
    private User mUser;
    private String mImage;
    private String mThumbnail;
    private String mMemo;
    private Location mLocation;
    private Date mTimestamp;
    private int mCommentCount;
    private boolean mHasComment;

    private Bitmap mTempBitmap = null;

    public ImageMemo() {
        super();
    }

    public ImageMemo(JSONObject jsonObject) {
        mId = jsonObject.optString("_id", null);
        mUser = new User(jsonObject.optJSONObject("user"));
        mImage = jsonObject.optString("img", null);
        mThumbnail = jsonObject.optString("thumbnail", null);
        mMemo = jsonObject.optString("text",  null);
        JSONObject locationJSONObject = jsonObject.optJSONObject("loc");
        JSONArray coordinateJSONArray = locationJSONObject.optJSONArray("coordinates");

        double longitude = coordinateJSONArray.optDouble(0, 0.0);
        double latitude = coordinateJSONArray.optDouble(1, 0.0);

        mLocation = new Location(mUser.getDisplayName());
        mLocation.setLongitude(longitude);
        mLocation.setLatitude(latitude);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                TIMESTAMP_PATTERN,
                Locale.getDefault()
        );
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            mTimestamp = simpleDateFormat.parse(jsonObject.getString("date"));
        } catch (JSONException | ParseException e) {
            mTimestamp = null;
        }

        mCommentCount = jsonObject.optInt("commentCount", 0);
        mHasComment = jsonObject.optBoolean("hasComment", false);
    }

    public ImageMemo(Parcel parcel) {
        String[] strings = new String[18];

        parcel.readStringArray(strings);

        mId = strings[0];

        mUser = new User(
                strings[1],
                strings[2],
                strings[3],
                strings[4],
                strings[5],
                Integer.parseInt(strings[6]),
                Integer.parseInt(strings[7]),
                Integer.parseInt(strings[8]),
                Boolean.parseBoolean(strings[9])
        );
        mImage = strings[10];
        mThumbnail = strings[11];
        mMemo = strings[12];

        mLocation = new Location(mUser.getDisplayName());
        mLocation.setLongitude(Double.parseDouble(strings[13]));
        mLocation.setLatitude(Double.parseDouble(strings[14]));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                TIMESTAMP_PATTERN,
                Locale.getDefault()
        );

        try {
            mTimestamp = simpleDateFormat.parse(strings[15]);
        } catch (ParseException e) {
            mTimestamp = null;
        }

        mCommentCount = Integer.parseInt(strings[16]);
        mHasComment = Boolean.parseBoolean(strings[17]);
    }

    public static final Creator<ImageMemo> CREATOR = new Creator<ImageMemo>() {
        @Override
        public ImageMemo createFromParcel(Parcel in) {
            return new ImageMemo(in);
        }

        @Override
        public ImageMemo[] newArray(int size) {
            return new ImageMemo[size];
        }
    };

    public String getId() {
        return mId;
    }

    public User getUser() {
        return mUser;
    }

    public String getImage() {
        return mImage;
    }

    public String getThumbnail() {
        return mThumbnail;
    }

    public String getMemo() {
        return mMemo;
    }

    public Location getLocation() {
        return mLocation;
    }

    public Date getTimestamp() {
        return mTimestamp;
    }

    public int getCommentCount() {
        return mCommentCount;
    }

    public boolean hasComment() {
        return mHasComment;
    }

    public Bitmap getTempBitmap() {
        return mTempBitmap;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public void setUser(User mUser) {
        this.mUser = mUser;
    }

    public void setImage(String mImage) {
        this.mImage = mImage;
    }

    public void setThumbnail(String thumbnail) {
        this.mThumbnail = thumbnail;
    }

    public void setMemo(String mMemo) {
        this.mMemo = mMemo;
    }

    public void setLocation(Location mLocation) {
        this.mLocation = mLocation;
    }

    public void setTimestamp(Date mTimestamp) {
        this.mTimestamp = mTimestamp;
    }

    public void setCommentCount(int commentCount) {
        this.mCommentCount = commentCount;
    }

    public void setHasComment(boolean hasComment) {
        this.mHasComment = hasComment;
    }

    public void setTempBitmap(Bitmap tempBitmap) {
        this.mTempBitmap = tempBitmap;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                TIMESTAMP_PATTERN,
                Locale.getDefault()
        );

        String[] strings = new String[]{
                mId,
                mUser.getId(),
                mUser.getEmail(),
                mUser.getDisplayName(),
                mUser.getProfileImage(),
                mUser.getThumbnail(),
                Integer.toString(mUser.getMemoCount()),
                Integer.toString(mUser.getFollowerCount()),
                Integer.toString(mUser.getFollowingCount()),
                Boolean.toString(mUser.isFollowing()),
                mImage,
                mThumbnail,
                mMemo,
                Double.toString(mLocation.getLongitude()),
                Double.toString(mLocation.getLatitude()),
                simpleDateFormat.format(mTimestamp),
                Integer.toString(mCommentCount),
                Boolean.toString(mHasComment)
        };
        dest.writeStringArray(strings);
    }
}
