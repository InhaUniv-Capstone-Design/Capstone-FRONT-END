package com.example.yakyeon2.auth;

import android.os.Bundle;

import com.example.yakyeon2.api.AuthContracts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Names, login ids, and phone numbers for account recovery.
 * Verification tokens and passwords are not stored here.
 */
public final class RecoveryForm {

    private static final String FIND_NAME = "recovery_find_name";
    private static final String FIND_PHONE = "recovery_find_phone";
    private static final String RESET_ID = "recovery_reset_id";
    private static final String RESET_PHONE = "recovery_reset_phone";
    private static final String ACCOUNT_IDS = "recovery_account_ids";
    private static final String ACCOUNT_TYPES = "recovery_account_types";

    private String findName = "";
    private String findPhone = "";
    private String resetLoginId = "";
    private String resetPhone = "";
    private List<AuthContracts.FoundAccount> accounts = Collections.emptyList();

    public void clear() {
        findName = "";
        findPhone = "";
        resetLoginId = "";
        resetPhone = "";
        accounts = Collections.emptyList();
    }

    public String findName() {
        return findName;
    }

    public void setFindName(String name) {
        findName = name == null ? "" : name;
    }

    public String findPhone() {
        return findPhone;
    }

    public void setFindPhone(String phone) {
        findPhone = phone == null ? "" : phone;
    }

    public String resetLoginId() {
        return resetLoginId;
    }

    public void setResetLoginId(String loginId) {
        resetLoginId = loginId == null ? "" : loginId;
    }

    public String resetPhone() {
        return resetPhone;
    }

    public void setResetPhone(String phone) {
        resetPhone = phone == null ? "" : phone;
    }

    public List<AuthContracts.FoundAccount> accounts() {
        return accounts;
    }

    public void setAccounts(List<AuthContracts.FoundAccount> found) {
        if (found == null || found.isEmpty()) {
            accounts = Collections.emptyList();
            return;
        }
        accounts = Collections.unmodifiableList(new ArrayList<>(found));
    }

    public void save(Bundle outState) {
        outState.putString(FIND_NAME, findName);
        outState.putString(FIND_PHONE, findPhone);
        outState.putString(RESET_ID, resetLoginId);
        outState.putString(RESET_PHONE, resetPhone);
        String[] ids = new String[accounts.size()];
        String[] types = new String[accounts.size()];
        for (int index = 0; index < accounts.size(); index++) {
            AuthContracts.FoundAccount account = accounts.get(index);
            ids[index] = account.loginId;
            types[index] = account.accountType;
        }
        outState.putStringArray(ACCOUNT_IDS, ids);
        outState.putStringArray(ACCOUNT_TYPES, types);
    }

    public void restore(Bundle savedState) {
        if (savedState == null) {
            return;
        }
        findName = text(savedState, FIND_NAME);
        findPhone = text(savedState, FIND_PHONE);
        resetLoginId = text(savedState, RESET_ID);
        resetPhone = text(savedState, RESET_PHONE);
        String[] ids = savedState.getStringArray(ACCOUNT_IDS);
        String[] types = savedState.getStringArray(ACCOUNT_TYPES);
        if (ids == null || types == null || ids.length != types.length) {
            accounts = Collections.emptyList();
            return;
        }
        List<AuthContracts.FoundAccount> found = new ArrayList<>();
        for (int index = 0; index < ids.length; index++) {
            if (ids[index] == null || ids[index].isEmpty() || types[index] == null || types[index].isEmpty()) {
                accounts = Collections.emptyList();
                return;
            }
            found.add(AuthContracts.rememberedAccount(ids[index], types[index]));
        }
        accounts = Collections.unmodifiableList(found);
    }

    private static String text(Bundle bundle, String key) {
        String value = bundle.getString(key, "");
        return value == null ? "" : value;
    }
}
