/*
 * Copyright (C) 2013 Koushik Dutta (@koush)
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

package com.evervolv.sudo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.evervolv.sudo.util.Constants;

import java.util.ArrayList;

public class DatabaseHelper {

    public static class Application extends SQLiteOpenHelper {
        private static final int CURRENT_VERSION = 1;

        public Application(Context context) {
            super(context, "superuser.sqlite", null, CURRENT_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            onUpgrade(db, 0, CURRENT_VERSION);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 0) {
                db.execSQL("create table if not exists log (id integer primary key autoincrement, desired_name text, username text, uid integer, desired_uid integer, command text not null, date integer, action text, package_name text, name text)");
                db.execSQL("create index if not exists log_uid_index on log(uid)");
                db.execSQL("create index if not exists log_desired_uid_index on log(desired_uid)");
                db.execSQL("create index if not exists log_command_index on log(command)");
                db.execSQL("create index if not exists log_date_index on log(date)");
                db.execSQL("create table if not exists settings (key text primary key not null, value text)");
                oldVersion = 1;
            }
        }

        public static ArrayList<LogEntry> getLogs(Context context, UidPolicy policy, int limit) {
            SQLiteDatabase db = new Application(context).getReadableDatabase();
            try {
                return getLogs(db, policy, limit);
            }
            finally {
                db.close();
            }
        }

        public static ArrayList<LogEntry> getLogs(SQLiteDatabase db, UidPolicy policy, int limit) {
            ArrayList<LogEntry> ret = new ArrayList<LogEntry>();
            Cursor c;
            if (!TextUtils.isEmpty(policy.command))
                c = db.query("log", null, "uid = ? and desired_uid = ? and command = ?", new String[] { String.valueOf(policy.uid), String.valueOf(policy.desiredUid), policy.command }, null, null, "date DESC", limit == -1 ? null : String.valueOf(limit));
            else
                c = db.query("log", null, "uid = ? and desired_uid = ?", new String[] { String.valueOf(policy.uid), String.valueOf(policy.desiredUid) }, null, null, "date DESC", limit == -1 ? null : String.valueOf(limit));
            try {
                while (c.moveToNext()) {
                    LogEntry l = new LogEntry();
                    ret.add(l);
                    l.getUidCommand(c);
                    l.id = c.getLong(c.getColumnIndex("id"));
                    l.date = c.getInt(c.getColumnIndex("date"));
                    l.action = c.getString(c.getColumnIndex("action"));
                }
            }
            catch (Exception ex) {
            }
            finally {
                c.close();
            }
            return ret;
        }

        public static ArrayList<LogEntry> getLogs(Context context) {
            SQLiteDatabase db = new Application(context).getReadableDatabase();
            try {
                return getLogs(context, db);
            }
            finally {
                db.close();
            }
        }

        public static ArrayList<LogEntry> getLogs(Context context, SQLiteDatabase db) {
            ArrayList<LogEntry> ret = new ArrayList<LogEntry>();
            Cursor c = db.query("log", null, null, null, null, null, "date DESC");
            try {
                while (c.moveToNext()) {
                    LogEntry l = new LogEntry();
                    ret.add(l);
                    l.getUidCommand(c);
                    l.id = c.getLong(c.getColumnIndex("id"));
                    l.date = c.getInt(c.getColumnIndex("date"));
                    l.action = c.getString(c.getColumnIndex("action"));
                }
            }
            catch (Exception ex) {
            }
            finally {
                c.close();
            }
            return ret;
        }

        public static void deleteLogs(Context context) {
            SQLiteDatabase db = new Application(context).getWritableDatabase();
            db.delete("log", null, null);
            db.close();
        }

        static void addLog(SQLiteDatabase db, LogEntry log) {
            ContentValues values = new ContentValues();
            values.put("uid", log.uid);
            // nulls are considered unique, even from other nulls. blerg.
            // http://stackoverflow.com/questions/3906811/null-permitted-in-primary-key-why-and-in-which-dbms
            if (log.command == null)
                log.command = "";
            values.put("command", log.command);
            values.put("action", log.action);
            values.put("date", log.date);
            values.put("name", log.name);
            values.put("desired_uid", log.desiredUid);
            values.put("package_name", log.packageName);
            values.put("desired_name", log.desiredName);
            values.put("username", log.username);
            db.insert("log", null, values);
        }

        public static UidPolicy addLog(Context context, LogEntry log) {
            // nulls are considered unique, even from other nulls. blerg.
            // http://stackoverflow.com/questions/3906811/null-permitted-in-primary-key-why-and-in-which-dbms
            if (log.command == null)
                log.command = "";

            // grab the policy and add a log
            UidPolicy u = null;
            SQLiteDatabase binary = new Binary(context).getReadableDatabase();
            Cursor c = binary.query("uid_policy", null, "uid = ? and (command = ? or command = ?) and desired_uid = ?", new String[] { String.valueOf(log.uid), log.command, "", String.valueOf(log.desiredUid) }, null, null, null, null);
            try {
                if (c.moveToNext()) {
                    u = Binary.getPolicy(c);
                }
            }
            finally {
                c.close();
                binary.close();
            }

            if (u != null && !u.logging)
                return u;

            if (!Constants.getLogging(context))
                return u;

            SQLiteDatabase app = new Application(context).getWritableDatabase();
            try {
                // delete logs over 2 weeks
                app.delete("log", "date < ?", new String[] { String.valueOf((System.currentTimeMillis() - 14L * 24L * 60L * 60L * 1000L) / 1000L) });
                addLog(app, log);
            }
            finally {
                app.close();
            }

            return u;
        }
    }

    public static class Binary extends SQLiteOpenHelper {
        private static final int CURRENT_VERSION = 6;
        Context mContext;
        public Binary(Context context) {
            super(context, "su.sqlite", null, CURRENT_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            onUpgrade(db, 0, CURRENT_VERSION);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 0) {
                db.execSQL("create table if not exists uid_policy (logging integer, desired_name text, username text, policy text, until integer, command text not null, uid integer, desired_uid integer, package_name text, name text, primary key(uid, command, desired_uid))");
                // skip past to v4, as the next migrations have legacy tables, which were moved
                oldVersion = 4;
            }

            if (oldVersion == 1 || oldVersion == 2) {
                db.execSQL("create table if not exists settings (key TEXT PRIMARY KEY, value TEXT)");
                oldVersion = 3;
            }

            // migrate the logs and settings outta this db. fix for db locking issues by su, which
            // only needs a readonly db.
            if (oldVersion == 3) {
                SQLiteDatabase app = new Application(mContext).getWritableDatabase();

                ArrayList<LogEntry> logs = Application.getLogs(mContext, db);
                app.beginTransaction();
                try {
                    for (LogEntry log: logs) {
                        Application.addLog(app, log);
                    }

                    Cursor c = db.query("settings", null, null, null, null, null, null);
                    while (c.moveToNext()) {
                        String key = c.getString(c.getColumnIndex("key"));
                        String value = c.getString(c.getColumnIndex("value"));
                        ContentValues cv = new ContentValues();
                        cv.put("key", key);
                        cv.put("value", value);

                        app.replace("settings", null, cv);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    app.setTransactionSuccessful();
                    app.endTransaction();
                    app.close();
                }

                db.execSQL("drop table if exists log");
                db.execSQL("drop table if exists settings");
                oldVersion = 4;
            }

            if (oldVersion == 4) {
                db.execSQL("alter table uid_policy add column notification integer");
                db.execSQL("update uid_policy set notification = 1");
                oldVersion = 5;
            }

            if (oldVersion == 5) {
                // fix bug where null commands are unique from other nulls. eww!
                ArrayList<UidPolicy> policies = getPolicies(db);
                db.delete("uid_policy", null, null);
                for (UidPolicy policy: policies) {
                    setPolicy(db, policy);
                }
                oldVersion = 6;
            }
        }

        public static void setPolicy(Context context, UidPolicy policy) {
            policy.getPackageInfo(context);
            SQLiteDatabase db = new Binary(context).getWritableDatabase();
            try {
                setPolicy(db, policy);
            }
            finally {
                db.close();
            }
        }

        public static void setPolicy(SQLiteDatabase db, UidPolicy policy) {
            ContentValues values = new ContentValues();
            values.put("logging", policy.logging);
            values.put("notification", policy.notification);
            values.put("uid", policy.uid);
            // nulls are considered unique, even from other nulls. blerg.
            // http://stackoverflow.com/questions/3906811/null-permitted-in-primary-key-why-and-in-which-dbms
            if (policy.command == null)
                policy.command = "";
            values.put("command", policy.command);
            values.put("policy", policy.policy);
            values.put("until", policy.until);
            values.put("name", policy.name);
            values.put("package_name", policy.packageName);
            values.put("desired_uid", policy.desiredUid);
            values.put("desired_name", policy.desiredName);
            values.put("username", policy.username);
            db.replace("uid_policy", null, values);
        }

        static UidPolicy getPolicy(Cursor c) {
            UidPolicy u = new UidPolicy();
            u.getUidCommand(c);
            u.policy = c.getString(c.getColumnIndex("policy"));
            u.until = c.getInt(c.getColumnIndex("until"));
            u.logging = c.getInt(c.getColumnIndex("logging")) != 0;
            u.notification = c.getInt(c.getColumnIndex("notification")) != 0;
            return u;
        }

        public static UidPolicy getPolicy(Context context, int index) {
            SQLiteDatabase db = new Binary(context).getWritableDatabase();
            try {
                return getPolicies(db).get(index);
            } catch (IndexOutOfBoundsException e) {
                Log.d("TOOLBOX", "CAUGHT!!!");
                return null;
            }
            finally {
                db.close();
            }
        }

        public static ArrayList<UidPolicy> getPolicies(SQLiteDatabase db) {
            ArrayList<UidPolicy> ret = new ArrayList<UidPolicy>();

            db.delete("uid_policy", "until > 0 and until < ?", new String[] { String.valueOf(System.currentTimeMillis()) });

            Cursor c = db.query("uid_policy", null, null, null, null, null, null);
            try {
                while (c.moveToNext()) {
                    UidPolicy u = getPolicy(c);
                    ret.add(u);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            finally {
                c.close();
            }
            return ret;
        }

        public static ArrayList<UidPolicy> getPolicies(Context context) {
            SQLiteDatabase db = new Binary(context).getWritableDatabase();
            try {
                return getPolicies(db);
            }
            finally {
                db.close();
            }
        }

        public static void delete(Context context, UidPolicy policy) {
            SQLiteDatabase db = new Binary(context).getWritableDatabase();
            if (!TextUtils.isEmpty(policy.command))
                db.delete("uid_policy", "uid = ? and command = ? and desired_uid = ?", new String[] { String.valueOf(policy.uid), policy.command, String.valueOf(policy.desiredUid) });
            else
                db.delete("uid_policy", "uid = ? and desired_uid = ?", new String[] { String.valueOf(policy.uid), String.valueOf(policy.desiredUid) });
            db.close();
        }

        public static UidPolicy get(Context context, int uid, int desiredUid, String command) {
            SQLiteDatabase db = new Binary(context).getReadableDatabase();
            Cursor c;
            if (!TextUtils.isEmpty(command))
                c = db.query("uid_policy", null, "uid = ? and command = ? and desired_uid = ?", new String[] { String.valueOf(uid), command, String.valueOf(desiredUid) }, null, null, null);
            else
                c = db.query("uid_policy", null, "uid = ? and desired_uid = ?", new String[] { String.valueOf(uid), String.valueOf(desiredUid) }, null, null, null);
            try {
                if (c.moveToNext()) {
                    return getPolicy(c);
                }
            }
            finally {
                c.close();
                db.close();
            }
            return null;
        }
    }
}
