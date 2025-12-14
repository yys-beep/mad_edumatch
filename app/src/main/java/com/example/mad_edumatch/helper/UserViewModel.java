package com.example.mad_edumatch.helper;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

public class UserViewModel extends ViewModel {
    private final SavedStateHandle state;

    public UserViewModel(SavedStateHandle savedStateHandle) {
        this.state = savedStateHandle;
    }

    public void setUserName(String userName) {
        state.set("userName", userName);
    }

    public String getUserName() {
        return state.get("userName");
    }

    public void setUserRole(String userRole) {
        state.set("userRole", userRole);
    }

    public String getUserRole() {
        return state.get("userRole");
    }
}
