package org.seoro.seoro.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EmojiComment implements Parcelable {
    public enum Emoji {
        HAPPY,
        SAD,
        LIKE,
        DISLIKE
    }

    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    String mId;
    String mMemoId;
    User mUser;
    Emoji mEmoji;
    Date mTimestamp;

    public EmojiComment(JSONObject jsonObject) {
        mId = jsonObject.optString("_id");
        mMemoId = jsonObject.optString("memo");
        mUser = new User(jsonObject.optJSONObject("user"));
        mEmoji = Emoji.valueOf(jsonObject.optString("emoji", "HAPPY"));
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
    }

    public EmojiComment(Parcel parcel) {
        String[] strings = new String[13];

        parcel.readStringArray(strings);

        mId = strings[0];
        mMemoId = strings[1];

        mUser = new User(
                strings[2],
                strings[3],
                strings[4],
                strings[5],
                strings[6],
                Integer.parseInt(strings[7]),
                Integer.parseInt(strings[8]),
                Integer.parseInt(strings[9]),
                Boolean.parseBoolean(strings[10])
        );
        mEmoji = Emoji.valueOf(strings[11]);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                TIMESTAMP_PATTERN,
                Locale.getDefault()
        );

        try {
            mTimestamp = simpleDateFormat.parse(strings[12]);
        } catch (ParseException e) {
            mTimestamp = null;
        }
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
                mMemoId,
                mUser.getId(),
                mUser.getEmail(),
                mUser.getDisplayName(),
                mUser.getProfileImage(),
                mUser.getThumbnail(),
                Integer.toString(mUser.getMemoCount()),
                Integer.toString(mUser.getFollowerCount()),
                Integer.toString(mUser.getFollowingCount()),
                Boolean.toString(mUser.isFollowing()),
                mEmoji.name(),
                simpleDateFormat.format(mTimestamp),
        };
        dest.writeStringArray(strings);
    }

    public void setId(String id) {
        this.mId = id;
    }

    public void setMemoId(String memoId) {
        this.mMemoId = memoId;
    }

    public void setUser(User user) {
        this.mUser = user;
    }

    public void setEmoji(Emoji emoji) {
        this.mEmoji = emoji;
    }

    public void setTimestamp(Date timestamp) {
        this.mTimestamp = timestamp;
    }

    public String getId() {
        return mId;
    }

    public String getMemoId() {
        return mMemoId;
    }

    public User getUser() {
        return mUser;
    }

    public Emoji getEmoji() {
        return mEmoji;
    }

    public Date getTimestamp() {
        return mTimestamp;
    }
}
