package com.irisvision.flexrcoremodule.ztest;

import com.irisvision.flexrcoremodule.observable.FlexProvider;

public class LoginProvider extends FlexProvider {
    public String getUsername = "";
    @Override
    protected String providerFlag() {
        return "login";
    }
}
