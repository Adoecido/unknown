package vegas.sony.friend;

import com.google.gson.JsonObject;
import vegas.sony.SonyVegas;
import vegas.sony.config.ConfigJsonUtils;
import vegas.sony.friend.FriendEntry;
import vegas.sony.utils.Base64Util;

public class Friend
extends FriendEntry {
    private String name;
    private String alias;

    @Override
    public String getName() {
        return this.name;
    }

    public Friend(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }


    @Override
    public Friend loadJson(JsonObject jsonObject) {
        this.name = ConfigJsonUtils.getDecodedBase64String(jsonObject, "name_2");
        this.alias = ConfigJsonUtils.getDecodedBase64String(jsonObject, "alias_2");
        this.setTargeted(ConfigJsonUtils.getBooleanOrFalse(jsonObject, "target"));
        this.loadLegacyFields(jsonObject);
        return this;
    }

    @Override
    public String getAlias() {
        return this.alias;
    }

    private void loadLegacyFields(JsonObject jsonObject) {
        if (jsonObject.get("name") != null && !jsonObject.get("name").isJsonNull()) {
            this.name = jsonObject.get("name").getAsString();
        }
        if (jsonObject.get("alias") != null && !jsonObject.get("alias").isJsonNull()) {
            this.alias = jsonObject.get("alias").getAsString();
        }
    }

    @Override
    public String getDisplayName() {
        if (SonyVegas.INSTANCE.getFriendManager().useAlias.getEffectiveValue().booleanValue()) {
            return this.getAlias();
        }
        return this.name;
    }

    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name_2", Base64Util.encodeUtf8Base64(this.name));
        jsonObject.addProperty("alias_2", Base64Util.encodeUtf8Base64(this.alias));
        jsonObject.addProperty("target", Boolean.valueOf(this.isTargeted()));
        return jsonObject;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}

