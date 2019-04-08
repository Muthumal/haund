package lk.janiru.greentrack.db.repository;
/*
 * Developed by Lahiru Muthumal on 3/27/2019.
 * Last modified $file.lastModified.
 *
 * (C) Copyright 2019.year avalanche.lk (Pvt) Limited.
 * All Rights Reserved.
 *
 * These materials are unpublished, proprietary, confidential source code of
 * avalanche.lk (Pvt) Limited and constitute a TRADE SECRET
 * of avalanche.lk (Pvt) Limited.
 *
 * avalanche.lk (Pvt) Limited retains all title to and intellectual
 * property rights in these materials.
 *
 */

import android.content.Context;

import lk.janiru.greentrack.db.AppDatabase;
import lk.janiru.greentrack.db.dao.UserDao;
import lk.janiru.greentrack.db.entiity.User;

public class UserRepository {
    private final AppDatabase APP_DATABASE_INSTANCE;
    private final UserDao userDao;

    public UserRepository(Context context) {
        APP_DATABASE_INSTANCE = AppDatabase.getDatabase(context);
        userDao=APP_DATABASE_INSTANCE.userDao();
    }

    public void insert(User user){
        userDao.insert(user);
    }

    public void update(User user){
        user.setId(1);
        userDao.update(user);
    }

    public User getById(String Id){
        return userDao.getById(Id);
    }

    public User getUser(){
        return userDao.getById(1);
    }
}
