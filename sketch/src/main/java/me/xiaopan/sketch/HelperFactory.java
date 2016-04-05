/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xiaopan.sketch;

public class HelperFactory {
    private static final String NAME = "HelperFactory";

    private DisplayHelper obsoletingDisplayHelper;

    public DownloadHelper getDownloadHelper(Sketch sketch, String uri) {
        return new DownloadHelper(sketch, uri);
    }

    public LoadHelper getLoadHelper(Sketch sketch, String uri) {
        return new LoadHelper(sketch, uri);
    }

    public DisplayHelper getDisplayHelper(Sketch sketch, String uri, SketchImageViewInterface sketchImageViewInterface) {
        if (this.obsoletingDisplayHelper == null) {
            return new DisplayHelper(sketch, uri, sketchImageViewInterface);
        } else {
            DisplayHelper displayHelper = this.obsoletingDisplayHelper;
            this.obsoletingDisplayHelper = null;
            displayHelper.init(sketch, uri, sketchImageViewInterface);
            return displayHelper;
        }
    }

    public DisplayHelper getDisplayHelper(Sketch sketch, DisplayParams displayParams, SketchImageViewInterface sketchImageViewInterface) {
        if (this.obsoletingDisplayHelper == null) {
            return new DisplayHelper(sketch, displayParams, sketchImageViewInterface);
        } else {
            DisplayHelper displayHelper = this.obsoletingDisplayHelper;
            this.obsoletingDisplayHelper = null;
            displayHelper.init(sketch, displayParams, sketchImageViewInterface);
            return displayHelper;
        }
    }

    /**
     * 用完了要回收
     * @param obsoletingDisplayHelper DisplayHelper
     */
    public void recycleDisplayHelper(DisplayHelper obsoletingDisplayHelper) {
        obsoletingDisplayHelper.reset();
        if (this.obsoletingDisplayHelper == null) {
            this.obsoletingDisplayHelper = obsoletingDisplayHelper;
        }
    }

    /**
     * 获取标识符
     *
     * @return 标识符
     */
    public String getIdentifier() {
        return NAME;
    }

    /**
     * 追加标识符
     */
    public StringBuilder appendIdentifier(StringBuilder builder) {
        return builder.append(NAME);
    }
}