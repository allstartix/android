/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.client.database.migrations;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import com.nextcloud.client.core.Clock;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.providers.FileContentProvider;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.util.Locale;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQueryBuilder;

public class LegacyMigrationHelper {

    private static final String TAG = LegacyMigrationHelper.class.getSimpleName();

    private static final String ALTER_TABLE = "ALTER TABLE ";
    private static final String ADD_COLUMN = " ADD COLUMN ";
    private static final String INTEGER = " INTEGER, ";
    private static final String TEXT = " TEXT, ";

    private static final String UPGRADE_VERSION_MSG = "OUT of the ADD in onUpgrade; oldVersion == %d, newVersion == %d";

    private static final String[] PROJECTION_FILE_AND_STORAGE_PATH = new String[]{
        ProviderMeta.ProviderTableMeta._ID, ProviderMeta.ProviderTableMeta.FILE_STORAGE_PATH, ProviderMeta.ProviderTableMeta.FILE_PATH
    };
    
    private final Context context;
    private final Clock clock;

    public LegacyMigrationHelper(Context context, Clock clock) {
        this.context = context;
        this.clock = clock;
    }

    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        Log_OC.i(TAG, "Entering in onUpgrade");
        boolean upgraded = false;

        if (oldVersion < 25 && newVersion >= 25) {
            Log_OC.i(TAG, "Entering in the #25 Adding encryption flag to file");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_IS_ENCRYPTED + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_ENCRYPTED_NAME + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION + " INTEGER ");
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 26 && newVersion >= 26) {
            Log_OC.i(TAG, "Entering in the #26 Adding text and element color to capabilities");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR + " TEXT ");

                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 27 && newVersion >= 27) {
            Log_OC.i(TAG, "Entering in the #27 Adding token to ocUpload");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN + " TEXT ");
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 28 && newVersion >= 28) {
            Log_OC.i(TAG, "Entering in the #28 Adding CRC32 column to filesystem table");
            db.beginTransaction();
            try {
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32 + " TEXT ");
                }
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 29 && newVersion >= 29) {
            Log_OC.i(TAG, "Entering in the #29 Adding background default/plain to capabilities");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_DEFAULT + " INTEGER ");

                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_PLAIN + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 30 && newVersion >= 30) {
            Log_OC.i(TAG, "Entering in the #30 Re-add 25, 26 if needed");
            db.beginTransaction();
            try {
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.FILE_IS_ENCRYPTED)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_IS_ENCRYPTED + " INTEGER ");
                }
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.FILE_ENCRYPTED_NAME)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_ENCRYPTED_NAME + " TEXT ");
                }
                if (oldVersion > FileContentProvider.ARBITRARY_DATA_TABLE_INTRODUCTION_VERSION) {
                    if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderMeta.ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION)) {
                        db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION + " INTEGER ");
                    }
                    if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR)) {
                        db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR + " TEXT ");
                    }
                    if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR)) {
                        db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR + " TEXT ");
                    }
                    if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME,
                                             ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32)) {
                        try {
                            db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME +
                                           ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32 + " TEXT ");
                        } catch (SQLiteException e) {
                            Log_OC.d(TAG, "Known problem on adding same column twice when upgrading from 24->30");
                        }
                    }
                }

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 31 && newVersion >= 31) {
            Log_OC.i(TAG, "Entering in the #31 add mount type");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_MOUNT_TYPE + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 32 && newVersion >= 32) {
            Log_OC.i(TAG, "Entering in the #32 add ocshares.is_password_protected");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED + " INTEGER "); // boolean

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 33 && newVersion >= 33) {
            Log_OC.i(TAG, "Entering in the #3 Adding activity to capability");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_ACTIVITY + " INTEGER ");
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 34 && newVersion >= 34) {
            Log_OC.i(TAG, "Entering in the #34 add redirect to external links");
            db.beginTransaction();
            try {
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT + " INTEGER "); // boolean
                }
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 35 && newVersion >= 35) {
            Log_OC.i(TAG, "Entering in the #35 add note to share table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_NOTE + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 36 && newVersion >= 36) {
            Log_OC.i(TAG, "Entering in the #36 add has-preview to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_HAS_PREVIEW + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 37 && newVersion >= 37) {
            Log_OC.i(TAG, "Entering in the #37 add hide-download to share table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 38 && newVersion >= 38) {
            Log_OC.i(TAG, "Entering in the #38 add richdocuments");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT + " INTEGER "); // boolean
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST + " TEXT "); // string

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 39 && newVersion >= 39) {
            Log_OC.i(TAG, "Entering in the #39 add richdocuments direct editing");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING + " INTEGER "); // bool

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 40 && newVersion >= 40) {
            Log_OC.i(TAG, "Entering in the #40 add unreadCommentsCount to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 41 && newVersion >= 41) {
            Log_OC.i(TAG, "Entering in the #41 add eTagOnServer");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_ETAG_ON_SERVER + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 42 && newVersion >= 42) {
            Log_OC.i(TAG, "Entering in the #42 add richDocuments templates");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_TEMPLATES + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 43 && newVersion >= 43) {
            Log_OC.i(TAG, "Entering in the #43 add ownerId and owner display name to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_OWNER_ID + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_OWNER_DISPLAY_NAME + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 44 && newVersion >= 44) {
            Log_OC.i(TAG, "Entering in the #44 add note to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_NOTE + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 45 && newVersion >= 45) {
            Log_OC.i(TAG, "Entering in the #45 add sharees to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_SHAREES + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 46 && newVersion >= 46) {
            Log_OC.i(TAG, "Entering in the #46 add optional mimetypes to capabilities table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST
                               + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 47 && newVersion >= 47) {
            Log_OC.i(TAG, "Entering in the #47 add askForPassword to capability table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD +
                               " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 48 && newVersion >= 48) {
            Log_OC.i(TAG, "Entering in the #48 add product name to capabilities table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 49 && newVersion >= 49) {
            Log_OC.i(TAG, "Entering in the #49 add extended support to capabilities table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_EXTENDED_SUPPORT + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 50 && newVersion >= 50) {
            Log_OC.i(TAG, "Entering in the #50 add persistent enable date to synced_folders table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS + " INTEGER ");

                db.execSQL("UPDATE " + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME + " SET " +
                               ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS + " = CASE " +
                               " WHEN enabled = 0 THEN " + SyncedFolder.EMPTY_ENABLED_TIMESTAMP_MS + " " +
                               " ELSE " + clock.getCurrentTime() +
                               " END ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 51 && newVersion >= 51) {
            Log_OC.i(TAG, "Entering in the #51 add show/hide to folderSync table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_HIDDEN + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 52 && newVersion >= 52) {
            Log_OC.i(TAG, "Entering in the #52 add etag for directEditing to capability");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_DIRECT_EDITING_ETAG + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 53 && newVersion >= 53) {
            Log_OC.i(TAG, "Entering in the #53 add rich workspace to file table");
            db.beginTransaction();
            try {
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.FILE_RICH_WORKSPACE)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_RICH_WORKSPACE + " TEXT ");
                }
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 54 && newVersion >= 54) {
            Log_OC.i(TAG, "Entering in the #54 add synced.existing," +
                " rename uploads.force_overwrite to uploads.name_collision_policy");
            db.beginTransaction();
            try {
                // Add synced.existing
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXISTING + " INTEGER "); // boolean


                // Rename uploads.force_overwrite to uploads.name_collision_policy
                String tmpTableName = ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME + "_old";
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME + " RENAME TO " + tmpTableName);
                createUploadsTable(db);
                db.execSQL("INSERT INTO " + ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME + " (" +
                               ProviderMeta.ProviderTableMeta._ID + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_PATH + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_REMOTE_PATH + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_ACCOUNT_NAME + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_FILE_SIZE + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_STATUS + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_TIME + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LAST_RESULT + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_WIFI_ONLY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_CREATED_BY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN +
                               ") " +
                               " SELECT " +
                               ProviderMeta.ProviderTableMeta._ID + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_PATH + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_REMOTE_PATH + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_ACCOUNT_NAME + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_FILE_SIZE + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_STATUS + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_TIME + ", " +
                               "force_overwrite" + ", " + // See FileUploader.NameCollisionPolicy
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LAST_RESULT + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_WIFI_ONLY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_CREATED_BY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN +
                               " FROM " + tmpTableName);
                db.execSQL("DROP TABLE " + tmpTableName);

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 55 && newVersion >= 55) {
            Log_OC.i(TAG, "Entering in the #55 add synced.name_collision_policy.");
            db.beginTransaction();
            try {
                // Add synced.name_collision_policy
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY + " INTEGER "); // integer

                // make sure all existing folders set to FileUploader.NameCollisionPolicy.ASK_USER.
                db.execSQL("UPDATE " + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME + " SET " +
                               ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY + " = " +
                               NameCollisionPolicy.ASK_USER.serialize());
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 56 && newVersion >= 56) {
            Log_OC.i(TAG, "Entering in the #56 add decrypted remote path");
            db.beginTransaction();
            try {
                // Add synced.name_collision_policy
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_PATH_DECRYPTED + " TEXT "); // strin

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 57 && newVersion >= 57) {
            Log_OC.i(TAG, "Entering in the #57 add etag for capabilities");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_ETAG + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 58 && newVersion >= 58) {
            Log_OC.i(TAG, "Entering in the #58 add public link to share table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_LINK + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 59 && newVersion >= 59) {
            Log_OC.i(TAG, "Entering in the #59 add public label to share table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_LABEL + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 60 && newVersion >= 60) {
            Log_OC.i(TAG, "Entering in the #60 add user status to capability table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_USER_STATUS + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 61 && newVersion >= 61) {
            Log_OC.i(TAG, "Entering in the #61 reset eTag to force capability refresh");
            db.beginTransaction();
            try {
                db.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 62 && newVersion >= 62) {
            Log_OC.i(TAG, "Entering in the #62 add logo to capability");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_LOGO + " TEXT ");

                // force refresh
                db.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (oldVersion < 63 && newVersion >= 63) {
            Log_OC.i(TAG, "Adding file locking columns");
            db.beginTransaction();
            try {
                // locking capabilities
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME + ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_LOCKING_VERSION + " TEXT ");
                // force refresh
                db.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1");
                // locking properties
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCKED + " INTEGER "); // boolean
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_TYPE + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_OWNER + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_OWNER_DISPLAY_NAME + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_OWNER_EDITOR + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_TIMESTAMP + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_TIMEOUT + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_TOKEN + " TEXT ");
                db.execSQL("UPDATE " + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME + " SET " + ProviderMeta.ProviderTableMeta.FILE_ETAG + " = '' WHERE 1=1");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 64 && newVersion >= 64) {
            Log_OC.i(TAG, "Entering in the #64 add metadata size to files");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_METADATA_SIZE + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }
    }

    private void createOCSharesTable(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME + "("
                       + ProviderMeta.ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                       + ProviderMeta.ProviderTableMeta.OCSHARES_FILE_SOURCE + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_ITEM_SOURCE + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_TYPE + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_WITH + TEXT
                       + ProviderMeta.ProviderTableMeta.OCSHARES_PATH + TEXT
                       + ProviderMeta.ProviderTableMeta.OCSHARES_PERMISSIONS + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_SHARED_DATE + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_EXPIRATION_DATE + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_TOKEN + TEXT
                       + ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME + TEXT
                       + ProviderMeta.ProviderTableMeta.OCSHARES_IS_DIRECTORY + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.OCSHARES_USER_ID + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + TEXT
                       + ProviderMeta.ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_NOTE + TEXT
                       + ProviderMeta.ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD + INTEGER
                       + ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_LINK + TEXT
                       + ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_LABEL + " TEXT );");
    }

    private void createCapabilitiesTable(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME + "("
                       + ProviderMeta.ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MAYOR + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MINOR + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MICRO + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_STRING + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_EDITION + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_EXTENDED_SUPPORT + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED + INTEGER // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED + INTEGER    // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED + INTEGER // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL + INTEGER    // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD + INTEGER       // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL + INTEGER      // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_RESHARING + INTEGER           // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING + INTEGER     // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING + INTEGER     // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING + INTEGER   // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_UNDELETE + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_VERSIONING + INTEGER   // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_EXTERNAL_LINKS + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_NAME + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_COLOR + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_LOGO + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_ACTIVITY + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_DEFAULT + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_PLAIN + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_TEMPLATES + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_DIRECT_EDITING_ETAG + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_USER_STATUS + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI + INTEGER
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_ETAG + TEXT
                       + ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_LOCKING_VERSION + " TEXT );");
    }

    private void createUploadsTable(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME + "("
                       + ProviderMeta.ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                       + ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_PATH + TEXT
                       + ProviderMeta.ProviderTableMeta.UPLOADS_REMOTE_PATH + TEXT
                       + ProviderMeta.ProviderTableMeta.UPLOADS_ACCOUNT_NAME + TEXT
                       + ProviderMeta.ProviderTableMeta.UPLOADS_FILE_SIZE + " LONG, "
                       + ProviderMeta.ProviderTableMeta.UPLOADS_STATUS + INTEGER               // UploadStatus
                       + ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + INTEGER      // Upload LocalBehaviour
                       + ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_TIME + INTEGER
                       + ProviderMeta.ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + INTEGER
                       + ProviderMeta.ProviderTableMeta.UPLOADS_LAST_RESULT + INTEGER     // Upload LastResult
                       + ProviderMeta.ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.UPLOADS_IS_WIFI_ONLY + INTEGER // boolean
                       + ProviderMeta.ProviderTableMeta.UPLOADS_CREATED_BY + INTEGER    // Upload createdBy
                       + ProviderMeta.ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN + " TEXT );");

    /* before:
    // PRIMARY KEY should always imply NOT NULL. Unfortunately, due to a
    // bug in some early versions, this is not the case in SQLite.
    //db.execSQL("CREATE TABLE " + TABLE_UPLOAD + " (" + " path TEXT PRIMARY KEY NOT NULL UNIQUE,"
    //        + " uploadStatus INTEGER NOT NULL, uploadObject TEXT NOT NULL);");
    // uploadStatus is used to easy filtering, it has precedence over
    // uploadObject.getUploadStatus()
    */
    }

    private void createSyncedFoldersTable(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME + "("
                       + ProviderMeta.ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "                 // id
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH + " TEXT, "           // local path
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH + " TEXT, "          // remote path
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY + " INTEGER, "         // wifi_only
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY + " INTEGER, "     // charging only
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXISTING + " INTEGER, "          // existing
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED + " INTEGER, "           // enabled
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS + " INTEGER, " // enable date
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE + " INTEGER, " // subfolder by date
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT + "  TEXT, "             // account
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION + " INTEGER, "     // upload action
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY + " INTEGER, " // name collision policy
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_TYPE + " INTEGER, "              // type
                       + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_HIDDEN + " INTEGER );"           // hidden
                  );
    }

    private void createExternalLinksTable(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME + "("
                       + ProviderMeta.ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "          // id
                       + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_ICON_URL + " TEXT, "     // icon url
                       + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE + " TEXT, "     // language
                       + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TYPE + " INTEGER, "      // type
                       + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_NAME + " TEXT, "         // name
                       + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_URL + " TEXT, "          // url
                       + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT + " INTEGER );" // redirect
                  );
    }

    private void createArbitraryData(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME + "("
                       + ProviderMeta.ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "      // id
                       + ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID + " TEXT, " // cloud id (account name + FQDN)
                       + "'" + ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_KEY + "'" + " TEXT, "      // key
                       + ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_VALUE + " TEXT );"   // value
                  );
    }

    private void createVirtualTable(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE '" + ProviderMeta.ProviderTableMeta.VIRTUAL_TABLE_NAME + "' ("
                       + ProviderMeta.ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "          // id
                       + ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE + " TEXT, "                // type
                       + ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID + " INTEGER )"        // file id
                  );
    }

    private void createFileSystemTable(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME + "("
                       + ProviderMeta.ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "      // id
                       + ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH + " TEXT, "
                       + ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER + " INTEGER, "
                       + ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_FOUND_RECENTLY + " LONG, "
                       + ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD + " INTEGER, "
                       + ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID + " STRING, "
                       + ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32 + " STRING, "
                       + ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_MODIFIED + " LONG );"
                  );
    }

    private boolean checkIfColumnExists(SupportSQLiteDatabase database, String table, String column) {
        Cursor cursor = database.query("SELECT * FROM " + table + " LIMIT 0");
        boolean exists = cursor.getColumnIndex(column) != -1;
        cursor.close();

        return exists;
    }


    /**
     * Version 10 of database does not modify its scheme. It coincides with the upgrade of the ownCloud account names
     * structure to include in it the path to the server instance. Updating the account names and path to local files in
     * the files table is a must to keep the existing account working and the database clean.
     *
     * @param db Database where table of files is included.
     */
    private void updateAccountName(SupportSQLiteDatabase db) {
        Log_OC.d(TAG, "THREAD:  " + Thread.currentThread().getName());
        AccountManager ama = AccountManager.get(context);
        try {
            // get accounts from AccountManager ;  we can't be sure if accounts in it are updated or not although
            // we know the update was previously done in {link @FileActivity#onCreate} because the changes through
            // AccountManager are not synchronous
            Account[] accounts = AccountManager.get(context).getAccountsByType(MainApp.getAccountType(context));
            String serverUrl;
            String username;
            String oldAccountName;
            String newAccountName;
            String[] accountOwner = new String[1];

            for (Account account : accounts) {
                // build both old and new account name
                serverUrl = ama.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL);
                username = AccountUtils.getUsernameForAccount(account);
                oldAccountName = AccountUtils.buildAccountNameOld(Uri.parse(serverUrl), username);
                newAccountName = AccountUtils.buildAccountName(Uri.parse(serverUrl), username);

                // update values in database
                db.beginTransaction();
                try {
                    ContentValues cv = new ContentValues();
                    cv.put(ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER, newAccountName);
                    accountOwner[0] = oldAccountName;
                    int num = db.update(ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                                        SQLiteDatabase.CONFLICT_REPLACE,
                                        cv,
                                        ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                        accountOwner);

                    Log_OC.d(TAG, "Updated account in database: old name == " + oldAccountName +
                        ", new name == " + newAccountName + " (" + num + " rows updated )");

                    // update path for downloaded files
                    updateDownloadedFiles(db, newAccountName, oldAccountName);

                    db.setTransactionSuccessful();

                } catch (SQLException e) {
                    Log_OC.e(TAG, "SQL Exception upgrading account names or paths in database", e);
                } finally {
                    db.endTransaction();
                }
            }
        } catch (Exception e) {
            Log_OC.e(TAG, "Exception upgrading account names or paths in database", e);
        }
    }

    /**
     * Rename the local ownCloud folder of one account to match the a rename of the account itself. Updates the table of
     * files in database so that the paths to the local files keep being the same.
     *
     * @param db             Database where table of files is included.
     * @param newAccountName New name for the target OC account.
     * @param oldAccountName Old name of the target OC account.
     */
    private void updateDownloadedFiles(SupportSQLiteDatabase db, String newAccountName,
                                       String oldAccountName) {

        String whereClause = ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
            ProviderMeta.ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL";

        final SupportSQLiteQuery query = SupportSQLiteQueryBuilder.builder(ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME)
            .columns(PROJECTION_FILE_AND_STORAGE_PATH)
            .selection(whereClause, new String[]{newAccountName})
            .create();

        Cursor c = db.query(query);

        try {
            if (c.moveToFirst()) {
                // create storage path
                String oldAccountPath = FileStorageUtils.getSavePath(oldAccountName);
                String newAccountPath = FileStorageUtils.getSavePath(newAccountName);

                // move files
                File oldAccountFolder = new File(oldAccountPath);
                File newAccountFolder = new File(newAccountPath);
                oldAccountFolder.renameTo(newAccountFolder);

                String[] storagePath = new String[1];

                // update database
                do {
                    // Update database
                    String oldPath = c.getString(
                        c.getColumnIndexOrThrow(ProviderMeta.ProviderTableMeta.FILE_STORAGE_PATH));
                    OCFile file = new OCFile(
                        c.getString(c.getColumnIndexOrThrow(ProviderMeta.ProviderTableMeta.FILE_PATH)));
                    String newPath = FileStorageUtils.getDefaultSavePathFor(newAccountName, file);

                    ContentValues cv = new ContentValues();
                    cv.put(ProviderMeta.ProviderTableMeta.FILE_STORAGE_PATH, newPath);
                    storagePath[0] = oldPath;
                    db.update(ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                              SQLiteDatabase.CONFLICT_REPLACE,
                              cv,
                              ProviderMeta.ProviderTableMeta.FILE_STORAGE_PATH + "=?",
                              storagePath);

                    Log_OC.v(TAG, "Updated path of downloaded file: old file name == " + oldPath +
                        ", new file name == " + newPath);

                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }
    }

}
