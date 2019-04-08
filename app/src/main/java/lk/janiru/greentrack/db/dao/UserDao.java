package lk.janiru.greentrack.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import lk.janiru.greentrack.db.entiity.User;

import static android.icu.text.MessagePattern.ArgType.SELECT;

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
@Dao
public interface UserDao {

    @Insert
    void insert(User user);

    @Query("SELECT * FROM User WHERE userId=:id" )
    User getById(String id);

    @Query("SELECT * FROM User WHERE id=:id" )
    User getById(int id);

    @Update
    void update(User user);

}
