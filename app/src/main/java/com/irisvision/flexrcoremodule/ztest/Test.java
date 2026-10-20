package com.irisvision.flexrcoremodule.ztest;

import android.content.Intent;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.viewpager2.widget.ViewPager2;

import com.irisvision.flexrconcurrency.DispatchLike;
import com.irisvision.flexrconcurrency.FlexDispatcherGlobalScope;
import com.irisvision.flexrcoremodule.core.FlexActivity;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexInit;
import com.irisvision.flexrcoremodule.core.Init_;
import com.irisvision.flexrcoremodule.core.ScopeIdentity;
import com.irisvision.flexrcoremodule.observable.FlexConsumer;
import com.irisvision.flexrcoremodule.observable.FlexProvider;
import com.irisvision.flexrcoremodule.observable.FlexReactive;
import com.irisvision.flexrcoremodule.structure.FlexBaseContainer;
import com.irisvision.flexrcoremodule.ui.component.FlexDatePicker;
import com.irisvision.flexrcoremodule.ui.container.FlexBox;
import com.irisvision.flexrcoremodule.ui.container.FlexHBox;
import com.irisvision.flexrcoremodule.ui.layout.FlexInput;
import com.irisvision.flexrcoremodule.ui.list.flexlists.FlexList;
import com.irisvision.flexrcoremodule.valueconverter.Val;
import com.irisvision.flexrfxmodule.FlexChoreo;
import com.irisvision.flexrthememodule.Thematic;

import java.util.ArrayList;

public class Test extends FlexActivity implements Init_ {
    //public class Test extends FlexActivity implements FlexConsumer, Init_ {
    public Test(FlexContextInterface f) {
        FlexBaseContainer container = new FlexBaseContainer(f.getFlexContext());

        FlexInput editText = new FlexInput(f, false);
        editText.boxParams().Sizer(Val.Match, Val.Wrap).apply();

        FlexBox box = new FlexBox(f);
        box.boxParams();
        box.boxParams().margins("10, 20%, 20%, 70, Inverted");
        box.add(editText);

        new Thematic() {
            @Override
            public void identify() {

            }

            @Override
            public String ref(String scope) {
                return "";
            }

            @Override
            public void ifThemeChange() {

            }

            @Override
            public boolean isThemeAutoApply() {
                return true;
            }
        };

        FlexHBox flexHBox = new FlexHBox(f);
        GridLayout gridLayout = new GridLayout(f.getFlexContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        gridLayout.setOrientation(GridLayout.VERTICAL);

        FlexInput text_title = new FlexInput(f,true);
        text_title.autoApply(true);
        text_title.gridParams().colSpec(0,2);
        text_title.gridParams().rowSpec(0,2);


        GridView gridView = new GridView(f.getFlexContext());

        FrameLayout frameLayout = new FrameLayout(f.getFlexContext());

        MyCard myCard = new MyCard(f);
        myCard.initializer_();

        /*FlexProvider loginProvider = new LoginProvider();
        FlexReactive.get("login");
        FlexReactive.add(new LoginProvider());*/

        FlexInit.init(this);

        startActivity(new Intent(f.getFlexContext(), this.getClass()));

        new FlexActivity().startActivity(new Intent(f.getFlexContext(), FlexActivity.class));
        new FlexActivity().registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), re->{

        });

       FlexList user = new FlexList(f).line().adapter(new UserList(f));

        ViewPager2 viewPager2 = new ViewPager2(f.getFlexContext());

        FlexDatePicker.show(f, new FlexDatePicker.Action(FlexDatePicker.En){
            @Override
            public void dateChanged(DatePicker view, String dateString, int[] dateNumber) {
                System.out.println("Date to String : "+dateString);
                System.out.println("Date to Number : "+dateNumber[0]+"/"+dateNumber[1]+"/"+dateNumber[2]);
            }
        });

        FlexChoreo animCharter = new FlexChoreo(FlexChoreo.ExecMode.Condition, true, -1, f.getFlexFragment()){
            @Override
            public void callback(long frameTimeNanos) {
                System.out.println("g");
            }
        };
        animCharter
                .timer(2000)
                .launch();

        /*void tst(){
        FlexShape UseCase
        this.oldCompose()
                .likeRelative()
                .toCornerTop(0.50f, false)
                .edgeOffset(true,8)
                .toEdgeTop(FlexEdge.Triangle, 10, false);

        this.oldCompose()
                .relativeCorner(true)
                .toCornerLeft(0.40f, true)
                .edgeOffset(true,3)
                .toEdgeLeft(FlexEdge.Marker, 10, false);

        this.oldCompose()
                .relativeCorner(true)
                .toCornerRight(0.20f, true)
                .edgeOffset(true,3)
                .toEdgeRight(FlexEdge.Marker, 10, false);

        this.oldCompose()
                .likeRelative()
                .toAllCorners(60, false)
                .toAllEdges(FlexEdge.Marker, 15, false);

    }*/

    }

    /*@Override
    public void notifyDataChanged(FlexProvider flexProvider) {

    }*/

    // The Init_ interface can help some class to precess init for the these classes that don't need the all entire
    // FlexStructure, like FlexActivity, FlexAppBar, FlexBody ect ...
    @Override
    public void init() {
        /*FlexReactive.get("login").subscribeActivityScope(new FlexConsumer() {
            @Override
            public void notifyDataChanged(FlexProvider flexProvider) {

            }

            @Override
            public ScopeIdentity identity() {
                return null;
            }
        });*/

        /*FlexConsumer consumer = new FlexConsumer() {
            @Override
            public void notifyDataChanged(FlexProvider flexProvider) {

            }

            @Override
            public ScopeIdentity identity() {
                return null;
            }
        };

        FlexProvider profile = new LoginProvider();
        profile.subscribeFragScope(consumer);*/

        //FlexReactive.get("login").subscribeFragScope(this);

        FlexDispatcherGlobalScope scope = new FlexDispatcherGlobalScope(DispatchLike.IO);

        scope.launch(() -> new ArrayList<>());
    }
}
