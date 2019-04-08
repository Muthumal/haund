package lk.janiru.greentrack.db;

/*
 *
 * Project Name : ${PROJECT}
 * Created by Janiru on 3/27/2019 1:43 PM.
 *
 */

import android.content.Context;

import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import lk.janiru.greentrack.db.dao.UserDao;
import lk.janiru.greentrack.db.entiity.User;

@Database(entities = {User.class},version =5 ,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "database")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
//                            .addCallback(new Callback() {
//                                @Override
//                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
//                                    super.onCreate(db);
//                                    Executors.newSingleThreadScheduledExecutor().execute(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            AppDatabase database = getDatabase(context);
//                                            //database.userDao().insert(new User(null,null,null));
//                                        }
//                                    });
//                                }
//                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
