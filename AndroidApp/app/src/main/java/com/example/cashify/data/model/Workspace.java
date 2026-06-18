package com.example.cashify.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.google.firebase.firestore.Exclude;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "workspaces")
@TypeConverters(Workspace.WorkspaceConverters.class)
public class Workspace {

    @PrimaryKey
    @NonNull
    @Exclude
    private String id = "";
    private String name;
    private String type;
    private String ownerId;
    private List<String> members;
    private double balance;
    private double totalIncome;
    private double totalExpense;
    private String iconName;

    public Workspace() {}

    public Workspace(String name, String type, String ownerId) {
        this.name = name;
        this.type = type;
        this.ownerId = ownerId;
        this.balance = 0.0;
        this.totalIncome = 0.0;
        this.totalExpense = 0.0;
    }

    @Exclude
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    // Xử lý chuyển đổi List<String> sang JSON String cho SQLite
    public static class WorkspaceConverters {
        @TypeConverter
        public static List<String> fromString(String value) {
            Type listType = new TypeToken<ArrayList<String>>() {}.getType();
            return value == null ? new ArrayList<>() : new Gson().fromJson(value, listType);
        }

        @TypeConverter
        public static String fromList(List<String> list) {
            return list == null ? "[]" : new Gson().toJson(list);
        }
    }
}