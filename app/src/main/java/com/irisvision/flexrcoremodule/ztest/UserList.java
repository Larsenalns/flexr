package com.irisvision.flexrcoremodule.ztest;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapterRcv;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexItem;

public class UserList extends FlexAdapterRcv {

    public UserList(FlexContextInterface f) {
        super(f);
    }

    @Override
    protected FlexItem item(FlexContextInterface f) {
        //flexItem.initBuilder(f);
        return UserItem.instance(f);
    }
}
