// Headless fixtures: no game process, network calls or injection.
#include "../module_menu.cpp"
#include <fstream>
#include <set>
#include <stdexcept>

static void save_fixture(const char *path) {
    ImDrawData *data = ImGui::GetDrawData();
    unsigned char *atlas;
    int aw, ah;
    ImGui::GetIO().Fonts->GetTexDataAsRGBA32(&atlas, &aw, &ah);
    const int w = kMenuWidth, h = kMenuHeight;
    std::vector<unsigned char> pixels(w * h * 3, 0);
    for (int l = 0; l < data->CmdListsCount; ++l) {
        const ImDrawList *list = data->CmdLists[l];
        for (const ImDrawCmd &cmd : list->CmdBuffer) {
            if (cmd.UserCallback) continue;
            for (unsigned i = 0; i + 2 < cmd.ElemCount; i += 3) {
                const ImDrawVert &a = list->VtxBuffer[cmd.VtxOffset + list->IdxBuffer[cmd.IdxOffset + i]];
                const ImDrawVert &b = list->VtxBuffer[cmd.VtxOffset + list->IdxBuffer[cmd.IdxOffset + i + 1]];
                const ImDrawVert &c = list->VtxBuffer[cmd.VtxOffset + list->IdxBuffer[cmd.IdxOffset + i + 2]];
                const float d = (b.pos.y-c.pos.y)*(a.pos.x-c.pos.x)+(c.pos.x-b.pos.x)*(a.pos.y-c.pos.y);
                if (std::abs(d) < 0.00001f) continue;
                const int x0 = std::max(0, static_cast<int>(std::max(cmd.ClipRect.x, std::min(a.pos.x, std::min(b.pos.x,c.pos.x)))));
                const int y0 = std::max(0, static_cast<int>(std::max(cmd.ClipRect.y, std::min(a.pos.y, std::min(b.pos.y,c.pos.y)))));
                const int x1 = std::min(w, static_cast<int>(std::min(cmd.ClipRect.z, std::ceil(std::max(a.pos.x, std::max(b.pos.x,c.pos.x))))));
                const int y1 = std::min(h, static_cast<int>(std::min(cmd.ClipRect.w, std::ceil(std::max(a.pos.y, std::max(b.pos.y,c.pos.y))))));
                for (int y=y0; y<y1; ++y) for (int x=x0; x<x1; ++x) {
                    const float u=((b.pos.y-c.pos.y)*(x+.5f-c.pos.x)+(c.pos.x-b.pos.x)*(y+.5f-c.pos.y))/d;
                    const float v=((c.pos.y-a.pos.y)*(x+.5f-c.pos.x)+(a.pos.x-c.pos.x)*(y+.5f-c.pos.y))/d;
                    const float t=1-u-v;
                    if (u < 0 || v < 0 || t < 0) continue;
                    int tx=std::max(0,std::min(aw-1,static_cast<int>((a.uv.x*u+b.uv.x*v+c.uv.x*t)*aw)));
                    int ty=std::max(0,std::min(ah-1,static_cast<int>((a.uv.y*u+b.uv.y*v+c.uv.y*t)*ah)));
                    const unsigned char *tex=atlas+4*(ty*aw+tx);
                    const float alpha=tex[3]/255.f*((a.col>>24)*u+(b.col>>24)*v+(c.col>>24)*t)/255.f;
                    for (int ch=0; ch<3; ++ch) {
                        const float color=tex[ch]/255.f*(((a.col>>(ch*8))&255)*u+((b.col>>(ch*8))&255)*v+((c.col>>(ch*8))&255)*t);
                        unsigned char &out=pixels[(y*w+x)*3+ch];
                        out=static_cast<unsigned char>(out*(1-alpha)+color*alpha);
                    }
                }
            }
        }
    }
    std::ofstream out(path, std::ios::binary);
    out << "P6\n" << w << " " << h << "\n255\n";
    out.write(reinterpret_cast<const char *>(pixels.data()), pixels.size());
}

int main() try {
    const ImVec2 head_front_uv = skin_uv(8.f, 8.f, 64.f, 64.f);
    if (std::abs(head_front_uv.x - .125f) > .0001f
            || std::abs(head_front_uv.y - .125f) > .0001f)
        throw std::runtime_error("Skin UVs must keep the source top row at V=0");

    g_visible = true;
    if (!sonyvegas_module_menu_is_visible())
        throw std::runtime_error("Visible menu state was not exposed to input guards");
    if (!is_game_mouse_message(WM_INPUT)
            || !is_game_mouse_message(WM_LBUTTONDOWN)
            || is_game_mouse_message(WM_PAINT))
        throw std::runtime_error("Game mouse message filtering is incomplete");
    const LPARAM right_shift_metadata = static_cast<LPARAM>(
            MapVirtualKeyA(VK_RSHIFT, MAPVK_VK_TO_VSC) << 16);
    const LPARAM left_shift_metadata = static_cast<LPARAM>(
            MapVirtualKeyA(VK_LSHIFT, MAPVK_VK_TO_VSC) << 16);
    if (message_bind_code(WM_KEYDOWN, VK_SHIFT, right_shift_metadata)
                    != VK_RSHIFT
            || message_bind_code(WM_KEYDOWN, VK_SHIFT, left_shift_metadata)
                    != VK_LSHIFT)
        throw std::runtime_error("Left/right Shift was not resolved from the scancode");
    g_visible = false;

    g_imgui_context = ImGui::CreateContext();
    initialize_style();
    ImGuiIO &io = ImGui::GetIO();
    io.DisplaySize = ImVec2(kMenuWidth, kMenuHeight);
    io.DeltaTime = 1.f / 60.f;
    io.Fonts->Build();

    std::vector<MenuModule> parsed_modules;
    int parsed_bind = VK_RSHIFT;
    long long parsed_version = -1;
    if (!parse_state("{\"modules\":[{\"name\":\"target-test\",\"values\":[{\"id\":\"target\",\"name\":\"Target\",\"type\":\"text\",\"value\":\"\",\"suggestions\":[\"Xdavi\",\"Steve\"]}]}],\"_v\":1}",
            parsed_modules, parsed_bind, parsed_version)
            || parsed_modules.size() != 1
            || parsed_modules.front().values.size() != 1
            || parsed_modules.front().values.front().suggestions.size() != 2)
        throw std::runtime_error("Player suggestions were not parsed");

    std::vector<MenuModule> piercing_modules;
    if (!parse_state("{\"modules\":[{\"name\":\"piercing\",\"values\":[{\"id\":\"manual\",\"name\":\"Clan Members\",\"type\":\"player-flags\",\"value\":\"10\",\"options\":[\"Closer\",\"Farther\"]}]}],\"_v\":2}",
            piercing_modules, parsed_bind, parsed_version)
            || piercing_modules.front().values.front().type != "player-flags"
            || piercing_modules.front().values.front().options.size() != 2
            || piercing_modules.front().values.front().value != "10")
        throw std::runtime_error("Piercing avatar choices were not parsed");

    const char *all_value_types = R"json({"modules":[{"name":"complete-module","values":[
        {"id":"bind","name":"Bind","type":"bind","value":"75"},
        {"id":"boolean","name":"Boolean","type":"boolean","value":"true"},
        {"id":"number","name":"Number","type":"number","value":"3.5","minimum":1,"maximum":6,"increment":0.1,"decimals":1},
        {"id":"mode","name":"Mode","type":"mode","value":"legit","options":["legit","blatant"],"optionLabels":{"legit":"Legit","blatant":"Blatant"}},
        {"id":"range","name":"Range","type":"range","value":"6,13","lower":6,"upper":13,"minimum":1,"maximum":20},
        {"id":"color","name":"Color","type":"color","value":"#FF0000,255,fixed,#FFFFFF","color":"#FF0000","fadeColor":"#FFFFFF"},
        {"id":"flags","name":"Targets","type":"flags","value":"1000000","options":["Players","Mobs","Peaceful","Neutral","Ignore naked","Ignore invisible","Ignore behind walls"]},
        {"id":"player-flags","name":"Candidates","type":"player-flags","value":"10","options":["Rektaay","Defow"]},
        {"id":"invsee","name":"Invsee Ranking","type":"invsee-ranking","value":"Rektaay","players":[{"name":"Rektaay","sets":4},{"name":"Defow","sets":4},{"name":"Kak4shiHakai","sets":6}]},
        {"id":"text","name":"Player","type":"text","value":"Rektaay","suggestions":["Rektaay","Defow"]},
        {"id":"list","name":"Items","type":"list","value":"swords\naxes","options":["swords","axes"]},
        {"id":"player-list","name":"Players","type":"player-list","value":"Rektaay\nDefow","suggestions":["Rektaay","Defow"]},
        {"id":"json","name":"Legacy","type":"json","value":"{}"}
    ]}],"_v":3})json";
    std::vector<MenuModule> complete_modules;
    if (!parse_state(all_value_types, complete_modules, parsed_bind, parsed_version)
            || complete_modules.size() != 1
            || complete_modules.front().values.size() != 13)
        throw std::runtime_error("The complete setting payload was not parsed");
    const std::set<std::string> expected_types = {"bind", "boolean", "number", "mode", "range", "color",
            "flags", "player-flags", "invsee-ranking", "text", "list", "player-list", "json"};
    std::set<std::string> parsed_types;
    for (const MenuValue &value : complete_modules.front().values) parsed_types.insert(value.type);
    if (parsed_types != expected_types)
        throw std::runtime_error("At least one supported setting type disappeared from the native GUI payload");
    const MenuValue &mode_fixture = complete_modules.front().values[3];
    if (mode_fixture.options.size() != 2
            || mode_fixture.option_labels.at("blatant") != "Blatant")
        throw std::runtime_error("Mode labels were not preserved by the native GUI parser");
    std::vector<MenuModule> notification_modules;
    if (!parse_state("{\"modules\":[{\"name\":\"client\",\"values\":[]}],\"config\":{\"notifications\":false}}",
            notification_modules, parsed_bind, parsed_version) || g_notifications.load())
        throw std::runtime_error("Notifications off did not reach the native settings toggle");
    if (!parse_state("{\"modules\":[{\"name\":\"client\",\"values\":[]}],\"config\":{}}",
            notification_modules, parsed_bind, parsed_version) || !g_notifications.load())
        throw std::runtime_error("Older snapshots changed the default notification visibility");
    static const char *fixture_aliases[kPanelRouteCount] = {
            "state01", "version", "module1", "value01", "bind001",
            "gui-bind", "reset01", "save001", "binds-in-game", "configs",
            "remote-config", "community", "party01", "notifications",
            "friends", "hud-fix"};
    std::vector<unsigned char> bootstrap;
    bootstrap.push_back('S');
    bootstrap.push_back('V');
    bootstrap.push_back('U');
    bootstrap.push_back('B');
    auto put16 = [&](unsigned short value) {
        bootstrap.push_back(static_cast<unsigned char>(value & 0xFF));
        bootstrap.push_back(static_cast<unsigned char>(value >> 8));
    };
    put16(2);   /* payload version */
    put16(4321);
    put16(161); /* GUI bind */
    bootstrap.push_back(9);  /* root: "/fixture/" */
    bootstrap.push_back(32); /* csrf */
    bootstrap.push_back(kPanelRouteCount);
    for (char c : std::string("/fixture/")) bootstrap.push_back((unsigned char)c);
    for (int c = 0; c < 32; ++c) bootstrap.push_back('c');
    for (const char *alias : fixture_aliases) {
        bootstrap.push_back(static_cast<unsigned char>(strlen(alias)));
        for (const char *cursor = alias; *cursor; ++cursor)
            bootstrap.push_back((unsigned char)*cursor);
    }
    accept_bootstrap(bootstrap.data(), bootstrap.size());
    if (!endpoint_route_equals(endpoint_snapshot(),
                ActionType::Notifications, "notifications"))
        throw std::runtime_error("Notification mutation route missing from the extended bootstrap");
    if (!endpoint_route_equals(endpoint_snapshot(),
                ActionType::Friend, "friends"))
        throw std::runtime_error("Settings friend actions still depend on a module route");
    const std::string friends_state = R"json({"modules":[],"friends":{
        "names":["SavedFriend","savedfriend","Steve","invalid-name"],
        "enabled":false,"middle":true,"suggestions":["SavedFriend","Steve"]}})json";
    std::vector<MenuModule> friends_modules;
    if (!parse_state(friends_state, friends_modules, parsed_bind, parsed_version)
            || !friends_modules.empty() || g_friends_list.friend_players.size() != 2
            || g_use_friends || !g_friends_middle_click
            || g_friends_list.suggestions.size() != 2)
        throw std::runtime_error("Settings did not load friends without the removed module");
    queue_friend_action("remove", "SavedFriend");
    MenuAction friend_action;
    if (!pop_action(friend_action) || friend_action.type != ActionType::Friend
            || friend_action.value != "remove" || friend_action.name != "SavedFriend"
            || !friend_action.module.empty() || g_friends_list.friend_players.size() != 1)
        throw std::runtime_error("Removing a friend still posts a module value");
    queue_friend_action("add", "Alex");
    if (!pop_action(friend_action) || friend_action.value != "add"
            || friend_action.name != "Alex" || g_friends_list.friend_players.size() != 2)
        throw std::runtime_error("Settings friend add is unavailable");
    queue_friend_action("middle", std::string(), false);
    if (!pop_action(friend_action) || friend_action.value != "middle"
            || friend_action.enabled || g_friends_middle_click)
        throw std::runtime_error("Settings lost the middle-click action");
    queue_friend_action("enabled", std::string(), true);
    if (!pop_action(friend_action) || friend_action.value != "enabled"
            || !friend_action.enabled || !g_use_friends)
        throw std::runtime_error("Settings lost the friend target filter");
    const MenuValue &invsee_fixture = complete_modules.front().values[8];
    if (invsee_fixture.invsee_players.size() != 3
            || invsee_fixture.invsee_players[0].name != "Rektaay"
            || invsee_fixture.invsee_players[2].sets != 6)
        throw std::runtime_error("Invsee ranking players were not preserved by the native GUI parser");

    const char *armor_counts = R"json({"modules":[{"name":"target","values":[
        {"id":"invsee","type":"invsee-ranking","value":"rarecrit","players":[
            {"name":"rarecrit","sets":8,"helmet":8,"chestplate":8,"leggings":8,"boots":8},
            {"name":"SpareParts","sets":0,"helmet":3,"chestplate":0,"leggings":2,"boots":1}
        ]}
    ]}]})json";
    std::vector<MenuModule> armor_modules;
    if (!parse_state(armor_counts, armor_modules, parsed_bind, parsed_version))
        throw std::runtime_error("Invsee armor payload was not parsed");
    const auto &armor_players = armor_modules.at(0).values.at(0).invsee_players;
    if (armor_players.size() != 2 || armor_players[1].name != "rarecrit"
            || armor_players[1].sets != 8 || armor_players[1].helmet != 8
            || armor_players[1].chestplate != 8 || armor_players[1].leggings != 8
            || armor_players[1].boots != 8 || armor_players[0].helmet != 3
            || armor_players[0].chestplate != 0 || armor_players[0].leggings != 2
            || armor_players[0].boots != 1)
        throw std::runtime_error("Invsee detail counts lost pieces or used total sets instead");

    std::string large_invsee = "{\"modules\":[{\"name\":\"target\",\"values\":[{\"id\":\"invsee\",\"type\":\"invsee-ranking\",\"players\":[";
    for (int index = 0; index < 100; ++index) {
        if (index) large_invsee += ",";
        large_invsee += "{\"name\":\"Player" + std::to_string(index) + "\",\"sets\":" + std::to_string(index) + "}";
    }
    large_invsee += "]}]}]}";
    std::vector<MenuModule> large_modules;
    if (!parse_state(large_invsee, large_modules, parsed_bind, parsed_version)
            || large_modules.at(0).values.at(0).invsee_players.size() != 100)
        throw std::runtime_error("The native Invsee ranking still truncates above 15 players");

    char completion_buffer[32] = "xda";
    TextAutocompleteContext completion_context;
    completion_context.suggestions =
            &parsed_modules.front().values.front().suggestions;
    ImGuiInputTextCallbackData completion_data{};
    completion_data.EventFlag = ImGuiInputTextFlags_CallbackCompletion;
    completion_data.Buf = completion_buffer;
    completion_data.BufTextLen = 3;
    completion_data.BufSize = IM_ARRAYSIZE(completion_buffer);
    completion_data.CursorPos = 3;
    completion_data.SelectionStart = completion_data.SelectionEnd = 3;
    completion_data.UserData = &completion_context;
    autocomplete_input_callback(&completion_data);
    if (!completion_context.completed
            || std::string(completion_buffer) != "Xdavi")
        throw std::runtime_error("Tab did not complete the target name");

    char player_list_buffer[64] = "Steve\nXda";
    TextAutocompleteContext player_list_context;
    player_list_context.suggestions = completion_context.suggestions;
    player_list_context.multiple = true;
    ImGuiInputTextCallbackData player_list_data{};
    player_list_data.EventFlag = ImGuiInputTextFlags_CallbackCompletion;
    player_list_data.Buf = player_list_buffer;
    player_list_data.BufTextLen = static_cast<int>(std::strlen(player_list_buffer));
    player_list_data.BufSize = IM_ARRAYSIZE(player_list_buffer);
    player_list_data.CursorPos = player_list_data.BufTextLen;
    player_list_data.SelectionStart = player_list_data.SelectionEnd =
            player_list_data.CursorPos;
    player_list_data.UserData = &player_list_context;
    autocomplete_input_callback(&player_list_data);
    if (!player_list_context.completed
            || std::string(player_list_buffer) != "Steve\nXdavi")
        throw std::runtime_error("Tab did not complete a manual player-list entry");

    HWND previous_imgui_window = g_imgui_window;
    g_imgui_window = reinterpret_cast<HWND>(static_cast<INT_PTR>(1));
    MSG translated{};
    translated.hwnd = g_imgui_window;
    translated.message = WM_KEYDOWN;
    if (should_translate_message(translated))
        throw std::runtime_error("Forwarded key would generate duplicate text");
    translated.message = WM_CHAR;
    if (should_translate_message(translated))
        throw std::runtime_error("Forwarded character would be translated again");
    g_imgui_window = previous_imgui_window;

    struct CategoryFixture { const char *label; int id; };
    const CategoryFixture categories[] = {
        {"Gladiador", 124}, {"C\xc2\xa7" "rombat", 122}, {"Network", 123},
        {"Misc", 126}, {"Render", 125}, {"Settings", 144}
    };
    std::set<unsigned int> icon_codes;
    for (const CategoryFixture &category : categories) {
        const char *icon = category_icon(category.id);
        unsigned int code = 0;
        ImTextCharFromUtf8(&code, icon, nullptr);
        if (!g_font_icons->FindGlyphNoFallback(static_cast<ImWchar>(code)))
            throw std::runtime_error(std::string("Missing category icon: ") + category.label);
        if (!icon_codes.insert(code).second)
            throw std::runtime_error("Category IDs resolved to duplicate icons");
        if (category.id == 144) continue;
        MenuModule module;
        module.id = category.label;
        module.label = category.label;
        module.category = category.label;
        module.category_id = category.id;
        g_modules.push_back(module);
    }
    g_connected = true;
    g_has_state = true;
    MenuModule &editorial = g_modules[1];
    editorial.id = "aim-assist";
    editorial.label = "AimAssist";
    editorial.description = "Editorial module fixture";
    editorial.enabled = false;
    editorial.binds = {0};
    MenuValue range;
    range.id = "range"; range.name = "Range"; range.type = "number";
    range.value = "4.50"; range.minimum = 1.0; range.maximum = 6.0;
    range.decimals = 2;
    MenuValue fov;
    fov.id = "fov"; fov.name = "FOV"; fov.type = "number";
    fov.value = "80"; fov.minimum = 10.0; fov.maximum = 180.0;
    MenuValue speed;
    speed.id = "speed"; speed.name = "Speed"; speed.type = "number";
    speed.value = "35"; speed.minimum = 1.0; speed.maximum = 100.0;
    MenuValue click;
    click.id = "click"; click.name = "Only Clicking";
    click.type = "boolean"; click.value = "true";
    MenuValue friends;
    friends.id = "friends"; friends.name = "Ignore Friends";
    friends.type = "boolean"; friends.value = "false";
    MenuValue mode;
    mode.id = "mode"; mode.name = "Focus Mode"; mode.type = "mode";
    mode.value = "legit"; mode.options = {"legit", "rage"};
    mode.option_labels = {{"legit", "Legit"}, {"rage", "Rage"}};
    editorial.values = {range, fov, speed, click, friends, mode};
    g_selected_category = editorial.category;
    g_selected_module = editorial.id;
    for (int frame = 0; frame < 16; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    save_fixture("module-editorial.ppm");
    io.AddMousePosEvent(600.f, 300.f);
    io.AddMouseButtonEvent(0, true);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddMouseButtonEvent(0, false);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    save_fixture("mode-wheel.ppm");
    io.AddMousePosEvent(700.f, 350.f);
    io.AddMouseWheelEvent(0.f, -1.f);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    for (int frame = 0; frame < 5; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    if (editorial.values.back().value != "rage")
        throw std::runtime_error("Mode wheel did not select the next option");
    save_fixture("mode-wheel-scrolled.ppm");

    begin_bind_capture(editorial.id);
    if (!handle_bind_capture_key('K') || editorial.binds.empty()
            || editorial.binds.front() != 'K')
        throw std::runtime_error("Keyboard module bind was not captured");
    begin_bind_capture(editorial.id);
    handle_bind_capture_key(VK_ESCAPE);
    if (editorial.binds.empty() || editorial.binds.front() != 'K')
        throw std::runtime_error("Escape changed the module bind");
    begin_bind_capture(editorial.id);
    handle_bind_capture_key(VK_BACK);
    if (!editorial.binds.empty())
        throw std::runtime_error("Backspace did not clear the module bind");

    MenuValue keyboard_bind;
    keyboard_bind.id = "keyboard-bind";
    keyboard_bind.name = "Keyboard bind";
    keyboard_bind.type = "bind";
    keyboard_bind.value = "0";
    editorial.values.push_back(keyboard_bind);
    begin_value_bind_capture(editorial.id, keyboard_bind.id);
    handle_bind_capture_key('L');
    if (editorial.values.back().value != std::to_string('L'))
        throw std::runtime_error("Keyboard setting bind was not captured");
    begin_value_bind_capture(editorial.id, keyboard_bind.id);
    handle_bind_capture_key(VK_ESCAPE);
    if (editorial.values.back().value != std::to_string('L'))
        throw std::runtime_error("Escape changed the setting bind");
    begin_value_bind_capture(editorial.id, keyboard_bind.id);
    handle_bind_capture_key(VK_BACK);
    if (editorial.values.back().value != "0")
        throw std::runtime_error("Backspace did not clear the setting bind");

    io.AddMousePosEvent(100.f, 500.f);
    io.AddMouseButtonEvent(0, true);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddMouseButtonEvent(0, false);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    for (int index = 0; index < 14; ++index) {
        MenuValue extra;
        extra.id = "extra-" + std::to_string(index);
        extra.name = "Extra setting " + std::to_string(index + 1);
        extra.type = "number"; extra.value = "50";
        extra.minimum = 0.0; extra.maximum = 100.0;
        editorial.values.push_back(extra);
    }
    MenuValue color;
    color.id = "color"; color.name = "Backtrack color";
    color.type = "color"; color.color = "#05866B";
    color.fade = true; color.fade_color = "#FFFFFF"; color.no_rainbow = true;
    editorial.values.push_back(color);
    for (int frame = 0; frame < 8; ++frame) {
        io.AddMousePosEvent(600.f, 400.f);
        io.AddMouseWheelEvent(0.f, -8.f);
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    save_fixture("module-scrolled.ppm");
    io.AddMousePosEvent(850.f, 352.f);
    io.AddMouseButtonEvent(0, true);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddMouseButtonEvent(0, false);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    save_fixture("color-picker.ppm");

    io.AddKeyEvent(ImGuiKey_Escape, true);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddKeyEvent(ImGuiKey_Escape, false);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddMousePosEvent(850.f, 427.f);
    io.AddMouseButtonEvent(0, true);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddMouseButtonEvent(0, false);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    save_fixture("color-effect-wheel.ppm");
    io.AddMousePosEvent(760.f, 470.f);
    for (int step = 0; step < 1; ++step) {
        io.AddMouseWheelEvent(0.f, 1.f);
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    for (int frame = 0; frame < 6; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    if (editorial.values.back().fade || editorial.values.back().rainbow)
        throw std::runtime_error("Color effect wheel did not switch Fade to Fixed");
    save_fixture("color-effect-fixed.ppm");
    io.AddKeyEvent(ImGuiKey_Escape, true);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddKeyEvent(ImGuiKey_Escape, false);
    MenuValue target_value = parsed_modules.front().values.front();
    editorial.values = {target_value};
    g_text_buffers.clear();
    g_selected_category = editorial.category;
    g_selected_module = editorial.id;
    for (int frame = 0; frame < 2; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    io.AddMousePosEvent(500.f, 171.f);
    io.AddMouseButtonEvent(0, true);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddMouseButtonEvent(0, false);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddInputCharactersUTF8("xda");
    for (int frame = 0; frame < 5; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    if (editorial.values.front().value != "xda")
        throw std::runtime_error("Target field did not receive typed text once");
    save_fixture("target-autocomplete.ppm");
    io.AddKeyEvent(ImGuiKey_Tab, true);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddKeyEvent(ImGuiKey_Tab, false);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    if (editorial.values.front().value != "Xdavi")
        throw std::runtime_error("Target field did not accept Tab completion");

    editorial.label = "Target";
    editorial.description = "Focuses and filters combat targets using priority logic.";
    editorial.enabled = true;
    MenuValue invsee_toggle;
    invsee_toggle.id = "invsee-toggle";
    invsee_toggle.name = "Invsee Synchronization";
    invsee_toggle.type = "boolean";
    invsee_toggle.value = "true";
    MenuValue invsee_ranking = invsee_fixture;
    for (int index = 3; index < 15; ++index) {
        MenuValue::InvseePlayer player;
        player.name = "Player" + std::to_string(index + 1);
        player.sets = index + 4;
        invsee_ranking.invsee_players.push_back(player);
    }
    editorial.values = {invsee_toggle, invsee_ranking};
    for (int frame = 0; frame < 4; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    save_fixture("invsee-ranking.ppm");
    io.AddMousePosEvent(350.f, 288.f);
    io.AddMouseButtonEvent(0, true);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    io.AddMouseButtonEvent(0, false);
    ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    if (editorial.values.back().value == "Rektaay")
        throw std::runtime_error("Clicking an Invsee ranking row did not select that player");
    editorial.values.back().invsee_players.clear();
    editorial.values.back().name = "Code copied - use /syncunknown in your ticket";
    for (int frame = 0; frame < 2; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    save_fixture("invsee-pending.ppm");

    g_selected_category = "Settings";
    {
        std::lock_guard<std::mutex> lock(g_cloud_mutex);
        g_cloud_slots = {
            CloudSlot{0, "hypixel pit", "Today, 20:14", true},
            CloudSlot{1, "mmc legit", "Yesterday", true},
            CloudSlot{2, "mmc semi", "3 days ago", true}
        };
    }
    g_cloud_loaded = true;
    g_settings_page = 1;
    if (!parse_state(friends_state, friends_modules, parsed_bind, parsed_version))
        throw std::runtime_error("Could not restore Settings friends fixture");
    for (int frame = 0; frame < 2; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    save_fixture("friends-settings.ppm");
    g_settings_page = 2;
    for (int frame = 0; frame < 2; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    save_fixture("configs.ppm");
    {
        std::lock_guard<std::mutex> lock(g_cloud_mutex);
        g_cloud_user = "Player One";
        g_community = {
            CommunityConfig{"one", "MMC Legit", "Player One", "Legit preset", 42, true},
            CommunityConfig{"two", "Hypixel Pit", "Player Two", "Pit preset", 18, false}
        };
    }
    g_settings_page = 3;
    for (int frame = 0; frame < 2; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    save_fixture("community.ppm");
    g_settings_page = 4;
    for (int frame = 0; frame < 2; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    save_fixture("party-empty.ppm");
    const std::string fixture("{\"code\":\"ABC123\",\"myRole\":\"leader\",\"members\":[{\"name\":\"Player One\",\"role\":\"leader\"},{\"name\":\"Player Two\",\"role\":\"member\"}]}");
    JsonParser parser(fixture);
    if (!parser.parse(g_party_state)) throw std::runtime_error("Invalid party fixture");
    for (int frame = 0; frame < 2; ++frame) {
        ImGui::NewFrame(); render_menu_frame(nullptr); ImGui::Render();
    }
    save_fixture("party-active.ppm");
    ImGui::DestroyContext();
    g_imgui_context = nullptr;
    return 0;
} catch (const std::exception &error) {
    std::fprintf(stderr, "Menu fixture failed: %s\n", error.what());
    return 1;
}
