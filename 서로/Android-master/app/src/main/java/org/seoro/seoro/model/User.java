package org.seoro.seoro.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

import java.util.regex.Pattern;

public class User implements Parcelable {
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$");
    public static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");
    public static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("^[^\\s\\t\\n\\r\\`\\~\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\+\\=\\{\\}\\[\\]\\:\\\"\\;\\'\\<\\>\\?\\,\\.\\/\\|\\\\]+$");

    private String mId;
    private String mEmail;
    private String mDisplayName;
    private String mProfileImage;
    private String mThumbnail;
    private int mMemoCount;
    private int mFollowerCount;
    private int mFollowingCount;
    private boolean mFollowing;

    public User(String id, String email, String displayName, String profileImage, String thumbnail, int memoCount, int followerCount, int followingCount, boolean following) {
        mId = id;
        mEmail = email;
        mDisplayName = displayName;
        mProfileImage = profileImage;
        mThumbnail = thumbnail;
        mMemoCount = memoCount;
        mFollowerCount = followerCount;
        mFollowingCount = followingCount;
        mFollowing = following;
    }

    public User(JSONObject jsonObject) {
        if (jsonObject != null) {
            mId = jsonObject.optString("_id", null);
            mEmail = jsonObject.optString("email", null);
            mDisplayName = jsonObject.optString("displayName", null);
            mProfileImage = jsonObject.optString("profile", null);
            mThumbnail = jsonObject.optString("thumbnail", null);
            mMemoCount = jsonObject.optInt("memoCount", 0);
            mFollowerCount = jsonObject.optInt("followerCount", 0);
            mFollowingCount = jsonObject.optInt("followingCount", 0);
            mFollowing = jsonObject.optBoolean("following", false);
        }
    }

    public User(Parcel parcel) {
        String[] strings = new String[9];

        parcel.readStringArray(strings);

        mId = strings[0];
        mEmail = strings[1];
        mDisplayName = strings[2];
        mProfileImage = strings[3];
        mThumbnail = strings[4];
        mMemoCount = Integer.parseInt(strings[5]);
        mFollowerCount = Integer.parseInt(strings[6]);
        mFollowingCount = Integer.parseInt(strings[7]);
        mFollowing = Boolean.parseBoolean(strings[8]);
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public void setId(String id) {
        this.mId = id;
    }

    public void setEmail(String email) {
        this.mEmail = email;
    }

    public void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    public void setProfileImage(String profileImage) {
        this.mProfileImage = profileImage;
    }

    public void setThumbnail(String thumbnail) {
        this.mThumbnail = thumbnail;
    }

    public void setMemoCount(int memoCount) {
        this.mMemoCount = memoCount;
    }

    public void setFollowerCount(int followerCount) {
        this.mFollowerCount = followerCount;
    }

    public void setFollowingCount(int followingCount) {
        this.mFollowingCount = followingCount;
    }

    public void setFollowing(boolean following) {
        this.mFollowing = following;
    }

    public String getId() {
        return mId;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getProfileImage() {
        return mProfileImage;
    }

    public String getThumbnail() {
        return mThumbnail;
    }

    public int getMemoCount() {
        return mMemoCount;
    }

    public int getFollowerCount() {
        return mFollowerCount;
    }

    public int getFollowingCount() {
        return mFollowingCount;
    }

    public boolean isFollowing() {
        return mFollowing;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String[] strings = new String[]{
                mId,
                mEmail,
                mDisplayName,
                mProfileImage,
                mThumbnail,
                Integer.toString(mMemoCount),
                Integer.toString(mFollowerCount),
                Integer.toString(mFollowingCount),
                Boolean.toString(mFollowing)
        };

        dest.writeStringArray(strings);
    }
}
