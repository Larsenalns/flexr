// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.layoutParams;

public class GridParams extends ExtendedParams{
    public GridParams(){}

    public final FlexParams colSpec(int colIndex, int colSpan){
        if (colSpan <= 1) colSpan = 1;
        return column(colIndex, colSpan);
    }

    public final FlexParams rowSpec(int rowIndex, int rowSpan){
        if (rowSpan <= 1) rowSpan = 1;
        return row(rowIndex, rowSpan);
    }
}
