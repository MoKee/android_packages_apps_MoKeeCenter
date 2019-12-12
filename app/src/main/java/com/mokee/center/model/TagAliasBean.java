/*
 * Copyright (C) 2019 The MoKee Open Source Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mokee.center.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Set;

public class TagAliasBean implements Serializable {
    @SerializedName("action")
    private int action;

    @SerializedName("tags")
    private Set<String> tags;

    @SerializedName("alias")
    private String alias;

    @SerializedName("isAliasAction")
    private boolean isAliasAction;

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isAliasAction() {
        return isAliasAction;
    }

    public void setAliasAction(boolean aliasAction) {
        isAliasAction = aliasAction;
    }
}
