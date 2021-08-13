package com.juphoon.helper.mms;

/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.messaging.Factory;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.media.VideoThumbnailRequest;
import com.android.messaging.sms.DatabaseMessages.DatabaseMessage;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.MediaMetadataRetrieverWrapper;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.juphoon.service.RmsDefine.Rms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class RcsDatabaseMessages {
    public static String TAG = RcsDatabaseMessages.class.getSimpleName();
    /**
     * RMS message
     */
    public static class RmsMessage extends DatabaseMessage implements Parcelable {
        private static int sIota = 0;
        public static final int INDEX_ID = sIota++;
        public static final int INDEX_TYPE = sIota++;
        public static final int INDEX_ADDRESS = sIota++;
        public static final int INDEX_BODY = sIota++;
        public static final int INDEX_DATE = sIota++;
        public static final int INDEX_THREAD_ID = sIota++;
        public static final int INDEX_STATUS = sIota++;
        public static final int INDEX_READ = sIota++;
        public static final int INDEX_SEEN = sIota++;
        public static final int INDEX_DATE_SENT = sIota++;
        public static final int INDEX_SUB_ID = sIota++;
        public static final int INDEX_RMS_MESSAGE_TYPE = sIota++;

        public static final int INDEX_FILE_NAME = sIota++;
        public static final int INDEX_FILE_TYPE = sIota++;
        public static final int INDEX_FILE_PATH = sIota++;
        public static final int INDEX_FILE_SIZE = sIota++;
        public static final int INDEX_FILE_DURATION = sIota++;
        public static final int INDEX_THUMB_PATH = sIota++;
        public static final int INDEX_TRANS_ID = sIota++;
        public static final int INDEX_TRANS_SIZE = sIota++;
        public static final int INDEX_IMDN = sIota++;
        public static final int INDEX_PA_UUID = sIota++;
        public static final int INDEX_RMS_EXTRA = sIota++;
        public static final int INDEX_MIX_TYPE = sIota++;
        public static final int INDEX_CONVERSATION_ID = sIota++;
        public static final int INDEX_IMDN_STATUS = sIota++;
        public static final int INDEX_GROUPCHAT_ID = sIota++;
        public static final int INDEX_RMS_EXTRA2 = sIota++;
        public static final int INDEX_RMS_CHATBOT_SERVICE_ID = sIota++;
        public static final int INDEX_RMS_TRAFFIC_TYPE = sIota++;
        public static final int INDEX_IMDN_TYPE = sIota++;
        public static final int INDEX_CONTRIBUTION_ID = sIota++;

        private static String[] sProjection;

        public static String[] getProjection() {
            if (sProjection == null) {
                String[] projection = new String[] {
                        Rms._ID,
                        Rms.TYPE,
                        Rms.ADDRESS,
                        Rms.BODY,
                        Rms.DATE,
                        Rms.THREAD_ID,
                        Rms.STATUS,
                        Rms.READ,
                        Rms.SEEN,
                        Rms.DATE_SENT,
                        Rms.SUB_ID,
                        Rms.MESSAGE_TYPE,

                        Rms.FILE_NAME,
                        Rms.FILE_TYPE,
                        Rms.FILE_PATH,
                        Rms.FILE_SIZE,
                        Rms.FILE_DURATION,
                        Rms.THUMB_PATH,
                        Rms.TRANS_ID,
                        Rms.TRANS_SIZE,
                        Rms.IMDN_STRING,
                        Rms.PA_UUID,
                        Rms.RMS_EXTRA,
                        Rms.MIX_TYPE,
                        Rms.CONVERSATION_ID,
                        Rms.IMDN_STATUS,
                        Rms.GROUP_CHAT_ID,
                        Rms.RMS_EXTRA2,
                        Rms.CHATBOT_SERVICE_ID,
                        Rms.TRAFFIC_TYPE,
                        Rms.IMDN_TYPE,
                        Rms.CONTRIBUTION_ID
                };
                if (!OsUtil.isAtLeastL_MR1()) {
                    Assert.equals(INDEX_SUB_ID, projection.length - 1);
                    String[] withoutSubId = new String[projection.length - 1];
                    System.arraycopy(projection, 0, withoutSubId, 0, withoutSubId.length);
                    projection = withoutSubId;
                }
                sProjection = projection;
            }
            return sProjection;
        }

        public String mUri;
        public String mAddress;
        public String mBody;
        public long mRowId;
        public long mTimestampInMillis;
        public long mTimestampSentInMillis;
        public int mType;
        public long mThreadId;
        public int mStatus;
        public boolean mRead;
        public boolean mSeen;
        public int mSubId;
        public int mMessageType;

        public String mContentType;
        public int mWidth;
        public int mHeight;
        public long mSize;
        public String mFileName;
        public String mFilePath;
//        public String mFileType;
        public long mDuration;
        public String mThumbPath;
        public String mTransId;
        public long mTransSize;
        public String mImdn;
        public String mPaUuId;
        public String mRmsExtra;
        public int mMixType;
        public String mConversationId; //不是同步表的conversationId
        public String mImdnStatus;
        public String mGroupChatId;
        public String mRmsExtra2;
        public String mRmsChatbotServiceId;
        public String mTrafficType;
        public int mImdnType;
        public String mContributionId;

        private RmsMessage() {
        }

        /**
         * Load from a cursor of a query that returns the RMS to import
         *
         * @param cursor
         */
        private void load(final Cursor cursor) {
            mRowId = cursor.getLong(INDEX_ID);
            mAddress = cursor.getString(INDEX_ADDRESS);
            mBody = cursor.getString(INDEX_BODY);
            mTimestampInMillis = cursor.getLong(INDEX_DATE);
            mTimestampSentInMillis = cursor.getLong(INDEX_DATE_SENT);
            mType = cursor.getInt(INDEX_TYPE);
            mThreadId = cursor.getLong(INDEX_THREAD_ID);
            mStatus = cursor.getInt(INDEX_STATUS);
            mRead = cursor.getInt(INDEX_READ) == 0 ? false : true;
            mSeen = cursor.getInt(INDEX_SEEN) == 0 ? false : true;
            mUri = ContentUris.withAppendedId(Rms.CONTENT_URI_LOG, mRowId).toString();
            mSubId = PhoneUtils.getDefault().getSubIdFromTelephony(cursor, INDEX_SUB_ID);
            mMessageType = cursor.getInt(INDEX_RMS_MESSAGE_TYPE);

            mFileName = cursor.getString(INDEX_FILE_NAME);
            mContentType = cursor.getString(INDEX_FILE_TYPE);
            mFilePath = cursor.getString(INDEX_FILE_PATH);
            mSize = cursor.getLong(INDEX_FILE_SIZE);
            mDuration = cursor.getLong(INDEX_FILE_DURATION);
            mThumbPath = cursor.getString(INDEX_THUMB_PATH);
            mTransId = cursor.getString(INDEX_TRANS_ID);
            mTransSize = cursor.getLong(INDEX_TRANS_SIZE);
            mImdn = cursor.getString(INDEX_IMDN);
            mPaUuId = cursor.getString(INDEX_PA_UUID);
            mRmsExtra = cursor.getString(INDEX_RMS_EXTRA);
            mMixType = cursor.getInt(INDEX_MIX_TYPE);
            mConversationId = cursor.getString(INDEX_CONVERSATION_ID);
            mImdnStatus = cursor.getString(INDEX_IMDN_STATUS);
            mGroupChatId = cursor.getString(INDEX_GROUPCHAT_ID);
            mRmsExtra2 = cursor.getString(INDEX_RMS_EXTRA2);
            mRmsChatbotServiceId = cursor.getString(INDEX_RMS_CHATBOT_SERVICE_ID);
            mTrafficType = cursor.getString(INDEX_RMS_TRAFFIC_TYPE);
            mImdnType = cursor.getInt(INDEX_IMDN_TYPE);
            mContributionId = cursor.getString(INDEX_CONTRIBUTION_ID);
            mWidth = 0;
            mHeight = 0;

            switch (mMessageType) {
                case Rms.RMS_MESSAGE_TYPE_GEO:
                    mContentType = ContentType.APP_GEO_MESSAGE;
                    mBody = mRmsExtra;
                    break;
                case Rms.RMS_MESSAGE_TYPE_SYSTEM:
                case Rms.RMS_MESSAGE_TYPE_TEXT:
                case Rms.RMS_MESSAGE_TYPE_CHATBOT_TEXT:
                    mContentType = ContentType.TEXT_PLAIN;
                    break;
                case Rms.RMS_MESSAGE_TYPE_CHATBOT_CARD:
                    mContentType = ContentType.APP_CHATBOT_CARD_MESSAGE;
                    break;
                default:
                    break;
            }

            if (isMedia()) {
                // For importing we don't load media since performance is critical
                // For loading when we receive rms, we do load media to get enough
                // information of the media file
                if (ContentType.isImageType(mContentType) && (!TextUtils.isEmpty(mThumbPath) || !TextUtils.isEmpty(mFilePath))) {
                    loadImage();
                } else if (ContentType.isVideoType(mContentType) && !TextUtils.isEmpty(mFilePath)) {
                    loadVideo();
                } // No need to load audio for parsing
            }
        }

        /**
         * Get content type from file extension
         */
        private static String extractContentType(final Context context, final Uri uri) {
            final String path = uri.getPath();
            final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String extension = MimeTypeMap.getFileExtensionFromUrl(path);
            if (TextUtils.isEmpty(extension)) {
                // getMimeTypeFromExtension() doesn't handle spaces in filenames nor can it handle
                // urlEncoded strings. Let's try one last time at finding the extension.
                final int dotPos = path.lastIndexOf('.');
                if (0 <= dotPos) {
                    extension = path.substring(dotPos + 1);
                }
            }
            return mimeTypeMap.getMimeTypeFromExtension(extension);
        }

        /**
         * Load image file of an image part and parse the dimensions and type
         */
        private void loadImage() {
            final Context context = Factory.get().getApplicationContext();
            final ContentResolver resolver = context.getContentResolver();
            final Uri uri = TextUtils.isEmpty(mFilePath) ? getThumbUri() : getDataUri();
            // We have to get the width and height of the image -- they're needed when adding
            // an attachment in bugle.
            InputStream is = null;
            try {
                is = resolver.openInputStream(uri);
                final BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, opt);
                mContentType = opt.outMimeType;
                mWidth = opt.outWidth;
                mHeight = opt.outHeight;
                if (TextUtils.isEmpty(mContentType)) {
                    // BitmapFactory couldn't figure out the image type. That's got to be a bad
                    // sign, but see if we can figure it out from the file extension.
                    mContentType = extractContentType(context, uri);
                }
            } catch (final FileNotFoundException e) {
                LogUtil.e(TAG, "DatabaseMessages.MmsPart.loadImage: file not found", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "IOException caught while closing stream", e);
                    }
                }
            }
        }

        /**
         * Load video file of a video part and parse the dimensions and type
         */
        private void loadVideo() {
            // This is a coarse check, and should not be applied to outgoing messages. However,
            // currently, this does not cause any problems.
            if (!VideoThumbnailRequest.shouldShowIncomingVideoThumbnails()) {
                return;
            }
            final Uri uri = getDataUri();
            final MediaMetadataRetrieverWrapper retriever = new MediaMetadataRetrieverWrapper();
            try {
                retriever.setDataSource(uri);
                // FLAG: This inadvertently fixes a problem with phone receiving audio
                // messages on some carrier. We should handle this in a less accidental way so that
                // we don't break it again. (The carrier changes the content type in the wrapper
                // in-transit from audio/mp4 to video/3gpp without changing the data)
                // Also note: There is a bug in some OEM device where mmr returns
                // video/ffmpeg for image files.  That shouldn't happen here but be aware.
                mContentType =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                final Bitmap bitmap = retriever.getFrameAtTime(-1);
                if (bitmap != null) {
                    mWidth = bitmap.getWidth();
                    mHeight = bitmap.getHeight();
                } else {
                    // Get here if it's not actually video (see above)
                    LogUtil.i(LogUtil.BUGLE_TAG, "loadVideo: Got null bitmap from " + uri);
                }
            } catch (IOException e) {
                LogUtil.i(LogUtil.BUGLE_TAG, "Error extracting metadata from " + uri, e);
            } finally {
                retriever.release();
            }
        }

        public boolean isText() {
            return ContentType.TEXT_PLAIN.equals(mContentType)
                    || ContentType.TEXT_HTML.equals(mContentType)
                    || ContentType.APP_WAP_XHTML.equals(mContentType);
        }

        public boolean isMedia() {
            return ContentType.isImageType(mContentType)
                    || ContentType.isVideoType(mContentType)
                    || ContentType.isAudioType(mContentType)
                    || ContentType.isVCardType(mContentType)
                    || ContentType.isGeoType(mContentType);
        }

        public boolean isFile() {
            return ContentType.isFileType(mContentType);
        }

        public boolean isImage() {
            return ContentType.isImageType(mContentType);
        }

        public boolean isVideo() {
            return ContentType.isVideoType(mContentType);
        }

        public boolean isAudio() {
            return ContentType.isAudioType(mContentType);
        }

        public boolean isGeo() {
            return ContentType.isGeoType(mContentType);
        }

        public boolean isChatbotCard() {
            return ContentType.isChatbotCard(mContentType);
        }

        public Uri getDataUri() {
            return Uri.parse("file://"+mFilePath);
        }

        public Uri getThumbUri() {
            return Uri.parse("file://" + mThumbPath);
        }

        /**
         * Get a new RmsMessage by loading from the cursor of a query that
         * returns the RMS to import
         * @param cursor
         * @return
         */
        public static RmsMessage get(final Cursor cursor) {
            final RmsMessage msg = new RmsMessage();
            msg.load(cursor);
            return msg;
        }

        public static RmsMessage get(long smsId) {
            final RmsMessage msg = new RmsMessage();
            Cursor cursor = Factory.get().getApplicationContext().getContentResolver().query(Rms.CONTENT_URI_LOG, getProjection(), Rms.SMS_ID + "=" + smsId, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToNext()) {
                        msg.load(cursor);
                    }
                } finally {
                    cursor.close();
                }
            }
            return msg;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public int getProtocol() {
            return MessageData.PROTOCOL_RMS;
        }

        @Override
        public String getUri() {
            return mUri;
        }

        public long getId() {
            return mRowId;
        }

        public long getTimestampInMillis() {
            return mTimestampInMillis;
        }

        public int getSubId() {
            return mSubId;
        }

        private RmsMessage(final Parcel in) {
            mUri = in.readString();
            mRowId = in.readLong();
            mTimestampInMillis = in.readLong();
            mTimestampSentInMillis = in.readLong();
            mType = in.readInt();
            mThreadId = in.readLong();
            mStatus = in.readInt();
            mRead = in.readInt() != 0;
            mSeen = in.readInt() != 0;
            mSubId = in.readInt();
            mMessageType = in.readInt();

            mAddress = in.readString();
            mBody = in.readString();

            mFileName = in.readString();
            mFilePath = in.readString();
            mSize = in.readLong();
            mDuration = in.readLong();
            mThumbPath = in.readString();
            mContentType = in.readString();
            mTransSize = in.readLong();
            mTransId = in.readString();
            mWidth = in.readInt();
            mHeight = in.readInt();
            mSize = in.readLong();
            mImdn = in.readString();
            mPaUuId = in.readString();
            mRmsExtra = in.readString();
            mMixType = in.readInt();
            mConversationId = in.readString();
            mImdnStatus = in.readString();
            mGroupChatId = in.readString();
            mRmsExtra2 = in.readString();
            mRmsChatbotServiceId = in.readString();
            mTrafficType = in.readString();
            mImdnType = in.readInt();
            mContributionId = in.readString();
        }

        public static final Parcelable.Creator<RmsMessage> CREATOR
                = new Parcelable.Creator<RmsMessage>() {
            @Override
            public RmsMessage createFromParcel(final Parcel in) {
                return new RmsMessage(in);
            }

            @Override
            public RmsMessage[] newArray(final int size) {
                return new RmsMessage[size];
            }
        };

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(mUri);
            out.writeLong(mRowId);
            out.writeLong(mTimestampInMillis);
            out.writeLong(mTimestampSentInMillis);
            out.writeInt(mType);
            out.writeLong(mThreadId);
            out.writeInt(mStatus);
            out.writeInt(mRead ? 1 : 0);
            out.writeInt(mSeen ? 1 : 0);
            out.writeInt(mSubId);
            out.writeInt(mMessageType);

            out.writeString(mAddress);
            out.writeString(mBody);

            out.writeString(mFileName);
            out.writeString(mFilePath);
            out.writeLong(mSize);
            out.writeLong(mDuration);
            out.writeString(mThumbPath);
            out.writeString(mContentType);
            out.writeLong(mTransSize);
            out.writeString(mTransId);
            out.writeInt(mWidth);
            out.writeInt(mHeight);
            out.writeLong(mSize);
            out.writeString(mImdn);            
            out.writeString(mPaUuId);
            out.writeString(mRmsExtra);
            out.writeInt(mMixType);
            out.writeString(mConversationId);
            out.writeString(mImdnStatus);
            out.writeString(mGroupChatId);
            out.writeString(mRmsExtra2);
            out.writeString(mRmsChatbotServiceId);
            out.writeString(mTrafficType);
            out.writeInt(mImdnType);
            out.writeString(mContributionId);
        }
    }

}
