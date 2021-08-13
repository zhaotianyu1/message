package com.juphoon.helper.mms;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.android.vcard.VCardBuilder;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntry.PhoneData;
import com.android.vcard.VCardEntryCommitter;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardNotSupportedException;
import com.android.vcard.exception.VCardVersionException;
import com.juphoon.rcs.tool.RcsFileUtils;
import com.juphoon.service.RmsDefine;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class RcsVCardHelper {

    public static class VCardItem {
        public String mName;
        public List<String> mListPhone;
        public Bitmap mAvatar;
    }

    /**
     * 只导入一个联系人
     *
     * @param context
     * @param contactUri
     * @return
     */
    public static Uri exportVcard(Context context, Uri contactUri) {
        if (contactUri == null) {
            return null;
        }
        if (!contactUri.getScheme().equals("content")) {
            return contactUri;
        }
        long contactId = -1;
        Cursor cursor = context.getContentResolver().query(contactUri,
                new String[]{ContactsContract.Contacts._ID}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    contactId = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }
        return exportProfileVCard(context, contactUri.toString().contains("profile"), contactId);
    }
    public static ArrayList<RcsVCardHelper.VCardBean> exportVCards(Context context, boolean profile, List<Long> contactIdList) {
        ArrayList<RcsVCardHelper.VCardBean> vCardBeanArrayList = new ArrayList<RcsVCardHelper.VCardBean>();
        for (Long contactId : contactIdList) {
            RcsVCardHelper.VCardBean vCardBean = new RcsVCardHelper.VCardBean();
            String name = "", lookupKey = "";
            List<String> phones = new ArrayList<>();
            List<Integer> phonesType = new ArrayList<>();
            boolean haveNumber = false;
            long photoId = 0;
            Uri contactUri = null;
            byte[] photo = null;
            if (profile) {
                contactUri = ContactsContract.Profile.CONTENT_URI;
            } else {
                contactUri = ContactsContract.Contacts.CONTENT_URI;
            }
            String[] projectContact = new String[]{
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.PHOTO_ID};
            Cursor cursor = context.getContentResolver().query(contactUri, projectContact, ContactsContract.Contacts._ID + "=" + contactId, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0);
                        haveNumber = (cursor.getInt(1) > 0);
                        lookupKey = cursor.getString(2);
                        photoId = cursor.getLong(3);
                    }
                } finally {
                    cursor.close();
                }
            }
            if (TextUtils.isEmpty(lookupKey)) {
                return null;
            }
            if (!profile) {
                if (haveNumber) {
                    Cursor cursorNumber = context.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE},
                            ContactsContract.Data.LOOKUP_KEY + "=?", new String[]{lookupKey}, null);
                    if (cursorNumber != null) {
                        try {
                            while (cursorNumber.moveToNext()) {
                                phones.add(cursorNumber.getString(0));
                                phonesType.add(Integer.valueOf(cursorNumber.getString(1)));
                            }
                        } finally {
                            cursorNumber.close();
                        }
                    }
                }
                if (photoId != 0) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
                    if (input != null) {
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        try {
                            while ((len = input.read(buffer)) != -1) {
                                output.write(buffer, 0, len);
                            }
                            photo = output.toByteArray();
                            input.close();
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                if (haveNumber) {
                    Cursor cursorNumber = context.getContentResolver().query(
                            ContactsContract.RawContactsEntity.PROFILE_CONTENT_URI,
                            new String[]{ContactsContract.RawContactsEntity.DATA1, ContactsContract.RawContactsEntity.DATA2},
                            ContactsContract.Data.MIMETYPE + "=?", new String[]{"vnd.android.cursor.item/phone_v2"}, null);
                    if (cursorNumber != null) {
                        try {
                            while (cursorNumber.moveToNext()) {
                                phones.add(cursorNumber.getString(0));
                            }
                        } finally {
                            cursorNumber.close();
                        }
                    }
                }
                if (photoId != 0) {
                    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), ContactsContract.Profile.CONTENT_URI);
                    if (input != null) {
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        try {
                            while ((len = input.read(buffer)) != -1) {
                                output.write(buffer, 0, len);
                            }
                            photo = output.toByteArray();
                            input.close();
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            vCardBean.name = name;
            vCardBean.phones = phones;
            vCardBean.phonesType = phonesType;
            vCardBean.photo = photo;
            vCardBeanArrayList.add(vCardBean);
        }
        return vCardBeanArrayList;
    }

    public static Uri genVcards(Context context, ArrayList<RcsVCardHelper.VCardBean> vCardBeanArrayList) {
        if (!new File(RmsDefine.RMS_FILE_PATH).exists()) {
            new File(RmsDefine.RMS_FILE_PATH).mkdirs();
        }
        File file = new File(RcsFileUtils.getNotExistFileByTime(RmsDefine.RMS_FILE_PATH, "vcf"));
        Uri vcfUri = Uri.fromFile(file);
        VCardComposer composer = new VCardComposer(context, VCardConfig.VCARD_TYPE_V30_GENERIC, "UTF-8", true);
        OutputStream outputStream;
        try {
            outputStream = context.getContentResolver().openOutputStream(vcfUri);
            Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            for (RcsVCardHelper.VCardBean cardBean : vCardBeanArrayList) {
                VCardBuilder builder = new VCardBuilder(VCardConfig.VCARD_TYPE_V30_GENERIC, "UTF-8");
                List<ContentValues> listName = new ArrayList<>();
                ContentValues nameValues = new ContentValues();
                nameValues.put("data1", cardBean.name);
                nameValues.put("mimetype", "vnd.android.cursor.item/name");
                listName.add(nameValues);
                builder.appendNameProperties(listName);
                List<ContentValues> listPhones = new ArrayList<>();
                for (int i = 0; i < cardBean.phones.size(); i++) {
                    ContentValues phoneValues = new ContentValues();
                    phoneValues.put("data1", cardBean.phones.get(i));
                    phoneValues.put("data2", cardBean.phonesType.get(i));
                    phoneValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
                    listPhones.add(phoneValues);
                }
                builder.appendPhones(listPhones, null);
                if (cardBean.photo != null) {
                    List<ContentValues> listPhoto = new ArrayList<>();
                    ContentValues photoValues = new ContentValues();
                    photoValues.put("data15", cardBean.photo);
                    photoValues.put("mimetype", "vnd.android.cursor.item/photo");
                    listPhoto.add(photoValues);
                    builder.appendPhotos(listPhoto);
                }
                writer.write(builder.toString());
            }
            composer.terminate();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vcfUri;
    }

    private static Uri exportProfileVCard(Context context, boolean profile, long contactId) {
        String name = "", lookupKey = "";
        List<String> phones = new ArrayList<>();
        List<Integer> phonesType = new ArrayList<>();
        boolean haveNumber = false;
        long photoId = 0;
        Uri contactUri = null;
        byte[] photo = null;
        if (profile) {
            contactUri = ContactsContract.Profile.CONTENT_URI;
        } else {
            contactUri = ContactsContract.Contacts.CONTENT_URI;
        }
        String[] projectContact = new String[]{
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.PHOTO_ID};
        Cursor cursor = context.getContentResolver().query(contactUri, projectContact, ContactsContract.Contacts._ID + "=" + contactId, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0);
                    haveNumber = (cursor.getInt(1) > 0);
                    lookupKey = cursor.getString(2);
                    photoId = cursor.getLong(3);
                }
            } finally {
                cursor.close();
            }
        }
        if (TextUtils.isEmpty(lookupKey)) {
            return null;
        }
        if (!profile) {
            if (haveNumber) {
                Cursor cursorNumber = context.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE},
                        ContactsContract.Data.LOOKUP_KEY + "=?", new String[]{lookupKey}, null);
                if (cursorNumber != null) {
                    try {
                        while (cursorNumber.moveToNext()) {
                            phones.add(cursorNumber.getString(0));
                            phonesType.add(Integer.valueOf(cursorNumber.getString(1)));
                        }
                    } finally {
                        cursorNumber.close();
                    }
                }
            }
            if (photoId != 0) {
                Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
                if (input != null) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    try {
                        while ((len = input.read(buffer)) != -1) {
                            output.write(buffer, 0, len);
                        }
                        photo = output.toByteArray();
                        input.close();
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (haveNumber) {
                Cursor cursorNumber = context.getContentResolver().query(
                        ContactsContract.RawContactsEntity.PROFILE_CONTENT_URI,
                        new String[]{ContactsContract.RawContactsEntity.DATA1, ContactsContract.RawContactsEntity.DATA2},
                        ContactsContract.Data.MIMETYPE + "=?", new String[]{"vnd.android.cursor.item/phone_v2"}, null);
                if (cursorNumber != null) {
                    try {
                        while (cursorNumber.moveToNext()) {
                            phones.add(cursorNumber.getString(0));
                        }
                    } finally {
                        cursorNumber.close();
                    }
                }
            }
            if (photoId != 0) {
                InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), ContactsContract.Profile.CONTENT_URI);
                if (input != null) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    try {
                        while ((len = input.read(buffer)) != -1) {
                            output.write(buffer, 0, len);
                        }
                        photo = output.toByteArray();
                        input.close();
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return genVCard(context, name, phones, photo, phonesType);
    }

    /**
     * 导入名片到联系人
     *
     * @param context
     * @param uri
     * @return
     */
    public static List<Uri> importToContacts(Context context, Uri uri) {
        List<Uri> createdUris = importToContacts30(context, uri);
        if (createdUris == null) {
            return importToContacts21(context, uri);
        }
        return createdUris;
    }

    private static List<Uri> importToContacts30(Context context, Uri uri) {
        VCardParser parse = null;
        InputStream is = null;
        List<Uri> createdUris = null;
        try {
            if (uri != null) {
                is = context.getContentResolver().openInputStream(uri);
            }
            if (is != null) {
                try {
                    VCardEntryConstructor constructor = new VCardEntryConstructor(
                            VCardConfig.VCARD_TYPE_V30_GENERIC);
                    VCardEntryCommitter mCardEntryCommitter = new VCardEntryCommitter(
                            context.getContentResolver());
                    parse = new VCardParser_V30(VCardConfig.VCARD_TYPE_V30_GENERIC);
                    constructor.addEntryHandler(mCardEntryCommitter);
                    parse.addInterpreter(constructor);
                    parse.parse(is);
                    return mCardEntryCommitter.getCreatedUris();
                } catch (IOException e) {
                } catch (VCardNestedException e) {
                } catch (VCardNotSupportedException e) {
                } catch (VCardVersionException e) {
                } catch (VCardException e) {
                } finally {
                }
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return createdUris;
    }

    private static List<Uri> importToContacts21(Context context, Uri uri) {
        VCardParser parse = null;
        InputStream is = null;
        List<Uri> createdUris = null;
        try {
            if (uri != null) {
                is = context.getContentResolver().openInputStream(uri);
            }
            if (is != null) {
                try {
                    VCardEntryConstructor constructor = new VCardEntryConstructor(
                            VCardConfig.VCARD_TYPE_V21_GENERIC);
                    VCardEntryCommitter mCardEntryCommitter = new VCardEntryCommitter(
                            context.getContentResolver());
                    parse = new VCardParser_V21(VCardConfig.VCARD_TYPE_V21_GENERIC);
                    constructor.addEntryHandler(mCardEntryCommitter);
                    parse.addInterpreter(constructor);
                    parse.parse(is);
                    return mCardEntryCommitter.getCreatedUris();
                } catch (IOException e) {
                } catch (VCardNestedException e) {
                } catch (VCardNotSupportedException e) {
                } catch (VCardVersionException e) {
                } catch (VCardException e) {
                } finally {
                }
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return createdUris;
    }

    /**
     * 解析名片
     *
     * @param context
     * @param uri
     * @return
     */
    public static VCardItem parseVCard(Context context, Uri uri) {
        VCardItem item = parseVcard30(context, uri);
        if (item == null) {
            return parseVcard21(context, uri);
        }
        return item;
    }

    /**
     * 解析3.0格式名片
     *
     * @param context
     * @param uri
     * @return
     */
    private static VCardItem parseVcard30(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        VCardParser parse = null;
        InputStream is = null;
        final ArrayList<VCardEntry> listVCardEntry = new ArrayList<>();
        try {
            is = context.getContentResolver().openInputStream(uri);
            if (is != null) {
                try {
                    VCardEntryConstructor constructor = new VCardEntryConstructor(VCardConfig.VCARD_TYPE_V30_GENERIC);
                    parse = new VCardParser_V30(VCardConfig.VCARD_TYPE_V30_GENERIC);
                    constructor.addEntryHandler(new VCardEntryHandler() {

                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onEntryCreated(VCardEntry entry) {
                            listVCardEntry.add(entry);
                        }

                        @Override
                        public void onEnd() {

                        }
                    });
                    parse.addInterpreter(constructor);
                    parse.parse(is);
                    if (listVCardEntry.size() > 0) {
                        VCardEntry entry = listVCardEntry.get(0);
                        VCardItem item = new VCardItem();
                        item.mName = entry.getNameData() != null ? entry.getNameData().displayName : "";
                        item.mListPhone = new ArrayList<>();
                        List<PhoneData> l = entry.getPhoneList();
                        if (l != null) {
                            for (PhoneData data : l) {
                                item.mListPhone.add(data.getNumber());
                            }
                        }
                        if (entry.getPhotoList() != null && entry.getPhotoList().size() > 0) {
                            byte[] buffer = entry.getPhotoList().get(0).getBytes();
                            item.mAvatar = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                        }
                        return item;
                    }
                } catch (IOException e) {
                } catch (VCardNestedException e) {
                } catch (VCardNotSupportedException e) {
                } catch (VCardVersionException e) {
                } catch (VCardException e) {
                } finally {
                }
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

    /**
     * 解析2.1格式名片
     *
     * @param context
     * @param uri
     * @return
     */
    private static VCardItem parseVcard21(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        VCardParser parse = null;
        InputStream is = null;
        final ArrayList<VCardEntry> listVCardEntry = new ArrayList<>();
        try {
            is = context.getContentResolver().openInputStream(uri);
            if (is != null) {
                try {
                    VCardEntryConstructor constructor = new VCardEntryConstructor(VCardConfig.VCARD_TYPE_V21_GENERIC);
                    parse = new VCardParser_V21(VCardConfig.VCARD_TYPE_V21_GENERIC);
                    constructor.addEntryHandler(new VCardEntryHandler() {

                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onEntryCreated(VCardEntry entry) {
                            listVCardEntry.add(entry);
                        }

                        @Override
                        public void onEnd() {

                        }
                    });
                    parse.addInterpreter(constructor);
                    parse.parse(is);
                    if (listVCardEntry.size() > 0) {
                        VCardEntry entry = listVCardEntry.get(0);
                        VCardItem item = new VCardItem();
                        item.mName = entry.getNameData() != null ? entry.getNameData().displayName : "";
                        item.mListPhone = new ArrayList<>();
                        List<PhoneData> l = entry.getPhoneList();
                        if (l != null) {
                            for (PhoneData data : l) {
                                item.mListPhone.add(data.getNumber());
                            }
                        }
                        if (entry.getPhotoList() != null && entry.getPhotoList().size() > 0) {
                            byte[] buffer = entry.getPhotoList().get(0).getBytes();
                            item.mAvatar = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                        }
                        return item;
                    }
                } catch (IOException e) {
                } catch (VCardNestedException e) {
                } catch (VCardNotSupportedException e) {
                } catch (VCardVersionException e) {
                } catch (VCardException e) {
                } finally {
                }
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

    public static class VCardBean {
        public String name;
        public List<String> phones;
        public byte[] photo;
        public List<Integer> phonesType;
    }

    public static Uri genVCard(Context context, String name, List<String> phones, byte[] photo, List<Integer> phonesType) {
        File file = new File(RcsFileUtils.getNotExistFileByTime(RmsDefine.RMS_FILE_PATH, "vcf"));
        Uri vcfUri = Uri.fromFile(file);
        VCardComposer composer = new VCardComposer(context, VCardConfig.VCARD_TYPE_V30_GENERIC, "UTF-8", true);
        OutputStream outputStream;
        try {
            outputStream = context.getContentResolver().openOutputStream(vcfUri);
            Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            VCardBuilder builder = new VCardBuilder(VCardConfig.VCARD_TYPE_V30_GENERIC, "UTF-8");
            List<ContentValues> listName = new ArrayList<>();
            ContentValues nameValues = new ContentValues();
            nameValues.put("data1", name);
            nameValues.put("mimetype", "vnd.android.cursor.item/name");
            listName.add(nameValues);
            builder.appendNameProperties(listName);
            List<ContentValues> listPhones = new ArrayList<>();
            for (int i = 0; i < phones.size(); i++) {
                ContentValues phoneValues = new ContentValues();
                phoneValues.put("data1", phones.get(i));
                phoneValues.put("data2", phonesType.get(i));
                phoneValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
                listPhones.add(phoneValues);
            }
            builder.appendPhones(listPhones, null);
            if (photo != null) {
                List<ContentValues> listPhoto = new ArrayList<>();
                ContentValues photoValues = new ContentValues();
                photoValues.put("data15", photo);
                photoValues.put("mimetype", "vnd.android.cursor.item/photo");
                listPhoto.add(photoValues);
                builder.appendPhotos(listPhoto);
            }
            writer.write(builder.toString());
            composer.terminate();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vcfUri;
    }

    public static void importToContacts(Context context,
                                        InputStream in_withcode) {
        VCardParser parse = null;
        if (in_withcode != null) {
            try {
                VCardEntryConstructor constructor = new VCardEntryConstructor(
                        VCardConfig.VCARD_TYPE_V30_GENERIC);
                VCardEntryCommitter mCardEntryCommitter = new VCardEntryCommitter(
                        context.getContentResolver());
                parse = new VCardParser_V30(VCardConfig.VCARD_TYPE_V30_GENERIC);
                constructor.addEntryHandler(mCardEntryCommitter);
                parse.addInterpreter(constructor);
                parse.parse(in_withcode);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (VCardNestedException e) {
                e.printStackTrace();
            } catch (VCardNotSupportedException e) {
                e.printStackTrace();
            } catch (VCardVersionException e) {
                e.printStackTrace();
            } catch (VCardException e) {
                e.printStackTrace();
            } finally {
                try {
                    in_withcode.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
