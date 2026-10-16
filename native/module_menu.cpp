#define IMGUI_DEFINE_MATH_OPERATORS
#ifndef NOMINMAX
#define NOMINMAX
#endif
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <bcrypt.h>
#include <gl/GL.h>
#include <wincodec.h>
#include <d3d11.h>
#include <dwmapi.h>

#include <algorithm>
#include <array>
#include <atomic>
#include <cctype>
#include <cmath>
#include <cstdlib>
#include <cfloat>
#include <cstring>
#include <cstdint>
#include <deque>
#include <iomanip>
#include <map>
#include <mutex>
#include <memory>
#include <set>
#include <sstream>
#include <string>
#include <thread>
#include <utility>
#include <vector>

#include "module_menu.h"
#include "obf.h"
#include "secure_transport.h"
#include "assets/crimson_logo_bytes.h"
#include "imgui/imgui.h"
#include "imgui/imgui_internal.h"
#include "imgui/imgui_impl_dx11.h"
#include "imgui/imgui_impl_opengl3.h"
#include "imgui/imgui_impl_win32.h"
#include "imgui/RawFonts.hpp"
#include "imgui/FontAwesome.hpp"

#ifndef GL_UNPACK_ALIGNMENT
#define GL_UNPACK_ALIGNMENT 0x0CF5
#endif

extern IMGUI_IMPL_API LRESULT ImGui_ImplWin32_WndProcHandler(
        HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam);

namespace {

// The client menu follows eyeofgod's editorial sheet: one compact surface,
// a category rail at the top, a module index in the left margin and the
// selected module laid out as an article. Keep these dimensions in sync with
// the coordinates below; position_overlay scales the complete sheet for the
// player's resolution.
constexpr int kMenuWidth = 900;
constexpr int kMenuHeight = 540;
constexpr int kMaxResponseBytes = 4 * 1024 * 1024;

static void menu_trace(const std::string &message) {
    (void)message;
}

const ImU32 kBackground = IM_COL32(11, 11, 11, 255);
const ImU32 kHeader = IM_COL32(17, 17, 17, 255);
const ImU32 kSidebar = kBackground;
const ImU32 kCard = IM_COL32(17, 17, 17, 255);
const ImU32 kCardBorder = IM_COL32(30, 30, 30, 255);
const ImU32 kAccent = IM_COL32(244, 244, 244, 255);
const ImU32 kAccentHover = IM_COL32(255, 255, 255, 255);
const ImU32 kText = IM_COL32(244, 244, 244, 255);
const ImU32 kMuted = IM_COL32(124, 124, 124, 255);
// The reference menu paints every filled accent surface as a vertical
// gradient (light on top, darker at the bottom) instead of a flat colour.
const ImU32 kAccentTop = kAccent;
const ImU32 kAccentBottom = IM_COL32(205, 205, 205, 255);
const ImU32 kCardBody = kBackground;
const ImU32 kCardHeader = IM_COL32(17, 17, 17, 255);
const ImU32 kCardEdge = IM_COL32(30, 30, 30, 255);
const ImU32 kCardOutline = IM_COL32(30, 30, 30, 255);
const ImU32 kTrack = IM_COL32(30, 30, 30, 255);
const ImU32 kGrab = IM_COL32(244, 244, 244, 255);
const ImU32 kGrabEdge = IM_COL32(124, 124, 124, 255);
const ImU32 kLabelOn = kText;
const ImU32 kLabelOff = kMuted;
const ImU32 kValueText = kMuted;
const float kCardHeaderHeight = 31.f;
const ImVec2 kCardBodyPadding = ImVec2(12.f, 10.f);

struct JsonValue {
    enum Kind { Null, Boolean, Number, String, Array, Object } kind = Null;
    bool boolean = false;
    double number = 0.0;
    std::string string;
    std::vector<JsonValue> array;
    std::map<std::string, JsonValue> object;

    const JsonValue *get(const char *key) const {
        if (kind != Object || key == nullptr) return nullptr;
        auto found = object.find(key);
        return found == object.end() ? nullptr : &found->second;
    }
};

static void append_utf8(std::string &out, unsigned codepoint) {
    if (codepoint <= 0x7f) {
        out.push_back(static_cast<char>(codepoint));
    } else if (codepoint <= 0x7ff) {
        out.push_back(static_cast<char>(0xc0 | (codepoint >> 6)));
        out.push_back(static_cast<char>(0x80 | (codepoint & 0x3f)));
    } else if (codepoint <= 0xffff) {
        out.push_back(static_cast<char>(0xe0 | (codepoint >> 12)));
        out.push_back(static_cast<char>(0x80 | ((codepoint >> 6) & 0x3f)));
        out.push_back(static_cast<char>(0x80 | (codepoint & 0x3f)));
    } else {
        out.push_back(static_cast<char>(0xf0 | (codepoint >> 18)));
        out.push_back(static_cast<char>(0x80 | ((codepoint >> 12) & 0x3f)));
        out.push_back(static_cast<char>(0x80 | ((codepoint >> 6) & 0x3f)));
        out.push_back(static_cast<char>(0x80 | (codepoint & 0x3f)));
    }
}

class JsonParser {
public:
    explicit JsonParser(const std::string &source)
            : begin_(source.c_str()), cursor_(source.c_str()),
              end_(source.c_str() + source.size()) {}

    bool parse(JsonValue &value) {
        skip_space();
        if (!parse_value(value)) return false;
        skip_space();
        return cursor_ == end_;
    }

private:
    const char *begin_;
    const char *cursor_;
    const char *end_;

    void skip_space() {
        while (cursor_ < end_ && std::isspace(
                static_cast<unsigned char>(*cursor_))) ++cursor_;
    }

    bool consume(char expected) {
        skip_space();
        if (cursor_ >= end_ || *cursor_ != expected) return false;
        ++cursor_;
        return true;
    }

    bool literal(const char *text) {
        size_t length = std::strlen(text);
        if (static_cast<size_t>(end_ - cursor_) < length
                || std::memcmp(cursor_, text, length) != 0) return false;
        cursor_ += length;
        return true;
    }

    static int hex_digit(char value) {
        if (value >= '0' && value <= '9') return value - '0';
        if (value >= 'a' && value <= 'f') return value - 'a' + 10;
        if (value >= 'A' && value <= 'F') return value - 'A' + 10;
        return -1;
    }

    bool unicode_escape(unsigned &value) {
        if (end_ - cursor_ < 4) return false;
        value = 0;
        for (int index = 0; index < 4; ++index) {
            int digit = hex_digit(*cursor_++);
            if (digit < 0) return false;
            value = (value << 4) | static_cast<unsigned>(digit);
        }
        return true;
    }

    bool parse_string(std::string &out) {
        skip_space();
        if (cursor_ >= end_ || *cursor_++ != '"') return false;
        out.clear();
        while (cursor_ < end_) {
            unsigned char current = static_cast<unsigned char>(*cursor_++);
            if (current == '"') return true;
            if (current < 0x20) return false;
            if (current != '\\') {
                out.push_back(static_cast<char>(current));
                continue;
            }
            if (cursor_ >= end_) return false;
            char escaped = *cursor_++;
            switch (escaped) {
                case '"': out.push_back('"'); break;
                case '\\': out.push_back('\\'); break;
                case '/': out.push_back('/'); break;
                case 'b': out.push_back('\b'); break;
                case 'f': out.push_back('\f'); break;
                case 'n': out.push_back('\n'); break;
                case 'r': out.push_back('\r'); break;
                case 't': out.push_back('\t'); break;
                case 'u': {
                    unsigned codepoint = 0;
                    if (!unicode_escape(codepoint)) return false;
                    if (codepoint >= 0xd800 && codepoint <= 0xdbff
                            && end_ - cursor_ >= 6
                            && cursor_[0] == '\\' && cursor_[1] == 'u') {
                        cursor_ += 2;
                        unsigned low = 0;
                        if (!unicode_escape(low) || low < 0xdc00
                                || low > 0xdfff) return false;
                        codepoint = 0x10000 + ((codepoint - 0xd800) << 10)
                                + (low - 0xdc00);
                    }
                    append_utf8(out, codepoint);
                    break;
                }
                default: return false;
            }
        }
        return false;
    }

    bool parse_array(JsonValue &value) {
        if (!consume('[')) return false;
        value.kind = JsonValue::Array;
        value.array.clear();
        skip_space();
        if (cursor_ < end_ && *cursor_ == ']') {
            ++cursor_;
            return true;
        }
        while (cursor_ < end_) {
            JsonValue entry;
            if (!parse_value(entry)) return false;
            value.array.push_back(std::move(entry));
            skip_space();
            if (cursor_ < end_ && *cursor_ == ']') {
                ++cursor_;
                return true;
            }
            if (!consume(',')) return false;
        }
        return false;
    }

    bool parse_object(JsonValue &value) {
        if (!consume('{')) return false;
        value.kind = JsonValue::Object;
        value.object.clear();
        skip_space();
        if (cursor_ < end_ && *cursor_ == '}') {
            ++cursor_;
            return true;
        }
        while (cursor_ < end_) {
            std::string key;
            if (!parse_string(key) || !consume(':')) return false;
            JsonValue entry;
            if (!parse_value(entry)) return false;
            value.object[key] = std::move(entry);
            skip_space();
            if (cursor_ < end_ && *cursor_ == '}') {
                ++cursor_;
                return true;
            }
            if (!consume(',')) return false;
        }
        return false;
    }

    bool parse_number(JsonValue &value) {
        const char *start = cursor_;
        if (cursor_ < end_ && *cursor_ == '-') ++cursor_;
        if (cursor_ >= end_) return false;
        if (*cursor_ == '0') {
            ++cursor_;
        } else {
            if (!std::isdigit(static_cast<unsigned char>(*cursor_))) return false;
            while (cursor_ < end_ && std::isdigit(
                    static_cast<unsigned char>(*cursor_))) ++cursor_;
        }
        if (cursor_ < end_ && *cursor_ == '.') {
            ++cursor_;
            if (cursor_ >= end_ || !std::isdigit(
                    static_cast<unsigned char>(*cursor_))) return false;
            while (cursor_ < end_ && std::isdigit(
                    static_cast<unsigned char>(*cursor_))) ++cursor_;
        }
        if (cursor_ < end_ && (*cursor_ == 'e' || *cursor_ == 'E')) {
            ++cursor_;
            if (cursor_ < end_ && (*cursor_ == '+' || *cursor_ == '-'))
                ++cursor_;
            if (cursor_ >= end_ || !std::isdigit(
                    static_cast<unsigned char>(*cursor_))) return false;
            while (cursor_ < end_ && std::isdigit(
                    static_cast<unsigned char>(*cursor_))) ++cursor_;
        }
        std::string raw(start, cursor_);
        char *tail = nullptr;
        value.number = std::strtod(raw.c_str(), &tail);
        if (tail == raw.c_str() || *tail != '\0') return false;
        value.kind = JsonValue::Number;
        return true;
    }

    bool parse_value(JsonValue &value) {
        skip_space();
        if (cursor_ >= end_) return false;
        if (*cursor_ == '{') return parse_object(value);
        if (*cursor_ == '[') return parse_array(value);
        if (*cursor_ == '"') {
            value.kind = JsonValue::String;
            return parse_string(value.string);
        }
        if (*cursor_ == 't' && literal(SV_STR(SV_9A91E94F6DD7E359, "true"))) {
            value.kind = JsonValue::Boolean;
            value.boolean = true;
            return true;
        }
        if (*cursor_ == 'f' && literal(SV_STR(SV_A5FEA6BD65D8D5BF, "false"))) {
            value.kind = JsonValue::Boolean;
            value.boolean = false;
            return true;
        }
        if (*cursor_ == 'n' && literal(SV_STR(SV_17081A972CF5FA37, "null"))) {
            value.kind = JsonValue::Null;
            return true;
        }
        return parse_number(value);
    }
};

static std::string json_string(const JsonValue *value,
        const std::string &fallback = std::string()) {
    return value != nullptr && value->kind == JsonValue::String
            ? value->string : fallback;
}

static bool json_bool(const JsonValue *value, bool fallback = false) {
    if (value == nullptr) return fallback;
    if (value->kind == JsonValue::Boolean) return value->boolean;
    if (value->kind == JsonValue::String) return value->string == SV_STR(SV_9A91E94F6DD7E359, "true");
    return fallback;
}

static double json_number(const JsonValue *value, double fallback = 0.0) {
    if (value == nullptr) return fallback;
    if (value->kind == JsonValue::Number) return value->number;
    if (value->kind == JsonValue::String) {
        char *tail = nullptr;
        double parsed = std::strtod(value->string.c_str(), &tail);
        if (tail != value->string.c_str() && *tail == '\0') return parsed;
    }
    return fallback;
}

struct MenuValue {
    std::string id;
    std::string name;
    std::string type;
    std::string value;
    std::string unit;
    bool active = true;
    bool no_rainbow = false;
    bool no_fade = false;
    bool no_alpha = false;
    int decimals = 0;
    int alpha = 255;
    double minimum = 0.0;
    double maximum = 1.0;
    double increment = 0.1;
    double lower = 0.0;
    double upper = 0.0;
    std::string color = SV_STR(SV_CACDD2E7C46B99BC, "#FFFFFF");
    std::string fade_color = SV_STR(SV_CACDD2E7C46B99BC, "#FFFFFF");
    bool rainbow = false;
    bool fade = false;
    std::vector<std::string> options;
    std::vector<std::string> suggestions;
    std::map<std::string, std::string> option_labels;
    struct InvseePlayer {
        std::string name;
        int sets = 0;
        int helmet = 0;
        int chestplate = 0;
        int leggings = 0;
        int boots = 0;
    };
    std::vector<InvseePlayer> invsee_players;
    std::vector<std::string> friend_players;
};

struct PlayerHeadTexture {
    // 0 = queued, 1 = loading, 2 = decoded, 3 = uploaded, -1 = failed.
    int state = 0;
    ULONGLONG retry_at = 0;
    UINT width = 0;
    UINT height = 0;
    std::vector<unsigned char> rgba;
    ID3D11ShaderResourceView *texture = nullptr;
    GLuint gl_texture = 0;
};

struct MenuModule {
    std::string id;
    std::string label;
    std::string description;
    std::string category;
    int category_id = 126;
    bool enabled = false;
    bool toggleable = true;
    bool bindable = true;
    std::vector<int> binds;
    std::vector<MenuValue> values;
};

// Panel route indices shared with the binary bootstrap payload. The order
// must match LocalControlServer.publishNativeMenuBootstrap.
enum PanelRoute {
    kPanelRouteState = 0,
    kPanelRouteVersion,
    kPanelRouteModule,
    kPanelRouteValue,
    kPanelRouteBind,
    kPanelRouteGuiBind,
    kPanelRouteReset,
    kPanelRouteSave,
    kPanelRouteBindsInGame,
    kPanelRouteConfigs,
    kPanelRouteRemoteConfig,
    kPanelRouteCommunity,
    kPanelRouteParty,
    kPanelRouteNotifications,
    kPanelRouteFriends,
    kPanelRouteHud,
    kPanelRouteCount
};

// Endpoint material is stored XOR-masked with a per-session key so no
// plain-text URL, host or token copy of the panel persists on the heap
// between requests. Values are unmasked only inside request scratch
// buffers that are wiped after every operation.
struct MenuEndpoint {
    unsigned short port = 0;
    unsigned char root_len = 0;
    unsigned char root_masked[80];
    unsigned char csrf_len = 0;
    unsigned char csrf_masked[48];
    unsigned char alias_len = 0;
    unsigned char alias_lengths[kPanelRouteCount] = {};
    unsigned char alias_masked[kPanelRouteCount][20];
    unsigned generation = 0;
};

enum class ActionType {
    Module, Value, Bind, GuiBind, BindsInGame, Notifications, Friend, Reset,
    ConfigSave, ConfigLoad, ConfigDelete,
    CommunityPublish, CommunityLoad, CommunityDelete,
    PartyCreate, PartyJoin, PartyLeave, PartyKick, PartySync,
    HudEdit, HudMove
};

struct MenuAction {
    ActionType type = ActionType::Module;
    std::string module;
    std::string value_id;
    std::string value;
    std::string name;
    std::string description;
    std::string config_id;
    bool enabled = false;
    bool commit = false;
    float hud_x = 0.f, hud_y = 0.f;
    int key = 0;
    int slot = 0;
    std::vector<std::string> shared_modules;
};

struct CloudSlot {
    int slot = 0;
    std::string name;
    std::string updated;
    bool used = false;
};

struct CommunityConfig {
    std::string id;
    std::string name;
    std::string owner;
    std::string description;
    int downloads = 0;
    bool mine = false;
};

struct TextBuffer {
    std::vector<char> bytes;
    std::string server_value;
    std::string pending_value;
};

struct ColorEditBuffer {
    MenuValue draft;
    bool initialized = false;
    bool editing = false;
    std::string pending;
};

std::mutex g_endpoint_mutex;
MenuEndpoint g_endpoint;
std::atomic<bool> g_endpoint_ready{false};
std::atomic<bool> g_stop{false};
std::atomic<bool> g_connected{false};
std::atomic<bool> g_has_state{false};
std::atomic<int> g_gui_bind{VK_RSHIFT};
std::atomic<bool> g_visible{false};
std::atomic<sonyvegas_module_menu_visibility_callback> g_visibility_callback{nullptr};
std::atomic<bool> g_binds_in_game{true};
std::atomic<bool> g_notifications{true};
bool g_stream_mode = false;
std::atomic<bool> g_worker_started{false};
std::atomic<bool> g_worker_running{false};
std::atomic<bool> g_head_worker_started{false};
std::atomic<bool> g_head_worker_running{false};
std::atomic<bool> g_ui_started{false};
std::atomic<bool> g_ui_running{false};

std::mutex g_data_mutex;
std::vector<MenuModule> g_modules;
std::mutex g_friends_mutex;
MenuValue g_friends_list;
bool g_use_friends = true;
bool g_friends_middle_click = false;
std::atomic<bool> g_state_success_logged{false};
std::string g_status = SV_STR(SV_11F7B42663D1224C, "Waiting for client");

std::mutex g_action_mutex;
std::deque<MenuAction> g_actions;

std::string g_selected_category;
std::string g_selected_module;
char g_search[64]{};
std::map<std::string, TextBuffer> g_text_buffers;
std::map<std::string, ColorEditBuffer> g_color_buffers;
unsigned g_color_generation = 0;

struct HudElement {
    std::string id;
    std::string label;
    float x = 0.f, y = 0.f, w = 0.f, h = 0.f;
    bool right = false, bottom = false;
};

struct HudPositionDraft {
    float x = 0.f, y = 0.f;
    bool dragging = false;
};

std::mutex g_hud_mutex;
std::vector<HudElement> g_hud_elements;
std::map<std::string, HudPositionDraft> g_hud_position_drafts;
std::string g_hud_drag_id;
ImVec2 g_hud_drag_offset{0.f, 0.f};
std::atomic<bool> g_hud_edit{false};
std::atomic<bool> g_overlay_hidden_by_focus{false};

static std::string value_edit_key(const std::string &module, const std::string &value) {
    return module + SV_STR(SV_8F3B14E40D98572C, ":") + value;
}

// The Java side stores colors through float HSB channels, so its re-emitted
// hex can land one channel step away from the picked value. Exact string
// matching on the acknowledgement then kept stale drafts forever and a later
// generic refresh snapped the visible color back. Compare semantically
// instead: RGB channels within one step are the same choice.
static bool color_wire_split(const std::string &text, std::vector<std::string> &parts) {
    parts.clear();
    size_t start = 0;
    for (;;) {
        size_t comma = text.find(',', start);
        if (comma == std::string::npos) {
            parts.push_back(text.substr(start));
            break;
        }
        parts.push_back(text.substr(start, comma - start));
        start = comma + 1;
    }
    return parts.size() == 3 || parts.size() == 4;
}

static bool color_hex_close(const std::string &a, const std::string &b) {
    if (a.size() != 7 || b.size() != 7 || a[0] != '#' || b[0] != '#') return false;
    char *tail = nullptr;
    long rgb_a = std::strtol(a.c_str() + 1, &tail, 16);
    if (tail != a.c_str() + 7) return false;
    long rgb_b = std::strtol(b.c_str() + 1, &tail, 16);
    if (tail != b.c_str() + 7) return false;
    for (int channel = 0; channel < 3; ++channel) {
        long da = (rgb_a >> ((2 - channel) * 8)) & 255;
        long db = (rgb_b >> ((2 - channel) * 8)) & 255;
        if (std::labs(da - db) > 2) return false;
    }
    return true;
}

static bool color_wire_matches(const std::string &server, const std::string &pending) {
    std::vector<std::string> a, b;
    if (!color_wire_split(server, a) || !color_wire_split(pending, b)
            || a.size() != b.size()) return false;
    if (!color_hex_close(a[0], b[0])) return false;
    if (a[1] != b[1] || a[2] != b[2]) return false;
    return a.size() == 3 || color_hex_close(a[3], b[3]);
}

// Called with g_data_mutex held. A refresh must not replace an open picker or
// a value still waiting for its exact acknowledgement with an older snapshot.
static void reconcile_color_edits(std::vector<MenuModule> &modules) {
    for (MenuModule &module : modules) {
        for (MenuValue &value : module.values) {
            if (value.type != SV_STR(SV_60C175CA12488454, "color")) continue;
            auto found = g_color_buffers.find(value_edit_key(module.id, value.id));
            if (found == g_color_buffers.end()) continue;
            ColorEditBuffer &edit = found->second;
            if (color_wire_matches(value.value, edit.pending)) {
                edit.pending.clear();
                edit.draft.value = value.value;
                edit.draft.color = value.color;
                edit.draft.alpha = value.alpha;
                edit.draft.fade = value.fade;
                edit.draft.rainbow = value.rainbow;
                edit.draft.fade_color = value.fade_color;
            }
            if (edit.editing || !edit.pending.empty()) value = edit.draft;
            else { edit.draft = value; edit.initialized = true; }
        }
    }
}

static ColorEditBuffer &color_edit_buffer(const MenuModule &module, const MenuValue &value) {
    ColorEditBuffer &edit = g_color_buffers[value_edit_key(module.id, value.id)];
    if (!edit.initialized || (!edit.editing && edit.pending.empty())) {
        edit.draft = value;
        edit.initialized = true;
    }
    return edit;
}
std::atomic<int> g_capture_kind{0};
std::mutex g_capture_mutex;
std::string g_capture_module;
std::string g_capture_value;
std::array<bool, 256> g_capture_key_down{};
std::atomic<int> g_suppressed_bind_key{0};
std::atomic<ULONGLONG> g_suppressed_bind_key_until{0};
std::atomic<ULONGLONG> g_hotkey_suppressed_until{0};
bool g_hotkey_was_down = false;
std::mutex g_lifecycle_mutex;
std::mutex g_imgui_mutex;
ImGuiContext *g_imgui_context = nullptr;
HGLRC g_imgui_gl_context = nullptr;
bool g_imgui_opengl_initialized = false;
bool g_embedded_mode = false;
ImFont *g_font_regular = nullptr;
ImFont *g_font_medium = nullptr;
ImFont *g_font_bold = nullptr;
ImFont *g_font_icons = nullptr;
ImFont *g_font_micro = nullptr;
ImFont *g_font_display = nullptr;
float g_menu_scale = 1.f;
std::mutex g_party_mutex;
JsonValue g_party_state;
std::string g_party_feedback;
std::atomic<bool> g_party_busy{false};
std::map<std::string, bool> g_party_selection;
HWND g_imgui_window = nullptr;
HWND g_game_input_window = nullptr;
WNDPROC g_game_original_proc = nullptr;
struct GameWindowRestoreState {
    HWND window = nullptr;
    LONG_PTR style = 0;
    LONG_PTR ex_style = 0;
    RECT bounds{};
    bool active = false;
};
GameWindowRestoreState g_game_window_restore;
ID3D11Device *g_device = nullptr;
ID3D11DeviceContext *g_device_context = nullptr;
IDXGISwapChain *g_swap_chain = nullptr;
ID3D11RenderTargetView *g_render_target = nullptr;
ID3D11ShaderResourceView *g_logo_texture = nullptr;
std::mutex g_player_head_mutex;
std::map<std::string, PlayerHeadTexture> g_player_heads;

static std::wstring utf8_to_wide(const std::string &value) {
    if (value.empty()) return std::wstring();
    int count = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS,
            value.c_str(), static_cast<int>(value.size()), nullptr, 0);
    if (count <= 0) return std::wstring(value.begin(), value.end());
    std::wstring result(static_cast<size_t>(count), L'\0');
    MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, value.c_str(),
            static_cast<int>(value.size()), &result[0], count);
    return result;
}

static bool http_request(const std::string &url, const wchar_t *method,
        const std::string &body, int &status, std::string &response) {
    // Legacy WinHTTP path retired: it copied the panel URL, user agent and
    // sync header into the game heap. All callers now use panel_request()
    // (loopback binary transport) or fetch_head_image() (public CDN).
    (void)url;
    (void)method;
    (void)body;
    status = 0;
    response.clear();
    return false;
}

// Per-session XOR key for MenuEndpoint storage. Generated once; the
// masked buffers are meaningless without it and it never leaves the DLL.
static unsigned char g_endpoint_mask[32];
static bool g_endpoint_mask_ready = false;

static void ensure_endpoint_mask() {
    unsigned char fresh[sizeof(g_endpoint_mask)];
    NTSTATUS generated = BCryptGenRandom(nullptr, fresh, sizeof(fresh),
            BCRYPT_USE_SYSTEM_PREFERRED_RNG);
    if (!BCRYPT_SUCCESS(generated)) {
        ULONGLONG tick = GetTickCount64();
        QueryPerformanceCounter(reinterpret_cast<LARGE_INTEGER *>(&tick));
        for (size_t index = 0; index < sizeof(fresh); ++index) {
            fresh[index] = static_cast<unsigned char>(
                    (tick >> ((index % 8) * 8)) ^ (index * 131 + 17));
        }
    }
    {
        static std::mutex mask_mutex;
        std::lock_guard<std::mutex> lock(mask_mutex);
        if (!g_endpoint_mask_ready) {
            memcpy(g_endpoint_mask, fresh, sizeof(g_endpoint_mask));
            g_endpoint_mask_ready = true;
        }
    }
    SecureZeroMemory(fresh, sizeof(fresh));
}

static void mask_store(unsigned char *destination, const unsigned char *source,
        size_t length) {
    for (size_t index = 0; index < length; ++index) {
        destination[index] = static_cast<unsigned char>(
                source[index]
                ^ g_endpoint_mask[index % sizeof(g_endpoint_mask)]);
    }
}

static void unmask_store(unsigned char *destination,
        const unsigned char *source, size_t length) {
    for (size_t index = 0; index < length; ++index) {
        destination[index] = static_cast<unsigned char>(
                source[index]
                ^ g_endpoint_mask[index % sizeof(g_endpoint_mask)]);
    }
}

// Request assembly scratch. Every byte written here is wiped when the
// request finishes; no address, URL or token copy outlives the call.
struct PanelScratch {
    unsigned char data[8192];
    size_t len = 0;
    bool overflow = false;
};

static void scratch_append(PanelScratch &scratch, const unsigned char *bytes,
        size_t length) {
    if (scratch.overflow || scratch.len + length > sizeof(scratch.data)) {
        scratch.overflow = true;
        return;
    }
    memcpy(scratch.data + scratch.len, bytes, length);
    scratch.len += length;
}

static void scratch_append_text(PanelScratch &scratch, const char *text) {
    scratch_append(scratch, reinterpret_cast<const unsigned char *>(text),
            strlen(text));
}

static void scratch_append_unmasked(PanelScratch &scratch,
        const unsigned char *masked, size_t length) {
    unsigned char chunk[64];
    while (length > 0) {
        size_t take = length < sizeof(chunk) ? length : sizeof(chunk);
        unmask_store(chunk, masked, take);
        scratch_append(scratch, chunk, take);
        masked += take;
        length -= take;
    }
    SecureZeroMemory(chunk, sizeof(chunk));
}

static void scratch_wipe(PanelScratch &scratch) {
    SecureZeroMemory(scratch.data, sizeof(scratch.data));
    scratch.len = 0;
    scratch.overflow = false;
}

// Appends a form field: name=encoded-value with proper separators. Field
// names arrive decoded from the obfuscated pool and are consumed here.
static void scratch_append_form(PanelScratch &scratch, const char *name,
        const std::string &value);

// One panel request over the loopback transport: no Host header, no user
// agent and no URL text ever exist in the process; the server accepts the
// headerless native call and authenticates it by the session-root token.
static bool panel_request(const MenuEndpoint &endpoint, bool post,
        const PanelScratch &target, const unsigned char *body,
        size_t body_len, int &status, std::string &response) {
    sv_request request;
    sv_response transport;
    const char *method = post ? SV_STR(SV_8C5FE0AFBC11BBC2, "POST")
            : SV_STR(SV_5317F41AA1903D00, "GET");
    if (target.overflow || endpoint.port == 0 || target.len == 0) {
        status = 0;
        return false;
    }
    SecureZeroMemory(&request, sizeof(request));
    SecureZeroMemory(&transport, sizeof(transport));
    request.host_ip = 0x7F000001u; /* loopback; htonl applied by transport */
    request.port = endpoint.port;
    request.method = reinterpret_cast<const unsigned char *>(method);
    request.method_len = strlen(method);
    request.target = target.data;
    request.target_len = target.len;
    if (post) {
        const char *content_type = SV_STR(SV_47791D5D8168B443, "application/x-www-form-urlencoded");
        request.body = body;
        request.body_len = body_len;
        request.content_type =
                reinterpret_cast<const unsigned char *>(content_type);
        request.content_type_len = strlen(content_type);
    }
    request.connect_timeout_ms = 1000;
    request.send_timeout_ms = 1000;
    request.recv_timeout_ms = 3500;
    request.max_response_bytes = static_cast<size_t>(kMaxResponseBytes);
    const bool ok = sv_request_perform(&request, &transport) != 0;
    if (ok) {
        status = static_cast<int>(transport.status);
        response.assign(reinterpret_cast<const char *>(transport.data),
                transport.size);
    } else {
        status = 0;
    }
    sv_response_discard(&transport);
    sonyvegas_obf_scrub();
    return ok;
}

// Downloads one CDN image. Public hosts resolve and validate through
// Schannel; host bytes exist only transiently inside the transport.
static bool fetch_head_image(const char *host, const std::string &path,
        std::string &body) {
    sv_request request;
    sv_response transport;
    int status = 0;
    const char *method = SV_STR(SV_5317F41AA1903D00, "GET");
    SecureZeroMemory(&request, sizeof(request));
    SecureZeroMemory(&transport, sizeof(transport));
    request.host = reinterpret_cast<const unsigned char *>(host);
    request.host_len = strlen(host);
    request.port = 443;
    request.use_tls = 1;
    request.pins = nullptr;
    request.pin_count = 0;
    request.method = reinterpret_cast<const unsigned char *>(method);
    request.method_len = strlen(method);
    request.target = reinterpret_cast<const unsigned char *>(path.data());
    request.target_len = path.size();
    request.connect_timeout_ms = 1000;
    request.send_timeout_ms = 1000;
    request.recv_timeout_ms = 3500;
    request.max_response_bytes = 4 * 1024 * 1024;
    const bool ok = sv_request_perform(&request, &transport) != 0;
    if (ok) {
        status = static_cast<int>(transport.status);
        if (status == 200) {
            body.assign(reinterpret_cast<const char *>(transport.data),
                    transport.size);
        }
    }
    sv_response_discard(&transport);
    sonyvegas_obf_scrub();
    return ok && status == 200;
}

static std::string player_head_key(const std::string &name) {
    std::string key;
    key.reserve(name.size());
    for (unsigned char current : name) {
        if ((current >= 'a' && current <= 'z')
                || (current >= 'A' && current <= 'Z')
                || (current >= '0' && current <= '9') || current == '_') {
            key.push_back(static_cast<char>(std::tolower(current)));
        }
    }
    return key;
}

static std::string player_texture_key(const std::string &name, char variant) {
    const std::string player = player_head_key(name);
    if (player.empty()) return std::string();
    return std::string(1, variant) + SV_STR(SV_8F3B14E40D98572C, ":") + player;
}

static void request_player_texture(const std::string &name, char variant) {
    const std::string key = player_texture_key(name, variant);
    if (key.empty()) return;
    std::lock_guard<std::mutex> lock(g_player_head_mutex);
    auto found = g_player_heads.find(key);
    if (found == g_player_heads.end()) {
        g_player_heads.emplace(key, PlayerHeadTexture{});
        return;
    }
    PlayerHeadTexture &texture = found->second;
    if (texture.state == -1 && GetTickCount64() >= texture.retry_at) {
        texture.state = 0;
        texture.retry_at = 0;
    }
}

static bool take_pending_player_head(std::string &key) {
    std::lock_guard<std::mutex> lock(g_player_head_mutex);
    for (auto &entry : g_player_heads) {
        if (entry.second.state != 0) continue;
        entry.second.state = 1;
        key = entry.first;
        return true;
    }
    return false;
}

static bool decode_player_head_png(const std::string &encoded,
        std::vector<unsigned char> &rgba, UINT &width, UINT &height) {
    if (encoded.empty() || encoded.size() > static_cast<size_t>(MAXDWORD))
        return false;
    IWICImagingFactory *factory = nullptr;
    IWICStream *stream = nullptr;
    IWICBitmapDecoder *decoder = nullptr;
    IWICBitmapFrameDecode *frame = nullptr;
    IWICFormatConverter *converter = nullptr;
    bool ok = false;
    if (SUCCEEDED(CoCreateInstance(CLSID_WICImagingFactory, nullptr,
            CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&factory)))
            && SUCCEEDED(factory->CreateStream(&stream))
            && SUCCEEDED(stream->InitializeFromMemory(
                    reinterpret_cast<BYTE *>(const_cast<char *>(encoded.data())),
                    static_cast<DWORD>(encoded.size())))
            && SUCCEEDED(factory->CreateDecoderFromStream(stream, nullptr,
                    WICDecodeMetadataCacheOnLoad, &decoder))
            && SUCCEEDED(decoder->GetFrame(0, &frame))
            && SUCCEEDED(factory->CreateFormatConverter(&converter))
            && SUCCEEDED(converter->Initialize(frame, GUID_WICPixelFormat32bppRGBA,
                    WICBitmapDitherTypeNone, nullptr, 0.f,
                    WICBitmapPaletteTypeCustom))
            && SUCCEEDED(converter->GetSize(&width, &height))
            && width > 0 && height > 0 && width <= 256 && height <= 256) {
        rgba.resize(static_cast<size_t>(width) * height * 4u);
        ok = SUCCEEDED(converter->CopyPixels(nullptr, width * 4u,
                static_cast<UINT>(rgba.size()), rgba.data()));
    }
    if (converter != nullptr) converter->Release();
    if (frame != nullptr) frame->Release();
    if (decoder != nullptr) decoder->Release();
    if (stream != nullptr) stream->Release();
    if (factory != nullptr) factory->Release();
    if (!ok) rgba.clear();
    return ok;
}

static void download_player_head(const std::string &key) {
    const size_t separator = key.find(':');
    const char variant = separator == std::string::npos ? 'a' : key[0];
    const std::string player = separator == std::string::npos
            ? key : key.substr(separator + 1);
    // Public CDN endpoints as host + path pairs; no URL is ever assembled.
    const char *mc_heads_host = SV_STR(SV_50927E9CB3971AE8, "mc-heads.net");
    const char *minotar_host = SV_STR(SV_B1D18DA3A1B48293, "minotar.net");
    struct HeadTarget {
        const char *host;
        std::string path;
    };
    HeadTarget targets[2] = {{nullptr, std::string()},
            {nullptr, std::string()}};
    if (variant == 's') {
        targets[0].path = SV_STR(SV_5DA2183EB86EB2EB, "/skin/") + player;
        targets[1].path = SV_STR(SV_5DA2183EB86EB2EB, "/skin/") + player
                + SV_STR(SV_35AB51EE5E96778F, ".png");
    } else if (variant == 'l' || variant == 'r') {
        const std::string side = variant == 'l'
                ? SV_STR(SV_B7EA5BC2C287AFCB, "left")
                : SV_STR(SV_A7F80C3429795EF9, "right");
        targets[0].path = SV_STR(SV_81A0E45502828A7C, "/body/") + player
                + SV_STR(SV_2442D216540D3695, "/128/") + side;
        targets[1].path = SV_STR(SV_81A0E45502828A7C, "/body/") + player
                + SV_STR(SV_CFEF7AC5F3A2CAF4, "/128.png");
    } else {
        targets[0].path = SV_STR(SV_2405EF400C316778, "/avatar/") + player
                + SV_STR(SV_50EE5AF6C123FEC2, "/32");
        targets[1].path = SV_STR(SV_EB2D60E605D034F9, "/helm/") + player
                + SV_STR(SV_E6178A07B2F5AA4E, "/32.png");
    }
    targets[0].host = mc_heads_host;
    targets[1].host = minotar_host;
    UINT width = 0;
    UINT height = 0;
    std::vector<unsigned char> rgba;
    bool ok = false;
    for (const HeadTarget &target : targets) {
        std::string body;
        if (fetch_head_image(target.host, target.path, body)
                && decode_player_head_png(body, rgba, width, height)) {
            ok = true;
            break;
        }
    }
    std::lock_guard<std::mutex> lock(g_player_head_mutex);
    auto found = g_player_heads.find(key);
    if (found == g_player_heads.end()) return;
    found->second.state = ok ? 2 : -1;
    found->second.retry_at = ok ? 0 : GetTickCount64() + 5000;
    found->second.width = width;
    found->second.height = height;
    found->second.rgba = std::move(rgba);
}

static ID3D11ShaderResourceView *player_d3d_texture(
        const std::string &name, char variant) {
    request_player_texture(name, variant);
    const std::string key = player_texture_key(name, variant);
    std::lock_guard<std::mutex> lock(g_player_head_mutex);
    auto found = g_player_heads.find(key);
    if (found == g_player_heads.end()) return nullptr;
    PlayerHeadTexture &head = found->second;
    if (head.state == 3) return head.texture;
    if (head.state != 2 || g_device == nullptr || head.rgba.empty())
        return nullptr;

    D3D11_TEXTURE2D_DESC description{};
    description.Width = head.width;
    description.Height = head.height;
    description.MipLevels = 1;
    description.ArraySize = 1;
    description.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    description.SampleDesc.Count = 1;
    description.Usage = D3D11_USAGE_IMMUTABLE;
    description.BindFlags = D3D11_BIND_SHADER_RESOURCE;
    D3D11_SUBRESOURCE_DATA pixels{};
    pixels.pSysMem = head.rgba.data();
    pixels.SysMemPitch = head.width * 4u;
    ID3D11Texture2D *texture = nullptr;
    if (FAILED(g_device->CreateTexture2D(&description, &pixels, &texture))
            || texture == nullptr) {
        head.state = -1;
        head.rgba.clear();
        return nullptr;
    }
    const HRESULT result = g_device->CreateShaderResourceView(texture,
            nullptr, &head.texture);
    texture->Release();
    if (FAILED(result) || head.texture == nullptr) {
        head.state = -1;
        head.rgba.clear();
        return nullptr;
    }
    head.state = 3;
    head.rgba.clear();
    return head.texture;
}

static GLuint player_gl_texture(const std::string &name, char variant) {
    request_player_texture(name, variant);
    const std::string key = player_texture_key(name, variant);
    std::lock_guard<std::mutex> lock(g_player_head_mutex);
    auto found = g_player_heads.find(key);
    if (found == g_player_heads.end()) return 0;
    PlayerHeadTexture &head = found->second;
    if (head.gl_texture != 0) return head.gl_texture;
    if (head.state != 2 || head.rgba.empty() || !g_imgui_opengl_initialized
            || g_imgui_gl_context == nullptr
            || wglGetCurrentContext() != g_imgui_gl_context) return 0;

    GLint previous_texture = 0;
    GLint previous_unpack_alignment = 4;
    glGetIntegerv(GL_TEXTURE_BINDING_2D, &previous_texture);
    glGetIntegerv(GL_UNPACK_ALIGNMENT, &previous_unpack_alignment);
    GLuint texture = 0;
    glGenTextures(1, &texture);
    if (texture == 0) return 0;
    glBindTexture(GL_TEXTURE_2D, texture);
    // Minecraft skins are pixel art. Nearest-neighbour sampling keeps every
    // texel crisp in the embedded OpenGL preview instead of blurring seams.
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
            static_cast<GLsizei>(head.width), static_cast<GLsizei>(head.height),
            0, GL_RGBA, GL_UNSIGNED_BYTE, head.rgba.data());
    glPixelStorei(GL_UNPACK_ALIGNMENT, previous_unpack_alignment);
    glBindTexture(GL_TEXTURE_2D, static_cast<GLuint>(previous_texture));
    head.gl_texture = texture;
    head.state = 3;
    head.rgba.clear();
    return texture;
}

static ImTextureID player_texture_id(const std::string &name, char variant) {
    if (g_embedded_mode && g_imgui_opengl_initialized) {
        const GLuint texture = player_gl_texture(name, variant);
        return texture == 0 ? nullptr
                : reinterpret_cast<ImTextureID>(static_cast<intptr_t>(texture));
    }
    return reinterpret_cast<ImTextureID>(player_d3d_texture(name, variant));
}

static ImTextureID player_head_texture(
        const std::string &name) {
    return player_texture_id(name, 'a');
}

static ImTextureID player_body_texture(
        const std::string &name, bool right) {
    return player_texture_id(name, right ? 'r' : 'l');
}

static ImTextureID player_skin_texture(const std::string &name) {
    return player_texture_id(name, 's');
}

static std::string form_encode(const std::string &value) {
    const char *hex = SV_STR(SV_8E2BBFC4612CD838, "0123456789ABCDEF");
    std::string encoded;
    encoded.reserve(value.size() * 3 / 2 + 8);
    for (unsigned char current : value) {
        if ((current >= 'a' && current <= 'z')
                || (current >= 'A' && current <= 'Z')
                || (current >= '0' && current <= '9')
                || current == '-' || current == '_' || current == '.') {
            encoded.push_back(static_cast<char>(current));
        } else if (current == ' ') {
            encoded.push_back('+');
        } else {
            encoded.push_back('%');
            encoded.push_back(hex[current >> 4]);
            encoded.push_back(hex[current & 15]);
        }
    }
    return encoded;
}

static void scratch_append_form(PanelScratch &scratch, const char *key,
        const std::string &value) {
    if (scratch.len > 0) scratch_append_text(scratch, SV_STR(SV_5AC3970236D6C68C, "&"));
    scratch_append_text(scratch, key);
    scratch_append_text(scratch, SV_STR(SV_CA1E55D2C512747A, "="));
    const std::string encoded = form_encode(value);
    scratch_append(scratch,
            reinterpret_cast<const unsigned char *>(encoded.data()),
            encoded.size());
}

static bool parse_state(const std::string &json,
        std::vector<MenuModule> &modules, int &gui_bind, long long &version) {
    JsonValue root;
    JsonParser parser(json);
    if (!parser.parse(root) || root.kind != JsonValue::Object) return false;
    {
        std::lock_guard<std::mutex> lock(g_party_mutex);
        const JsonValue *party = root.get(SV_STR(SV_B23EAB6E78E07B15, "remoteParty"));
        g_party_state = party == nullptr ? JsonValue() : *party;
    }
    const JsonValue *module_array = root.get(SV_STR(SV_E7A1D7A611D554DA, "modules"));
    if (module_array == nullptr || module_array->kind != JsonValue::Array)
        return false;
    // Settings owns the friend list; it does not require a public module.
    const JsonValue *friend_settings = root.get(SV_STR(SV_504A476A43493E94, "friends"));
    MenuValue friend_list;
    if (friend_settings != nullptr) {
        const JsonValue *names = friend_settings->get(SV_STR(SV_30AE22BED0D8BC05, "names"));
        if (names != nullptr && names->kind == JsonValue::Array) {
            std::set<std::string> seen;
            for (const JsonValue &name : names->array) {
                const std::string key = player_head_key(json_string(&name));
                if (!key.empty() && key.size() == name.string.size()
                        && name.string.size() <= 16u && friend_list.friend_players.size() < 128u
                        && seen.insert(key).second)
                    friend_list.friend_players.push_back(name.string);
            }
        }
        const JsonValue *suggestions = friend_settings->get(SV_STR(SV_22A692A3D7154D87, "suggestions"));
        if (suggestions != nullptr && suggestions->kind == JsonValue::Array) {
            for (const JsonValue &name : suggestions->array) {
                const std::string key = player_head_key(json_string(&name));
                if (!key.empty() && key.size() == name.string.size()
                        && name.string.size() <= 16u)
                    friend_list.suggestions.push_back(name.string);
            }
        }
    }
    {
        std::lock_guard<std::mutex> lock(g_friends_mutex);
        g_friends_list = std::move(friend_list);
        g_use_friends = json_bool(friend_settings == nullptr ? nullptr
                : friend_settings->get(SV_STR(SV_81D5224D5D878D8A, "enabled")), true);
        g_friends_middle_click = json_bool(friend_settings == nullptr ? nullptr
                : friend_settings->get(SV_STR(SV_BDAE29AF68A58302, "middle")));
    }
    {
        // HUD layout editor entries shipped alongside the module snapshot so
        // the position overlay can draw handles without a second request.
        std::vector<HudElement> hud_elements;
        const JsonValue *hud = root.get(SV_STR(SV_A1A3D85E41AB9A07, "hud"));
        const JsonValue *elements = hud == nullptr ? nullptr
                : hud->get(SV_STR(SV_4AAF22B5B8EAA1AA, "elements"));
        if (elements != nullptr && elements->kind == JsonValue::Array) {
            for (const JsonValue &entry : elements->array) {
                if (entry.kind != JsonValue::Object) continue;
                HudElement element;
                element.id = json_string(entry.get(SV_STR(SV_7EB277F4270C2A8B, "id")));
                element.label = json_string(entry.get(SV_STR(SV_49A53BCC985FD0FB, "label")), element.id);
                element.x = static_cast<float>(json_number(entry.get(SV_STR(SV_7F34FBA4A7FBC0D9, "x")), 0.0));
                element.y = static_cast<float>(json_number(entry.get(SV_STR(SV_9A0C83F0BF9B2DEE, "y")), 0.0));
                element.w = static_cast<float>(json_number(entry.get(SV_STR(SV_B75610F17AF4D2F7, "width")), 0.0));
                element.h = static_cast<float>(json_number(entry.get(SV_STR(SV_B9FF01C31D1569A1, "height")), 0.0));
                element.right = json_bool(entry.get(SV_STR(SV_A7F80C3429795EF9, "right")));
                element.bottom = json_bool(entry.get(SV_STR(SV_BD15E3775927C4A9, "bottom")));
                if (!element.id.empty() && element.w > 0.f && element.h > 0.f)
                    hud_elements.push_back(std::move(element));
            }
        }
        // The local g_hud_edit flag owns the editor lifecycle; the server-side
        // active flag only confirms it once the enable action has round-tripped.
        std::lock_guard<std::mutex> lock(g_hud_mutex);
        // A polled snapshot can precede the latest move. Keep the local position
        // until the final save request has completed.
        for (HudElement &element : hud_elements) {
            auto draft = g_hud_position_drafts.find(element.id);
            if (draft != g_hud_position_drafts.end()) {
                element.x = draft->second.x;
                element.y = draft->second.y;
            }
        }
        g_hud_elements = std::move(hud_elements);
    }
    std::vector<MenuModule> parsed;
    parsed.reserve(module_array->array.size());
    for (const JsonValue &module_json : module_array->array) {
        if (module_json.kind != JsonValue::Object) continue;
        MenuModule module;
        module.id = json_string(module_json.get(SV_STR(SV_26B56BB7D4E9A35E, "name")));
        module.label = json_string(module_json.get(SV_STR(SV_49A53BCC985FD0FB, "label")), module.id);
        // Older panel responses may contain the display label without the
        // randomized action id. Keep the module visible in that case instead
        // of silently dropping the entire entry.
        if (module.id.empty()) module.id = module.label;
        if (module.label.empty()) module.label = module.id;
        module.description = json_string(module_json.get(SV_STR(SV_214A893A3ACB8538, "description")));
        module.category = json_string(module_json.get(SV_STR(SV_1C4099A9C28B0DE9, "category")), SV_STR(SV_B9A1EF4DAB943C98, "Misc"));
        module.category_id = static_cast<int>(json_number(
                module_json.get(SV_STR(SV_914DD97679815456, "categoryId")), 126.0));
        if (module.category.empty()) {
            switch (module.category_id) {
                case 122: module.category = SV_STR(SV_E6CFF769F7579789, "Combat"); break;
                case 123: module.category = SV_STR(SV_ED680C50417B8A45, "Network"); break;
                case 124: module.category = SV_STR(SV_9815565A8F920BB5, "Gladiador"); break;
                case 125: module.category = SV_STR(SV_21FA538728C50786, "Render"); break;
                default: module.category = SV_STR(SV_B9A1EF4DAB943C98, "Misc"); break;
            }
        }
        module.enabled = json_bool(module_json.get(SV_STR(SV_81D5224D5D878D8A, "enabled")));
        module.toggleable = json_bool(module_json.get(
                SV_STR(SV_2EDDD08099B7245C, "toggleable")), true);
        module.bindable = json_bool(module_json.get(
                SV_STR(SV_4C6B6A12D9811612, "bindable")), true);
        const JsonValue *binds = module_json.get(SV_STR(SV_090B0A79694821B6, "bind"));
        if (binds != nullptr && binds->kind == JsonValue::Array) {
            for (const JsonValue &entry : binds->array) {
                if (entry.kind == JsonValue::Number)
                    module.binds.push_back(static_cast<int>(entry.number));
            }
        }
        const JsonValue *values = module_json.get(SV_STR(SV_09C42DD92AC4AC10, "values"));
        if (values != nullptr && values->kind == JsonValue::Array) {
            for (const JsonValue &value_json : values->array) {
                if (value_json.kind != JsonValue::Object) continue;
                MenuValue value;
                value.id = json_string(value_json.get(SV_STR(SV_7EB277F4270C2A8B, "id")));
                value.name = json_string(value_json.get(SV_STR(SV_26B56BB7D4E9A35E, "name")), SV_STR(SV_73706B0EAA7324A5, "Setting"));
                value.type = json_string(value_json.get(SV_STR(SV_55E18369AC7C26FA, "type")), SV_STR(SV_23AA0DEDF1CBB41A, "text"));
                value.value = json_string(value_json.get(SV_STR(SV_A65BAFDB94221BB3, "value")));
                value.unit = json_string(value_json.get(SV_STR(SV_BA381D14568A6DAA, "unit")));
                value.active = json_bool(value_json.get(SV_STR(SV_06517F7035F300DE, "active")), true);
                value.no_rainbow = json_bool(value_json.get(SV_STR(SV_0DA0EB52C5671C67, "noRainbow")));
                value.no_fade = json_bool(value_json.get(SV_STR(SV_B792F6DA9356887A, "noFade")));
                value.no_alpha = json_bool(value_json.get(SV_STR(SV_70499B760BC923E6, "noAlpha")));
                value.decimals = static_cast<int>(json_number(
                        value_json.get(SV_STR(SV_AE408A109D5BEB75, "decimals")), 0));
                value.alpha = static_cast<int>(json_number(
                        value_json.get(SV_STR(SV_076F0F4BB931E1D8, "alpha")), 255));
                value.minimum = json_number(value_json.get(SV_STR(SV_E0E868C0EF68F1C2, "minimum")), 0.0);
                value.maximum = json_number(value_json.get(SV_STR(SV_5DBC25A9020B80FF, "maximum")), 1.0);
                value.increment = json_number(value_json.get(SV_STR(SV_B10ECD646DBC2F9A, "increment")), 0.1);
                value.lower = json_number(value_json.get(SV_STR(SV_A49E90E70379325D, "lower")), value.minimum);
                value.upper = json_number(value_json.get(SV_STR(SV_3D9A862258FA5FAB, "upper")), value.maximum);
                value.color = json_string(value_json.get(SV_STR(SV_60C175CA12488454, "color")), SV_STR(SV_CACDD2E7C46B99BC, "#FFFFFF"));
                value.fade_color = json_string(value_json.get(SV_STR(SV_BEEDCC124B15BAEB, "fadeColor")), value.color);
                value.rainbow = json_bool(value_json.get(SV_STR(SV_D703FEE2323F2DF4, "rainbow")));
                value.fade = json_bool(value_json.get(SV_STR(SV_0C3A2F5C3FF788D1, "fade")));
                if (value.no_rainbow) value.rainbow = false;
                if (value.no_fade) value.fade = false;
                const JsonValue *options = value_json.get(SV_STR(SV_A8BA7947D886A9B9, "options"));
                if (options != nullptr && options->kind == JsonValue::Array) {
                    for (const JsonValue &entry : options->array) {
                        if (entry.kind == JsonValue::String)
                            value.options.push_back(entry.string);
                    }
                }
                const JsonValue *labels = value_json.get(SV_STR(SV_367F06E5996C9261, "optionLabels"));
                if (labels != nullptr && labels->kind == JsonValue::Object) {
                    for (const auto &entry : labels->object) {
                        if (entry.second.kind == JsonValue::String)
                            value.option_labels[entry.first] = entry.second.string;
                    }
                }
                const JsonValue *suggestions = value_json.get(
                        SV_STR(SV_22A692A3D7154D87, "suggestions"));
                if (suggestions != nullptr && suggestions->kind == JsonValue::Array) {
                    for (const JsonValue &entry : suggestions->array) {
                        if (entry.kind == JsonValue::String)
                            value.suggestions.push_back(entry.string);
                    }
                }
                const JsonValue *invsee_players = value_json.get(
                        SV_STR(SV_F5FEA188A9FDEBC4, "players"));
                if (invsee_players != nullptr
                        && invsee_players->kind == JsonValue::Array) {
                    for (const JsonValue &entry : invsee_players->array) {
                        if (entry.kind != JsonValue::Object) continue;
                        const std::string player_name = json_string(entry.get(
                                SV_STR(SV_26B56BB7D4E9A35E, "name")));
                        if (value.type == SV_STR(SV_84A35F8F4E928DF0, "invsee-ranking")) {
                            if (value.invsee_players.size() >= 100u) continue;
                            MenuValue::InvseePlayer player;
                            player.name = player_name;
                            player.sets = static_cast<int>(std::max(0.0,
                                    json_number(entry.get(
                                            SV_STR(SV_E6BF5132AEF2E878, "sets")), 0.0)));
                            player.helmet = static_cast<int>(std::max(0.0,
                                    json_number(entry.get(SV_STR(SV_FF48F1A610598B73, "helmet")), 0.0)));
                            player.chestplate = static_cast<int>(std::max(0.0,
                                    json_number(entry.get(SV_STR(SV_A2226D2F08908085, "chestplate")), 0.0)));
                            player.leggings = static_cast<int>(std::max(0.0,
                                    json_number(entry.get(SV_STR(SV_FFDB372C306C010A, "leggings")), 0.0)));
                            player.boots = static_cast<int>(std::max(0.0,
                                    json_number(entry.get(SV_STR(SV_C035DAA1667E7D56, "boots")), 0.0)));
                            if (!player_head_key(player.name).empty())
                                value.invsee_players.push_back(std::move(player));
                        } else if (value.type == SV_STR(SV_AB3346AD36BAB92A, "friends-list")
                                && value.friend_players.size() < 128u
                                && !player_head_key(player_name).empty()) {
                            bool duplicate = false;
                            for (const std::string &existing : value.friend_players) {
                                if (existing == player_name) {
                                    duplicate = true;
                                    break;
                                }
                            }
                            if (!duplicate) value.friend_players.push_back(player_name);
                        }
                    }
                    if (value.type == SV_STR(SV_84A35F8F4E928DF0, "invsee-ranking")) {
                        std::stable_sort(value.invsee_players.begin(),
                                value.invsee_players.end(),
                                [](const MenuValue::InvseePlayer &left,
                                   const MenuValue::InvseePlayer &right) {
                                    return left.sets < right.sets;
                                });
                    }
                }
                if (!value.id.empty()) module.values.push_back(std::move(value));
            }
        }
        if (!module.id.empty()) parsed.push_back(std::move(module));
    }
    const JsonValue *config = root.get(SV_STR(SV_A426B1410AA9A47D, "config"));
    if (config != nullptr && config->kind == JsonValue::Object) {
        g_binds_in_game = json_bool(config->get(SV_STR(SV_D72A4D987ECEBD50, "bindsOnlyInGame")), true);
        g_notifications = json_bool(config->get(SV_STR(SV_831215A429A1EA14, "notifications")), true);
        const JsonValue *binds = config->get(SV_STR(SV_5FE2FF591AF7CBEC, "guiBind"));
        if (binds != nullptr && binds->kind == JsonValue::Array
                && !binds->array.empty()
                && binds->array.front().kind == JsonValue::Number) {
            int candidate = static_cast<int>(binds->array.front().number);
            if (candidate != 0) gui_bind = candidate;
        }
    }
    version = static_cast<long long>(json_number(root.get(SV_STR(SV_2CA04562D6048078, "_v")), -1));
    modules.swap(parsed);
    return true;
}

std::mutex g_cloud_mutex;
std::vector<CloudSlot> g_cloud_slots;
std::vector<CommunityConfig> g_community;
std::string g_cloud_user;
std::string g_cloud_query;
std::atomic<bool> g_cloud_refresh{false};
std::atomic<bool> g_cloud_busy{false};
std::atomic<bool> g_cloud_failed{false};
std::atomic<bool> g_cloud_loaded{false};

static void request_cloud_refresh(const std::string &query) {
    {
        std::lock_guard<std::mutex> lock(g_cloud_mutex);
        g_cloud_query = query;
    }
    g_cloud_refresh = true;
}

static MenuEndpoint endpoint_snapshot() {
    std::lock_guard<std::mutex> lock(g_endpoint_mutex);
    return g_endpoint;
}

static void set_status_text(const char *text) {
    std::lock_guard<std::mutex> lock(g_data_mutex);
    g_status = text == nullptr ? SV_STR(SV_8C4D649A44E15777, "") : text;
}

static bool fetch_state(const MenuEndpoint &endpoint, long long &version) {
    int status = 0;
    std::string body;
    PanelScratch target;
    scratch_append_unmasked(target, endpoint.root_masked, endpoint.root_len);
    scratch_append_unmasked(target, endpoint.alias_masked[kPanelRouteState],
            endpoint.alias_len);
    const bool sent = panel_request(endpoint, false, target, nullptr, 0,
            status, body);
    scratch_wipe(target);
    if (!sent || status != 200) {
        menu_trace(std::string(SV_STR(SV_ADEC14E75BE01EF9, "HTTP fail"))
                + SV_STR(SV_F8DEE2D62771B53A, " status=") + std::to_string(status));
        return false;
    }
    std::vector<MenuModule> parsed;
    int bind = g_gui_bind.load();
    long long parsed_version = version;
    if (!parse_state(body, parsed, bind, parsed_version)) {
        menu_trace(SV_STR(SV_AC06DFD9B80D7D73, "PARSE fail"));
        return false;
    }
    if (!g_state_success_logged.exchange(true)) {
        menu_trace(std::string(SV_STR(SV_600BA89EE67E940C, "STATE ready modules="))
                + std::to_string(parsed.size())
                + SV_STR(SV_DAA2251B697EE9A2, " bytes=")
                + std::to_string(body.size()));
    }
    {
        std::lock_guard<std::mutex> lock(g_data_mutex);
        if (g_color_generation != endpoint.generation) {
            g_color_buffers.clear();
            g_color_generation = endpoint.generation;
        }
        reconcile_color_edits(parsed);
        g_modules.swap(parsed);
        g_status = SV_STR(SV_21B059333EB31F39, "Connected");
    }
    g_gui_bind = bind;
    version = parsed_version;
    g_connected = true;
    g_has_state = true;
    return true;
}

/** Pulls the cloud slots and the community listing in one round trip. */
static void fetch_cloud_configs(const MenuEndpoint &endpoint) {
    std::string query;
    {
        std::lock_guard<std::mutex> lock(g_cloud_mutex);
        query = g_cloud_query;
    }
    PanelScratch target;
    int status = 0;
    std::string body;
    scratch_append_unmasked(target, endpoint.root_masked, endpoint.root_len);
    scratch_append_unmasked(target, endpoint.alias_masked[kPanelRouteConfigs],
            endpoint.alias_len);
    if (!query.empty()) {
        scratch_append_text(target, SV_STR(SV_8ABECE139F6CE20D, "?q="));
        const std::string encoded = form_encode(query);
        scratch_append(target,
                reinterpret_cast<const unsigned char *>(encoded.data()),
                encoded.size());
    }
    g_cloud_busy = true;
    const bool ok = panel_request(endpoint, false, target, nullptr, 0,
            status, body) && status == 200;
    scratch_wipe(target);
    g_cloud_busy = false;
    menu_trace(std::string(SV_STR(SV_C9802C09484B988C, "CLOUD response status="))
            + std::to_string(status) + SV_STR(SV_DAA2251B697EE9A2, " bytes=")
            + std::to_string(body.size()));
    if (!ok) {
        g_cloud_failed = true;
        return;
    }
    JsonValue root;
    JsonParser parser(body);
    if (!parser.parse(root) || root.kind != JsonValue::Object) {
        menu_trace(SV_STR(SV_825354EF232AD19F, "CLOUD parse failed"));
        g_cloud_failed = true;
        return;
    }
    const bool slots_error = json_bool(root.get(SV_STR(SV_2C9316E366C9BF95, "slotsError")), false);
    const bool community_error = json_bool(root.get(SV_STR(SV_DCC754CCE80B53BF, "communityError")), false);
    const bool cloud_error = json_bool(root.get(SV_STR(SV_F58DB1363CB5B199, "cloudError")), false)
            || slots_error || community_error;
    if (cloud_error) {
        g_cloud_failed = true;
        menu_trace(std::string(SV_STR(SV_38F56D047199C822, "CLOUD unavailable slotsError="))
                + (slots_error ? SV_STR(SV_795E64E43EE175E1, "1") : SV_STR(SV_3DB37F28E0C46CEC, "0"))
                + SV_STR(SV_F858F61AC6A822C1, " communityError=") + (community_error ? SV_STR(SV_795E64E43EE175E1, "1") : SV_STR(SV_3DB37F28E0C46CEC, "0"))
                + SV_STR(SV_C1E4436A5A590D74, " code=") + json_string(root.get(SV_STR(SV_0C92385D948E43BE, "slotsErrorCode")), SV_STR(SV_8C4D649A44E15777, ""))
                + SV_STR(SV_24B3E8E52F730971, "/") + json_string(root.get(SV_STR(SV_10FB67DA7713D0E2, "communityErrorCode")), SV_STR(SV_8C4D649A44E15777, "")));
        return;
    }
    std::vector<CloudSlot> slots;
    const JsonValue *slot_array = root.get(SV_STR(SV_A587FCCC8005AF40, "slots"));
    if (slot_array != nullptr && slot_array->kind == JsonValue::Array) {
        for (const JsonValue &entry : slot_array->array) {
            CloudSlot slot;
            slot.slot = static_cast<int>(json_number(entry.get(SV_STR(SV_2BF1570DDCCDCF52, "slot")), 0.0));
            slot.name = json_string(entry.get(SV_STR(SV_26B56BB7D4E9A35E, "name")));
            slot.updated = json_string(entry.get(SV_STR(SV_45DEF01C13FEA6EF, "updatedAt")));
            slot.used = !slot.name.empty();
            slots.push_back(slot);
        }
    }
    std::vector<CommunityConfig> community;
    const JsonValue *community_array = root.get(SV_STR(SV_A10412E972C238B6, "community"));
    if (community_array != nullptr && community_array->kind == JsonValue::Array) {
        for (const JsonValue &entry : community_array->array) {
            CommunityConfig config;
            config.id = json_string(entry.get(SV_STR(SV_7EB277F4270C2A8B, "id")));
            config.name = json_string(entry.get(SV_STR(SV_26B56BB7D4E9A35E, "name")));
            config.owner = json_string(entry.get(SV_STR(SV_6D2293DE3E8AB176, "owner")));
            config.description = json_string(entry.get(SV_STR(SV_214A893A3ACB8538, "description")));
            config.downloads = static_cast<int>(
                    json_number(entry.get(SV_STR(SV_AFAA208845B82143, "downloads")), 0.0));
            config.mine = json_bool(entry.get(SV_STR(SV_7DE57CA05186D1E8, "mine")), false);
            if (!config.id.empty()) community.push_back(config);
        }
    }
    const std::string user = json_string(root.get(SV_STR(SV_0797C8FD1A0B831A, "user")));
    {
        std::lock_guard<std::mutex> lock(g_cloud_mutex);
        g_cloud_slots = std::move(slots);
        g_community = std::move(community);
        g_cloud_user = user;
    }
    g_cloud_failed = false;
    g_cloud_loaded = true;
    menu_trace(std::string(SV_STR(SV_A06B63F0C2C46D6F, "CLOUD loaded slots="))
            + std::to_string(g_cloud_slots.size())
            + SV_STR(SV_0E923A6E5D78D615, " community=")
            + std::to_string(g_community.size()));
}

static int route_index_for(ActionType type) {
    switch (type) {
        case ActionType::PartyCreate:
        case ActionType::PartyJoin:
        case ActionType::PartyLeave:
        case ActionType::PartyKick:
        case ActionType::PartySync: return kPanelRouteParty;
        case ActionType::Module: return kPanelRouteModule;
        case ActionType::Value: return kPanelRouteValue;
        case ActionType::Bind: return kPanelRouteBind;
        case ActionType::GuiBind: return kPanelRouteGuiBind;
        case ActionType::BindsInGame: return kPanelRouteBindsInGame;
        case ActionType::Notifications: return kPanelRouteNotifications;
        case ActionType::Friend: return kPanelRouteFriends;
        case ActionType::Reset: return kPanelRouteReset;
        case ActionType::ConfigSave:
        case ActionType::ConfigLoad:
        case ActionType::ConfigDelete: return kPanelRouteRemoteConfig;
        case ActionType::CommunityPublish:
        case ActionType::CommunityLoad:
        case ActionType::CommunityDelete: return kPanelRouteCommunity;
        case ActionType::HudEdit:
        case ActionType::HudMove: return kPanelRouteHud;
    }
    return -1;
}

// Unmasks one route alias into `plain` (no heap copy) and compares it with
// the expected text. Used by headless fixtures; nothing persists.
static bool endpoint_route_equals(const MenuEndpoint &endpoint,
        ActionType type, const char *expected) {
    unsigned char plain[20];
    const int route = route_index_for(type);
    const unsigned char length = route < 0
            ? 0 : endpoint.alias_lengths[route];
    if (length == 0 || length > sizeof(plain)) {
        return false;
    }
    unmask_store(plain, endpoint.alias_masked[route], length);
    const bool equals = strlen(expected) == length
            && memcmp(plain, expected, length) == 0;
    SecureZeroMemory(plain, sizeof(plain));
    return equals;
}

static bool post_action(const MenuEndpoint &endpoint,
        const MenuAction &action) {
    PanelScratch body;
    PanelScratch target;
    const int route = route_index_for(action.type);
    if (route < 0 || endpoint.alias_lengths[route] == 0
            || endpoint.alias_lengths[route] > 20) {
        return false;
    }
    scratch_append_unmasked(target, endpoint.root_masked,
            endpoint.root_len);
    scratch_append_unmasked(target, endpoint.alias_masked[route],
            endpoint.alias_lengths[route]);
    // The csrf token is charset-validated at accept time and needs no
    // form-encoding, so it is appended without a heap copy.
    {
        unsigned char plain[48];
        if (endpoint.csrf_len == 0 || endpoint.csrf_len > sizeof(plain)) {
            scratch_wipe(target);
            return false;
        }
        unmask_store(plain, endpoint.csrf_masked, endpoint.csrf_len);
        scratch_append_text(body, SV_STR(SV_FFCF5E3629C8CAAD, "csrf"));
        scratch_append_text(body, SV_STR(SV_CA1E55D2C512747A, "="));
        scratch_append(body, plain, endpoint.csrf_len);
        SecureZeroMemory(plain, sizeof(plain));
    }
    if (!action.module.empty()) scratch_append_form(body,SV_STR(SV_8CF0ADD8FF6274A4, "module"), action.module);
    switch (action.type) {
        case ActionType::Friend:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), action.value);
            if (!action.name.empty()) scratch_append_form(body,SV_STR(SV_26B56BB7D4E9A35E, "name"), action.name);
            scratch_append_form(body,SV_STR(SV_81D5224D5D878D8A, "enabled"), action.enabled ? SV_STR(SV_9A91E94F6DD7E359, "true") : SV_STR(SV_A5FEA6BD65D8D5BF, "false"));
            break;
        case ActionType::PartyCreate:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_038420E6B0FBE1CD, "create"));
            break;
        case ActionType::PartyJoin:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_D5CBB306AC91D410, "join"));
            scratch_append_form(body,SV_STR(SV_7F790B76397F2D40, "code"), action.name);
            break;
        case ActionType::PartyLeave:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_94231C0B8E34E3E3, "leave"));
            break;
        case ActionType::PartyKick:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_7D1CE60F5A226A19, "kick"));
            scratch_append_form(body,SV_STR(SV_FEAF9A1BC22358AF, "memberId"), action.name);
            break;
        case ActionType::PartySync:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_3C0DCCFE518D9A5B, "sync"));
            scratch_append_form(body,SV_STR(SV_2351581F3BB712C8, "mode"), action.enabled ? SV_STR(SV_94A1AD8E9F787D80, "activate") : SV_STR(SV_A426B1410AA9A47D, "config"));
            for (const std::string &id : action.shared_modules)
                scratch_append_form(body,(SV_STR(SV_C502DC31A95DDAB6, "share:") + id).c_str(), SV_STR(SV_8F51A280B9B008AB, "on"));
            break;
        case ActionType::Module:
        case ActionType::BindsInGame:
        case ActionType::Notifications:
            scratch_append_form(body,SV_STR(SV_81D5224D5D878D8A, "enabled"), action.enabled ? SV_STR(SV_9A91E94F6DD7E359, "true") : SV_STR(SV_A5FEA6BD65D8D5BF, "false"));
            break;
        case ActionType::Value:
            scratch_append_form(body,SV_STR(SV_0A3CB571BEB8891B, "valueId"), action.value_id);
            scratch_append_form(body,SV_STR(SV_A65BAFDB94221BB3, "value"), action.value);
            break;
        case ActionType::Bind:
        case ActionType::GuiBind:
            scratch_append_form(body,SV_STR(SV_F04D0789B642D4F2, "key"), std::to_string(action.key));
            break;
        case ActionType::Reset:
            break;
        case ActionType::ConfigSave:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_A53F4239941F5672, "save"));
            scratch_append_form(body,SV_STR(SV_2BF1570DDCCDCF52, "slot"), std::to_string(action.slot));
            scratch_append_form(body,SV_STR(SV_26B56BB7D4E9A35E, "name"), action.name);
            break;
        case ActionType::ConfigLoad:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_0A8B56DF694CD2BB, "load"));
            scratch_append_form(body,SV_STR(SV_2BF1570DDCCDCF52, "slot"), std::to_string(action.slot));
            break;
        case ActionType::ConfigDelete:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_0B5DE11A2BDD991A, "delete"));
            scratch_append_form(body,SV_STR(SV_2BF1570DDCCDCF52, "slot"), std::to_string(action.slot));
            break;
        case ActionType::CommunityPublish:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_44E0C591D7FD9DB2, "publish"));
            scratch_append_form(body,SV_STR(SV_26B56BB7D4E9A35E, "name"), action.name);
            scratch_append_form(body,SV_STR(SV_214A893A3ACB8538, "description"), action.description);
            break;
        case ActionType::CommunityLoad:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_0A8B56DF694CD2BB, "load"));
            scratch_append_form(body,SV_STR(SV_2C80FBAEED9C8A94, "configId"), action.config_id);
            break;
        case ActionType::CommunityDelete:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_0B5DE11A2BDD991A, "delete"));
            scratch_append_form(body,SV_STR(SV_2C80FBAEED9C8A94, "configId"), action.config_id);
            break;
        case ActionType::HudEdit:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"),
                    action.enabled ? SV_STR(SV_3DC4E11F08867AF6, "enable")
                            : SV_STR(SV_A56F7E28807C4C7C, "disable"));
            break;
        case ActionType::HudMove:
            scratch_append_form(body,SV_STR(SV_9EF4D8DA75F7776C, "action"), SV_STR(SV_31243804C191E447, "move"));
            scratch_append_form(body,SV_STR(SV_7EB277F4270C2A8B, "id"), action.value_id);
            scratch_append_form(body,SV_STR(SV_7F34FBA4A7FBC0D9, "x"), std::to_string(action.hud_x));
            scratch_append_form(body,SV_STR(SV_9A0C83F0BF9B2DEE, "y"), std::to_string(action.hud_y));
            scratch_append_form(body,SV_STR(SV_A26C76380D2DE48D, "commit"), action.commit ? SV_STR(SV_9A91E94F6DD7E359, "true") : SV_STR(SV_A5FEA6BD65D8D5BF, "false"));
            break;
    }
    int status = 0;
    std::string response;
    const bool sent = panel_request(endpoint, true, target,
            body.overflow ? nullptr : body.data, body.len, status, response);
    scratch_wipe(body);
    scratch_wipe(target);
    return sent && status == 200;
}

static bool pop_action(MenuAction &action) {
    std::lock_guard<std::mutex> lock(g_action_mutex);
    if (g_actions.empty()) return false;
    action = std::move(g_actions.front());
    g_actions.pop_front();
    return true;
}

static void queue_action(MenuAction action) {
    std::lock_guard<std::mutex> lock(g_action_mutex);
    if (action.type == ActionType::Value) {
        // Replace unsent changes to the same setting, never replay old colours.
        g_actions.erase(std::remove_if(g_actions.begin(), g_actions.end(),
                [&action](const MenuAction &queued) {
                    return queued.type == ActionType::Value
                            && queued.module == action.module
                            && queued.value_id == action.value_id;
                }), g_actions.end());
    }
    if (action.type == ActionType::HudMove) {
        // Keep the newest preview, but preserve the save at the end of each drag.
        g_actions.erase(std::remove_if(g_actions.begin(), g_actions.end(),
                [&action](const MenuAction &queued) {
                    return queued.type == ActionType::HudMove
                            && queued.value_id == action.value_id && !queued.commit;
                }), g_actions.end());
    }
    g_actions.push_back(std::move(action));
}

static void worker_loop() {
    g_worker_running = true;
    unsigned generation = 0;
    long long version = -1;
    ULONGLONG next_version_check = 0;
    ULONGLONG next_full_refresh = 0;
    while (!g_stop.load()) {
        if (!g_endpoint_ready.load()) {
            Sleep(80);
            continue;
        }
        MenuEndpoint endpoint = endpoint_snapshot();
        if (endpoint.generation != generation) {
            generation = endpoint.generation;
            version = -1;
            next_full_refresh = 0;
            g_has_state = false;
            g_connected = false;
            {
                std::lock_guard<std::mutex> lock(g_hud_mutex);
                g_hud_position_drafts.clear();
            }
            {
                std::lock_guard<std::mutex> lock(g_cloud_mutex);
                g_cloud_slots.clear();
                g_community.clear();
                g_cloud_user.clear();
            }
            g_cloud_loaded = false;
            g_cloud_failed = false;
            // Load private and Community configs as soon as the local
            // session is available. The settings page should not have to be
            // opened first, and reopening the menu must not show stale data.
            g_cloud_refresh = endpoint.alias_len != 0;
        }
        MenuAction action;
        if (pop_action(action)) {
            bool applied = post_action(endpoint, action);
            if (action.type == ActionType::HudMove && action.commit) {
                std::lock_guard<std::mutex> lock(g_hud_mutex);
                auto draft = g_hud_position_drafts.find(action.value_id);
                if (draft != g_hud_position_drafts.end() && !draft->second.dragging
                        && draft->second.x == action.hud_x
                        && draft->second.y == action.hud_y)
                    g_hud_position_drafts.erase(draft);
            }
            if (!applied && action.type == ActionType::Value) {
                std::lock_guard<std::mutex> lock(g_data_mutex);
                auto edit = g_color_buffers.find(value_edit_key(action.module, action.value_id));
                if (edit != g_color_buffers.end() && edit->second.pending == action.value) {
                    edit->second.pending.clear();
                    if (!edit->second.editing) edit->second.initialized = false;
                }
            }
            if (action.type == ActionType::PartyCreate || action.type == ActionType::PartyJoin
                    || action.type == ActionType::PartyLeave || action.type == ActionType::PartyKick || action.type == ActionType::PartySync) {
                std::lock_guard<std::mutex> lock(g_party_mutex);
                g_party_feedback = applied ? SV_STR(SV_615A7D97B9D02CC8, "Party updated.")
                        : SV_STR(SV_6FB2A1DE3832A126, "Party request failed. Check the code, connection and your role.");
                g_party_busy = false;
            }
            set_status_text(applied ? SV_STR(SV_5FD3AB39053D9213, "Saved") : SV_STR(SV_9978001ABED0499B, "Could not apply change"));
            if (!fetch_state(endpoint, version)) g_connected = false;
            const bool cloud_action = action.type == ActionType::ConfigSave
                    || action.type == ActionType::ConfigDelete
                    || action.type == ActionType::CommunityPublish
                    || action.type == ActionType::CommunityDelete;
            if (cloud_action && endpoint.alias_len != 0) g_cloud_refresh = true;
            next_version_check = GetTickCount64() + 250;
            next_full_refresh = GetTickCount64() + 1800;
            continue;
        }
        if (g_cloud_refresh.exchange(false) && endpoint.alias_len != 0) {
            fetch_cloud_configs(endpoint);
            continue;
        }
        if (!g_has_state.load()) {
            if (!fetch_state(endpoint, version)) {
                g_connected = false;
                set_status_text(SV_STR(SV_11F7B42663D1224C, "Waiting for client"));
                Sleep(350);
            } else {
                ULONGLONG loaded_at = GetTickCount64();
                next_version_check = loaded_at + 350;
                next_full_refresh = loaded_at + 1800;
            }
            continue;
        }
        ULONGLONG now = GetTickCount64();
        if (now >= next_full_refresh) {
            if (!fetch_state(endpoint, version)) g_connected = false;
            next_full_refresh = now + 1800;
        }
        if (now >= next_version_check) {
            int status = 0;
            std::string body;
            PanelScratch target;
            scratch_append_unmasked(target, endpoint.root_masked,
                    endpoint.root_len);
            scratch_append_unmasked(target,
                    endpoint.alias_masked[kPanelRouteVersion],
                    endpoint.alias_len);
            const bool sent = panel_request(endpoint, false, target, nullptr,
                    0, status, body);
            scratch_wipe(target);
            if (sent && status == 200) {
                char *tail = nullptr;
                long long remote_version = std::strtoll(body.c_str(), &tail, 10);
                g_connected = true;
                if (tail != body.c_str() && remote_version != version)
                    fetch_state(endpoint, version);
            } else {
                g_connected = false;
                set_status_text(SV_STR(SV_D399A4ACA718A5BD, "Client disconnected"));
            }
            next_version_check = now + 350;
        }
        Sleep(35);
    }
    g_worker_running = false;
}

static void player_head_worker_loop() {
    g_head_worker_running = true;
    const HRESULT initialized = CoInitializeEx(nullptr, COINIT_MULTITHREADED);
    while (!g_stop.load()) {
        std::string key;
        if (take_pending_player_head(key)) {
            download_player_head(key);
            continue;
        }
        Sleep(40);
    }
    if (SUCCEEDED(initialized)) CoUninitialize();
    g_head_worker_running = false;
}

// (The legacy line-splitting bootstrap parser was removed with the SVUI1
// text format; the binary parser lives in accept_bootstrap above.)

static void accept_bootstrap(const unsigned char *data, size_t length);

// Binary bootstrap payload (little endian):
//   magic 'S','V','U','B' | u16 version=2 | u16 port | i16 gui_bind
//   | u8 root_len | u8 csrf_len | u8 alias_count | root | csrf
//   | alias_count × (u8 len + bytes)
// The publisher (Java) and this parser keep the alias order in sync with
// the PanelRoute enum. Nothing here materializes a URL: the root is the
// session path ("/c/<token>/") and the port selects the loopback listener.
struct BlobReader {
    const unsigned char *data;
    size_t remaining;
};

static bool blob_u8(BlobReader &reader, unsigned char &out) {
    if (reader.remaining < 1) return false;
    out = reader.data[0];
    ++reader.data;
    --reader.remaining;
    return true;
}

static bool blob_u16(BlobReader &reader, unsigned short &out) {
    if (reader.remaining < 2) return false;
    out = static_cast<unsigned short>(reader.data[0]
            | (reader.data[1] << 8));
    reader.data += 2;
    reader.remaining -= 2;
    return true;
}

static bool blob_i16(BlobReader &reader, short &out) {
    unsigned short raw = 0;
    if (!blob_u16(reader, raw)) return false;
    memcpy(&out, &raw, sizeof(out));
    return true;
}

static bool blob_bytes(BlobReader &reader, unsigned char *out, size_t count) {
    if (reader.remaining < count) return false;
    memcpy(out, reader.data, count);
    reader.data += count;
    reader.remaining -= count;
    return true;
}

static bool printable_token(const unsigned char *bytes, size_t length,
        size_t minimum) {
    if (length < minimum) return false;
    for (size_t index = 0; index < length; ++index) {
        const unsigned char current = bytes[index];
        if (!((current >= 'a' && current <= 'z')
                    || (current >= 'A' && current <= 'Z')
                    || (current >= '0' && current <= '9')
                    || current == '_' || current == '-' || current == '.')) {
            return false;
        }
    }
    return true;
}

static void accept_bootstrap(const unsigned char *data, size_t length) {
    MenuEndpoint next{};
    unsigned char root_plain[80];
    unsigned char csrf_plain[48];
    unsigned char aliases[kPanelRouteCount][20];
    unsigned char alias_lengths[kPanelRouteCount];
    BlobReader reader{data, length};
    unsigned char root_len = 0;
    unsigned char csrf_len = 0;
    unsigned char alias_count = 0;
    unsigned short port = 0;
    short gui_bind = 0;
    bool valid = data != nullptr && length >= 12 && length <= 4096;
    if (valid) valid = data[0] == 'S' && data[1] == 'V'
            && data[2] == 'U' && data[3] == 'B';
    if (valid) { reader.data = data + 4; reader.remaining = length - 4; }
    if (valid) {
        unsigned short version = 0;
        valid = blob_u16(reader, version) && version == 2
                && blob_u16(reader, port) && port != 0
                && blob_i16(reader, gui_bind)
                && ((gui_bind >= 1 && gui_bind <= 255)
                        || (gui_bind >= -100 && gui_bind <= -96))
                && blob_u8(reader, root_len)
                && blob_u8(reader, csrf_len)
                && blob_u8(reader, alias_count);
        valid = valid && root_len >= 9 && root_len <= sizeof(root_plain)
                && csrf_len >= 16 && csrf_len <= sizeof(csrf_plain)
                && alias_count == kPanelRouteCount;
    }
    if (valid) valid = blob_bytes(reader, root_plain, root_len);
    if (valid) {
        // The root is a session path ("/c/<token>/"): slashes separate the
        // segments, so the printable-token charset only applies to the
        // alias table below. Here every byte must be visible ASCII.
        valid = root_plain[0] == '/' && root_plain[root_len - 1] == '/';
        for (size_t index = 0; valid && index < root_len; ++index) {
            if (root_plain[index] < 0x21 || root_plain[index] > 0x7E) {
                valid = false;
            }
        }
    }
    if (valid) valid = blob_bytes(reader, csrf_plain, csrf_len)
            && printable_token(csrf_plain, csrf_len, 16);
    for (unsigned char route = 0; valid && route < alias_count; ++route) {
        valid = blob_u8(reader, alias_lengths[route])
                && alias_lengths[route] >= 6
                && alias_lengths[route] <= sizeof(aliases[0])
                && blob_bytes(reader, aliases[route], alias_lengths[route])
                && printable_token(aliases[route], alias_lengths[route], 6);
    }
    if (valid && reader.remaining != 0) valid = false;
    if (valid) {
        ensure_endpoint_mask();
        next.port = port;
        next.root_len = root_len;
        mask_store(next.root_masked, root_plain, root_len);
        next.csrf_len = csrf_len;
        mask_store(next.csrf_masked, csrf_plain, csrf_len);
        unsigned char max_alias = 0;
        for (unsigned char route = 0; route < kPanelRouteCount; ++route) {
            mask_store(next.alias_masked[route], aliases[route],
                    alias_lengths[route]);
            next.alias_lengths[route] = alias_lengths[route];
            if (alias_lengths[route] > max_alias) {
                max_alias = alias_lengths[route];
            }
        }
        next.alias_len = max_alias;
        g_gui_bind = static_cast<int>(gui_bind);
        {
            std::lock_guard<std::mutex> lock(g_endpoint_mutex);
            if (g_endpoint.port == next.port
                    && g_endpoint.root_len == next.root_len
                    && g_endpoint.csrf_len == next.csrf_len
                    && g_endpoint.alias_len == next.alias_len
                    && memcmp(g_endpoint.root_masked, next.root_masked,
                            sizeof(next.root_masked)) == 0
                    && memcmp(g_endpoint.csrf_masked, next.csrf_masked,
                            sizeof(next.csrf_masked)) == 0
                    && memcmp(g_endpoint.alias_masked, next.alias_masked,
                            sizeof(next.alias_masked)) == 0
                    && memcmp(g_endpoint.alias_lengths, next.alias_lengths,
                            sizeof(next.alias_lengths)) == 0) {
                g_endpoint_ready = true;
                goto done;
            }
            next.generation = g_endpoint.generation + 1;
            g_endpoint = next;
        }
        g_endpoint_ready = true;
    }
done:
    SecureZeroMemory(root_plain, sizeof(root_plain));
    SecureZeroMemory(csrf_plain, sizeof(csrf_plain));
    SecureZeroMemory(aliases, sizeof(aliases));
    SecureZeroMemory(alias_lengths, sizeof(alias_lengths));
    sonyvegas_obf_scrub();
}

static int bind_to_vk(int key) {
    switch (key) {
        case -100: return VK_LBUTTON;
        case -99: return VK_RBUTTON;
        case -98: return VK_MBUTTON;
        case -97: return VK_XBUTTON1;
        case -96: return VK_XBUTTON2;
        default: return key >= 1 && key <= 255 ? key : VK_RSHIFT;
    }
}

static std::string key_name(int key) {
    switch (key) {
        case 0: return SV_STR(SV_1DBC487DDC4D9836, "None");
        case -100: return SV_STR(SV_4C5987449CF0A0AD, "Mouse 1");
        case -99: return SV_STR(SV_EB1BE65A3C6526FC, "Mouse 2");
        case -98: return SV_STR(SV_C63D4C628FE4C18D, "Mouse 3");
        case -97: return SV_STR(SV_DA908568F77A6217, "Mouse 4");
        case -96: return SV_STR(SV_4E8384092581B547, "Mouse 5");
        case VK_LBUTTON: return SV_STR(SV_4C5987449CF0A0AD, "Mouse 1");
        case VK_RBUTTON: return SV_STR(SV_EB1BE65A3C6526FC, "Mouse 2");
        case VK_MBUTTON: return SV_STR(SV_C63D4C628FE4C18D, "Mouse 3");
        case VK_XBUTTON1: return SV_STR(SV_DA908568F77A6217, "Mouse 4");
        case VK_XBUTTON2: return SV_STR(SV_4E8384092581B547, "Mouse 5");
        case VK_ESCAPE: return SV_STR(SV_93126D1E6F673AC1, "Escape");
        case VK_DELETE: return SV_STR(SV_59058343D2C1D19E, "Delete");
        case VK_INSERT: return SV_STR(SV_D41C5E635032A7B0, "Insert");
        default: break;
    }
    UINT scan = MapVirtualKeyA(static_cast<UINT>(key), MAPVK_VK_TO_VSC);
    LONG parameter = static_cast<LONG>(scan << 16);
    if (key == VK_RCONTROL || key == VK_RMENU || key == VK_LEFT
            || key == VK_RIGHT || key == VK_UP || key == VK_DOWN
            || key == VK_HOME || key == VK_END || key == VK_PRIOR
            || key == VK_NEXT || key == VK_DIVIDE || key == VK_NUMLOCK)
        parameter |= 1 << 24;
    char name[96]{};
    if (GetKeyNameTextA(parameter, name, sizeof(name)) > 0) return name;
    char fallback[24]{};
    _snprintf_s(fallback, sizeof(fallback), _TRUNCATE, SV_STR(SV_F15FBA4458F9FDE7, "VK %d"), key);
    return fallback;
}

static bool is_key_down(int key) {
    return (GetAsyncKeyState(bind_to_vk(key)) & 0x8000) != 0;
}

static void arm_bind_capture(int kind) {
    for (int key = 0; key < static_cast<int>(g_capture_key_down.size()); ++key)
        g_capture_key_down[static_cast<size_t>(key)] =
                (GetAsyncKeyState(key) & 0x8000) != 0;
    g_capture_kind = kind;
}

static void cancel_bind_capture() {
    g_capture_kind = 0;
    std::lock_guard<std::mutex> lock(g_capture_mutex);
    g_capture_module.clear();
    g_capture_value.clear();
}

static void position_overlay();
static void position_hud_overlay();
static void start_hud_edit();
static void stop_hud_edit(bool restore_menu_layout);
static void render_hud_editor();
static void make_game_borderless();
static void restore_game_window();

static void hide_menu() {
    if (g_hud_edit.load()) stop_hud_edit(false);
    const bool was_visible = g_visible.exchange(false);
    g_capture_kind = 0;
    if (!g_embedded_mode && g_imgui_window != nullptr)
        ShowWindow(g_imgui_window, SW_HIDE);
    g_overlay_hidden_by_focus = false;
    if (!was_visible) return;
    if (!g_embedded_mode) restore_game_window();
    sonyvegas_module_menu_visibility_callback callback =
            g_visibility_callback.load();
    if (callback != nullptr) callback(FALSE);
}

static void toggle_menu() {
    bool next = !g_visible.load();
    if (g_imgui_window == nullptr && !g_embedded_mode) return;
    if (next) {
        g_visible = true;
        sonyvegas_module_menu_visibility_callback callback =
                g_visibility_callback.load();
        const BOOL needs_borderless = callback != nullptr && callback(TRUE);
        if (!g_embedded_mode) {
            if (needs_borderless) make_game_borderless();
            position_overlay();
            ShowWindow(g_imgui_window, SW_SHOWNOACTIVATE);
            SetWindowPos(g_imgui_window, HWND_TOPMOST, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_SHOWWINDOW | SWP_NOACTIVATE);
        }
    } else {
        hide_menu();
    }
}

static void update_cached_bind(const std::string &module, int key) {
    std::lock_guard<std::mutex> lock(g_data_mutex);
    for (MenuModule &entry : g_modules) {
        if (entry.id != module) continue;
        entry.binds.clear();
        if (key != 0) entry.binds.push_back(key);
        break;
    }
}

static void finish_bind_capture(int key) {
    int kind = g_capture_kind.exchange(0);
    if (kind == 0) return;
    g_hotkey_suppressed_until = GetTickCount64() + 500;
    if (kind == 2) {
        if (key != 0) {
            g_gui_bind = key;
            MenuAction action;
            action.type = ActionType::GuiBind;
            action.key = key;
            queue_action(std::move(action));
        }
        return;
    }
    std::string module;
    std::string value_id;
    {
        std::lock_guard<std::mutex> lock(g_capture_mutex);
        module.swap(g_capture_module);
        value_id.swap(g_capture_value);
    }
    if (module.empty()) return;
    if (kind == 3 && !value_id.empty()) {
        {
            std::lock_guard<std::mutex> lock(g_data_mutex);
            for (MenuModule &entry : g_modules) {
                if (entry.id != module) continue;
                for (MenuValue &setting : entry.values) {
                    if (setting.id == value_id) {
                        setting.value = std::to_string(key);
                        break;
                    }
                }
                break;
            }
        }
        MenuAction action;
        action.type = ActionType::Value;
        action.module = module;
        action.value_id = value_id;
        action.value = std::to_string(key);
        queue_action(std::move(action));
        return;
    }
    update_cached_bind(module, key);
    MenuAction action;
    action.type = ActionType::Bind;
    action.module = module;
    action.key = key;
    queue_action(std::move(action));
}

static bool handle_bind_capture_key(int key) {
    const int kind = g_capture_kind.load();
    if (kind == 0 || key == 0) return false;
    g_suppressed_bind_key = key;
    g_suppressed_bind_key_until = GetTickCount64() + 250;
    if (key == VK_ESCAPE) {
        cancel_bind_capture();
    } else if (key == VK_BACK || key == VK_DELETE) {
        // Keep a usable key assigned to the menu itself; Backspace/Delete
        // clears module and setting binds.
        if (kind == 1 || kind == 3) finish_bind_capture(0);
        else cancel_bind_capture();
    } else {
        finish_bind_capture(key);
    }
    return true;
}

static void poll_bind_capture() {
    if (g_capture_kind.load() == 0) return;
    for (int key = 8; key <= 255; ++key) {
        // Mouse buttons keep their negative wire values and are handled by
        // the overlay window procedure. This fallback is for keyboard input.
        if (key == VK_LBUTTON || key == VK_RBUTTON || key == VK_MBUTTON
                || key == VK_XBUTTON1 || key == VK_XBUTTON2)
            continue;
        const bool down = (GetAsyncKeyState(key) & 0x8000) != 0;
        const bool pressed = down && !g_capture_key_down[static_cast<size_t>(key)];
        g_capture_key_down[static_cast<size_t>(key)] = down;
        if (pressed && handle_bind_capture_key(key)) break;
    }
}

static int message_bind_code(UINT message, WPARAM wparam, LPARAM lparam) {
    switch (message) {
        case WM_KEYDOWN:
        case WM_SYSKEYDOWN: {
            int key = static_cast<int>(wparam);
            const UINT scan = (static_cast<UINT_PTR>(lparam) >> 16) & 0xffu;
            const bool extended = (static_cast<UINT_PTR>(lparam)
                    & 0x01000000u) != 0;
            if (key == VK_SHIFT) {
                const UINT resolved = MapVirtualKeyA(scan, MAPVK_VSC_TO_VK_EX);
                if (resolved == VK_LSHIFT || resolved == VK_RSHIFT)
                    key = static_cast<int>(resolved);
            } else if (key == VK_CONTROL) {
                key = extended ? VK_RCONTROL : VK_LCONTROL;
            } else if (key == VK_MENU) {
                key = extended ? VK_RMENU : VK_LMENU;
            }
            return key;
        }
        case WM_LBUTTONDOWN: return -100;
        case WM_RBUTTONDOWN: return -99;
        case WM_MBUTTONDOWN: return -98;
        case WM_XBUTTONDOWN:
            return GET_XBUTTON_WPARAM(wparam) == XBUTTON1 ? -97 : -96;
        default: return 0;
    }
}

static std::string lower_copy(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(),
            [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return value;
}

static bool contains_ci(const std::string &text, const std::string &needle) {
    if (needle.empty()) return true;
    return lower_copy(text).find(lower_copy(needle)) != std::string::npos;
}

static void completion_token_bounds(const char *buffer, int text_length,
        int cursor, bool multiple, int &start, int &end) {
    cursor = ImClamp(cursor, 0, text_length);
    start = multiple ? cursor : 0;
    end = multiple ? cursor : text_length;
    if (multiple) {
        while (start > 0 && buffer[start - 1] != '\n'
                && buffer[start - 1] != ',')
            --start;
        while (start < cursor && std::isspace(
                static_cast<unsigned char>(buffer[start])))
            ++start;
        while (end < text_length && buffer[end] != '\n' && buffer[end] != ',')
            ++end;
    }
}

static std::string find_inline_completion(const char *buffer, int text_length,
        int cursor, bool multiple, const std::vector<std::string> &suggestions,
        std::string *query_result = nullptr, int *start_result = nullptr) {
    int start = 0;
    int end = 0;
    completion_token_bounds(buffer, text_length, cursor, multiple, start, end);
    const std::string query(buffer + start, buffer + cursor);
    if (query_result != nullptr) *query_result = query;
    if (start_result != nullptr) *start_result = start;
    if (query.empty()) return {};
    const std::string lowered = lower_copy(query);
    for (const std::string &candidate : suggestions) {
        if (candidate.size() <= query.size()) continue;
        const std::string candidate_lower = lower_copy(candidate);
        if (candidate_lower.compare(0, lowered.size(), lowered) == 0)
            return candidate;
    }
    return {};
}

struct TextAutocompleteContext {
    const std::vector<std::string> *suggestions = nullptr;
    bool multiple = false;
    bool completed = false;
};

static int autocomplete_input_callback(ImGuiInputTextCallbackData *data) {
    TextAutocompleteContext *context =
            static_cast<TextAutocompleteContext *>(data->UserData);
    if (context == nullptr || context->suggestions == nullptr
            || data->EventFlag != ImGuiInputTextFlags_CallbackCompletion)
        return 0;
    int start = 0;
    int end = 0;
    completion_token_bounds(data->Buf, data->BufTextLen, data->CursorPos,
            context->multiple, start, end);
    const std::string completion = find_inline_completion(data->Buf,
            data->BufTextLen, data->CursorPos, context->multiple,
            *context->suggestions);
    if (completion.empty()) return 0;
    data->DeleteChars(start, end - start);
    data->InsertChars(start, completion.c_str());
    data->SelectionStart = data->SelectionEnd = data->CursorPos;
    context->completed = true;
    return 0;
}

static void render_inline_completion(ImGuiID input_id, const ImVec2 &item_min,
        const ImVec2 &item_max, const char *buffer, bool multiple,
        const std::vector<std::string> &suggestions, bool active) {
    const int length = static_cast<int>(std::strlen(buffer));
    std::string query;
    int token_start = 0;
    const std::string completion = active ? find_inline_completion(buffer,
            length, length, multiple, suggestions, &query, &token_start)
            : std::string();
    static std::map<ImGuiID, float> animation;
    static std::map<ImGuiID, std::string> previous;
    float &amount = animation[input_id];
    if (completion.empty()) {
        amount = 0.f;
        previous[input_id].clear();
        return;
    }
    if (previous[input_id] != completion) amount = 0.f;
    previous[input_id] = completion;
    amount = ImLerp(amount, 1.f,
            ImClamp(ImGui::GetIO().DeltaTime * 13.f, 0.f, 1.f));

    int line = 0;
    if (multiple) {
        for (int index = 0; index < token_start; ++index)
            if (buffer[index] == '\n') ++line;
    }
    ImFont *font = g_font_regular != nullptr ? g_font_regular : ImGui::GetFont();
    const float font_size = ImGui::GetFontSize();
    const float prefix_width = font->CalcTextSizeA(font_size, FLT_MAX, 0.f,
            query.c_str()).x;
    const char *suffix = completion.c_str() + query.size();
    ImDrawList *draw = ImGui::GetWindowDrawList();
    draw->PushClipRect(item_min + ImVec2(2.f, 2.f),
            item_max - ImVec2(2.f, 2.f), true);
    draw->AddText(font, font_size,
            ImVec2(item_min.x + ImGui::GetStyle().FramePadding.x
                            + prefix_width + (1.f - amount) * 5.f,
                    item_min.y + ImGui::GetStyle().FramePadding.y
                            + line * ImGui::GetTextLineHeight()),
            IM_COL32(124, 124, 124, static_cast<int>(150.f * amount)),
            suffix);
    draw->PopClipRect();
}

// §-skipping substring search over UTF-8 display text (§ = 0xC2 0xA7 plus one
// code byte). Never materializes the cleaned string; the query is user-typed
// and carries no separators.
static bool contains_ci_visible(const std::string &text, const std::string &needle) {
    if (needle.empty()) return true;
    std::string lowered = needle;
    for (char &ch : lowered) ch = static_cast<char>(std::tolower((unsigned char)ch));
    const size_t count = text.size();
    for (size_t i = 0; i < count;) {
        size_t ti = i;
        size_t ni = 0;
        for (;;) {
            while (ti + 3 <= count
                    && static_cast<unsigned char>(text[ti]) == 0xC2
                    && static_cast<unsigned char>(text[ti + 1]) == 0xA7) {
                ti += 3;
            }
            if (ni >= lowered.size()) return true;
            if (ti >= count
                    || static_cast<char>(std::tolower(
                            static_cast<unsigned char>(text[ti]))) != lowered[ni]) {
                break;
            }
            ++ti;
            ++ni;
        }
        if (i + 3 <= count
                && static_cast<unsigned char>(text[i]) == 0xC2
                && static_cast<unsigned char>(text[i + 1]) == 0xA7) {
            i += 3;
        } else {
            ++i;
        }
    }
    return false;
}

static bool formatting_reset_at(const char *cursor, const char *end) {
    return cursor + 2 < end
            && static_cast<unsigned char>(cursor[0]) == 0xC2
            && static_cast<unsigned char>(cursor[1]) == 0xA7;
}

static ImVec2 calc_visible_text_size(ImFont *font, float size,
        const char *text, const char *text_end = nullptr) {
    if (text == nullptr) return ImVec2(0.f, 0.f);
    const char *end = text_end != nullptr ? text_end : text + std::strlen(text);
    float line_width = 0.f;
    float maximum_width = 0.f;
    float height = size;
    const char *cursor = text;
    while (cursor < end) {
        if (formatting_reset_at(cursor, end)) {
            cursor += 3;
            continue;
        }
        const char *segment_end = cursor;
        while (segment_end < end && !formatting_reset_at(segment_end, end)
                && *segment_end != '\n') ++segment_end;
        if (segment_end > cursor) {
            line_width += font->CalcTextSizeA(size, FLT_MAX, 0.f,
                    cursor, segment_end).x;
            maximum_width = std::max(maximum_width, line_width);
        }
        if (segment_end < end && *segment_end == '\n') {
            line_width = 0.f;
            height += size;
            cursor = segment_end + 1;
        } else {
            cursor = segment_end;
        }
    }
    return ImVec2(std::max(maximum_width, line_width), height);
}

static void add_visible_text(ImDrawList *draw, ImFont *font, float size,
        ImVec2 pos, ImU32 color, const char *text, const char *text_end = nullptr,
        const ImVec4 *clip = nullptr) {
    if (text == nullptr) return;
    const char *end = text_end != nullptr ? text_end : text + std::strlen(text);
    const float origin_x = pos.x;
    const char *cursor = text;
    while (cursor < end) {
        if (formatting_reset_at(cursor, end)) {
            cursor += 3;
            continue;
        }
        const char *segment_end = cursor;
        while (segment_end < end && !formatting_reset_at(segment_end, end)
                && *segment_end != '\n') ++segment_end;
        if (segment_end > cursor) {
            draw->AddText(font, size, pos, color, cursor, segment_end, 0.f, clip);
            pos.x += font->CalcTextSizeA(size, FLT_MAX, 0.f,
                    cursor, segment_end).x;
        }
        if (segment_end < end && *segment_end == '\n') {
            pos.x = origin_x;
            pos.y += size;
            cursor = segment_end + 1;
        } else {
            cursor = segment_end;
        }
    }
}

static std::string uppercase_copy(std::string text) {
    for (size_t index = 0; index < text.size(); ++index) {
        if (index + 2 < text.size()
                && static_cast<unsigned char>(text[index]) == 0xC2
                && static_cast<unsigned char>(text[index + 1]) == 0xA7) {
            index += 2;
            continue;
        }
        text[index] = static_cast<char>(std::toupper(
                static_cast<unsigned char>(text[index])));
    }
    return text;
}

static std::string format_number(double value, int decimals) {
    std::ostringstream output;
    output << std::fixed << std::setprecision(std::max(0,
            std::min(decimals, 8))) << value;
    return output.str();
}

static double quantize(double value, const MenuValue &setting) {
    value = std::max(setting.minimum, std::min(setting.maximum, value));
    if (setting.increment > 0.0) {
        value = setting.minimum + std::round(
                (value - setting.minimum) / setting.increment)
                * setting.increment;
        value = std::max(setting.minimum, std::min(setting.maximum, value));
    }
    return value;
}

static void queue_value(MenuModule &module, MenuValue &setting,
        const std::string &serialized) {
    setting.value = serialized;
    if (setting.type == SV_STR(SV_60C175CA12488454, "color")) {
        ColorEditBuffer &edit = g_color_buffers[value_edit_key(module.id, setting.id)];
        edit.draft = setting;
        edit.pending = serialized;
        edit.initialized = true;
    }
    MenuAction action;
    action.type = ActionType::Value;
    action.module = module.id;
    action.value_id = setting.id;
    action.value = serialized;
    queue_action(std::move(action));
}

static bool parse_hex_color(const std::string &text, float color[3]) {
    if (text.size() != 7 || text[0] != '#') return false;
    char *tail = nullptr;
    unsigned long rgb = std::strtoul(text.c_str() + 1, &tail, 16);
    if (tail == text.c_str() + 1 || *tail != '\0') return false;
    color[0] = static_cast<float>((rgb >> 16) & 255) / 255.f;
    color[1] = static_cast<float>((rgb >> 8) & 255) / 255.f;
    color[2] = static_cast<float>(rgb & 255) / 255.f;
    return true;
}

static std::string color_hex(const float color[3]) {
    int red = static_cast<int>(std::round(
            std::max(0.f, std::min(1.f, color[0])) * 255.f));
    int green = static_cast<int>(std::round(
            std::max(0.f, std::min(1.f, color[1])) * 255.f));
    int blue = static_cast<int>(std::round(
            std::max(0.f, std::min(1.f, color[2])) * 255.f));
    char text[8]{};
    _snprintf_s(text, sizeof(text), _TRUNCATE, SV_STR(SV_8A53522B064124FC, "#%02X%02X%02X"),
            red, green, blue);
    return text;
}

static std::string color_serialized(const MenuValue &setting) {
    const char *mode = setting.fade ? SV_STR(SV_0C3A2F5C3FF788D1, "fade")
            : setting.rainbow ? SV_STR(SV_D703FEE2323F2DF4, "rainbow") : SV_STR(SV_1E7268940546A7DC, "fixed");
    return setting.color + SV_STR(SV_C04C1EC13A261F68, ",") + std::to_string(setting.alpha)
            + SV_STR(SV_C04C1EC13A261F68, ",") + mode + SV_STR(SV_C04C1EC13A261F68, ",") + setting.fade_color;
}


// ---------------------------------------------------------------------------
// Widgets shaped after the reference menu: a checkbox whose ticked box is an
// accent gradient, a slider whose label and value sit above a thin track, and
// a card that carries its own title bar with an accent underline.
// ---------------------------------------------------------------------------

// Every label in the menu is drawn one pixel down in near black first, which
// is what lifts the text off the flat grey panels.
static void draw_text_shadow(ImDrawList *draw, const ImVec2 &pos, ImU32 color,
        const char *text, const char *text_end = nullptr,
        ImFont *font = nullptr, float size = 0.f,
        const ImVec4 *clip = nullptr) {
    const unsigned alpha = (color >> IM_COL32_A_SHIFT) & 0xffu;
    if (alpha == 0u) return;
    const ImU32 shadow = IM_COL32(0, 0, 0, (alpha * 150u) / 255u);
    ImFont *used_font = font != nullptr ? font : ImGui::GetFont();
    const float used_size = size > 0.f ? size : ImGui::GetFontSize();
    add_visible_text(draw, used_font, used_size,
            ImVec2(pos.x + 1.f, pos.y + 1.f), shadow, text, text_end, clip);
    add_visible_text(draw, used_font, used_size, pos, color, text, text_end,
            clip);
}

static void visible_text_line(ImU32 color, const std::string &text) {
    ImFont *font = ImGui::GetFont();
    const float size = ImGui::GetFontSize();
    const ImVec2 pos = ImGui::GetCursorScreenPos();
    const ImVec2 bounds = calc_visible_text_size(font, size, text.c_str());
    ImGui::Dummy(bounds);
    draw_text_shadow(ImGui::GetWindowDrawList(), pos, color, text.c_str(),
            nullptr, font, size);
}

// Widgets ramp their opacity up when the menu opens, the way the reference
// slider does with its "fill" value.
static float widget_fade(ImGuiID id, float speed = 2.f) {
    static std::map<ImGuiID, float> fades;
    float &fade = fades[id];
    fade = ImClamp(fade + speed * ImGui::GetIO().DeltaTime, 0.f, 1.f);
    return fade * ImGui::GetStyle().Alpha;
}

static ImU32 with_fade(ImU32 color, float fade) {
    const unsigned alpha = (color >> IM_COL32_A_SHIFT) & 0xffu;
    const unsigned faded = static_cast<unsigned>(alpha * ImClamp(fade, 0.f, 1.f));
    return (color & ~IM_COL32_A_MASK) | (faded << IM_COL32_A_SHIFT);
}

static void draw_gradient_rounded(ImDrawList *draw, const ImVec2 &min,
        const ImVec2 &max, ImU32 top, ImU32 bottom, ImU32 mask, float rounding) {
    draw->AddRectFilledMultiColor(min, max, top, top, bottom, bottom);
    // AddRectFilledMultiColor cannot round, so the corners are cut back out
    // with the surrounding colour - the same trick the reference uses.
    for (float step = 0.f; step < 2.f; step += 1.f)
        draw->AddRect(min - ImVec2(step, step), max + ImVec2(step, step), mask,
                rounding + step, 0, 1.5f);
}

static bool menu_checkbox(const char *label, bool *value,
        ImU32 mask = kCardBody) {
    ImGuiWindow *window = ImGui::GetCurrentWindow();
    if (window->SkipItems) return false;
    const ImGuiID id = window->GetID(label);
    const char *label_end = ImGui::FindRenderedTextEnd(label);
    const ImVec2 label_size = calc_visible_text_size(ImGui::GetFont(),
            ImGui::GetFontSize(), label, label_end);
    const float box = 14.f;
    const float height = ImMax(box, label_size.y);
    const ImVec2 pos = window->DC.CursorPos;
    const ImRect bb(pos, ImVec2(pos.x + box + 8.f + label_size.x,
            pos.y + height));
    ImGui::ItemSize(bb, 0.f);
    if (!ImGui::ItemAdd(bb, id)) return false;

    bool hovered = false;
    bool held = false;
    const bool pressed = ImGui::ButtonBehavior(bb, id, &hovered, &held);
    if (pressed) {
        *value = !*value;
        ImGui::MarkItemEdited(id);
    }

    ImDrawList *draw = window->DrawList;
    const ImVec2 box_min(pos.x, pos.y + (height - box) * 0.5f);
    const ImVec2 box_max(box_min.x + box, box_min.y + box);
    if (*value) {
        draw_gradient_rounded(draw, box_min, box_max, kAccentTop, kAccentBottom,
                mask, 2.5f);
        ImGui::RenderCheckMark(draw, ImVec2(box_min.x + 3.f, box_min.y + 3.f),
                IM_COL32(24, 24, 24, 255), box - 6.f);
    } else {
        draw->AddRectFilled(box_min, box_max, kTrack, 2.5f);
        draw->AddRect(box_min, box_max,
                hovered ? IM_COL32(70, 70, 72, 255) : IM_COL32(52, 52, 54, 255),
                2.5f);
    }
    if (label_size.x > 0.f)
        draw_text_shadow(draw, ImVec2(box_max.x + 8.f,
                pos.y + (height - label_size.y) * 0.5f),
                *value ? kLabelOn : kLabelOff, label,
                ImGui::FindRenderedTextEnd(label));
    return pressed;
}

struct SliderResult {
    bool changed = false;
    bool finished = false;
};

static SliderResult menu_slider(const char *id_label, const char *label,
        float *value, float minimum, float maximum, const char *format) {
    SliderResult result;
    ImGuiWindow *window = ImGui::GetCurrentWindow();
    if (window->SkipItems) return result;
    ImGuiContext &g = *ImGui::GetCurrentContext();
    const ImGuiID id = window->GetID(id_label);
    const float width = ImMax(60.f, ImGui::GetContentRegionAvail().x);
    const float text_height = g.FontSize;
    const ImVec2 pos = window->DC.CursorPos;
    const ImRect frame(ImVec2(pos.x, pos.y + text_height + 5.f),
            ImVec2(pos.x + width, pos.y + text_height + 13.f));
    const ImRect total(pos, frame.Max);
    ImGui::ItemSize(total, 0.f);
    if (!ImGui::ItemAdd(total, id, &frame)) return result;

    const bool hovered = ImGui::ItemHoverable(frame, id,
            g.LastItemData.InFlags);
    if (hovered && ImGui::IsMouseClicked(ImGuiMouseButton_Left, id)) {
        ImGui::SetKeyOwner(ImGuiKey_MouseLeft, id);
        ImGui::SetActiveID(id, window);
        ImGui::SetFocusID(id, window);
        ImGui::FocusWindow(window);
        g.ActiveIdUsingNavDirMask |= (1 << ImGuiDir_Left)
                | (1 << ImGuiDir_Right);
    }

    ImRect grab;
    result.changed = ImGui::SliderBehavior(frame, id, ImGuiDataType_Float, value,
            &minimum, &maximum, format, ImGuiSliderFlags_AlwaysClamp, &grab);
    if (result.changed) ImGui::MarkItemEdited(id);
    result.finished = ImGui::IsItemDeactivatedAfterEdit();

    ImDrawList *draw = window->DrawList;
    const float fade = widget_fade(id);
    draw->AddRectFilled(frame.Min, frame.Max, with_fade(kTrack, fade), 4.f);
    const float fill_x = ImClamp(grab.GetCenter().x, frame.Min.x, frame.Max.x);
    if (fill_x > frame.Min.x + 2.f)
        draw_gradient_rounded(draw, ImVec2(frame.Min.x + 1.f, frame.Min.y + 1.f),
                ImVec2(fill_x, frame.Max.y - 1.f), with_fade(kAccentTop, fade),
                with_fade(kAccentBottom, fade), kTrack, 3.f);
    draw->AddRect(frame.Min, frame.Max, with_fade(kCardEdge, fade), 4.f);
    draw->AddRectFilled(ImVec2(fill_x - 3.f, frame.Min.y - 2.f),
            ImVec2(fill_x + 3.f, frame.Max.y + 2.f), with_fade(kGrab, fade), 3.f);
    draw->AddRect(ImVec2(fill_x - 3.f, frame.Min.y - 2.f),
            ImVec2(fill_x + 3.f, frame.Max.y + 2.f), with_fade(kGrabEdge, fade),
            3.f);

    char buffer[64]{};
    ImGui::DataTypeFormatString(buffer, IM_ARRAYSIZE(buffer),
            ImGuiDataType_Float, value, format);
    const bool has_label = label != nullptr && label[0] != 0;
    const ImVec2 label_size = has_label
            ? calc_visible_text_size(ImGui::GetFont(), ImGui::GetFontSize(), label)
            : ImVec2(0.f, 0.f);
    if (has_label) draw_text_shadow(draw, pos, with_fade(kLabelOn, fade), label);
    char value_text[80]{};
    _snprintf_s(value_text, sizeof(value_text), _TRUNCATE, SV_STR(SV_C684A98B65970E50, "- %s"), buffer);
    const float value_x = has_label ? pos.x + label_size.x + 7.f : pos.x;
    draw_text_shadow(draw, ImVec2(value_x, pos.y), with_fade(kValueText, fade),
            value_text);
    return result;
}

static void menu_card_begin(const char *id, const char *title,
        const ImVec2 &size) {
    ImGuiWindow *parent = ImGui::GetCurrentWindow();
    ImDrawList *draw = parent->DrawList;
    const ImVec2 origin = parent->DC.CursorPos;
    draw->AddRectFilled(origin, origin + size, kCardBody, 10.f);
    draw->AddRectFilled(origin, origin + ImVec2(size.x, kCardHeaderHeight),
            kCardHeader, 10.f, ImDrawFlags_RoundCornersTop);
    draw->AddLine(origin + ImVec2(0.f, kCardHeaderHeight),
            origin + ImVec2(size.x - 0.5f, kCardHeaderHeight), kAccent);
    draw->AddRect(origin + ImVec2(1.f, 1.f), origin + size - ImVec2(1.f, 1.f),
            kCardEdge, 10.f);
    draw->AddRect(origin, origin + size, kCardOutline, 10.f);
    draw_text_shadow(draw, origin + ImVec2(11.f,
            (kCardHeaderHeight - ImGui::GetFontSize()) * 0.5f),
            IM_COL32(200, 200, 200, 255), title);
    ImGui::PushStyleColor(ImGuiCol_ChildBg, IM_COL32(0, 0, 0, 0));
    ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, kCardBodyPadding);
    ImGui::SetCursorScreenPos(origin + ImVec2(0.f, kCardHeaderHeight));
    ImGui::BeginChild(id, ImVec2(size.x, size.y - kCardHeaderHeight),
            ImGuiChildFlags_AlwaysUseWindowPadding,
            ImGuiWindowFlags_NoScrollbar | ImGuiWindowFlags_NoScrollWithMouse);
}

static void menu_card_end() {
    ImGui::EndChild();
    ImGui::PopStyleVar();
    ImGui::PopStyleColor();
}

static float value_card_height(const MenuValue &setting) {
    const float header = kCardHeaderHeight;
    if (setting.type == SV_STR(SV_090B0A79694821B6, "bind")) return header + 76.f;
    if (setting.type == SV_STR(SV_4DFD00155697EA79, "boolean")) return header + 40.f;
    if (setting.type == SV_STR(SV_2D47078D80D9411D, "range")) return header + 104.f;
    if (setting.type == SV_STR(SV_60C175CA12488454, "color"))
        return header + (setting.fade ? 166.f : 126.f);
    if (setting.type == SV_STR(SV_6AD981CC4B9E110E, "flags"))
        return header + 14.f + 25.f * static_cast<float>(setting.options.size());
    if (setting.type == SV_STR(SV_5CB8522F1D48346D, "player-flags"))
        return header + 14.f + 34.f * static_cast<float>(setting.options.size());
    if (setting.type == SV_STR(SV_23AA0DEDF1CBB41A, "text")) return header + 50.f;
    if (setting.type == SV_STR(SV_60EDCA2A6D1A2489, "list") || setting.type == SV_STR(SV_BA7FA36853B641DB, "player-list")
            || setting.type == SV_STR(SV_546A9D9F51735639, "json")) return header + 108.f;
    return header + 54.f;
}

static void begin_value_bind_capture(const std::string &module,
        const std::string &value_id) {
    {
        std::lock_guard<std::mutex> lock(g_capture_mutex);
        g_capture_module = module;
        g_capture_value = value_id;
    }
    arm_bind_capture(3);
}

static void render_checkbox_label(const char *label, bool &value,
        ImU32 mask = kCardBody) {
    menu_checkbox(label, &value, mask);
}

static void render_value_card(MenuModule &module, MenuValue &setting,
        float width) {
    ImGui::PushID(setting.id.c_str());
    float height = value_card_height(setting);
    menu_card_begin(SV_STR(SV_08629D2C55F08737, "##value"), setting.name.c_str(),
            ImVec2(width, height));
    ImGui::SetNextItemWidth(-1.f);

    if (setting.type == SV_STR(SV_090B0A79694821B6, "bind")) {
        int key = static_cast<int>(std::strtol(setting.value.c_str(), nullptr, 10));
        bool listening = g_capture_kind.load() == 3;
        if (listening) {
            std::lock_guard<std::mutex> lock(g_capture_mutex);
            listening = g_capture_module == module.id
                    && g_capture_value == setting.id;
        }
        std::string label = listening ? SV_STR(SV_B372AB19B31BD412, "Press a key...")
                : SV_STR(SV_E9C16F3D37CB2787, "Bind: ") + key_name(key);
        if (ImGui::Button(label.c_str(), ImVec2(-1.f, 30.f)))
            begin_value_bind_capture(module.id, setting.id);
        ImGui::TextDisabled(SV_STR(SV_0CE66E2AB29DDD54, "Esc cancels; Backspace clears the bind"));
    } else if (setting.type == SV_STR(SV_4DFD00155697EA79, "boolean")) {
        bool enabled = setting.value == SV_STR(SV_9A91E94F6DD7E359, "true");
        render_checkbox_label(SV_STR(SV_ACD81F07989E7AF8, "Enabled"), enabled);
        if (enabled != (setting.value == SV_STR(SV_9A91E94F6DD7E359, "true")))
            queue_value(module, setting, enabled ? SV_STR(SV_9A91E94F6DD7E359, "true") : SV_STR(SV_A5FEA6BD65D8D5BF, "false"));
    } else if (setting.type == SV_STR(SV_CC5F642EAA76DBF9, "number")) {
        float number = static_cast<float>(std::strtod(
                setting.value.c_str(), nullptr));
        char format[32]{};
        _snprintf_s(format, sizeof(format), _TRUNCATE, SV_STR(SV_50DCC1C9AFFDCA89, "%%.%df%s"),
                std::max(0, std::min(setting.decimals, 8)),
                setting.unit.c_str());
        SliderResult slide = menu_slider(SV_STR(SV_1A23C4E7EAD584FD, "##number"),
                SV_STR(SV_8C4D649A44E15777, ""), &number, static_cast<float>(setting.minimum),
                static_cast<float>(setting.maximum), format);
        if (slide.changed) {
            double next = quantize(number, setting);
            setting.value = format_number(next, setting.decimals);
        }
        if (slide.finished) queue_value(module, setting, setting.value);
    } else if (setting.type == SV_STR(SV_2D47078D80D9411D, "range")) {
        float lower = static_cast<float>(setting.lower);
        float upper = static_cast<float>(setting.upper);
        char format[32]{};
        _snprintf_s(format, sizeof(format), _TRUNCATE, SV_STR(SV_50DCC1C9AFFDCA89, "%%.%df%s"),
                std::max(0, std::min(setting.decimals, 8)),
                setting.unit.c_str());
        SliderResult lower_slide = menu_slider(SV_STR(SV_F3BA8B6915599A89, "##lower"),
                SV_STR(SV_080FF942E275EFA8, "Minimum"), &lower,
                static_cast<float>(setting.minimum), upper, format);
        bool lower_changed = lower_slide.changed;
        bool lower_done = lower_slide.finished;
        ImGui::Dummy(ImVec2(1.f, 4.f));
        SliderResult upper_slide = menu_slider(SV_STR(SV_4388764923029AC6, "##upper"),
                SV_STR(SV_9D63E6ADDCD0A0D7, "Maximum"), &upper,
                lower, static_cast<float>(setting.maximum), format);
        bool upper_changed = upper_slide.changed;
        bool upper_done = upper_slide.finished;
        if (lower_changed || upper_changed) {
            setting.lower = quantize(lower, setting);
            setting.upper = quantize(upper, setting);
            if (setting.lower > setting.upper) setting.lower = setting.upper;
            setting.value = format_number(setting.lower, setting.decimals)
                    + SV_STR(SV_C04C1EC13A261F68, ",") + format_number(setting.upper, setting.decimals);
        }
        if (lower_done || upper_done)
            queue_value(module, setting, setting.value);
    } else if (setting.type == SV_STR(SV_2351581F3BB712C8, "mode")) {
        std::string preview = setting.value;
        auto preview_label = setting.option_labels.find(setting.value);
        if (preview_label != setting.option_labels.end())
            preview = preview_label->second;
        ImDrawList *combo_draw = ImGui::GetWindowDrawList();
        const bool combo_open = ImGui::BeginCombo(
                SV_STR(SV_1D223686F9F16093, "##mode"),
                SV_STR(SV_8C4D649A44E15777, ""));
        const ImVec2 combo_min = ImGui::GetItemRectMin();
        draw_text_shadow(combo_draw,
                combo_min + ImGui::GetStyle().FramePadding,
                ImGui::GetColorU32(ImGuiCol_Text), preview.c_str());
        if (combo_open) {
            for (const std::string &option : setting.options) {
                std::string label = option;
                auto found = setting.option_labels.find(option);
                if (found != setting.option_labels.end()) label = found->second;
                bool selected = option == setting.value;
                ImGui::PushID(option.c_str());
                if (ImGui::Selectable(SV_STR(SV_8C4D649A44E15777, ""),
                        selected, ImGuiSelectableFlags_SpanAvailWidth,
                        ImVec2(0.f, ImGui::GetTextLineHeight())))
                    queue_value(module, setting, option);
                draw_text_shadow(ImGui::GetWindowDrawList(),
                        ImGui::GetItemRectMin(), ImGui::GetColorU32(ImGuiCol_Text),
                        label.c_str());
                if (selected) ImGui::SetItemDefaultFocus();
                ImGui::PopID();
            }
            ImGui::EndCombo();
        }
    } else if (setting.type == SV_STR(SV_60C175CA12488454, "color")) {
        float first[3]{1.f, 1.f, 1.f};
        parse_hex_color(setting.color, first);
        if (ImGui::ColorEdit3(SV_STR(SV_8BDCB38ABD916A0F, "##color"), first,
                ImGuiColorEditFlags_NoInputs | ImGuiColorEditFlags_NoLabel))
            setting.color = color_hex(first);
        bool color_done = ImGui::IsItemDeactivatedAfterEdit();
        float alpha_value = static_cast<float>(setting.alpha);
        SliderResult alpha_slide = menu_slider(SV_STR(SV_40CA32D11C6A1EF8, "##alpha"),
                SV_STR(SV_EAF0E21DCEF978AB, "Alpha"), &alpha_value, 0.f, 255.f, SV_STR(SV_471801AEE3EDE799, "%.0f"));
        bool alpha_done = alpha_slide.finished;
        setting.alpha = static_cast<int>(alpha_value + 0.5f);
        const char *mode = setting.fade ? SV_STR(SV_5708C7CE404FF94D, "Fade")
                : setting.rainbow ? SV_STR(SV_13E7548BCF2C12C3, "Rainbow") : SV_STR(SV_666F8FC976EEC504, "Fixed");
        if (ImGui::BeginCombo(SV_STR(SV_9AC9F97AEA93F407, "##effect"), mode)) {
            if (ImGui::Selectable(SV_STR(SV_666F8FC976EEC504, "Fixed"), !setting.fade && !setting.rainbow)) {
                setting.fade = false;
                setting.rainbow = false;
                queue_value(module, setting, color_serialized(setting));
            }
            if (!setting.no_rainbow
                    && ImGui::Selectable(SV_STR(SV_13E7548BCF2C12C3, "Rainbow"), setting.rainbow)) {
                setting.fade = false;
                setting.rainbow = true;
                queue_value(module, setting, color_serialized(setting));
            }
            if (!setting.no_fade
                    && ImGui::Selectable(SV_STR(SV_5708C7CE404FF94D, "Fade"), setting.fade)) {
                setting.fade = true;
                setting.rainbow = false;
                queue_value(module, setting, color_serialized(setting));
            }
            ImGui::EndCombo();
        }
        bool fade_done = false;
        if (setting.fade && !setting.no_fade) {
            float second[3]{1.f, 1.f, 1.f};
            parse_hex_color(setting.fade_color, second);
            if (ImGui::ColorEdit3(SV_STR(SV_09794C18C1C7F32C, "##fade"), second,
                    ImGuiColorEditFlags_NoInputs | ImGuiColorEditFlags_NoLabel))
                setting.fade_color = color_hex(second);
            fade_done = ImGui::IsItemDeactivatedAfterEdit();
        }
        if (color_done || alpha_done || fade_done)
            queue_value(module, setting, color_serialized(setting));
    } else if (setting.type == SV_STR(SV_6AD981CC4B9E110E, "flags")
            || setting.type == SV_STR(SV_5CB8522F1D48346D, "player-flags")) {
        std::string flags = setting.value;
        if (flags.size() < setting.options.size())
            flags.resize(setting.options.size(), '0');
        for (size_t index = 0; index < setting.options.size(); ++index) {
            bool selected = flags[index] == '1';
            render_checkbox_label(setting.options[index].c_str(), selected);
            if (selected != (flags[index] == '1')) {
                flags[index] = selected ? '1' : '0';
                queue_value(module, setting, flags);
            }
        }
    } else {
        const std::string key = module.id + SV_STR(SV_8181E3BA16810C79, "\n") + setting.id;
        TextBuffer &edit = g_text_buffers[key];
        size_t capacity = (setting.type == SV_STR(SV_23AA0DEDF1CBB41A, "text")) ? 2049u : 16385u;
        if (edit.bytes.empty()) {
            edit.bytes.resize(capacity, 0);
            strncpy_s(edit.bytes.data(), edit.bytes.size(),
                    setting.value.c_str(), _TRUNCATE);
            edit.server_value = setting.value;
        } else if (edit.pending_value.empty()
                && edit.server_value != setting.value) {
            std::fill(edit.bytes.begin(), edit.bytes.end(), '\0');
            strncpy_s(edit.bytes.data(), edit.bytes.size(),
                    setting.value.c_str(), _TRUNCATE);
            edit.server_value = setting.value;
        } else if (!edit.pending_value.empty()
                && edit.pending_value == setting.value) {
            edit.pending_value.clear();
            edit.server_value = setting.value;
        }
        const bool single_line = setting.type == SV_STR(SV_23AA0DEDF1CBB41A, "text");
        const bool autocomplete = !setting.suggestions.empty()
                && (single_line || setting.type == SV_STR(SV_BA7FA36853B641DB, "player-list"));
        TextAutocompleteContext autocomplete_context;
        autocomplete_context.suggestions = autocomplete ? &setting.suggestions : nullptr;
        autocomplete_context.multiple = !single_line;
        const ImGuiInputTextFlags input_flags = autocomplete
                ? ImGuiInputTextFlags_CallbackCompletion
                : ImGuiInputTextFlags_None;
        const ImGuiID input_id = ImGui::GetID(SV_STR(SV_741760C58DC3A53E, "##text"));
        bool changed;
        if (single_line) {
            changed = ImGui::InputText(SV_STR(SV_741760C58DC3A53E, "##text"), edit.bytes.data(),
                    edit.bytes.size(), input_flags,
                    autocomplete ? autocomplete_input_callback : nullptr,
                    autocomplete ? &autocomplete_context : nullptr);
        } else {
            changed = ImGui::InputTextMultiline(SV_STR(SV_741760C58DC3A53E, "##text"), edit.bytes.data(),
                    edit.bytes.size(), ImVec2(-1.f, 78.f), input_flags,
                    autocomplete ? autocomplete_input_callback : nullptr,
                    autocomplete ? &autocomplete_context : nullptr);
        }
        changed = changed || autocomplete_context.completed;
        const ImVec2 input_min = ImGui::GetItemRectMin();
        const ImVec2 input_max = ImGui::GetItemRectMax();
        const bool input_active = ImGui::IsItemActive();
        const bool input_deactivated = ImGui::IsItemDeactivatedAfterEdit();
        if (autocomplete) {
            render_inline_completion(input_id, input_min, input_max,
                    edit.bytes.data(), !single_line, setting.suggestions,
                    input_active);
        }
        if (changed) {
            setting.value = edit.bytes.data();
            edit.pending_value = setting.value;
        }
        if (input_deactivated) {
            edit.pending_value = edit.bytes.data();
            queue_value(module, setting, edit.pending_value);
        }
    }

    menu_card_end();
    ImGui::PopID();
}

static const char *category_icon(int category_id) {
    switch (category_id) {
        case 122: return ICON_FA_CROSSHAIRS;
        case 123: return ICON_FA_GLOBE;
        case 124: return ICON_FA_SHIELD_ALT;
        case 125: return ICON_FA_EYE;
        case 126: return ICON_FA_TH_LARGE;
        case 144: return ICON_FA_COG;
        default: return ICON_FA_TH_LARGE;
    }
}

static bool bottom_tab(const char *label, const char *icon, ImFont *icon_font,
        bool selected, float width) {
    ImVec2 start = ImGui::GetCursorScreenPos();
    ImGui::InvisibleButton(label, ImVec2(width, 60.f));
    bool pressed = ImGui::IsItemClicked();
    ImDrawList *draw = ImGui::GetWindowDrawList();
    if (selected)
        draw->AddRectFilled(start, start + ImVec2(width, 60.f),
                IM_COL32(48, 48, 48, 255), 10.f);
    ImVec2 size = ImGui::CalcTextSize(label);
    ImVec2 icon_size = icon_font != nullptr
            ? icon_font->CalcTextSizeA(26.f, FLT_MAX, 0.f, icon)
            : ImGui::CalcTextSize(SV_STR(SV_0A4EB44BF700C513, "*"));
    if (icon_font != nullptr) {
        draw->AddText(icon_font, 26.f,
                start + ImVec2((width - icon_size.x) * 0.5f, 5.f),
                selected ? IM_COL32(230, 230, 230, 255)
                         : IM_COL32(127, 127, 127, 255), icon);
    } else {
        draw->AddCircleFilled(start + ImVec2(width * 0.5f, 19.f), 5.f,
                selected ? IM_COL32(230, 230, 230, 255)
                         : IM_COL32(127, 127, 127, 255));
    }
    draw_text_shadow(draw, start + ImVec2((width - size.x) * 0.5f, 34.f),
            selected ? IM_COL32(230, 230, 230, 255)
                     : IM_COL32(127, 127, 127, 255), label);
    return pressed;
}

static bool sidebar_row(const char *id, const char *label, bool selected,
        const bool *indicator) {
    ImVec2 start = ImGui::GetCursorScreenPos();
    ImGui::PushID(id);
    ImGui::InvisibleButton(SV_STR(SV_92F4CF6166EACD9B, "##module"), ImVec2(176.f, 28.f));
    bool pressed = ImGui::IsItemClicked();
    bool hovered = ImGui::IsItemHovered();
    bool held = ImGui::IsItemActive();
    ImDrawList *draw = ImGui::GetWindowDrawList();
    ImFont *font = g_font_regular != nullptr ? g_font_regular : ImGui::GetFont();
    static std::map<ImGuiID, float> presses;
    float &press = presses[ImGui::GetItemID()];
    const float dt = ImGui::GetIO().DeltaTime;
    press = held ? ImClamp(press + dt / .08f, 0.f, 1.f)
                 : ImMax(0.f, press - dt / .5f);
    const float squish = press * 5.f;
    if (selected) {
        draw->AddLine(ImVec2(start.x - 13.f, start.y + 15.f),
                ImVec2(start.x - 3.f, start.y + 15.f),
                IM_COL32(0, 0, 0, 185), 2.f);
        draw->AddLine(ImVec2(start.x - 14.f, start.y + 14.f),
                ImVec2(start.x - 4.f, start.y + 14.f), kText, 1.f);
    }
    const ImVec4 label_clip(start.x, start.y, start.x + 156.f, start.y + 28.f);
    draw_text_shadow(draw,
            start + ImVec2((hovered ? 2.f : 0.f) + squish,
                    (28.f - 16.f) * .5f - 1.f),
            selected ? kText : (indicator != nullptr && *indicator
                    ? kText : hovered ? kMuted : IM_COL32(69, 69, 69, 255)),
            label, nullptr, font, 16.f, &label_clip);
    if (indicator != nullptr && *indicator) {
        draw->AddRectFilled(start + ImVec2(169.f, 12.f),
                start + ImVec2(175.f, 18.f), IM_COL32(0, 0, 0, 190), 1.f);
        draw->AddRectFilled(start + ImVec2(168.f, 11.f),
                start + ImVec2(174.f, 17.f), kText, 1.f);
    }
    ImGui::PopID();
    return pressed;
}

static bool sidebar_module(MenuModule &module, bool selected) {
    const bool enabled = module.enabled;
    return sidebar_row(module.id.c_str(), module.label.c_str(), selected,
            &enabled);
}

static void begin_bind_capture(const std::string &module) {
    {
        std::lock_guard<std::mutex> lock(g_capture_mutex);
        g_capture_module = module;
        g_capture_value.clear();
    }
    arm_bind_capture(1);
}

static bool editorial_pill(const char *id, const char *label, bool active,
        const ImVec2 &size) {
    ImVec2 start = ImGui::GetCursorScreenPos();
    ImGui::InvisibleButton(id, size);
    const bool pressed = ImGui::IsItemClicked();
    const bool hovered = ImGui::IsItemHovered();
    const bool held = ImGui::IsItemActive();
    ImDrawList *draw = ImGui::GetWindowDrawList();
    const ImU32 edge = hovered ? kText : kMuted;
    static std::map<ImGuiID, float> presses;
    float &press = presses[ImGui::GetItemID()];
    const float dt = ImGui::GetIO().DeltaTime;
    press = ImClamp(press + (held ? dt : -dt) / .09f, 0.f, 1.f);
    const float drop = 7.f * press;
    const float rest = ImMax(1.f, 7.f - drop);
    const ImVec2 top = start + ImVec2(0.f, drop);
    const ImVec2 bottom = top + size;
    const float rounding = 9.f;
    draw->AddRectFilled(top + ImVec2(0.f, 3.f),
            bottom + ImVec2(0.f, rest),
            active ? IM_COL32(104, 104, 104, 255)
                    : IM_COL32(14, 14, 14, 255), rounding);
    if (active) {
        draw->AddRectFilled(top, bottom, kText, rounding);
    } else {
        draw->AddRectFilled(top, bottom, IM_COL32(31, 31, 31, 255), rounding);
        draw->AddLine(top + ImVec2(rounding * .7f, 1.f),
                ImVec2(bottom.x - rounding * .7f, top.y + 1.f),
                hovered ? IM_COL32(82, 82, 82, 255)
                        : IM_COL32(58, 58, 58, 255), 1.f);
        if (hovered)
            draw->AddRect(top, bottom, edge, rounding, 0, 1.f);
    }
    ImFont *font = g_font_regular != nullptr ? g_font_regular : ImGui::GetFont();
    const float font_size = 14.f;
    ImVec2 text_size = calc_visible_text_size(font, font_size, label);
    const ImVec2 text_pos = top + ImVec2((size.x - text_size.x) * 0.5f,
            (size.y - font_size) * 0.5f - 1.f);
    if (active) {
        add_visible_text(draw, font, font_size, text_pos, kBackground, label);
    } else {
        draw_text_shadow(draw, text_pos, hovered ? kText : kMuted, label,
                nullptr, font, font_size);
    }
    return pressed;
}

static bool editorial_player_row(const char *id, const std::string &name,
        bool selected, const ImVec2 &size) {
    const ImVec2 start = ImGui::GetCursorScreenPos();
    ImGui::InvisibleButton(id, size);
    const bool pressed = ImGui::IsItemClicked();
    const bool hovered = ImGui::IsItemHovered();
    ImDrawList *draw = ImGui::GetWindowDrawList();
    const ImU32 fill = selected ? IM_COL32(244, 244, 244, 255)
            : hovered ? IM_COL32(25, 25, 25, 255)
                    : IM_COL32(16, 16, 16, 255);
    const ImU32 edge = selected ? kText
            : hovered ? IM_COL32(80, 80, 80, 255) : kCardBorder;
    draw->AddRectFilled(start, start + size, fill, 6.f);
    draw->AddRect(start, start + size, edge, 6.f);

    const float head_size = std::max(18.f, size.y - 8.f);
    const ImVec2 head_min = start + ImVec2(4.f, (size.y - head_size) * .5f);
    const ImVec2 head_max = head_min + ImVec2(head_size, head_size);
    ImTextureID texture = player_head_texture(name);
    if (texture != nullptr) {
        draw->AddImage(reinterpret_cast<ImTextureID>(texture), head_min,
                head_max, ImVec2(0.f, 0.f), ImVec2(1.f, 1.f));
    } else {
        draw->AddRectFilled(head_min, head_max,
                selected ? IM_COL32(36, 36, 36, 255)
                        : IM_COL32(45, 45, 45, 255), 3.f);
        if (!name.empty()) {
            char initial[2]{static_cast<char>(std::toupper(
                    static_cast<unsigned char>(name[0]))), 0};
            ImFont *fallback_font = g_font_bold != nullptr
                    ? g_font_bold : ImGui::GetFont();
            const ImVec2 initial_size = fallback_font->CalcTextSizeA(
                    13.f, FLT_MAX, 0.f, initial);
            draw->AddText(fallback_font, 13.f,
                    head_min + (ImVec2(head_size, head_size) - initial_size) * .5f,
                    IM_COL32(210, 210, 210, 255), initial);
        }
    }
    ImFont *font = g_font_medium != nullptr ? g_font_medium : ImGui::GetFont();
    const float font_size = 14.f;
    const ImVec2 text_size = font->CalcTextSizeA(font_size, FLT_MAX, 0.f,
            name.c_str());
    draw->AddText(font, font_size,
            ImVec2(head_max.x + 8.f, start.y + (size.y - text_size.y) * .5f),
            selected ? kBackground : (hovered ? kText : IM_COL32(190, 190, 190, 255)),
            name.c_str());
    return pressed;
}

static ImU32 color_with_alpha(ImU32 color, float alpha) {
    const unsigned value = static_cast<unsigned>(ImClamp(alpha, 0.f, 1.f)
            * static_cast<float>((color >> 24) & 0xffu));
    return (color & 0x00ffffffu) | (value << 24);
}

static bool invsee_ranking_row(const char *id,
        const MenuValue::InvseePlayer &player, int rank, bool selected,
        bool tied, float tie_alpha, const ImVec2 &size) {
    const ImVec2 start = ImGui::GetCursorScreenPos();
    ImGui::InvisibleButton(id, size);
    const bool pressed = ImGui::IsItemClicked();
    const bool hovered = ImGui::IsItemHovered();
    ImDrawList *draw = ImGui::GetWindowDrawList();
    const ImU32 fill = selected ? IM_COL32(29, 29, 29, 255)
            : hovered ? IM_COL32(22, 22, 22, 255)
                      : IM_COL32(15, 15, 15, 255);
    const ImU32 edge = selected ? IM_COL32(112, 112, 112, 255)
            : hovered ? IM_COL32(58, 58, 58, 255) : kCardBorder;
    draw->AddRectFilled(start, start + size, fill, 7.f);
    draw->AddRect(start, start + size, edge, 7.f, 0, 1.f);
    if (selected) {
        draw->AddRectFilled(start + ImVec2(0.f, 7.f),
                start + ImVec2(2.f, size.y - 7.f), kText, 1.f);
    }

    char rank_text[8]{};
    _snprintf_s(rank_text, sizeof(rank_text), _TRUNCATE,
            SV_STR(SV_89A36DA992D8B31C, "%02d"), rank);
    ImFont *micro = g_font_micro != nullptr ? g_font_micro : ImGui::GetFont();
    ImFont *regular = g_font_medium != nullptr ? g_font_medium : ImGui::GetFont();
    draw->AddText(micro, 10.f, start + ImVec2(8.f, 17.f),
            selected ? kText : IM_COL32(91, 91, 91, 255), rank_text);

    const float head_size = size.y - 10.f;
    const ImVec2 head_min = start + ImVec2(31.f, 5.f);
    const ImVec2 head_max = head_min + ImVec2(head_size, head_size);
    ImTextureID texture = player_head_texture(player.name);
    if (texture != nullptr) {
        draw->AddImage(reinterpret_cast<ImTextureID>(texture), head_min,
                head_max, ImVec2(0.f, 0.f), ImVec2(1.f, 1.f));
    } else {
        draw->AddRectFilled(head_min, head_max, IM_COL32(38, 38, 38, 255), 4.f);
        if (!player.name.empty()) {
            char initial[2]{static_cast<char>(std::toupper(
                    static_cast<unsigned char>(player.name[0]))), 0};
            const ImVec2 initial_size = regular->CalcTextSizeA(
                    12.f, FLT_MAX, 0.f, initial);
            draw->AddText(regular, 12.f,
                    head_min + (ImVec2(head_size, head_size) - initial_size) * .5f,
                    kMuted, initial);
        }
    }

    const float name_x = head_max.x + 8.f;
    const float set_area = 54.f;
    const ImVec4 name_clip(name_x, start.y,
            start.x + size.x - set_area - (tied ? 31.f : 0.f),
            start.y + size.y);
    draw->AddText(regular, 13.f, ImVec2(name_x, start.y + 14.f),
            selected ? kText : IM_COL32(204, 204, 204, 255),
            player.name.c_str(), nullptr, 0.f, &name_clip);

    if (tied && tie_alpha > 0.f) {
        const ImVec2 chip_min(start.x + size.x - 73.f, start.y + 14.f);
        const ImVec2 chip_max(chip_min.x + 27.f, chip_min.y + 14.f);
        draw->AddRectFilled(chip_min, chip_max,
                color_with_alpha(IM_COL32(47, 47, 47, 255), tie_alpha), 7.f);
        draw->AddText(micro, 8.f, chip_min + ImVec2(5.f, 3.f),
                color_with_alpha(kMuted, tie_alpha),
                SV_STR(SV_08031F95E575BE56, "TIE"));
    }

    char sets[24]{};
    _snprintf_s(sets, sizeof(sets), _TRUNCATE,
            player.sets == 1 ? SV_STR(SV_B3222EA6DE30CE55, "1 SET")
                             : SV_STR(SV_8BE48F72F97519B6, "%d SETS"),
            player.sets);
    constexpr float sets_font_size = 11.f;
    const ImVec2 sets_size = micro->CalcTextSizeA(
            sets_font_size, FLT_MAX, 0.f, sets);
    draw->AddText(micro, sets_font_size,
            ImVec2(start.x + size.x - sets_size.x - 8.f, start.y + 14.f),
            selected ? kText : IM_COL32(194, 194, 194, 255), sets);
    return pressed;
}

static void draw_invsee_body_placeholder(ImDrawList *draw,
        const ImVec2 &minimum, const ImVec2 &maximum) {
    const float center = (minimum.x + maximum.x) * .5f;
    const float height = maximum.y - minimum.y;
    const ImU32 fill = IM_COL32(38, 38, 38, 255);
    draw->AddRectFilled(ImVec2(center - height * .12f, minimum.y + height * .08f),
            ImVec2(center + height * .12f, minimum.y + height * .29f), fill, 5.f);
    draw->AddRectFilled(ImVec2(center - height * .17f, minimum.y + height * .32f),
            ImVec2(center + height * .17f, minimum.y + height * .65f), fill, 5.f);
    draw->AddRectFilled(ImVec2(center - height * .16f, minimum.y + height * .68f),
            ImVec2(center - height * .02f, maximum.y - height * .06f), fill, 4.f);
    draw->AddRectFilled(ImVec2(center + height * .02f, minimum.y + height * .68f),
            ImVec2(center + height * .16f, maximum.y - height * .06f), fill, 4.f);
}

struct ModelPoint {
    float x;
    float y;
    float z;
};

struct SkinRegion {
    float x0;
    float y0;
    float x1;
    float y1;
};

struct ModelFace {
    std::array<ModelPoint, 4> corners;
    SkinRegion region;
    float shade;
    float depth;
};

static ImVec2 project_model_point(const ModelPoint &point, float angle,
        const ImVec2 &origin, float scale) {
    const float cosine = std::cos(angle);
    const float sine = std::sin(angle);
    const float rotated_x = point.x * cosine - point.z * sine;
    const float rotated_z = point.x * sine + point.z * cosine;
    constexpr float pitch = .18f;
    // An affine projection keeps adjacent cuboids joined. The previous
    // per-vertex perspective scale pulled heads, arms and legs apart as their
    // depth changed during rotation.
    return ImVec2(origin.x + rotated_x * scale,
            origin.y - (point.y + rotated_z * pitch) * scale);
}

static float model_depth(const std::array<ModelPoint, 4> &corners,
        float angle) {
    const float cosine = std::cos(angle);
    const float sine = std::sin(angle);
    float depth = 0.f;
    for (const ModelPoint &point : corners)
        depth += point.x * sine + point.z * cosine - point.y * .18f;
    return depth * .25f;
}

static void append_model_cuboid(std::vector<ModelFace> &faces,
        float x0, float x1, float y0, float y1, float z0, float z1,
        const SkinRegion &right, const SkinRegion &left,
        const SkinRegion &front, const SkinRegion &back,
        const SkinRegion &top, const SkinRegion &bottom) {
    faces.push_back(ModelFace{{{{x0, y1, z0}, {x1, y1, z0},
            {x1, y0, z0}, {x0, y0, z0}}}, front, 1.00f, 0.f});
    faces.push_back(ModelFace{{{{x1, y1, z1}, {x0, y1, z1},
            {x0, y0, z1}, {x1, y0, z1}}}, back, .70f, 0.f});
    faces.push_back(ModelFace{{{{x0, y1, z1}, {x0, y1, z0},
            {x0, y0, z0}, {x0, y0, z1}}}, left, .82f, 0.f});
    faces.push_back(ModelFace{{{{x1, y1, z0}, {x1, y1, z1},
            {x1, y0, z1}, {x1, y0, z0}}}, right, .92f, 0.f});
    faces.push_back(ModelFace{{{{x0, y1, z1}, {x1, y1, z1},
            {x1, y1, z0}, {x0, y1, z0}}}, top, 1.08f, 0.f});
    faces.push_back(ModelFace{{{{x0, y0, z0}, {x1, y0, z0},
            {x1, y0, z1}, {x0, y0, z1}}}, bottom, .56f, 0.f});
}

static ImVec2 skin_uv(float x, float y, float texture_width,
        float texture_height) {
    // WIC's first row is the top row, and both the OpenGL and D3D11 ImGui
    // backends use that same row as V=0 for these uploaded textures. Flipping
    // V here remaps the head to the leg/arm regions of a standard skin.
    return ImVec2(x / texture_width, y / texture_height);
}

static void draw_model_face(ImDrawList *draw, ImTextureID texture,
        const ModelFace &face, const ImVec2 &origin, float scale, float angle) {
    ImVec2 points[4];
    for (size_t index = 0; index < face.corners.size(); ++index)
        points[index] = project_model_point(face.corners[index], angle,
                origin, scale);
    float signed_area = 0.f;
    for (size_t index = 0; index < 4u; ++index) {
        const ImVec2 &current = points[index];
        const ImVec2 &next = points[(index + 1u) % 4u];
        signed_area += current.x * next.y - next.x * current.y;
    }
    // Only the three surfaces facing the camera belong in the final model.
    // Back faces showing through transparent overlay pixels made limbs look
    // doubled and produced the dark fragments visible in the old preview.
    if (signed_area <= .01f) return;
    // Sample inside each atlas rectangle.  Sampling exactly on a boundary
    // lets the linear sampler pull a transparent pixel from the neighboring
    // face, which creates dark seams and soft halos around pixel skins.
    constexpr float atlas_inset = .5f;
    const ImVec2 uv_top_left = skin_uv(face.region.x0 + atlas_inset,
            face.region.y0 + atlas_inset, 64.f, 64.f);
    const ImVec2 uv_top_right = skin_uv(face.region.x1 - atlas_inset,
            face.region.y0 + atlas_inset, 64.f, 64.f);
    const ImVec2 uv_bottom_right = skin_uv(face.region.x1 - atlas_inset,
            face.region.y1 - atlas_inset, 64.f, 64.f);
    const ImVec2 uv_bottom_left = skin_uv(face.region.x0 + atlas_inset,
            face.region.y1 - atlas_inset, 64.f, 64.f);
    const ImU32 color = ImGui::ColorConvertFloat4ToU32(
            ImVec4(face.shade, face.shade, face.shade, 1.f));
    draw->AddImageQuad(texture, points[0], points[1], points[2], points[3],
            uv_top_left, uv_top_right, uv_bottom_right, uv_bottom_left, color);
}

static void render_invsee_player_model(ImDrawList *draw, ImTextureID texture,
        const ImVec2 &minimum, const ImVec2 &maximum, float angle) {
    const float available_width = maximum.x - minimum.x;
    const float available_height = maximum.y - minimum.y;
    const float scale = std::min((available_width - 18.f) / 17.f,
            (available_height - 18.f) / 34.f);
    if (scale <= 0.f) return;
    const ImVec2 origin((minimum.x + maximum.x) * .5f,
            maximum.y - 9.f);

    const SkinRegion head_right{0.f, 8.f, 8.f, 16.f};
    const SkinRegion head_left{16.f, 8.f, 24.f, 16.f};
    const SkinRegion head_front{8.f, 8.f, 16.f, 16.f};
    const SkinRegion head_back{24.f, 8.f, 32.f, 16.f};
    const SkinRegion head_top{8.f, 0.f, 16.f, 8.f};
    const SkinRegion head_bottom{16.f, 0.f, 24.f, 8.f};
    const SkinRegion body_right{16.f, 20.f, 20.f, 32.f};
    const SkinRegion body_left{28.f, 20.f, 32.f, 32.f};
    const SkinRegion body_front{20.f, 20.f, 28.f, 32.f};
    const SkinRegion body_back{32.f, 20.f, 40.f, 32.f};
    const SkinRegion body_top{20.f, 16.f, 28.f, 20.f};
    const SkinRegion body_bottom{28.f, 16.f, 36.f, 20.f};
    const SkinRegion arm_right{40.f, 20.f, 44.f, 32.f};
    const SkinRegion arm_left{48.f, 20.f, 52.f, 32.f};
    const SkinRegion arm_front{44.f, 20.f, 48.f, 32.f};
    const SkinRegion arm_back{52.f, 20.f, 56.f, 32.f};
    const SkinRegion arm_top{44.f, 16.f, 48.f, 20.f};
    const SkinRegion arm_bottom{48.f, 16.f, 52.f, 20.f};
    const SkinRegion leg_right{0.f, 20.f, 4.f, 32.f};
    const SkinRegion leg_left{8.f, 20.f, 12.f, 32.f};
    const SkinRegion leg_front{4.f, 20.f, 8.f, 32.f};
    const SkinRegion leg_back{12.f, 20.f, 16.f, 32.f};
    const SkinRegion leg_top{4.f, 16.f, 8.f, 20.f};
    const SkinRegion leg_bottom{8.f, 16.f, 12.f, 20.f};
    // The second leg occupies the 16..32 strip in the lower half of a
    // 64x64 skin. Keep every side four pixels wide; using the adjacent arm
    // strip here stretches the leg and makes that limb look torn apart.
    const SkinRegion left_leg_right{16.f, 52.f, 20.f, 64.f};
    const SkinRegion left_leg_left{24.f, 52.f, 28.f, 64.f};
    const SkinRegion left_leg_front{20.f, 52.f, 24.f, 64.f};
    const SkinRegion left_leg_back{28.f, 52.f, 32.f, 64.f};
    const SkinRegion left_leg_top{20.f, 48.f, 24.f, 52.f};
    const SkinRegion left_leg_bottom{24.f, 48.f, 28.f, 52.f};
    const SkinRegion left_arm_right{32.f, 52.f, 36.f, 64.f};
    const SkinRegion left_arm_left{40.f, 52.f, 44.f, 64.f};
    const SkinRegion left_arm_front{36.f, 52.f, 40.f, 64.f};
    const SkinRegion left_arm_back{44.f, 52.f, 48.f, 64.f};
    const SkinRegion left_arm_top{36.f, 48.f, 40.f, 52.f};
    const SkinRegion left_arm_bottom{40.f, 48.f, 44.f, 52.f};

    // The second 32x16 strip in a 64x64 skin is the optional outer layer.
    // Rendering it as slightly larger cuboids preserves hats, hair, jackets
    // and sleeves instead of flattening the player to the base layer.
    const SkinRegion head_overlay_right{32.f, 8.f, 40.f, 16.f};
    const SkinRegion head_overlay_left{48.f, 8.f, 56.f, 16.f};
    const SkinRegion head_overlay_front{40.f, 8.f, 48.f, 16.f};
    const SkinRegion head_overlay_back{56.f, 8.f, 64.f, 16.f};
    const SkinRegion head_overlay_top{40.f, 0.f, 48.f, 8.f};
    const SkinRegion head_overlay_bottom{48.f, 0.f, 56.f, 8.f};
    const SkinRegion body_overlay_right{16.f, 36.f, 20.f, 48.f};
    const SkinRegion body_overlay_left{28.f, 36.f, 32.f, 48.f};
    const SkinRegion body_overlay_front{20.f, 36.f, 28.f, 48.f};
    const SkinRegion body_overlay_back{32.f, 36.f, 40.f, 48.f};
    const SkinRegion body_overlay_top{20.f, 32.f, 28.f, 36.f};
    const SkinRegion body_overlay_bottom{28.f, 32.f, 36.f, 36.f};
    const SkinRegion arm_overlay_right{40.f, 36.f, 44.f, 48.f};
    const SkinRegion arm_overlay_left{48.f, 36.f, 52.f, 48.f};
    const SkinRegion arm_overlay_front{44.f, 36.f, 48.f, 48.f};
    const SkinRegion arm_overlay_back{52.f, 36.f, 56.f, 48.f};
    const SkinRegion arm_overlay_top{44.f, 32.f, 48.f, 36.f};
    const SkinRegion arm_overlay_bottom{48.f, 32.f, 52.f, 36.f};
    const SkinRegion leg_overlay_right{0.f, 36.f, 4.f, 48.f};
    const SkinRegion leg_overlay_left{8.f, 36.f, 12.f, 48.f};
    const SkinRegion leg_overlay_front{4.f, 36.f, 8.f, 48.f};
    const SkinRegion leg_overlay_back{12.f, 36.f, 16.f, 48.f};
    const SkinRegion leg_overlay_top{4.f, 32.f, 8.f, 36.f};
    const SkinRegion leg_overlay_bottom{8.f, 32.f, 12.f, 36.f};
    const SkinRegion left_leg_overlay_right{0.f, 52.f, 4.f, 64.f};
    const SkinRegion left_leg_overlay_left{8.f, 52.f, 12.f, 64.f};
    const SkinRegion left_leg_overlay_front{4.f, 52.f, 8.f, 64.f};
    const SkinRegion left_leg_overlay_back{12.f, 52.f, 16.f, 64.f};
    const SkinRegion left_leg_overlay_top{4.f, 48.f, 8.f, 52.f};
    const SkinRegion left_leg_overlay_bottom{8.f, 48.f, 12.f, 52.f};
    const SkinRegion left_arm_overlay_right{48.f, 52.f, 52.f, 64.f};
    const SkinRegion left_arm_overlay_left{56.f, 52.f, 60.f, 64.f};
    const SkinRegion left_arm_overlay_front{52.f, 52.f, 56.f, 64.f};
    const SkinRegion left_arm_overlay_back{60.f, 52.f, 64.f, 64.f};
    const SkinRegion left_arm_overlay_top{52.f, 48.f, 56.f, 52.f};
    const SkinRegion left_arm_overlay_bottom{56.f, 48.f, 60.f, 52.f};

    std::vector<ModelFace> faces;
    faces.reserve(60);
    append_model_cuboid(faces, -4.f, 4.f, 24.f, 32.f, -4.f, 4.f,
            head_right, head_left, head_front, head_back,
            head_top, head_bottom);
    append_model_cuboid(faces, -4.f, 4.f, 12.f, 24.f, -2.f, 2.f,
            body_right, body_left, body_front, body_back,
            body_top, body_bottom);
    append_model_cuboid(faces, -8.f, -4.f, 12.f, 24.f, -2.f, 2.f,
            arm_right, arm_left, arm_front, arm_back,
            arm_top, arm_bottom);
    append_model_cuboid(faces, 4.f, 8.f, 12.f, 24.f, -2.f, 2.f,
            left_arm_right, left_arm_left, left_arm_front, left_arm_back,
            left_arm_top, left_arm_bottom);
    append_model_cuboid(faces, -4.f, 0.f, 0.f, 12.f, -2.f, 2.f,
            leg_right, leg_left, leg_front, leg_back,
            leg_top, leg_bottom);
    append_model_cuboid(faces, 0.f, 4.f, 0.f, 12.f, -2.f, 2.f,
            left_leg_right, left_leg_left, left_leg_front, left_leg_back,
            left_leg_top, left_leg_bottom);

    append_model_cuboid(faces, -4.5f, 4.5f, 23.5f, 32.5f, -4.5f, 4.5f,
            head_overlay_right, head_overlay_left, head_overlay_front,
            head_overlay_back, head_overlay_top, head_overlay_bottom);
    append_model_cuboid(faces, -4.25f, 4.25f, 11.75f, 24.25f,
            -2.25f, 2.25f, body_overlay_right, body_overlay_left,
            body_overlay_front, body_overlay_back, body_overlay_top,
            body_overlay_bottom);
    append_model_cuboid(faces, -8.25f, -3.75f, 11.75f, 24.25f,
            -2.25f, 2.25f, arm_overlay_right, arm_overlay_left,
            arm_overlay_front, arm_overlay_back, arm_overlay_top,
            arm_overlay_bottom);
    append_model_cuboid(faces, 3.75f, 8.25f, 11.75f, 24.25f,
            -2.25f, 2.25f, left_arm_overlay_right,
            left_arm_overlay_left, left_arm_overlay_front,
            left_arm_overlay_back, left_arm_overlay_top,
            left_arm_overlay_bottom);
    append_model_cuboid(faces, -4.25f, .25f, -.25f, 12.25f,
            -2.25f, 2.25f, leg_overlay_right, leg_overlay_left,
            leg_overlay_front, leg_overlay_back, leg_overlay_top,
            leg_overlay_bottom);
    append_model_cuboid(faces, -.25f, 4.25f, -.25f, 12.25f,
            -2.25f, 2.25f, left_leg_overlay_right,
            left_leg_overlay_left, left_leg_overlay_front,
            left_leg_overlay_back, left_leg_overlay_top,
            left_leg_overlay_bottom);
    for (ModelFace &face : faces) face.depth = model_depth(face.corners, angle);
    std::stable_sort(faces.begin(), faces.end(),
            [](const ModelFace &left, const ModelFace &right) {
                return left.depth > right.depth;
            });
    draw->PushClipRect(minimum, maximum, true);
    for (const ModelFace &face : faces)
        draw_model_face(draw, texture, face, origin, scale, angle);
    draw->PopClipRect();
}

static void render_invsee_ranking(MenuModule &module, MenuValue &setting) {
    const float available_width = ImGui::GetContentRegionAvail().x;
    const float available_height = std::max(258.f,
            ImGui::GetContentRegionAvail().y - 4.f);
    const float ranking_width = std::min(244.f,
            std::max(210.f, available_width * .39f));
    const float gap = 12.f;
    const float detail_width = std::max(250.f,
            available_width - ranking_width - gap);

    // Queue the selected player's complete 64x64 skin before the full roster
    // so the 3D model is not held behind fifteen head downloads.
    if (!setting.value.empty()) {
        request_player_texture(setting.value, 's');
    }

    std::string signature;
    for (const MenuValue::InvseePlayer &player : setting.invsee_players) {
        signature.append(player.name).push_back(':');
        signature.append(std::to_string(player.sets)).push_back(';');
    }
    const std::string notice_key = module.id
            + SV_STR(SV_8181E3BA16810C79, "\n") + setting.id;
    static std::map<std::string, std::pair<std::string, double>> notices;
    std::pair<std::string, double> &notice = notices[notice_key];
    if (notice.first != signature) {
        notice.first = signature;
        notice.second = ImGui::GetTime();
    }
    const float tie_alpha = ImClamp(static_cast<float>(
            5.0 - (ImGui::GetTime() - notice.second)), 0.f, 1.f);

    // A tie in the first four positions deserves a single, readable alert in
    // addition to the small per-row marker.  Keep the message tied to the
    // current report signature so it fades out after the update instead of
    // covering the ranking permanently.
    std::vector<size_t> top_tie_indices;
    const size_t top_count = std::min<size_t>(4u, setting.invsee_players.size());
    for (size_t index = 0; index < top_count; ++index) {
        size_t same_sets = 0;
        for (size_t other = 0; other < top_count; ++other) {
            if (setting.invsee_players[other].sets
                    == setting.invsee_players[index].sets) {
                ++same_sets;
            }
        }
        if (same_sets > 1u) top_tie_indices.push_back(index);
    }
    std::string top_tie_message;
    if (!top_tie_indices.empty() && tie_alpha > 0.f) {
        top_tie_message = SV_STR(SV_F7EDF337008A648C, "TIED TOP 4: ");
        for (size_t offset = 0; offset < top_tie_indices.size(); ++offset) {
            if (offset != 0u) top_tie_message.append(
                    SV_STR(SV_A5D48D1B4BDBEC55, ", "));
            const size_t index = top_tie_indices[offset];
            top_tie_message.append(SV_STR(SV_14EFDFA863D1CB10, "#"));
            top_tie_message.append(std::to_string(index + 1u));
            top_tie_message.push_back(' ');
            top_tie_message.append(setting.invsee_players[index].name);
        }
    }
    const bool show_top_tie_alert = !top_tie_message.empty();

    ImGui::PushStyleColor(ImGuiCol_ChildBg, IM_COL32(13, 13, 13, 255));
    ImGui::BeginChild(SV_STR(SV_A3AE719CE6DE159A, "##invsee_ranking"),
            ImVec2(ranking_width, available_height), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    ImGui::SetCursorPos(ImVec2(12.f, 10.f));
    ImFont *micro = g_font_micro != nullptr ? g_font_micro : ImGui::GetFont();
    ImFont *regular = g_font_regular != nullptr ? g_font_regular : ImGui::GetFont();
    ImGui::PushFont(micro);
    ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kText),
            SV_STR(SV_84693D00E97C295D, "%s"),
            SV_STR(SV_F593450B37FC52A1, "INVSEE RANKING"));
    ImGui::PopFont();
    ImGui::SetCursorPos(ImVec2(12.f, 27.f));
    ImGui::PushFont(micro);
    ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
            SV_STR(SV_84693D00E97C295D, "%s"),
            setting.invsee_players.empty()
                    ? SV_STR(SV_22D77314502C2435, "WAITING FOR REPORT")
                    : SV_STR(SV_5C82D16D8B2BF74C, "LOWEST SETS FIRST"));
    ImGui::PopFont();
    if (!setting.invsee_players.empty()) {
        char count[12]{};
        _snprintf_s(count, sizeof(count), _TRUNCATE,
                SV_STR(SV_6B79DB7A575A5E7E, "%d/100"),
                static_cast<int>(setting.invsee_players.size()));
        const ImVec2 count_size = micro->CalcTextSizeA(9.f, FLT_MAX, 0.f, count);
        ImGui::GetWindowDrawList()->AddText(micro, 9.f,
                ImGui::GetWindowPos()
                        + ImVec2(ranking_width - count_size.x - 12.f, 13.f),
                kMuted, count);
    }
    if (show_top_tie_alert) {
        // Fit the alert between the ranking subtitle and the first row.  The
        // row viewport stays at its original position so existing scrolling
        // and hit targets remain unchanged.
        const ImVec2 alert_min = ImGui::GetWindowPos() + ImVec2(10.f, 32.f);
        const ImVec2 alert_max = alert_min + ImVec2(ranking_width - 20.f, 15.f);
        const ImU32 alert_fill = color_with_alpha(IM_COL32(54, 48, 34, 255),
                tie_alpha);
        const ImU32 alert_edge = color_with_alpha(IM_COL32(143, 113, 54, 255),
                tie_alpha);
        const ImU32 alert_text = color_with_alpha(IM_COL32(232, 202, 139, 255),
                tie_alpha);
        ImDrawList *ranking_draw = ImGui::GetWindowDrawList();
        ranking_draw->AddRectFilled(alert_min, alert_max, alert_fill, 6.f);
        ranking_draw->AddRect(alert_min, alert_max, alert_edge, 6.f, 0, 1.f);
        // Keep the message short enough for the compact ranking column.  We
        // draw it without ImGui's CPU fine-text clipping path because that
        // path is not safe in the headless fixture renderer.
        if (top_tie_message.size() > 48u) top_tie_message.resize(48u);
        ranking_draw->AddText(micro, 8.f,
                alert_min + ImVec2(8.f, 3.f), alert_text,
                top_tie_message.c_str());
    }
    const float rows_top = 48.f;
    ImGui::SetCursorPos(ImVec2(8.f, rows_top));
    ImGui::PushStyleVar(ImGuiStyleVar_ScrollbarSize, 5.f);
    ImGui::PushStyleVar(ImGuiStyleVar_ScrollbarRounding, 5.f);
    ImGui::BeginChild(SV_STR(SV_99F7DAC87735A635, "##invsee_rows"),
            ImVec2(ranking_width - 16.f, available_height - rows_top - 8.f),
            ImGuiChildFlags_None, setting.invsee_players.size() > 5u
                    ? ImGuiWindowFlags_AlwaysVerticalScrollbar
                    : ImGuiWindowFlags_None);
    if (setting.invsee_players.empty()) {
        ImGui::SetCursorPos(ImVec2(9.f, 18.f));
        ImGui::PushFont(regular);
        ImGui::PushStyleColor(ImGuiCol_Text, kMuted);
        ImGui::PushTextWrapPos(ranking_width - 30.f);
        ImGui::TextWrapped(SV_STR(SV_84693D00E97C295D, "%s"), setting.name.c_str());
        ImGui::PopTextWrapPos();
        ImGui::PopStyleColor();
        ImGui::PopFont();
    } else {
        for (size_t index = 0; index < setting.invsee_players.size(); ++index) {
            const MenuValue::InvseePlayer &player = setting.invsee_players[index];
            bool tied = false;
            if (index < 4u) {
                for (size_t other = 0; other < top_count; ++other) {
                    if (other != index
                            && setting.invsee_players[other].sets == player.sets) {
                        tied = true;
                        break;
                    }
                }
            }
            ImGui::PushID(static_cast<int>(index));
            const float row_width = std::max(170.f,
                    ImGui::GetContentRegionAvail().x - 2.f);
            if (invsee_ranking_row(
                    SV_STR(SV_4F08787DAD2E4C3B, "##invsee_player"), player,
                    static_cast<int>(index) + 1,
                    player.name == setting.value, tied, tie_alpha,
                    ImVec2(row_width, 40.f))) {
                queue_value(module, setting, player.name);
            }
            ImGui::PopID();
            ImGui::Dummy(ImVec2(1.f, 5.f));
        }
    }
    ImGui::EndChild();
    ImGui::PopStyleVar(2);
    ImGui::EndChild();
    ImGui::PopStyleColor();

    ImGui::SameLine(0.f, gap);
    ImGui::PushStyleColor(ImGuiCol_ChildBg, IM_COL32(13, 13, 13, 255));
    ImGui::BeginChild(SV_STR(SV_3DFCE890AEEE1861, "##invsee_detail"),
            ImVec2(detail_width, available_height), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    ImDrawList *draw = ImGui::GetWindowDrawList();
    const ImVec2 origin = ImGui::GetWindowPos();
    const MenuValue::InvseePlayer *selected = nullptr;
    int selected_rank = 0;
    for (size_t index = 0; index < setting.invsee_players.size(); ++index) {
        if (setting.invsee_players[index].name == setting.value) {
            selected = &setting.invsee_players[index];
            selected_rank = static_cast<int>(index) + 1;
            break;
        }
    }
    if (selected == nullptr) {
        ImGui::SetCursorPos(ImVec2(20.f, available_height * .42f));
        ImGui::PushFont(regular);
        ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
                SV_STR(SV_84693D00E97C295D, "%s"),
                setting.invsee_players.empty()
                        ? SV_STR(SV_D07E4553E777B784, "PLAYER DETAILS WILL APPEAR HERE")
                        : SV_STR(SV_BED11DA2F29A8DA1, "SELECT A PLAYER TO SET TARGET"));
        ImGui::PopFont();
    } else {
        ImFont *display = g_font_display != nullptr
                ? g_font_display : g_font_bold;
        draw->AddText(display, 25.f, origin + ImVec2(16.f, 12.f),
                kText, selected->name.c_str());
        char summary[48]{};
        _snprintf_s(summary, sizeof(summary), _TRUNCATE,
                selected->sets == 1
                        ? SV_STR(SV_43CAB61E17FF52EA, "#%d  /  1 SET")
                        : SV_STR(SV_251A371CEE2D8416, "#%d  /  %d SETS"),
                selected_rank, selected->sets);
        draw->AddText(micro, 9.f, origin + ImVec2(17.f, 43.f),
                kMuted, summary);

        const float card_top = 65.f;
        const float card_bottom = available_height - 38.f;
        const float skin_width = std::min(220.f,
                std::max(96.f, detail_width - 152.f));
        const ImVec2 skin_min = origin + ImVec2(14.f, card_top);
        const ImVec2 skin_max = origin + ImVec2(14.f + skin_width, card_bottom);
        draw->AddRectFilled(skin_min, skin_max, IM_COL32(10, 10, 10, 255), 8.f);
        draw->AddRect(skin_min, skin_max, kCardBorder, 8.f);
        draw->AddText(micro, 8.f, skin_min + ImVec2(10.f, 9.f), kMuted,
                SV_STR(SV_BCEF74BBD2BAE9D4, "SKIN"));
        const ImVec2 body_min = skin_min + ImVec2(8.f, 26.f);
        const ImVec2 body_max = skin_max - ImVec2(8.f, 8.f);
        ImTextureID skin = player_skin_texture(selected->name);
        if (skin != nullptr) {
            // Keep the preview in a readable three-quarter turn.  A full
            // 360-degree yaw regularly put the model exactly in profile,
            // collapsing the cuboids into thin strips and making the skin
            // look corrupted inside the compact card.
            const float angle = .36f + static_cast<float>(
                    std::sin(ImGui::GetTime() * .22) * .14);
            render_invsee_player_model(draw, skin, body_min, body_max, angle);
        } else {
            draw_invsee_body_placeholder(draw, body_min, body_max);
        }

        const ImVec2 gear_min(skin_max.x + 10.f, skin_min.y);
        const ImVec2 gear_max(origin.x + detail_width - 14.f, skin_max.y);
        draw->AddRectFilled(gear_min, gear_max,
                IM_COL32(10, 10, 10, 255), 8.f);
        draw->AddRect(gear_min, gear_max, kCardBorder, 8.f);
        draw->AddText(micro, 8.f, gear_min + ImVec2(10.f, 9.f),
                kMuted, SV_STR(SV_078E3A0EA39A0950, "ARMOR SETS"));
        const char *piece_labels[] = {SV_STR(SV_64BBFC913A0A2B15, "HELMET"), SV_STR(SV_4449642989AF092D, "CHESTPLATE"), SV_STR(SV_45812864DF87AFE8, "LEGGINGS"), SV_STR(SV_0476FF9FDE81F3A3, "BOOTS")};
        const int piece_counts[] = {selected->helmet, selected->chestplate,
                selected->leggings, selected->boots};
        const float row_height = 28.f;
        const float row_top = gear_min.y + 29.f;
        for (int index = 0; index < 4; ++index) {
            const ImVec2 row_min(gear_min.x + 8.f,
                    row_top + index * row_height);
            const ImVec2 row_max(gear_max.x - 8.f,
                    row_min.y + row_height - 3.f);
            draw->AddRectFilled(row_min, row_max,
                    IM_COL32(17, 17, 17, 255), 5.f);
            draw->AddText(micro, 8.f, row_min + ImVec2(7.f, 7.f),
                    IM_COL32(162, 162, 162, 255), piece_labels[index]);
            char count[16]{};
            _snprintf_s(count, sizeof(count), _TRUNCATE, SV_STR(SV_B2C9E7759FDDFBD4, "%d"),
                    piece_counts[index]);
            const ImVec2 count_size = micro->CalcTextSizeA(
                    10.f, FLT_MAX, 0.f, count);
            draw->AddText(micro, 10.f,
                    ImVec2(row_max.x - count_size.x - 7.f, row_min.y + 6.f),
                    kText, count);
        }
        draw->AddText(micro, 8.f,
                origin + ImVec2(15.f, available_height - 23.f), kMuted,
                SV_STR(SV_0CDD24B8781C7F39, "CLICK A PLAYER TO SELECT TARGET"));
    }
    ImGui::EndChild();
    ImGui::PopStyleColor();
}

struct EditorialSliderResult {
    bool changed = false;
    bool finished = false;
};

static EditorialSliderResult editorial_slider(const char *id,
        const char *label, float *value, float minimum, float maximum,
        const char *value_text) {
    EditorialSliderResult result;
    ImGuiWindow *window = ImGui::GetCurrentWindow();
    const ImVec2 start = ImGui::GetCursorScreenPos();
    const float width = ImGui::GetContentRegionAvail().x;
    const float height = 32.f;
    ImGui::InvisibleButton(id, ImVec2(width, height));
    const bool hovered = ImGui::IsItemHovered();
    const bool active = ImGui::IsItemActive();
    const float label_width = std::min(170.f,
            std::max(92.f, width * 0.27f));
    ImFont *font = g_font_regular != nullptr ? g_font_regular : ImGui::GetFont();
    const ImVec2 value_size = font->CalcTextSizeA(13.f, FLT_MAX, 0.f,
            value_text);
    const float track_x0 = start.x + label_width;
    const float track_x1 = start.x + width - value_size.x - 24.f;
    const float center_y = start.y + height * 0.5f;
    if (active && maximum > minimum && track_x1 > track_x0) {
        const float ratio = ImClamp((ImGui::GetIO().MousePos.x - track_x0)
                / (track_x1 - track_x0), 0.f, 1.f);
        const float next = minimum + (maximum - minimum) * ratio;
        if (std::fabs(next - *value) > 0.00001f) {
            *value = next;
            result.changed = true;
            ImGui::MarkItemEdited(ImGui::GetItemID());
        }
    }
    result.finished = ImGui::IsItemDeactivatedAfterEdit();
    const float ratio = maximum > minimum
            ? ImClamp((*value - minimum) / (maximum - minimum), 0.f, 1.f) : 0.f;
    ImDrawList *draw = window->DrawList;
    add_visible_text(draw, font, 15.f, ImVec2(start.x, center_y - 8.f),
            hovered || active ? kText : kMuted, label);
    draw->AddLine(ImVec2(track_x0, center_y), ImVec2(track_x1, center_y),
            IM_COL32(69, 69, 69, 255), 2.f);
    draw->AddLine(ImVec2(track_x0, center_y),
            ImVec2(track_x0 + (track_x1 - track_x0) * ratio, center_y),
            hovered || active ? kText : kMuted, 2.f);
    const float thumb = track_x0 + (track_x1 - track_x0) * ratio;
    draw->AddRectFilled(ImVec2(thumb - 2.f, center_y - 8.f),
            ImVec2(thumb + 2.f, center_y + 8.f),
            hovered || active ? kText : kMuted, 1.5f);
    draw->AddText(font, 13.f,
            ImVec2(start.x + width - value_size.x, center_y - 7.f),
            kText, value_text);
    return result;
}

static bool editorial_select_row(const char *id, const char *label,
        const char *value) {
    const ImVec2 start = ImGui::GetCursorScreenPos();
    const float width = ImGui::GetContentRegionAvail().x;
    ImGui::InvisibleButton(id, ImVec2(width, 32.f));
    const bool pressed = ImGui::IsItemClicked();
    const bool hovered = ImGui::IsItemHovered();
    ImFont *font = g_font_regular != nullptr ? g_font_regular : ImGui::GetFont();
    ImDrawList *draw = ImGui::GetWindowDrawList();
    add_visible_text(draw, font, 15.f, ImVec2(start.x, start.y + 7.f),
            hovered ? kText : kMuted, label);
    const ImVec2 size = calc_visible_text_size(font, 13.f, value);
    add_visible_text(draw, font, 13.f,
            ImVec2(start.x + width - size.x, start.y + 8.f),
            hovered ? kText : kMuted, value);
    return pressed;
}

static bool editorial_color_picker(const char *id, const char *label,
        std::string &hex, bool *editing = nullptr) {
    ImGui::PushID(id);
    float color[3]{1.f, 1.f, 1.f};
    parse_hex_color(hex, color);
    const ImVec2 start = ImGui::GetCursorScreenPos();
    const float width = ImGui::GetContentRegionAvail().x;
    const bool pressed = editorial_select_row(SV_STR(SV_F3A1BEA5BFECC051, "##color_row"),
            label, SV_STR(SV_8C4D649A44E15777, ""));
    ImFont *font = g_font_regular != nullptr ? g_font_regular : ImGui::GetFont();
    const ImVec2 hex_size = font->CalcTextSizeA(13.f, FLT_MAX, 0.f, hex.c_str());
    ImDrawList *draw = ImGui::GetWindowDrawList();
    draw->AddText(font, 13.f,
            ImVec2(start.x + width - hex_size.x - 58.f, start.y + 8.f),
            kMuted, hex.c_str());
    const ImVec2 swatch_min(start.x + width - 44.f, start.y + 8.f);
    const ImVec2 swatch_max(start.x + width, start.y + 24.f);
    draw->AddRectFilled(swatch_min, swatch_max,
            ImGui::ColorConvertFloat4ToU32(ImVec4(color[0], color[1], color[2], 1.f)),
            8.f);
    draw->AddRect(swatch_min, swatch_max, IM_COL32(69, 69, 69, 255), 8.f);
    if (pressed) ImGui::OpenPopup(SV_STR(SV_23CE504C94D293BE, "##picker"));

    const ImGuiID popup_id = ImGui::GetID(SV_STR(SV_23CE504C94D293BE, "##picker"));
    static std::map<ImGuiID, bool> dirty;
    static std::map<ImGuiID, bool> was_open;
    struct PickerHsv { float hue = 0.f, saturation = 0.f, value = 0.f; std::string hex; };
    static std::map<ImGuiID, PickerHsv> hsv_states;
    const ImVec2 popup_size(232.f, 194.f);
    ImGuiViewport *viewport = ImGui::GetMainViewport();
    const ImVec2 work_min = viewport->WorkPos + ImVec2(8.f, 8.f);
    const ImVec2 work_max = viewport->WorkPos + viewport->WorkSize
            - popup_size - ImVec2(8.f, 8.f);
    ImVec2 popup_position(start.x + width - popup_size.x,
            start.y - popup_size.y - 6.f);
    if (popup_position.y < work_min.y)
        popup_position.y = start.y + 34.f;
    popup_position.x = ImClamp(popup_position.x, work_min.x, work_max.x);
    popup_position.y = ImClamp(popup_position.y, work_min.y, work_max.y);
    ImGui::SetNextWindowPos(popup_position, ImGuiCond_Always);
    ImGui::SetNextWindowSize(popup_size, ImGuiCond_Always);
    ImGui::PushStyleColor(ImGuiCol_PopupBg, kHeader);
    ImGui::PushStyleColor(ImGuiCol_Border, IM_COL32(30, 30, 30, 255));
    ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(10.f, 10.f));
    if (ImGui::BeginPopup(SV_STR(SV_23CE504C94D293BE, "##picker"),
            ImGuiWindowFlags_NoScrollbar | ImGuiWindowFlags_NoScrollWithMouse
                    | ImGuiWindowFlags_NoMove | ImGuiWindowFlags_NoResize
                    | ImGuiWindowFlags_NoSavedSettings)) {
        PickerHsv &hsv = hsv_states[popup_id];
        if (!was_open[popup_id] || hsv.hex != hex) {
            ImGui::ColorConvertRGBtoHSV(color[0], color[1], color[2],
                    hsv.hue, hsv.saturation, hsv.value);
            hsv.hex = hex;
        }
        float &hue = hsv.hue;
        float &saturation = hsv.saturation;
        float &value = hsv.value;
        const ImVec2 square_size(174.f, 136.f);
        const ImVec2 square_min = ImGui::GetCursorScreenPos();
        ImGui::InvisibleButton(SV_STR(SV_BAD1B8FF6C3EF348, "##picker_value"),
                square_size);
        bool changed = false;
        if (ImGui::IsItemActive()) {
            const ImVec2 mouse = ImGui::GetIO().MousePos;
            saturation = ImClamp((mouse.x - square_min.x) / square_size.x,
                    0.f, 1.f);
            value = 1.f - ImClamp((mouse.y - square_min.y) / square_size.y,
                    0.f, 1.f);
            changed = true;
        }

        float hue_r = 1.f, hue_g = 1.f, hue_b = 1.f;
        ImGui::ColorConvertHSVtoRGB(hue, 1.f, 1.f, hue_r, hue_g, hue_b);
        ImDrawList *picker_draw = ImGui::GetWindowDrawList();
        picker_draw->AddRectFilledMultiColor(square_min,
                square_min + square_size,
                IM_COL32_WHITE,
                ImGui::ColorConvertFloat4ToU32(ImVec4(hue_r, hue_g, hue_b, 1.f)),
                ImGui::ColorConvertFloat4ToU32(ImVec4(hue_r, hue_g, hue_b, 1.f)),
                IM_COL32_WHITE);
        picker_draw->AddRectFilledMultiColor(square_min,
                square_min + square_size,
                IM_COL32(0, 0, 0, 0), IM_COL32(0, 0, 0, 0),
                IM_COL32(0, 0, 0, 255), IM_COL32(0, 0, 0, 255));
        picker_draw->AddCircle(ImVec2(square_min.x + saturation * square_size.x,
                        square_min.y + (1.f - value) * square_size.y),
                5.f, IM_COL32_WHITE, 16, 1.5f);

        const ImVec2 hue_min(square_min.x + square_size.x + 10.f, square_min.y);
        const ImVec2 hue_size(14.f, square_size.y);
        ImGui::SetCursorScreenPos(hue_min);
        ImGui::InvisibleButton(SV_STR(SV_9F35F697E1B9767F, "##picker_hue"), hue_size);
        if (ImGui::IsItemActive()) {
            hue = ImClamp((ImGui::GetIO().MousePos.y - hue_min.y) / hue_size.y,
                    0.f, 1.f);
            changed = true;
        }
        const ImU32 hue_colors[] = {
            IM_COL32(255, 0, 0, 255), IM_COL32(255, 255, 0, 255),
            IM_COL32(0, 255, 0, 255), IM_COL32(0, 255, 255, 255),
            IM_COL32(0, 0, 255, 255), IM_COL32(255, 0, 255, 255),
            IM_COL32(255, 0, 0, 255)
        };
        for (int segment = 0; segment < 6; ++segment) {
            const float y0 = hue_min.y + hue_size.y * segment / 6.f;
            const float y1 = hue_min.y + hue_size.y * (segment + 1) / 6.f;
            picker_draw->AddRectFilledMultiColor(ImVec2(hue_min.x, y0),
                    ImVec2(hue_min.x + hue_size.x, y1),
                    hue_colors[segment], hue_colors[segment],
                    hue_colors[segment + 1], hue_colors[segment + 1]);
        }
        const float hue_y = hue_min.y + hue * hue_size.y;
        picker_draw->AddRect(ImVec2(hue_min.x - 2.f, hue_y - 2.f),
                ImVec2(hue_min.x + hue_size.x + 2.f, hue_y + 2.f),
                IM_COL32_WHITE, 1.f, 0, 1.5f);

        if (changed) {
            ImGui::ColorConvertHSVtoRGB(hue, saturation, value,
                    color[0], color[1], color[2]);
            hex = color_hex(color);
            hsv.hex = hex;
            dirty[popup_id] = true;
        }
        ImGui::SetCursorScreenPos(ImVec2(square_min.x,
                square_min.y + square_size.y + 9.f));
        const ImVec2 preview_min = ImGui::GetCursorScreenPos();
        ImGui::Dummy(ImVec2(42.f, 18.f));
        picker_draw->AddRectFilled(preview_min, preview_min + ImVec2(42.f, 18.f),
                ImGui::ColorConvertFloat4ToU32(
                        ImVec4(color[0], color[1], color[2], 1.f)), 9.f);
        ImGui::SameLine(0.f, 10.f);
        ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
                SV_STR(SV_84693D00E97C295D, "%s"), hex.c_str());
        ImGui::EndPopup();
    }
    ImGui::PopStyleVar();
    ImGui::PopStyleColor(2);
    const bool open = ImGui::IsPopupOpen(SV_STR(SV_23CE504C94D293BE, "##picker"));
    const bool commit = dirty[popup_id]
            && (ImGui::IsMouseReleased(ImGuiMouseButton_Left) || was_open[popup_id] && !open);
    if (commit) dirty[popup_id] = false;
    was_open[popup_id] = open;
    if (editing != nullptr) *editing = open;
    ImGui::PopID();
    return commit;
}

static void editorial_value(MenuModule &module, MenuValue &incoming) {
    MenuValue &setting = incoming.type == SV_STR(SV_60C175CA12488454, "color")
            ? color_edit_buffer(module, incoming).draft : incoming;
    ImGui::PushID(setting.id.c_str());
    const std::string &name = setting.name;
    if (setting.type == SV_STR(SV_84A35F8F4E928DF0, "invsee-ranking")) {
        render_invsee_ranking(module, setting);
        ImGui::PopID();
        return;
    }
    if (setting.type == SV_STR(SV_4DFD00155697EA79, "boolean")) {
        if (contains_ci_visible(name,
                SV_STR(SV_B677BCBD3AA678D7, "Clear cache"))
                || contains_ci_visible(name, SV_STR(SV_36011D9C95BB02B3, "Copy Sync Code"))) {
            const float width = calc_visible_text_size(ImGui::GetFont(),
                    ImGui::GetFontSize(), name.c_str()).x + 30.f;
            if (editorial_pill(SV_STR(SV_DE68DAB8D7AAAEA0, "##clear_cache"),
                    name.c_str(), false, ImVec2(width, 28.f))) {
                queue_value(module, setting,
                        SV_STR(SV_9A91E94F6DD7E359, "true"));
                // This is a momentary action. Keep the local control neutral
                // while the Java side clears and acknowledges the cache.
                setting.value = SV_STR(SV_A5FEA6BD65D8D5BF, "false");
            }
            ImGui::PopID();
            return;
        }
        bool enabled = setting.value == SV_STR(SV_9A91E94F6DD7E359, "true");
        const float width = calc_visible_text_size(ImGui::GetFont(),
                ImGui::GetFontSize(), name.c_str()).x + 30.f;
        if (editorial_pill(SV_STR(SV_ACD81F07989E7AF8, "Enabled"),
                name.c_str(), enabled, ImVec2(width, 28.f))) {
            enabled = !enabled;
            queue_value(module, setting, enabled
                    ? SV_STR(SV_9A91E94F6DD7E359, "true")
                    : SV_STR(SV_A5FEA6BD65D8D5BF, "false"));
        }
    } else if (setting.type == SV_STR(SV_CC5F642EAA76DBF9, "number")) {
        float number = static_cast<float>(std::strtod(setting.value.c_str(), nullptr));
        char value[64]{};
        _snprintf_s(value, sizeof(value), _TRUNCATE, SV_STR(SV_08F02AEBA33BB30F, "%.*f%s"),
                std::max(0, std::min(setting.decimals, 8)), number,
                setting.unit.c_str());
        EditorialSliderResult slider = editorial_slider(SV_STR(SV_1A23C4E7EAD584FD, "##number"), name.c_str(),
                &number, static_cast<float>(setting.minimum),
                static_cast<float>(setting.maximum), value);
        if (slider.changed) setting.value = format_number(
                quantize(number, setting), setting.decimals);
        if (slider.finished) queue_value(module, setting, setting.value);
    } else if (setting.type == SV_STR(SV_2D47078D80D9411D, "range")) {
        float lower = static_cast<float>(setting.lower);
        float upper = static_cast<float>(setting.upper);
        char low_text[64]{}, high_text[64]{};
        _snprintf_s(low_text, sizeof(low_text), _TRUNCATE, SV_STR(SV_08F02AEBA33BB30F, "%.*f%s"),
                setting.decimals, lower, setting.unit.c_str());
        _snprintf_s(high_text, sizeof(high_text), _TRUNCATE, SV_STR(SV_08F02AEBA33BB30F, "%.*f%s"),
                setting.decimals, upper, setting.unit.c_str());
        const std::string lower_name = name + SV_STR(SV_5C7880630205C5EF, " Min");
        const std::string upper_name = name + SV_STR(SV_DBC8CE59D1B31AAF, " Max");
        EditorialSliderResult low = editorial_slider(SV_STR(SV_F3BA8B6915599A89, "##lower"),
                lower_name.c_str(), &lower, static_cast<float>(setting.minimum),
                upper, low_text);
        EditorialSliderResult high = editorial_slider(SV_STR(SV_4388764923029AC6, "##upper"),
                upper_name.c_str(), &upper, lower,
                static_cast<float>(setting.maximum), high_text);
        if (low.changed || high.changed) {
            setting.lower = quantize(lower, setting);
            setting.upper = quantize(upper, setting);
            setting.value = format_number(setting.lower, setting.decimals)
                    + SV_STR(SV_C04C1EC13A261F68, ",") + format_number(setting.upper, setting.decimals);
        }
        if (low.finished || high.finished)
            queue_value(module, setting, setting.value);
    } else if (setting.type == SV_STR(SV_2351581F3BB712C8, "mode")) {
        std::string preview = setting.value;
        const auto current = setting.option_labels.find(setting.value);
        if (current != setting.option_labels.end()) preview = current->second;
        if (editorial_select_row(SV_STR(SV_1D223686F9F16093, "##mode"), name.c_str(), preview.c_str()))
            ImGui::OpenPopup(SV_STR(SV_2DA385E62A07FFAB, "##mode_picker"));
        const int visible_rows = std::max(1, std::min(3,
                static_cast<int>(setting.options.size())));
        const float wheel_height = visible_rows * 30.f;
        ImGui::SetNextWindowSize(ImVec2(240.f, wheel_height + 6.f),
                ImGuiCond_Always);
        ImGui::PushStyleColor(ImGuiCol_PopupBg, kHeader);
        ImGui::PushStyleColor(ImGuiCol_Border, IM_COL32(30, 30, 30, 255));
        ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(8.f, 3.f));
        ImGui::PushStyleVar(ImGuiStyleVar_PopupRounding, 10.f);
        if (ImGui::BeginPopup(SV_STR(SV_2DA385E62A07FFAB, "##mode_picker"),
                ImGuiWindowFlags_NoScrollbar | ImGuiWindowFlags_NoScrollWithMouse)) {
            int selected = 0;
            for (size_t index = 0; index < setting.options.size(); ++index)
                if (setting.options[index] == setting.value)
                    selected = static_cast<int>(index);
            ImGuiWindow *popup = ImGui::GetCurrentWindow();
            const ImGuiID wheel_id = popup->GetID(SV_STR(SV_B1E5EF6262CA6DD2, "##mode_wheel"));
            static std::map<ImGuiID, float> wheel_offsets;
            static std::map<ImGuiID, float> wheel_selections;
            float &wheel_offset = wheel_offsets[wheel_id];
            float &wheel_selection = wheel_selections[wheel_id];
            const int max_offset = std::max(0,
                    static_cast<int>(setting.options.size()) - visible_rows);
            const float target_offset = static_cast<float>(ImClamp(
                    selected - visible_rows / 2, 0, max_offset));
            if (ImGui::IsWindowAppearing()) {
                wheel_offset = target_offset;
                wheel_selection = static_cast<float>(selected);
            }

            const ImVec2 area = ImGui::GetCursorScreenPos();
            const ImVec2 area_size(224.f, wheel_height);
            ImGui::InvisibleButton(SV_STR(SV_B1E5EF6262CA6DD2, "##mode_wheel"), area_size);
            const bool wheel_hovered = ImGui::IsItemHovered();
            if (wheel_hovered && ImGui::GetIO().MouseWheel != 0.f
                    && !setting.options.empty()) {
                const int direction = ImGui::GetIO().MouseWheel > 0.f ? -1 : 1;
                const int next = ImClamp(selected + direction, 0,
                        static_cast<int>(setting.options.size()) - 1);
                if (next != selected) {
                    selected = next;
                    queue_value(module, setting, setting.options[selected]);
                }
            }
            if (wheel_hovered && ImGui::IsItemClicked()
                    && !setting.options.empty()) {
                const int next = ImClamp(static_cast<int>(std::round(
                        wheel_offset + (ImGui::GetIO().MousePos.y
                                - area.y - 15.f) / 30.f)), 0,
                        static_cast<int>(setting.options.size()) - 1);
                if (next != selected) {
                    selected = next;
                    queue_value(module, setting, setting.options[selected]);
                }
            }
            const float next_offset = static_cast<float>(ImClamp(
                    selected - visible_rows / 2, 0, max_offset));
            const float animation = ImClamp(ImGui::GetIO().DeltaTime * 15.f,
                    0.f, 1.f);
            wheel_offset = ImLerp(wheel_offset, next_offset, animation);
            wheel_selection = ImLerp(wheel_selection,
                    static_cast<float>(selected), animation);

            ImDrawList *wheel_draw = ImGui::GetWindowDrawList();
            const float selected_y = area.y + 15.f
                    + (wheel_selection - wheel_offset) * 30.f;
            wheel_draw->PushClipRect(area, area + area_size, true);
            wheel_draw->AddRectFilled(ImVec2(area.x + 2.f, selected_y - 13.f),
                    ImVec2(area.x + area_size.x - 2.f, selected_y + 13.f),
                    IM_COL32(30, 30, 30, 255), 13.f);
            ImFont *wheel_font = g_font_regular != nullptr
                    ? g_font_regular : ImGui::GetFont();
            for (size_t index = 0; index < setting.options.size(); ++index) {
                std::string option_name = setting.options[index];
                const auto found = setting.option_labels.find(setting.options[index]);
                if (found != setting.option_labels.end()) option_name = found->second;
                const float distance = static_cast<float>(index) - wheel_selection;
                const float row_y = area.y + 15.f
                        + (static_cast<float>(index) - wheel_offset) * 30.f;
                const float alpha = ImClamp(1.f - std::fabs(distance) * .42f,
                        .12f, 1.f);
                ImU32 color = index == static_cast<size_t>(selected)
                        ? kText : IM_COL32(124, 124, 124,
                                static_cast<int>(210.f * alpha));
                add_visible_text(wheel_draw, wheel_font, 14.f,
                        ImVec2(area.x + 12.f + std::fabs(distance) * 7.f,
                                row_y - 7.f), color, option_name.c_str());
            }
            wheel_draw->PopClipRect();
            ImGui::EndPopup();
        }
        ImGui::PopStyleVar(2);
        ImGui::PopStyleColor(2);
    } else if (setting.type == SV_STR(SV_090B0A79694821B6, "bind")) {
        bool listening = g_capture_kind.load() == 3;
        if (listening) {
            std::lock_guard<std::mutex> lock(g_capture_mutex);
            listening = g_capture_module == module.id
                    && g_capture_value == setting.id;
        }
        const int key = static_cast<int>(std::strtol(setting.value.c_str(), nullptr, 10));
        const std::string bind = listening ? SV_STR(SV_B8DE1E32F23E60B2, "PRESS KEY  |  ESC CANCELS  |  BACKSPACE CLEARS")
                : SV_STR(SV_3395CE580734C9F7, "BOUND TO ") + uppercase_copy(key_name(key));
        if (editorial_select_row(SV_STR(SV_D39F3129EA4B49FD, "##bind"), name.c_str(), bind.c_str()))
            begin_value_bind_capture(module.id, setting.id);
    } else if (setting.type == SV_STR(SV_60C175CA12488454, "color")) {
        bool primary_open = false, secondary_open = false;
        if (editorial_color_picker(SV_STR(SV_8BDCB38ABD916A0F, "##color"),
                name.c_str(), setting.color, &primary_open))
            queue_value(module, setting, color_serialized(setting));
        bool alpha_active = false;
        if (!setting.no_alpha) {
            float alpha = static_cast<float>(setting.alpha);
            char alpha_text[24]{};
            _snprintf_s(alpha_text, sizeof(alpha_text), _TRUNCATE, SV_STR(SV_471801AEE3EDE799, "%.0f"), alpha);
            EditorialSliderResult alpha_slider = editorial_slider(SV_STR(SV_40CA32D11C6A1EF8, "##alpha"), SV_STR(SV_EAF0E21DCEF978AB, "Alpha"),
                    &alpha, 0.f, 255.f, alpha_text);
            setting.alpha = static_cast<int>(alpha + .5f);
            alpha_active = ImGui::IsItemActive();
            if (alpha_slider.finished)
                queue_value(module, setting, color_serialized(setting));
        }
        bool effect_popup_open = false;
        const int effect_count = setting.no_rainbow && setting.no_fade ? 0
                : (1 + (setting.no_rainbow ? 0 : 1) + (setting.no_fade ? 0 : 1));
        if (effect_count > 1) {
            const char *effect = setting.fade ? SV_STR(SV_5708C7CE404FF94D, "Fade")
                    : setting.rainbow ? SV_STR(SV_13E7548BCF2C12C3, "Rainbow") : SV_STR(SV_666F8FC976EEC504, "Fixed");
            if (editorial_select_row(SV_STR(SV_9AC9F97AEA93F407, "##effect"), SV_STR(SV_B37D61D311B739AE, "Effect"), effect))
                ImGui::OpenPopup(SV_STR(SV_BAAF6C09A1B999DA, "##effect_picker"));
            const float effect_height = effect_count * 30.f;
            ImGui::SetNextWindowSize(ImVec2(240.f, effect_height + 6.f),
                    ImGuiCond_Always);
            ImGui::PushStyleColor(ImGuiCol_PopupBg, kHeader);
            ImGui::PushStyleColor(ImGuiCol_Border, IM_COL32(30, 30, 30, 255));
            ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(8.f, 3.f));
            ImGui::PushStyleVar(ImGuiStyleVar_PopupRounding, 10.f);
            if (ImGui::BeginPopup(SV_STR(SV_BAAF6C09A1B999DA, "##effect_picker"),
                    ImGuiWindowFlags_NoScrollbar | ImGuiWindowFlags_NoScrollWithMouse)) {
                int index_of_rainbow = setting.no_rainbow ? -1 : 1;
                int index_of_fade = setting.no_fade ? -1 : effect_count - 1;
                int selected = setting.fade && index_of_fade >= 0 ? index_of_fade
                        : setting.rainbow && index_of_rainbow >= 0 ? index_of_rainbow : 0;
                ImGuiWindow *popup = ImGui::GetCurrentWindow();
                const ImGuiID wheel_id = popup->GetID(
                        SV_STR(SV_94E267541B14B4B6, "##effect_wheel"));
                static std::map<ImGuiID, float> effect_selections;
                float &animated_selection = effect_selections[wheel_id];
                if (ImGui::IsWindowAppearing())
                    animated_selection = static_cast<float>(selected);

                const ImVec2 area = ImGui::GetCursorScreenPos();
                const ImVec2 area_size(224.f, effect_height);
                ImGui::InvisibleButton(SV_STR(SV_94E267541B14B4B6, "##effect_wheel"),
                        area_size);
                int next = selected;
                if (ImGui::IsItemHovered() && ImGui::GetIO().MouseWheel != 0.f) {
                    next = ImClamp(selected
                            + (ImGui::GetIO().MouseWheel > 0.f ? -1 : 1),
                            0, effect_count - 1);
                }
                if (ImGui::IsItemHovered() && ImGui::IsItemClicked()) {
                    next = ImClamp(static_cast<int>((ImGui::GetIO().MousePos.y
                            - area.y) / 30.f), 0, effect_count - 1);
                }
                if (next != selected) {
                    setting.fade = index_of_fade >= 0 && next == index_of_fade;
                    setting.rainbow = index_of_rainbow >= 0 && next == index_of_rainbow;
                    queue_value(module, setting, color_serialized(setting));
                    selected = next;
                }
                animated_selection = ImLerp(animated_selection,
                        static_cast<float>(selected),
                        ImClamp(ImGui::GetIO().DeltaTime * 15.f, 0.f, 1.f));

                ImDrawList *draw = ImGui::GetWindowDrawList();
                const float selected_y = area.y + 15.f
                        + animated_selection * 30.f;
                draw->PushClipRect(area, area + area_size, true);
                draw->AddRectFilled(ImVec2(area.x + 2.f, selected_y - 13.f),
                        ImVec2(area.x + area_size.x - 2.f, selected_y + 13.f),
                        IM_COL32(30, 30, 30, 255), 13.f);
                ImFont *wheel_font = g_font_regular != nullptr
                        ? g_font_regular : ImGui::GetFont();
                for (int index = 0; index < effect_count; ++index) {
                    const char *label = index == 0
                            ? SV_STR(SV_666F8FC976EEC504, "Fixed")
                            : index == index_of_rainbow
                                    ? SV_STR(SV_13E7548BCF2C12C3, "Rainbow")
                                    : SV_STR(SV_5708C7CE404FF94D, "Fade");
                    const float distance = static_cast<float>(index)
                            - animated_selection;
                    const float alpha = ImClamp(1.f - std::fabs(distance) * .42f,
                            .12f, 1.f);
                    draw->AddText(wheel_font, 14.f,
                            ImVec2(area.x + 12.f + std::fabs(distance) * 7.f,
                                    area.y + 8.f + index * 30.f),
                            index == selected ? kText : IM_COL32(124, 124, 124,
                                    static_cast<int>(210.f * alpha)), label);
                }
                draw->PopClipRect();
                ImGui::EndPopup();
            }
            ImGui::PopStyleVar(2);
            ImGui::PopStyleColor(2);
            effect_popup_open = ImGui::IsPopupOpen(SV_STR(SV_BAAF6C09A1B999DA, "##effect_picker"));
        }
        if (setting.fade && !setting.no_fade) {
            if (editorial_color_picker(SV_STR(SV_09794C18C1C7F32C, "##fade"),
                    SV_STR(SV_B5DFD5D55C9E3341, "Secondary color"),
                    setting.fade_color, &secondary_open))
                queue_value(module, setting, color_serialized(setting));
        }
        g_color_buffers[value_edit_key(module.id, setting.id)].editing =
                primary_open || secondary_open || alpha_active || effect_popup_open;
        incoming = setting;
    } else if (setting.type == SV_STR(SV_6AD981CC4B9E110E, "flags")
            || setting.type == SV_STR(SV_5CB8522F1D48346D, "player-flags")) {
        std::string flags = setting.value;
        if (flags.size() < setting.options.size())
            flags.resize(setting.options.size(), '0');
        visible_text_line(kMuted, name);
        for (size_t index = 0; index < setting.options.size(); ++index) {
            ImGui::PushID(static_cast<int>(index));
            const bool selected = flags[index] == '1';
            const bool player = setting.type == SV_STR(SV_5CB8522F1D48346D, "player-flags");
            const float width = player ? std::min(280.f, ImGui::GetContentRegionAvail().x)
                    : calc_visible_text_size(ImGui::GetFont(),
                            ImGui::GetFontSize(),
                            setting.options[index].c_str()).x + 30.f;
            const bool pressed = player
                    ? editorial_player_row(SV_STR(SV_6E72095D1C4F324D, "##player_flag"),
                            setting.options[index], selected, ImVec2(width, 30.f))
                    : editorial_pill(SV_STR(SV_EE7AEBA0A5D56CA1, "##flag"),
                            setting.options[index].c_str(), selected,
                            ImVec2(width, 28.f));
            if (pressed) {
                flags[index] = selected ? '0' : '1';
                queue_value(module, setting, flags);
            }
            ImGui::PopID();
        }
        ImGui::Dummy(ImVec2(1.f, 4.f));
    } else {
        const std::string key = module.id + SV_STR(SV_8181E3BA16810C79, "\n") + setting.id;
        TextBuffer &edit = g_text_buffers[key];
        const bool single_line = setting.type == SV_STR(SV_23AA0DEDF1CBB41A, "text");
        const size_t capacity = single_line ? 2049u : 16385u;
        if (edit.bytes.empty()) {
            edit.bytes.resize(capacity, 0);
            strncpy_s(edit.bytes.data(), edit.bytes.size(), setting.value.c_str(), _TRUNCATE);
            edit.server_value = setting.value;
        } else if (edit.pending_value.empty() && edit.server_value != setting.value) {
            std::fill(edit.bytes.begin(), edit.bytes.end(), '\0');
            strncpy_s(edit.bytes.data(), edit.bytes.size(), setting.value.c_str(), _TRUNCATE);
            edit.server_value = setting.value;
        } else if (!edit.pending_value.empty()
                && edit.pending_value == setting.value) {
            edit.pending_value.clear();
            edit.server_value = setting.value;
        }
        visible_text_line(kMuted, name);
        ImGui::SetNextItemWidth(-1.f);
        const bool autocomplete = !setting.suggestions.empty()
                && (single_line || setting.type == SV_STR(SV_BA7FA36853B641DB, "player-list"));
        TextAutocompleteContext autocomplete_context;
        autocomplete_context.suggestions = autocomplete ? &setting.suggestions : nullptr;
        autocomplete_context.multiple = !single_line;
        const ImGuiInputTextFlags input_flags = autocomplete
                ? ImGuiInputTextFlags_CallbackCompletion
                : ImGuiInputTextFlags_None;
        const ImGuiID input_id = ImGui::GetID(SV_STR(SV_741760C58DC3A53E, "##text"));
        bool changed = single_line
                ? ImGui::InputText(SV_STR(SV_741760C58DC3A53E, "##text"), edit.bytes.data(), edit.bytes.size(),
                        input_flags, autocomplete ? autocomplete_input_callback : nullptr,
                        autocomplete ? &autocomplete_context : nullptr)
                : ImGui::InputTextMultiline(SV_STR(SV_741760C58DC3A53E, "##text"), edit.bytes.data(), edit.bytes.size(),
                        ImVec2(-1.f, 72.f), input_flags,
                        autocomplete ? autocomplete_input_callback : nullptr,
                        autocomplete ? &autocomplete_context : nullptr);
        changed = changed || autocomplete_context.completed;
        const ImVec2 input_min = ImGui::GetItemRectMin();
        const ImVec2 input_max = ImGui::GetItemRectMax();
        const bool input_active = ImGui::IsItemActive();
        const bool input_deactivated = ImGui::IsItemDeactivatedAfterEdit();
        if (autocomplete)
            render_inline_completion(input_id, input_min, input_max,
                    edit.bytes.data(), !single_line, setting.suggestions,
                    input_active);
        if (changed) {
            setting.value = edit.bytes.data();
            edit.pending_value = setting.value;
        }
        if (input_deactivated) {
            edit.pending_value = edit.bytes.data();
            queue_value(module, setting, edit.pending_value);
        }
        ImGui::Dummy(ImVec2(1.f, 4.f));
    }
    ImGui::PopID();
}

static void render_module_content(MenuModule &module, ImFont *,
        float content_width, float body_height) {
    ImGui::SetCursorPos(ImVec2(248.f, 78.f));
    ImGui::BeginChild(SV_STR(SV_90819E212D418C13, "##module_content"),
            ImVec2(content_width, body_height), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar | ImGuiWindowFlags_NoBackground);
    const std::string &module_name = module.label;
    ImFont *title_font = g_font_display != nullptr ? g_font_display : g_font_bold;
    draw_text_shadow(ImGui::GetWindowDrawList(), ImGui::GetCursorScreenPos(),
            kText, module_name.c_str(), nullptr, title_font, 34.f);
    ImGui::Dummy(ImVec2(1.f, 34.f));

    const float title_width = calc_visible_text_size(title_font, 34.f,
            module_name.c_str()).x;
    if (module.toggleable) {
        ImGui::SetCursorPos(ImVec2(std::min(title_width + 20.f,
                content_width - 230.f), 4.f));
        if (editorial_pill(SV_STR(SV_B0CE54A4AD377380, "##module_enabled"), module.enabled ? SV_STR(SV_B019F7BCDF6945DE, "ON") : SV_STR(SV_96211AA86D1680F4, "OFF"),
                module.enabled, ImVec2(56.f, 25.f))) {
            module.enabled = !module.enabled;
            MenuAction action;
            action.type = ActionType::Module;
            action.module = module.id;
            action.enabled = module.enabled;
            queue_action(std::move(action));
        }
    }
    int capture = g_capture_kind.load();
    std::string captured_module;
    {
        std::lock_guard<std::mutex> lock(g_capture_mutex);
        captured_module = g_capture_module;
    }
    if (module.bindable) {
        std::string bind = capture == 1 && captured_module == module.id
                ? SV_STR(SV_B8DE1E32F23E60B2, "PRESS KEY  |  ESC CANCELS  |  BACKSPACE CLEARS") : SV_STR(SV_3395CE580734C9F7, "BOUND TO ")
                        + uppercase_copy(key_name(module.binds.empty() ? 0 : module.binds[0]));
        ImGui::SetCursorPos(ImVec2(std::min(title_width + 88.f,
                content_width - 162.f), 8.f));
        if (editorial_select_row(SV_STR(SV_C2506F29AB3AA96C, "##module_bind"), SV_STR(SV_8C4D649A44E15777, ""), bind.c_str()))
            begin_bind_capture(module.id);
    }

    ImGui::SetCursorPos(ImVec2(0.f, 60.f));
    ImGui::BeginChild(SV_STR(SV_977A6EBEBFC61A02, "##settings_scroll"),
            ImVec2(content_width, std::max(80.f, body_height - 60.f)),
            ImGuiChildFlags_None, ImGuiWindowFlags_NoBackground
                    | ImGuiWindowFlags_NoScrollbar);
    int visible_values = 0;
    bool previous_boolean = false;
    for (MenuValue &setting : module.values) {
        if (!setting.active) continue;
        const bool boolean = setting.type == SV_STR(SV_4DFD00155697EA79, "boolean");
        if (boolean && previous_boolean) {
            const float next_width = calc_visible_text_size(ImGui::GetFont(),
                    ImGui::GetFontSize(), setting.name.c_str()).x + 30.f;
            if (ImGui::GetItemRectMax().x + 8.f + next_width
                    <= ImGui::GetWindowPos().x + content_width)
                ImGui::SameLine(0.f, 8.f);
        }
        editorial_value(module, setting);
        previous_boolean = boolean;
        ++visible_values;
    }
    if (visible_values == 0)
        ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
                SV_STR(SV_999AA8782881B9EF, "THIS MODULE HAS NO SETTINGS"));
    ImGui::Dummy(ImVec2(1.f, 24.f));
    ImGui::EndChild();
    ImGui::EndChild();
}

// The menu key, the client switches and the cloud config pages live in their
// own bottom tab instead of cluttering the title bar.
static int g_settings_page = 0;
static char g_config_name[33]{};
static char g_publish_name[33]{};
static char g_publish_description[161]{};
static char g_community_search[49]{};
static int g_config_slot = 0;

static void queue_cloud_action(MenuAction action) {
    queue_action(std::move(action));
}

static void editorial_section(const char *label) {
    ImGui::PushFont(g_font_micro != nullptr ? g_font_micro : g_font_regular);
    const ImVec2 pos = ImGui::GetCursorScreenPos();
    draw_text_shadow(ImGui::GetWindowDrawList(), pos,
            IM_COL32(105, 105, 105, 255), label);
    ImGui::Dummy(ImVec2(1.f, ImGui::GetTextLineHeight()));
    ImGui::PopFont();
}

static bool editorial_button(const char *label, const ImVec2 &size,
        bool primary = false) {
    ImGui::PushStyleColor(ImGuiCol_Button,
            primary ? kText : kHeader);
    ImGui::PushStyleColor(ImGuiCol_ButtonHovered,
            primary ? IM_COL32(218, 218, 218, 255)
                    : IM_COL32(30, 30, 30, 255));
    ImGui::PushStyleColor(ImGuiCol_ButtonActive,
            primary ? IM_COL32(190, 190, 190, 255)
                    : IM_COL32(38, 38, 38, 255));
    ImGui::PushStyleColor(ImGuiCol_Text, primary ? kBackground : kText);
    const bool pressed = ImGui::Button(label, size);
    ImGui::PopStyleColor(4);
    return pressed;
}

static void queue_friend_action(const char *operation,
        const std::string &name = std::string(), bool enabled = false) {
    MenuAction action;
    action.type = ActionType::Friend;
    action.value = operation;
    action.name = name;
    action.enabled = enabled;
    queue_action(std::move(action));
    std::lock_guard<std::mutex> lock(g_friends_mutex);
    if (std::strcmp(operation, SV_STR(SV_5AC4ACBFBABF1B12, "add")) == 0) {
        g_friends_list.friend_players.push_back(name);
    } else if (std::strcmp(operation, SV_STR(SV_D89DD57F8939DCFA, "remove")) == 0) {
        auto &names = g_friends_list.friend_players;
        names.erase(std::remove_if(names.begin(), names.end(),
                [&name](const std::string &existing) {
                    return player_head_key(existing) == player_head_key(name);
                }), names.end());
    } else if (std::strcmp(operation, SV_STR(SV_BDAE29AF68A58302, "middle")) == 0) {
        g_friends_middle_click = enabled;
    } else if (std::strcmp(operation, SV_STR(SV_81D5224D5D878D8A, "enabled")) == 0) {
        g_use_friends = enabled;
    }
}

static bool friends_row(const std::string &name, const ImVec2 &size) {
    const ImVec2 start = ImGui::GetCursorScreenPos();
    ImGui::InvisibleButton(SV_STR(SV_73DEB155B3698E42, "##friend_row"), size);
    const bool hovered = ImGui::IsItemHovered();
    const ImVec2 remove_min(start.x + size.x - 32.f, start.y + 9.f);
    const ImVec2 remove_max = remove_min + ImVec2(24.f, 24.f);
    const bool remove_hovered = ImGui::IsMouseHoveringRect(
            remove_min, remove_max, true);
    ImDrawList *draw = ImGui::GetWindowDrawList();
    const ImU32 fill = hovered ? IM_COL32(24, 35, 28, 255)
            : IM_COL32(16, 20, 17, 255);
    const ImU32 edge = hovered ? IM_COL32(72, 181, 108, 190)
            : IM_COL32(45, 75, 54, 210);
    draw->AddRectFilled(start, start + size, fill, 7.f);
    draw->AddRect(start, start + size, edge, 7.f, 0, 1.f);
    draw->AddRectFilled(start + ImVec2(0.f, 7.f),
            start + ImVec2(2.f, size.y - 7.f), IM_COL32(78, 214, 119, 255), 1.f);

    const float head_size = std::max(22.f, size.y - 10.f);
    const ImVec2 head_min = start + ImVec2(9.f, (size.y - head_size) * .5f);
    const ImVec2 head_max = head_min + ImVec2(head_size, head_size);
    ImTextureID texture = player_head_texture(name);
    if (texture != nullptr) {
        draw->AddImage(reinterpret_cast<ImTextureID>(texture), head_min,
                head_max, ImVec2(0.f, 0.f), ImVec2(1.f, 1.f));
    } else {
        draw->AddRectFilled(head_min, head_max, IM_COL32(39, 55, 43, 255), 5.f);
        if (!name.empty()) {
            char initial[2]{static_cast<char>(std::toupper(
                    static_cast<unsigned char>(name[0]))), 0};
            ImFont *font = g_font_bold != nullptr ? g_font_bold : ImGui::GetFont();
            const ImVec2 initial_size = font->CalcTextSizeA(13.f, FLT_MAX, 0.f, initial);
            draw->AddText(font, 13.f,
                    head_min + (ImVec2(head_size, head_size) - initial_size) * .5f,
                    IM_COL32(171, 235, 184, 255), initial);
        }
    }
    ImFont *font = g_font_medium != nullptr ? g_font_medium : ImGui::GetFont();
    draw->AddText(font, 14.f, ImVec2(head_max.x + 11.f,
            start.y + (size.y - 14.f) * .5f), IM_COL32(116, 226, 148, 255), name.c_str());
    draw->AddRectFilled(remove_min, remove_max,
            remove_hovered ? IM_COL32(108, 43, 49, 235)
                           : IM_COL32(34, 42, 36, 235), 6.f);
    const ImU32 cross_color = remove_hovered ? IM_COL32(255, 194, 198, 255)
            : IM_COL32(126, 158, 135, 255);
    draw->AddLine(remove_min + ImVec2(7.f, 7.f),
            remove_max - ImVec2(7.f, 7.f), cross_color, 1.6f);
    draw->AddLine(ImVec2(remove_max.x - 7.f, remove_min.y + 7.f),
            ImVec2(remove_min.x + 7.f, remove_max.y - 7.f), cross_color, 1.6f);
    if (remove_hovered)
        ImGui::SetTooltip(SV_STR(SV_0B66EEFADAD2EB92, "Remove friend"));
    return remove_hovered && ImGui::IsMouseClicked(ImGuiMouseButton_Left);
}

static void render_settings_sidebar() {
    // SV_STR returns memory from a small rotating TLS buffer. Keep owned
    // copies here because these labels outlive the expression that decrypts
    // them and are rendered again on every frame.
    static const std::array<std::string, 5> kPages = {
        std::string(SV_STR(SV_6D04B3CEAC7178A2, "Other")),
        std::string(SV_STR(SV_02BABF8969D1F030, "Friends")),
        std::string(SV_STR(SV_122617E94D2C2119, "Config")),
        std::string(SV_STR(SV_5D99DA387A853B5E, "Community")), SV_STR(SV_ED522AEEF1DEDF22, "Party")};
    ImGui::SetCursorPos(ImVec2(30.f, 78.f));
    ImGui::BeginChild(SV_STR(SV_DDB4539D934A13E3, "##settings_list"), ImVec2(180.f, 434.f),
            ImGuiChildFlags_None, ImGuiWindowFlags_NoScrollbar);
    for (int index = 0; index < 5; ++index) {
        if (sidebar_row(kPages[index].c_str(), kPages[index].c_str(), g_settings_page == index,
                nullptr)) {
            g_settings_page = index;
            if (index == 2 || index == 3) g_cloud_refresh = true;
        }
    }
    ImGui::EndChild();
}

static void render_settings_friends(float content_width) {
    MenuValue friend_snapshot;
    bool middle_click;
    bool use_friends;
    {
        std::lock_guard<std::mutex> lock(g_friends_mutex);
        friend_snapshot = g_friends_list;
        middle_click = g_friends_middle_click;
        use_friends = g_use_friends;
    }
    MenuValue *friends = &friend_snapshot;

    static char friend_input[32]{};
    ImGui::BeginDisabled(endpoint_snapshot().alias_len == 0);
    ImGui::SetCursorPos(ImVec2(0.f, 68.f));
    ImGui::BeginChild(SV_STR(SV_1E94E5BD3F542563, "##friends_page"),
            ImVec2(content_width, 352.f), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    const float left_width = std::min(370.f, content_width * .58f);
    ImGui::BeginChild(SV_STR(SV_91E0B6402EE857BC, "##friends_list_card"),
            ImVec2(left_width, 352.f), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    editorial_section(SV_STR(SV_95E6853F88EC380D, "ADD FRIEND"));
    ImGui::SetNextItemWidth(left_width - 98.f);
    const bool autocomplete = !friends->suggestions.empty();
    TextAutocompleteContext autocomplete_context;
    autocomplete_context.suggestions = autocomplete ? &friends->suggestions : nullptr;
    autocomplete_context.multiple = false;
    const ImGuiInputTextFlags input_flags = ImGuiInputTextFlags_CharsNoBlank
            | ImGuiInputTextFlags_EnterReturnsTrue
            | (autocomplete ? ImGuiInputTextFlags_CallbackCompletion
                            : ImGuiInputTextFlags_None);
    const ImGuiID input_id = ImGui::GetID(SV_STR(SV_7C33FB6F4C7110D3, "##friend_name"));
    const bool submitted = ImGui::InputTextWithHint(SV_STR(SV_7C33FB6F4C7110D3, "##friend_name"),
            SV_STR(SV_CA3A085109D358CF, "Player nickname"), friend_input, IM_ARRAYSIZE(friend_input),
            input_flags, autocomplete ? autocomplete_input_callback : nullptr,
            autocomplete ? &autocomplete_context : nullptr);
    const ImVec2 input_min = ImGui::GetItemRectMin();
    const ImVec2 input_max = ImGui::GetItemRectMax();
    if (autocomplete) {
        render_inline_completion(input_id, input_min, input_max, friend_input,
                false, friends->suggestions, ImGui::IsItemActive());
    }
    ImGui::SameLine(0.f, 8.f);
    const bool can_add = std::strlen(friend_input) > 0;
    ImGui::BeginDisabled(!can_add);
    const bool add_clicked = editorial_button(SV_STR(SV_1858911831ED9ABE, "Add"),
            ImVec2(82.f, 28.f), can_add);
    ImGui::EndDisabled();
    if (can_add && (add_clicked || submitted)) {
        std::string candidate(friend_input);
        bool valid = candidate.size() <= 16u;
        for (char current : candidate) {
            if ((current >= 'A' && current <= 'Z') || (current >= 'a' && current <= 'z')
                    || (current >= '0' && current <= '9') || current == '_') continue;
            valid = false;
            break;
        }
        if (valid && !candidate.empty()) {
            bool duplicate = false;
            std::vector<std::string> next = friends->friend_players;
            for (const std::string &existing : next) {
                if (player_head_key(existing) == player_head_key(candidate)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate && next.size() < 128u) {
                next.push_back(candidate);
                queue_friend_action(SV_STR(SV_5AC4ACBFBABF1B12, "add"), candidate);
                friends->friend_players = next;
                friend_input[0] = 0;
            }
        }
    }
    ImGui::Dummy(ImVec2(1.f, 8.f));
    editorial_section(SV_STR(SV_52CFCCEEEB8D524F, "FRIENDS"));
    ImGui::BeginChild(SV_STR(SV_F172544AB7B7D55A, "##friends_rows"),
            ImVec2(-1.f, 274.f), ImGuiChildFlags_None,
            friends->friend_players.size() > 7u
                    ? ImGuiWindowFlags_AlwaysVerticalScrollbar : ImGuiWindowFlags_None);
    if (friends->friend_players.empty()) {
        ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
                SV_STR(SV_84693D00E97C295D, "%s"),
                SV_STR(SV_A48B11C0115A1D21, "No friends added yet."));
    } else {
        for (size_t index = 0; index < friends->friend_players.size(); ++index) {
            ImGui::PushID(static_cast<int>(index));
            const std::string name = friends->friend_players[index];
            const bool remove = friends_row(name,
                    ImVec2(std::max(190.f, ImGui::GetContentRegionAvail().x), 42.f));
            ImGui::PopID();
            if (remove) {
                std::vector<std::string> next = friends->friend_players;
                next.erase(next.begin() + static_cast<std::ptrdiff_t>(index));
                queue_friend_action(SV_STR(SV_D89DD57F8939DCFA, "remove"), name);
                friends->friend_players = next;
                break;
            }
            ImGui::Dummy(ImVec2(1.f, 6.f));
        }
    }
    ImGui::EndChild();
    ImGui::EndChild();

    ImGui::SameLine(0.f, 18.f);
    ImGui::BeginChild(SV_STR(SV_02824E4E6D7D9607, "##friends_options"),
            ImVec2(content_width - left_width - 18.f, 352.f), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    editorial_section(SV_STR(SV_59AB4FF2D72C97F3, "QUICK ADD"));
    ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
            SV_STR(SV_84693D00E97C295D, "%s"),
            SV_STR(SV_3652AF8F14636A8E, "Use the box to add a player by name."));
    ImGui::Dummy(ImVec2(1.f, 18.f));
    const float width = std::min(ImGui::GetContentRegionAvail().x, 190.f);
    editorial_section(SV_STR(SV_4EB36BB1FD58E082, "TARGET FILTER"));
    if (editorial_pill(SV_STR(SV_F55E132AE8E2104D, "##use_friends"), SV_STR(SV_C94D846C0652E1B7, "Use Friends"), use_friends,
            ImVec2(width, 30.f))) {
        queue_friend_action(SV_STR(SV_81D5224D5D878D8A, "enabled"), std::string(), !use_friends);
    }
    ImGui::Dummy(ImVec2(1.f, 18.f));
    editorial_section(SV_STR(SV_BC9F801BFDC1991A, "MIDDLE CLICK"));
    if (editorial_pill(SV_STR(SV_374DA4A749477092, "##middle_click"), SV_STR(SV_AA9436DB3426DBF9, "Middle Click"), middle_click,
            ImVec2(width, 30.f))) {
        queue_friend_action(SV_STR(SV_BDAE29AF68A58302, "middle"), std::string(), !middle_click);
    }
    ImGui::Dummy(ImVec2(1.f, 8.f));
    ImGui::PushStyleColor(ImGuiCol_Text, kMuted);
    ImGui::TextWrapped(SV_STR(SV_84693D00E97C295D, "%s"),
            SV_STR(SV_69287E10A3F0A7BD, "Click a player with the scroll wheel to add or remove them."));
    ImGui::PopStyleColor();
    ImGui::EndChild();
    ImGui::EndChild();
    ImGui::EndDisabled();
}

static void render_settings_other(HWND window, float card_width) {
    ImGui::SetCursorPos(ImVec2(0.f, 66.f));
    menu_card_begin(SV_STR(SV_B9008B27D099D29A, "##menu_key_card"), SV_STR(SV_143246C4A6A24B2F, "Menu key"),
            ImVec2(card_width, kCardHeaderHeight + 50.f));
    std::string menu_bind = g_capture_kind.load() == 2
            ? SV_STR(SV_B372AB19B31BD412, "Press a key...")
            : SV_STR(SV_00FF0BDD6D6F73A2, "Menu: ") + key_name(g_gui_bind.load());
    if (ImGui::Button(menu_bind.c_str(), ImVec2(-1.f, 30.f))) {
        {
            std::lock_guard<std::mutex> capture_lock(g_capture_mutex);
            g_capture_module.clear();
            g_capture_value.clear();
        }
        arm_bind_capture(2);
    }
    menu_card_end();

    ImGui::SetCursorPos(ImVec2(card_width + 12.f, 66.f));
    menu_card_begin(SV_STR(SV_403053F2D4178724, "##client_card"), SV_STR(SV_8A4D5EF269379357, "Client"),
            ImVec2(card_width, kCardHeaderHeight + 146.f));
    bool stream = g_stream_mode;
    if (menu_checkbox(SV_STR(SV_D672F6641F0B80A7, "Stream Mode"), &stream)) {
        if (SetWindowDisplayAffinity(window, stream ? 0x00000011 : WDA_NONE))
            g_stream_mode = stream;
        else
            set_status_text(SV_STR(SV_ABF83524AC01B904, "Could not apply Stream Mode"));
    }
    bool only_in_game = g_binds_in_game.load();
    if (menu_checkbox(SV_STR(SV_8C149C88172CC456, "Binds only in game"),
            &only_in_game)) {
        MenuAction action;
        action.type = ActionType::BindsInGame;
        action.enabled = only_in_game;
        queue_action(std::move(action));
        g_binds_in_game = only_in_game;
    }
    bool notifications = g_notifications.load();
    ImGui::BeginDisabled(endpoint_snapshot().alias_len == 0);
    if (menu_checkbox(SV_STR(SV_98BA0EB42B858841, "Notifications"), &notifications)) {
        MenuAction action;
        action.type = ActionType::Notifications;
        action.enabled = notifications;
        queue_action(std::move(action));
        g_notifications = notifications;
    }
    ImGui::EndDisabled();
    ImGui::Dummy(ImVec2(1.f, 6.f));
    ImGui::BeginDisabled(endpoint_snapshot().alias_len == 0);
    if (ImGui::Button(SV_STR(SV_34DE84DF8AA53E98, "Choose position modules"), ImVec2(-1.f, 30.f)))
        start_hud_edit();
    ImGui::EndDisabled();
    menu_card_end();
}

static void render_settings_config(float content_width, float card_width) {
    std::vector<CloudSlot> slots;
    {
        std::lock_guard<std::mutex> lock(g_cloud_mutex);
        slots = g_cloud_slots;
    }

    int used_count = 0;
    int first_empty_slot = -1;
    for (int slot = 0; slot < 4; ++slot) {
        bool used = false;
        for (const CloudSlot &entry : slots) {
            if (entry.slot == slot && entry.used) {
                used = true;
                ++used_count;
                break;
            }
        }
        if (!used && first_empty_slot < 0) first_empty_slot = slot;
    }

    ImGui::SetCursorPos(ImVec2(0.f, 68.f));
    ImGui::BeginChild(SV_STR(SV_4C28605DF337957D, "##configs_card"),
            ImVec2(content_width, 352.f), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    editorial_section(SV_STR(SV_CD81197F3FF83735, "NEW CONFIG"));
    ImGui::SetNextItemWidth(content_width - 130.f);
    ImGui::InputTextWithHint(SV_STR(SV_346688C10D8A4B29, "##config_name"),
            SV_STR(SV_E0679733BF73D04B, "Enter config name..."),
            g_config_name, IM_ARRAYSIZE(g_config_name));
    ImGui::SameLine(0.f, 8.f);
    const bool can_create = g_config_name[0] != 0 && first_empty_slot >= 0;
    ImGui::BeginDisabled(!can_create);
    if (editorial_button(SV_STR(SV_0E1EB52E101D796D, "Save config"),
            ImVec2(110.f, 28.f), can_create) && can_create) {
        MenuAction action;
        action.type = ActionType::ConfigSave;
        action.slot = first_empty_slot;
        action.name = g_config_name;
        queue_cloud_action(std::move(action));
    }
    ImGui::EndDisabled();

    ImGui::Dummy(ImVec2(1.f, 12.f));
    editorial_section(SV_STR(SV_08474BBC60E7FC85, "SAVED CONFIGS"));
    ImGui::SameLine(content_width - 71.f);
    ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
            SV_STR(SV_52E3D36F33B6696D, "%d / 4"), used_count);
    ImGui::Dummy(ImVec2(1.f, 3.f));
    if (g_cloud_failed.load()) {
        ImGui::PushStyleColor(ImGuiCol_Text, kMuted);
        ImGui::TextUnformatted(SV_STR(SV_EBC608238989D3A4, "Cloud unavailable."));
        ImGui::PopStyleColor();
    } else if (!g_cloud_loaded.load()) {
        ImGui::PushStyleColor(ImGuiCol_Text, kMuted);
        ImGui::TextUnformatted(SV_STR(SV_11ABB72F25E0DE56, "Loading..."));
        ImGui::PopStyleColor();
    }
    for (const CloudSlot &entry : slots) {
        if (!entry.used || entry.slot < 0 || entry.slot >= 4) continue;
        ImGui::PushID(entry.slot);
        ImGui::PushStyleColor(ImGuiCol_ChildBg, kHeader);
        ImGui::BeginChild(SV_STR(SV_B8D212BCF177D7DF, "##config_row"), ImVec2(-1.f, 48.f),
                ImGuiChildFlags_None, ImGuiWindowFlags_NoScrollbar);
        ImGui::SetCursorPos(ImVec2(12.f, entry.updated.empty() ? 16.f : 8.f));
        draw_text_shadow(ImGui::GetWindowDrawList(), ImGui::GetCursorScreenPos(),
                kText, entry.name.c_str(), nullptr, g_font_regular, 15.f);
        if (!entry.updated.empty()) {
            ImGui::SetCursorPos(ImVec2(12.f, 25.f));
            draw_text_shadow(ImGui::GetWindowDrawList(), ImGui::GetCursorScreenPos(),
                    kMuted, entry.updated.c_str(), nullptr, g_font_micro, 11.f);
        }
        ImGui::SetCursorPos(ImVec2(content_width - 210.f, 10.f));
        if (editorial_button(SV_STR(SV_FA205BAF8888F3FA, "Load"), ImVec2(58.f, 28.f))) {
            MenuAction action;
            action.type = ActionType::ConfigLoad;
            action.slot = entry.slot;
            queue_cloud_action(std::move(action));
        }
        ImGui::SameLine(0.f, 6.f);
        if (editorial_button(SV_STR(SV_13CAA4A28A8B5B78, "Save"), ImVec2(58.f, 28.f))) {
            MenuAction action;
            action.type = ActionType::ConfigSave;
            action.slot = entry.slot;
            action.name = entry.name;
            queue_cloud_action(std::move(action));
        }
        ImGui::SameLine(0.f, 6.f);
        if (editorial_button(SV_STR(SV_59058343D2C1D19E, "Delete"), ImVec2(70.f, 28.f))) {
            MenuAction action;
            action.type = ActionType::ConfigDelete;
            action.slot = entry.slot;
            queue_cloud_action(std::move(action));
        }
        ImGui::EndChild();
        ImGui::PopStyleColor();
        ImGui::Dummy(ImVec2(1.f, 5.f));
        ImGui::PopID();
    }
    if (used_count == 0 && g_cloud_loaded.load()) {
        ImGui::PushStyleColor(ImGuiCol_Text, kMuted);
        ImGui::TextUnformatted(SV_STR(SV_8A255644A9BF13B8, "No saved configs yet."));
        ImGui::PopStyleColor();
    }
    ImGui::Dummy(ImVec2(1.f, 12.f));
    ImGui::EndChild();
}

static void render_settings_community(float content_width, float card_width) {
    std::vector<CommunityConfig> configs;
    std::string user;
    {
        std::lock_guard<std::mutex> lock(g_cloud_mutex);
        configs = g_community;
        user = g_cloud_user;
    }

    ImGui::SetCursorPos(ImVec2(0.f, 68.f));
    ImGui::BeginChild(SV_STR(SV_0AC237FAB133737C, "##community_page"),
            ImVec2(content_width, 352.f), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    const float left_width = 236.f;
    const float right_x = left_width + 18.f;
    const float right_width = content_width - right_x;

    ImGui::SetCursorPos(ImVec2(0.f, 0.f));
    ImGui::BeginChild(SV_STR(SV_C3A3B91A41ACC0C3, "##publish_card"),
            ImVec2(left_width, 352.f), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    editorial_section(SV_STR(SV_FA8B05DB91130208, "PUBLISH MY CONFIG"));
    ImGui::SetNextItemWidth(-1.f);
    ImGui::InputTextWithHint(SV_STR(SV_6BD00A952AF795AC, "##publish_name"), SV_STR(SV_2523F6DA8027008C, "Config name"), g_publish_name,
            IM_ARRAYSIZE(g_publish_name));
    ImGui::SetNextItemWidth(-1.f);
    ImGui::InputTextWithHint(SV_STR(SV_F26547A5224169B2, "##publish_desc"), SV_STR(SV_8056BF516477A707, "Description"),
            g_publish_description, IM_ARRAYSIZE(g_publish_description));
    const bool can_publish = g_publish_name[0] != 0;
    ImGui::BeginDisabled(!can_publish);
    if (editorial_button(SV_STR(SV_372B30A181AFD434, "Publish"),
            ImVec2(-1.f, 28.f), can_publish) && can_publish) {
        MenuAction action;
        action.type = ActionType::CommunityPublish;
        action.name = g_publish_name;
        action.description = g_publish_description;
        queue_cloud_action(std::move(action));
    }
    ImGui::EndDisabled();
    ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
            SV_STR(SV_FF170AE063691B1E, "Others download it encrypted"));
    ImGui::EndChild();

    ImGui::SetCursorPos(ImVec2(right_x, 0.f));
    ImGui::BeginChild(SV_STR(SV_A76F0A1733CB8CCE, "##browse_card"),
            ImVec2(right_width, 352.f), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    editorial_section(SV_STR(SV_035B0120A385DE5E, "COMMUNITY CONFIGS"));
    ImGui::SetNextItemWidth(-64.f);
    ImGui::InputTextWithHint(SV_STR(SV_6FEBB7B21C10F69D, "##community_search"), SV_STR(SV_500A03534EB4AC9F, "Search"),
            g_community_search, IM_ARRAYSIZE(g_community_search));
    ImGui::SameLine(0.f, 6.f);
    if (editorial_button(SV_STR(SV_0C89402BAF7335D1, "Find"),
            ImVec2(-1.f, 26.f)))
        request_cloud_refresh(g_community_search);
    ImGui::Dummy(ImVec2(1.f, 4.f));
    ImGui::BeginChild(SV_STR(SV_DEF85B974510654C, "##community_list"),
            ImVec2(-1.f, 285.f), ImGuiChildFlags_None,
            ImGuiWindowFlags_NoScrollbar);
    if (configs.empty()) {
        ImGui::PushStyleColor(ImGuiCol_Text, kMuted);
        ImGui::TextUnformatted(g_cloud_failed.load() ? SV_STR(SV_EBC608238989D3A4, "Cloud unavailable.")
                : g_cloud_loaded.load() ? SV_STR(SV_7002D587558C49B8, "Nothing published yet.")
                                        : SV_STR(SV_11ABB72F25E0DE56, "Loading..."));
        ImGui::PopStyleColor();
    }
    for (const CommunityConfig &config : configs) {
        ImGui::PushID(config.id.c_str());
        ImGui::PushStyleColor(ImGuiCol_ChildBg, kHeader);
        ImGui::BeginChild(SV_STR(SV_8C402193A707BD7D, "##community_row"),
                ImVec2(-1.f, 62.f), ImGuiChildFlags_None,
                ImGuiWindowFlags_NoScrollbar);
        ImDrawList *draw = ImGui::GetWindowDrawList();
        const ImVec2 row = ImGui::GetWindowPos() + ImVec2(10.f, 10.f);
        draw_text_shadow(draw, row, kText, config.name.c_str(), nullptr,
                g_font_regular, 15.f);
        char meta[96]{};
        _snprintf_s(meta, sizeof(meta), _TRUNCATE, SV_STR(SV_EE21580E2B1891BF, "by %s - %d copies"),
                config.owner.c_str(), config.downloads);
        draw_text_shadow(draw, ImVec2(row.x, row.y + 22.f), kMuted, meta,
                nullptr, g_font_micro, 11.f);
        ImGui::SetCursorPos(ImVec2(right_width - 158.f, 17.f));
        if (editorial_button(SV_STR(SV_8EFB698CA8F490AC, "Copy"), ImVec2(68.f, 27.f))) {
            MenuAction action;
            action.type = ActionType::CommunityLoad;
            action.config_id = config.id;
            queue_cloud_action(std::move(action));
        }
        if (config.mine || (!user.empty() && user == config.owner)) {
            ImGui::SameLine(0.f, 6.f);
            if (editorial_button(SV_STR(SV_8AAAEAA81B5B0C3E, "Remove"), ImVec2(76.f, 27.f))) {
                MenuAction action;
                action.type = ActionType::CommunityDelete;
                action.config_id = config.id;
                queue_cloud_action(std::move(action));
            }
        }
        ImGui::EndChild();
        ImGui::PopStyleColor();
        ImGui::Dummy(ImVec2(1.f, 6.f));
        ImGui::PopID();
    }
    ImGui::EndChild();
    ImGui::EndChild();
    ImGui::EndChild();
}

static void queue_party_action(MenuAction action) {
    if (g_party_busy.exchange(true)) return;
    queue_action(std::move(action));
}

static void render_settings_party(float) {
    JsonValue party;
    std::string feedback;
    {
        std::lock_guard<std::mutex> lock(g_party_mutex);
        party = g_party_state;
        feedback = g_party_feedback;
    }
    const std::string code = json_string(party.get(SV_STR(SV_7F790B76397F2D40, "code")));
    const bool leader = json_string(party.get(SV_STR(SV_A8C66A7BD412E9ED, "myRole"))) == SV_STR(SV_8F066BC9696C4888, "leader");
    const bool available = g_connected.load() && endpoint_snapshot().alias_len != 0;
    const bool busy = g_party_busy.load();
    static char join_code[7]{};
    static bool activate = false;

    ImGui::SetCursorPos(ImVec2(0.f, 68.f));
    ImGui::BeginChild(SV_STR(SV_87E5CAF2F5637B82, "##party_page"), ImVec2(622.f, 352.f),
            ImGuiChildFlags_None, ImGuiWindowFlags_NoScrollbar);
    if (!feedback.empty()) {
        ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted), SV_STR(SV_84693D00E97C295D, "%s"),
                feedback.c_str());
        ImGui::Dummy(ImVec2(1.f, 5.f));
    }

    ImGui::BeginDisabled(!available || busy);
    if (code.empty()) {
        editorial_section(SV_STR(SV_F6030F8B5A20D9B0, "PARTY"));
        ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
                SV_STR(SV_7D49AFA221C7CD00, "Create a party or join with a 6-character code."));
        ImGui::Dummy(ImVec2(1.f, 8.f));
        if (editorial_button(SV_STR(SV_2739619F191E30C7, "Create party"), ImVec2(180.f, 30.f), true)) {
            MenuAction action;
            action.type = ActionType::PartyCreate;
            queue_party_action(std::move(action));
        }

        ImGui::Dummy(ImVec2(1.f, 20.f));
        editorial_section(SV_STR(SV_F3A96E1C8EC0DFDD, "JOIN A PARTY"));
        ImGui::SetNextItemWidth(260.f);
        ImGui::InputTextWithHint(SV_STR(SV_7DEF9785608E761F, "##party_code"), SV_STR(SV_D6F672216F18FB71, "Party code"), join_code,
                sizeof(join_code), ImGuiInputTextFlags_CharsUppercase
                        | ImGuiInputTextFlags_CharsNoBlank);
        ImGui::SameLine(0.f, 8.f);
        ImGui::BeginDisabled(std::strlen(join_code) != 6);
        if (editorial_button(SV_STR(SV_ED11C1259183FD75, "Join party"), ImVec2(120.f, 30.f),
                std::strlen(join_code) == 6)) {
            MenuAction action;
            action.type = ActionType::PartyJoin;
            action.name = join_code;
            queue_party_action(std::move(action));
        }
        ImGui::EndDisabled();
    } else {
        const float content_y = feedback.empty() ? 0.f : 26.f;
        const float room_width = leader ? 236.f : 420.f;
        ImGui::SetCursorPos(ImVec2(0.f, content_y));
        ImGui::BeginChild(SV_STR(SV_B24A2F5AB0FC6100, "##party_room"), ImVec2(room_width, 326.f),
                ImGuiChildFlags_None, ImGuiWindowFlags_NoScrollbar);
        editorial_section(SV_STR(SV_5316BC467DF6FA3D, "PARTY ROOM"));
        ImGui::Text(SV_STR(SV_A1E87255CA648548, "Code: %s"), code.c_str());
        if (editorial_button(SV_STR(SV_C395F0168563E5AB, "Copy code"), ImVec2(96.f, 27.f)))
            ImGui::SetClipboardText(code.c_str());
        ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(kMuted),
                leader ? SV_STR(SV_2F3FA05C17BEE29D, "You are the leader") : SV_STR(SV_60E64D71302F16F7, "You are a member"));
        ImGui::Dummy(ImVec2(1.f, 8.f));
        editorial_section(SV_STR(SV_924E3567D1DCF0E0, "MEMBERS"));
        ImGui::BeginChild(SV_STR(SV_ADD26E6BDCBEF566, "##party_members"), ImVec2(0.f, 145.f),
                ImGuiChildFlags_None, ImGuiWindowFlags_NoScrollbar);
        const JsonValue *members = party.get(SV_STR(SV_3FFCD60A55B5366C, "members"));
        if (members != nullptr && members->kind == JsonValue::Array) {
            for (const JsonValue &member : members->array) {
                const std::string name = json_string(member.get(SV_STR(SV_26B56BB7D4E9A35E, "name")));
                const std::string role = json_string(member.get(SV_STR(SV_519EE08DD828C5E8, "role")));
                ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(
                        role == SV_STR(SV_8F066BC9696C4888, "leader") ? kText : kMuted), SV_STR(SV_DD3886C09F26BEDD, "%s (%s)"),
                        name.c_str(), role.c_str());
                const JsonValue *member_id = member.get(SV_STR(SV_7EB277F4270C2A8B, "id"));
                if (leader && role != SV_STR(SV_8F066BC9696C4888, "leader")
                        && member_id != nullptr && member_id->kind == JsonValue::String) {
                    ImGui::SameLine();
                    ImGui::PushID(member_id->string.c_str());
                    if (editorial_button(SV_STR(SV_3F30389B34152FA7, "Kick"), ImVec2(58.f, 21.f))) {
                        MenuAction action;
                        action.type = ActionType::PartyKick;
                        action.name = member_id->string;
                        queue_party_action(std::move(action));
                    }
                    ImGui::PopID();
                }
            }
        }
        ImGui::EndChild();
        if (editorial_button(SV_STR(SV_CD0BF094B5F3563A, "Leave party"), ImVec2(120.f, 28.f))) {
            MenuAction action;
            action.type = ActionType::PartyLeave;
            queue_party_action(std::move(action));
        }
        ImGui::EndChild();

        // Sharing controls only exist while this player is the party leader.
        if (leader) {
            ImGui::SetCursorPos(ImVec2(room_width + 18.f, content_y));
            ImGui::BeginChild(SV_STR(SV_286DDE74538CB4B8, "##party_sync"),
                    ImVec2(622.f - room_width - 18.f, 326.f),
                    ImGuiChildFlags_None, ImGuiWindowFlags_NoScrollbar);
            editorial_section(SV_STR(SV_CB3F67B585FA918C, "SHARE MODULE SETTINGS"));
            const char *activate_label = SV_STR(SV_E5C578D2B614E2D3, "Also enable selected modules");
            const float activate_width = std::min(
                    ImGui::GetContentRegionAvail().x,
                    ImGui::CalcTextSize(activate_label).x + 30.f);
            if (editorial_pill(SV_STR(SV_964985D0CAA79DC0, "##activate_modules"), activate_label, activate,
                    ImVec2(activate_width, 28.f)))
                activate = !activate;
            ImGui::Dummy(ImVec2(1.f, 6.f));
            ImGui::BeginChild(SV_STR(SV_6DC884E1AC27C005, "##party_modules"), ImVec2(0.f, 218.f),
                    ImGuiChildFlags_None, ImGuiWindowFlags_NoScrollbar);
            for (const MenuModule &module : g_modules) {
                ImGui::PushID(module.id.c_str());
                const std::string &label = module.label;
                const bool selected = g_party_selection[module.id];
                const float width = std::min(ImGui::GetContentRegionAvail().x,
                        calc_visible_text_size(ImGui::GetFont(),
                                ImGui::GetFontSize(), label.c_str()).x + 30.f);
                if (editorial_pill(SV_STR(SV_4B9063E51F13A4A3, "##party_module"), label.c_str(), selected,
                        ImVec2(width, 27.f)))
                    g_party_selection[module.id] = !selected;
                ImGui::PopID();
            }
            ImGui::Dummy(ImVec2(1.f, 12.f));
            ImGui::EndChild();

            MenuAction sync;
            sync.type = ActionType::PartySync;
            sync.enabled = activate;
            for (const MenuModule &module : g_modules)
                if (g_party_selection[module.id])
                    sync.shared_modules.push_back(module.id);
            ImGui::BeginDisabled(sync.shared_modules.empty());
            if (editorial_button(SV_STR(SV_EAA37FE1BBB4ED19, "Sync selected modules"), ImVec2(-1.f, 28.f),
                    !sync.shared_modules.empty()))
                queue_party_action(std::move(sync));
            ImGui::EndDisabled();
            ImGui::EndChild();
        }
    }
    ImGui::EndDisabled();

    if (!available)
        ImGui::TextWrapped(SV_STR(SV_5F8E7C7DB9DFA8B1, "Connect the client to manage your party."));
    if (busy)
        ImGui::TextUnformatted(SV_STR(SV_C7CAA7706B41B4E2, "Working..."));
    ImGui::EndChild();
}

static void render_settings_page(HWND window, float content_width,
        float body_height) {
    static const std::array<std::string, 5> kTitles = {
        std::string(SV_STR(SV_6D04B3CEAC7178A2, "Other")),
        SV_STR(SV_02BABF8969D1F030, "Friends"),
        std::string(SV_STR(SV_122617E94D2C2119, "Config")),
        std::string(SV_STR(SV_5D99DA387A853B5E, "Community")), SV_STR(SV_ED522AEEF1DEDF22, "Party")};
    static const std::array<std::string, 5> kSubtitles = {
        std::string(SV_STR(SV_4089358E84081E17, "Menu key and client wide switches")),
        SV_STR(SV_CC5EBEB9927A461A, "Manage your local player friends"),
        std::string(SV_STR(SV_787AFB47CDEEF80F, "Your configs, stored encrypted in the cloud")),
        std::string(SV_STR(SV_489C3A897BF60790, "Publish a config or copy someone else's")),
        SV_STR(SV_C8F595DEC6D2BB42, "Create a room, invite friends and share settings")};
    ImGui::SetCursorPos(ImVec2(248.f, 78.f));
    ImGui::BeginChild(SV_STR(SV_0C12521D0DA22487, "##settings_page"), ImVec2(content_width, body_height),
            ImGuiChildFlags_None, ImGuiWindowFlags_NoScrollbar);
    ImFont *title_font = g_font_display != nullptr ? g_font_display : g_font_bold;
    draw_text_shadow(ImGui::GetWindowDrawList(), ImGui::GetCursorScreenPos(),
            kText, kTitles[g_settings_page].c_str(), nullptr, title_font, 34.f);
    ImGui::Dummy(ImVec2(1.f, 34.f));
    draw_text_shadow(ImGui::GetWindowDrawList(), ImGui::GetCursorScreenPos(),
            kMuted, kSubtitles[g_settings_page].c_str(), nullptr,
            g_font_regular, 16.f);
    ImGui::Dummy(ImVec2(1.f, 16.f));

    if (g_settings_page == 2 || g_settings_page == 3) {
        ImGui::SetCursorPos(ImVec2(content_width - 92.f, 2.f));
        if (ImGui::Button(g_cloud_busy.load() ? SV_STR(SV_40D92820D1F1E3D9, "Loading") : SV_STR(SV_6FFCC7F5C2DBE76F, "Refresh"),
                ImVec2(90.f, 28.f)))
            request_cloud_refresh(g_community_search);
    }

    const float card_width = (content_width - 12.f) * 0.5f;
    if (g_settings_page == 0) {
        render_settings_other(window, card_width);
    } else if (g_settings_page == 1) {
        render_settings_friends(content_width);
    } else if (g_settings_page == 2) {
        render_settings_config(content_width, card_width);
    } else if (g_settings_page == 4) {
        render_settings_party(card_width);
    } else {
        render_settings_community(content_width, card_width);
    }
    ImGui::EndChild();
}

static void render_menu_frame(HWND window) {
    ImGui::SetNextWindowPos(ImVec2(0.f, 0.f));
    ImGui::SetNextWindowSize(ImVec2(static_cast<float>(kMenuWidth),
            static_cast<float>(kMenuHeight)));
    ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(0.f, 0.f));
    ImGui::PushStyleVar(ImGuiStyleVar_WindowRounding, 4.f);
    ImGui::PushStyleColor(ImGuiCol_WindowBg, kBackground);
    if (g_hud_edit.load()) {
        ImGui::PopStyleColor();
        ImGui::PopStyleVar(2);
        render_hud_editor();
        return;
    }
    ImGui::Begin(SV_STR(SV_7221AA1548282644, "##crimson_modules"), nullptr,
            ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove
            | ImGuiWindowFlags_NoScrollbar);
    ImGui::PushFont(g_font_regular);

    ImDrawList *draw = ImGui::GetWindowDrawList();
    ImVec2 origin = ImGui::GetWindowPos();
    draw->AddRectFilled(origin, origin + ImVec2(kMenuWidth, kMenuHeight),
            kBackground, 4.f);
    draw->AddRect(origin, origin + ImVec2(kMenuWidth, kMenuHeight),
            IM_COL32(55, 55, 55, 255), 4.f, 0, 1.f);

    std::lock_guard<std::mutex> data_lock(g_data_mutex);
    std::vector<std::string> categories;
    for (const MenuModule &module : g_modules) {
        if (std::find(categories.begin(), categories.end(), module.category)
                == categories.end()) categories.push_back(module.category);
    }
    const std::string settings_category = SV_STR(SV_295B1ECB81785443, "Settings");
    categories.push_back(settings_category);
    if (g_selected_category.empty() && !categories.empty())
        g_selected_category = categories.front();
    if (std::find(categories.begin(), categories.end(), g_selected_category)
            == categories.end() && !categories.empty())
        g_selected_category = categories.front();
    const bool settings_page = g_selected_category == settings_category;

    // Segmented category rail from eyeofgod. Its active cell glides to the
    // selected category while all hierarchy remains strictly monochrome.
    static float rail_position = 0.f;
    static bool search_open = false;
    const float rail_y = 18.f;
    const float rail_h = 30.f;
    const float rail_pad = 5.f;
    const float search_size = 40.f;
    const float maximum_rail_width = 680.f;
    const float cell_width = std::min(106.f,
            (maximum_rail_width - rail_pad * 2.f)
                    / std::max(1.f, static_cast<float>(categories.size())));
    const float rail_width = cell_width * static_cast<float>(categories.size())
            + rail_pad * 2.f;
    const float rail_x = (kMenuWidth - rail_width) * .5f - search_size * .5f;
    const float search_x = rail_x + rail_width + 10.f;

    int selected_category_index = 0;
    for (size_t index = 0; index < categories.size(); ++index)
        if (categories[index] == g_selected_category)
            selected_category_index = static_cast<int>(index);
    rail_position = ImLerp(rail_position,
            static_cast<float>(selected_category_index),
            ImClamp(ImGui::GetIO().DeltaTime * 14.f, 0.f, 1.f));

    if (!search_open) {
        draw->AddRectFilled(origin + ImVec2(rail_x, rail_y),
                origin + ImVec2(rail_x + rail_width, rail_y + rail_h + rail_pad * 2.f),
                kHeader, (rail_h + rail_pad * 2.f) * .5f);
        const float pill_x = rail_x + rail_pad + rail_position * cell_width;
        draw->AddRectFilled(origin + ImVec2(pill_x, rail_y + rail_pad),
                origin + ImVec2(pill_x + cell_width, rail_y + rail_pad + rail_h),
                IM_COL32(32, 32, 32, 255), rail_h * .5f);
        for (size_t index = 0; index < categories.size(); ++index) {
            const std::string label = uppercase_copy(categories[index]);
            const float x = rail_x + rail_pad + cell_width * index;
            ImGui::SetCursorPos(ImVec2(x, rail_y + rail_pad));
            ImGui::PushID(static_cast<int>(index));
            ImGui::InvisibleButton(SV_STR(SV_06D3211B92AC969F, "##category"), ImVec2(cell_width, rail_h));
            const bool hovered = ImGui::IsItemHovered();
            if (ImGui::IsItemClicked()) {
                g_selected_category = categories[index];
                g_selected_module.clear();
                g_search[0] = '\0';
            }
            ImGui::PopID();
            ImFont *font = g_font_micro != nullptr ? g_font_micro : g_font_regular;
            const ImVec2 size = font->CalcTextSizeA(11.f, FLT_MAX, 0.f,
                    label.c_str());
            draw->AddText(font, 11.f,
                    origin + ImVec2(x + (cell_width - size.x) * .5f,
                            rail_y + rail_pad + (rail_h - 11.f) * .5f),
                    index == static_cast<size_t>(selected_category_index)
                            ? kText : hovered ? kMuted
                                    : IM_COL32(69, 69, 69, 255),
                    label.c_str());
        }
        ImGui::SetCursorPos(ImVec2(search_x, rail_y));
        ImGui::InvisibleButton(SV_STR(SV_1416A0B372701116, "##open_search"), ImVec2(search_size, search_size));
        const bool search_hovered = ImGui::IsItemHovered();
        if (ImGui::IsItemClicked()) search_open = true;
        draw->AddCircle(origin + ImVec2(search_x + 18.f, rail_y + 18.f),
                6.f, search_hovered ? kText : IM_COL32(69, 69, 69, 255), 0, 1.5f);
        draw->AddLine(origin + ImVec2(search_x + 22.f, rail_y + 22.f),
                origin + ImVec2(search_x + 27.f, rail_y + 27.f),
                search_hovered ? kText : IM_COL32(69, 69, 69, 255), 1.5f);
    } else {
        const float search_width = rail_width + search_size + 10.f;
        ImGui::SetCursorPos(ImVec2(rail_x, rail_y));
        ImGui::SetNextItemWidth(search_width);
        ImGui::PushStyleColor(ImGuiCol_FrameBg, kHeader);
        ImGui::PushStyleColor(ImGuiCol_FrameBgHovered, kHeader);
        ImGui::PushStyleColor(ImGuiCol_FrameBgActive, kHeader);
        ImGui::PushStyleColor(ImGuiCol_Border, IM_COL32(30, 30, 30, 255));
        ImGui::PushStyleVar(ImGuiStyleVar_FrameBorderSize, 1.f);
        ImGui::PushStyleVar(ImGuiStyleVar_FrameRounding, 20.f);
        ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(18.f, 10.f));
        ImGui::SetKeyboardFocusHere();
        ImGui::InputTextWithHint(SV_STR(SV_A78BB3B265E7C25F, "##search"), SV_STR(SV_4D9372A43D7A8CA8, "SEARCH MODULES..."), g_search,
                IM_ARRAYSIZE(g_search));
        ImGui::PopStyleVar(3);
        ImGui::PopStyleColor(4);
        if (ImGui::IsKeyPressed(ImGuiKey_Escape)
                || (!ImGui::IsItemActive() && ImGui::IsMouseClicked(ImGuiMouseButton_Left)
                        && !ImGui::IsItemHovered())) {
            search_open = false;
            g_search[0] = '\0';
        }
    }

    std::string query(g_search);
    std::vector<MenuModule *> visible;
    for (MenuModule &module : g_modules) {
        if ((!query.empty() || module.category == g_selected_category)
                && contains_ci_visible(module.label, query)) visible.push_back(&module);
    }
    MenuModule *selected = nullptr;
    for (MenuModule *module : visible) {
        if (module->id == g_selected_module) selected = module;
    }
    if (selected == nullptr && !visible.empty()) {
        selected = visible.front();
        g_selected_module = selected->id;
        if (!query.empty()) g_selected_category = selected->category;
    }

    if (settings_page) {
        render_settings_sidebar();
        render_settings_page(window, 622.f, 434.f);
    } else {
        ImGui::SetCursorPos(ImVec2(30.f, 78.f));
        ImGui::BeginChild(SV_STR(SV_377B8DD4EE7412DB, "##module_list"), ImVec2(180.f, 434.f),
                ImGuiChildFlags_None, ImGuiWindowFlags_NoScrollbar);
        for (MenuModule *module : visible) {
            if (sidebar_module(*module, module->id == g_selected_module)) {
                g_selected_module = module->id;
                g_selected_category = module->category;
                selected = module;
            }
        }
        if (visible.empty()) {
            ImGui::PushStyleColor(ImGuiCol_Text, kMuted);
            ImGui::TextWrapped(SV_STR(SV_A631D91E8E7B87FF, "No modules found."));
            ImGui::PopStyleColor();
        }
        ImGui::EndChild();

        if (selected != nullptr)
            render_module_content(*selected, g_font_bold, 622.f, 434.f);
    }

    ImGui::PopFont();
    ImGui::End();
    ImGui::PopStyleColor();
    ImGui::PopStyleVar(2);
}

static void create_render_target() {
    ID3D11Texture2D *buffer = nullptr;
    if (g_swap_chain != nullptr
            && SUCCEEDED(g_swap_chain->GetBuffer(0, IID_PPV_ARGS(&buffer)))) {
        g_device->CreateRenderTargetView(buffer, nullptr, &g_render_target);
        buffer->Release();
    }
}

// The loader ships the crimson mark as a raw RGBA block; the menu uploads the
// very same bytes so both windows carry an identical logo.
static bool create_logo_texture() {
    if (g_logo_texture != nullptr) return true;
    if (g_device == nullptr) return false;
    D3D11_TEXTURE2D_DESC description{};
    description.Width = CRIMSON_LOGO_WIDTH;
    description.Height = CRIMSON_LOGO_HEIGHT;
    description.MipLevels = 1;
    description.ArraySize = 1;
    description.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    description.SampleDesc.Count = 1;
    description.Usage = D3D11_USAGE_IMMUTABLE;
    description.BindFlags = D3D11_BIND_SHADER_RESOURCE;

    D3D11_SUBRESOURCE_DATA pixels{};
    pixels.pSysMem = CRIMSON_LOGO_RGBA;
    pixels.SysMemPitch = CRIMSON_LOGO_WIDTH * 4;

    ID3D11Texture2D *texture = nullptr;
    if (FAILED(g_device->CreateTexture2D(&description, &pixels, &texture))
            || texture == nullptr) {
        return false;
    }
    const HRESULT result = g_device->CreateShaderResourceView(texture, nullptr,
            &g_logo_texture);
    texture->Release();
    return SUCCEEDED(result) && g_logo_texture != nullptr;
}

static void cleanup_logo_texture() {
    if (g_logo_texture != nullptr) {
        g_logo_texture->Release();
        g_logo_texture = nullptr;
    }
}

static void cleanup_player_head_textures() {
    std::lock_guard<std::mutex> lock(g_player_head_mutex);
    for (auto &entry : g_player_heads) {
        if (entry.second.texture != nullptr) {
            entry.second.texture->Release();
            entry.second.texture = nullptr;
        }
    }
    g_player_heads.clear();
}

static void cleanup_render_target() {
    if (g_render_target != nullptr) {
        g_render_target->Release();
        g_render_target = nullptr;
    }
}

static bool create_d3d_device(HWND window) {
    DXGI_SWAP_CHAIN_DESC description{};
    description.BufferCount = 2;
    description.BufferDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    description.BufferDesc.RefreshRate.Numerator = 60;
    description.BufferDesc.RefreshRate.Denominator = 1;
    description.Flags = DXGI_SWAP_CHAIN_FLAG_ALLOW_MODE_SWITCH;
    description.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    description.OutputWindow = window;
    description.SampleDesc.Count = 1;
    description.Windowed = TRUE;
    description.SwapEffect = DXGI_SWAP_EFFECT_DISCARD;
    D3D_FEATURE_LEVEL selected{};
    const D3D_FEATURE_LEVEL levels[] = {
        D3D_FEATURE_LEVEL_11_0, D3D_FEATURE_LEVEL_10_0
    };
    HRESULT result = D3D11CreateDeviceAndSwapChain(nullptr,
            D3D_DRIVER_TYPE_HARDWARE, nullptr, 0, levels,
            static_cast<UINT>(IM_ARRAYSIZE(levels)), D3D11_SDK_VERSION,
            &description, &g_swap_chain, &g_device, &selected,
            &g_device_context);
    if (FAILED(result)) return false;
    create_render_target();
    create_logo_texture();
    return g_render_target != nullptr;
}

static void cleanup_d3d_device() {
    cleanup_player_head_textures();
    cleanup_logo_texture();
    cleanup_render_target();
    if (g_swap_chain != nullptr) {
        g_swap_chain->Release();
        g_swap_chain = nullptr;
    }
    if (g_device_context != nullptr) {
        g_device_context->Release();
        g_device_context = nullptr;
    }
    if (g_device != nullptr) {
        g_device->Release();
        g_device = nullptr;
    }
}

static void initialize_style() {
    ImGui::StyleColorsDark();
    ImGuiIO &io = ImGui::GetIO();
    io.IniFilename = nullptr;
    io.LogFilename = nullptr;
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
    // Keep category icons in a dedicated atlas font at their rendered size.
    g_font_regular = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterMedium_compressed_data, InterMedium_compressed_size, 15.f);
    static const ImWchar icon_ranges[] = {0xe000, 0xf8ff, 0};
    ImFontConfig icon_config;
    icon_config.MergeMode = false;
    icon_config.PixelSnapH = true;
    g_font_icons = io.Fonts->AddFontFromMemoryCompressedTTF(
            FontAwesomeData, FontAwesomeSize, 20.f, &icon_config, icon_ranges);
    g_font_medium = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterMedium_compressed_data, InterMedium_compressed_size, 17.f);
    g_font_bold = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterSemiBold_compressed_data, InterSemiBold_compressed_size, 18.f);
    g_font_micro = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterMedium_compressed_data, InterMedium_compressed_size, 11.f);
    g_font_display = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterSemiBold_compressed_data, InterSemiBold_compressed_size, 34.f);
    if (g_font_regular == nullptr) g_font_regular = io.Fonts->AddFontDefault();
    if (g_font_bold == nullptr) g_font_bold = g_font_regular;
    if (g_font_medium == nullptr) g_font_medium = g_font_regular;
    if (g_font_icons == nullptr) g_font_icons = g_font_regular;
    if (g_font_micro == nullptr) g_font_micro = g_font_regular;
    if (g_font_display == nullptr) g_font_display = g_font_bold;

    ImGuiStyle &style = ImGui::GetStyle();
    style.WindowBorderSize = 0.f;
    style.WindowPadding = ImVec2(0.f, 0.f);
    style.WindowRounding = 4.f;
    style.ChildRounding = 4.f;
    style.FrameRounding = 14.f;
    style.PopupRounding = 8.f;
    style.FrameBorderSize = 0.f;
    style.ScrollbarRounding = 2.f;
    style.ScrollbarSize = 4.f;
    style.GrabRounding = 3.f;
    style.ItemSpacing = ImVec2(8.f, 6.f);
    style.ItemInnerSpacing = ImVec2(6.f, 5.f);
    style.FramePadding = ImVec2(9.f, 6.f);
    style.Colors[ImGuiCol_Text] = ImColor(244, 244, 244);
    style.Colors[ImGuiCol_TextDisabled] = ImColor(69, 69, 69);
    style.Colors[ImGuiCol_WindowBg] = ImColor(11, 11, 11);
    style.Colors[ImGuiCol_ChildBg] = ImColor(11, 11, 11);
    style.Colors[ImGuiCol_PopupBg] = ImColor(17, 17, 17);
    style.Colors[ImGuiCol_Border] = ImColor(30, 30, 30);
    style.Colors[ImGuiCol_FrameBg] = ImColor(17, 17, 17);
    style.Colors[ImGuiCol_FrameBgHovered] = ImColor(24, 24, 24);
    style.Colors[ImGuiCol_FrameBgActive] = ImColor(30, 30, 30);
    style.Colors[ImGuiCol_CheckMark] = ImColor(11, 11, 11);
    style.Colors[ImGuiCol_SliderGrab] = ImColor(124, 124, 124);
    style.Colors[ImGuiCol_SliderGrabActive] = ImColor(244, 244, 244);
    style.Colors[ImGuiCol_Button] = ImColor(17, 17, 17);
    style.Colors[ImGuiCol_ButtonHovered] = ImColor(30, 30, 30);
    style.Colors[ImGuiCol_ButtonActive] = ImColor(38, 38, 38);
    style.Colors[ImGuiCol_Header] = ImColor(30, 30, 30);
    style.Colors[ImGuiCol_HeaderHovered] = ImColor(38, 38, 38);
    style.Colors[ImGuiCol_HeaderActive] = ImColor(48, 48, 48);
}

static BOOL CALLBACK find_game_window(HWND window, LPARAM parameter) {
    HWND *result = reinterpret_cast<HWND *>(parameter);
    DWORD process_id = 0;
    wchar_t class_name[64]{};
    GetWindowThreadProcessId(window, &process_id);
    if (process_id != GetCurrentProcessId()
            || GetClassNameW(window, class_name,
                    static_cast<int>(IM_ARRAYSIZE(class_name))) <= 0) {
        return TRUE;
    }
    if (wcscmp(class_name, SV_WSTR(SV_5E59FD2BF0A848C7, L"LWJGL")) == 0
            || wcscmp(class_name, SV_WSTR(SV_862DDF7320DD459C, L"LWJGL3")) == 0
            || wcscmp(class_name, SV_WSTR(SV_43DBB52F0BB87710, L"GLFW30")) == 0) {
        *result = window;
        return FALSE;
    }
    return TRUE;
}

static HWND resolve_game_window() {
    HWND result = nullptr;
    EnumWindows(find_game_window, reinterpret_cast<LPARAM>(&result));
    return result;
}

static bool is_forwarded_keyboard_message(UINT message) {
    return message == WM_KEYDOWN || message == WM_KEYUP
            || message == WM_SYSKEYDOWN || message == WM_SYSKEYUP
            || message == WM_CHAR || message == WM_SYSCHAR
            || message == WM_UNICHAR || message == WM_IME_CHAR;
}

static bool is_game_mouse_message(UINT message) {
    switch (message) {
        case WM_INPUT:
        case WM_MOUSEMOVE:
        case WM_MOUSEWHEEL:
        case WM_MOUSEHWHEEL:
        case WM_LBUTTONDOWN:
        case WM_LBUTTONUP:
        case WM_LBUTTONDBLCLK:
        case WM_RBUTTONDOWN:
        case WM_RBUTTONUP:
        case WM_RBUTTONDBLCLK:
        case WM_MBUTTONDOWN:
        case WM_MBUTTONUP:
        case WM_MBUTTONDBLCLK:
        case WM_XBUTTONDOWN:
        case WM_XBUTTONUP:
        case WM_XBUTTONDBLCLK:
            return true;
        default:
            return false;
    }
}

static LPARAM translate_game_mouse_lparam(HWND source, UINT message,
        LPARAM lparam) {
    // Wheel coordinates are screen-relative and the ImGui backend only reads
    // the delta from them. Button and move coordinates need to be converted
    // from the game client to the independent overlay client.
    if (message == WM_MOUSEWHEEL || message == WM_MOUSEHWHEEL
            || source == nullptr || g_imgui_window == nullptr)
        return lparam;
    POINT point{static_cast<short>(LOWORD(lparam)),
            static_cast<short>(HIWORD(lparam))};
    if (!ClientToScreen(source, &point)
            || !ScreenToClient(g_imgui_window, &point))
        return lparam;
    return MAKELPARAM(static_cast<short>(point.x),
            static_cast<short>(point.y));
}

static void forward_game_mouse_message(HWND source, UINT message,
        WPARAM wparam, LPARAM lparam) {
    if (g_imgui_window == nullptr || message == WM_INPUT) return;
    PostMessageW(g_imgui_window, message, wparam,
            translate_game_mouse_lparam(source, message, lparam));
}

static bool should_translate_message(const MSG &message) {
    // Minecraft already translated the physical key before its window proc
    // received it. The resulting WM_CHAR is forwarded below, so translating
    // the copied key-down on the overlay thread would either duplicate text
    // or fail because that thread does not own the game's keyboard queue.
    return message.hwnd != g_imgui_window
            && (message.message == WM_KEYDOWN
                    || message.message == WM_SYSKEYDOWN);
}

static LRESULT CALLBACK game_input_window_proc(HWND window, UINT message,
        WPARAM wparam, LPARAM lparam) {
    if (g_visible.load() && is_game_mouse_message(message)) {
        // Keep the game from consuming menu input. Forwarding button and
        // move messages also preserves ImGui's mouse capture when a drag
        // leaves the overlay window (the old swallow-only path could leave a
        // slider permanently held).
        forward_game_mouse_message(window, message, wparam, lparam);
        return message == WM_INPUT
                ? DefWindowProcW(window, message, wparam, lparam) : 0;
    }
    if (g_visible.load() && g_imgui_window != nullptr
            && is_forwarded_keyboard_message(message)) {
        if ((message == WM_KEYDOWN || message == WM_SYSKEYDOWN)
                && g_capture_kind.load() != 0
                && handle_bind_capture_key(static_cast<int>(wparam)))
            return 0;
        // Forward the character produced on Minecraft's input thread. The
        // non-activating overlay cannot reliably create that character by
        // translating a copied key-down on its separate UI thread.
        PostMessageW(g_imgui_window, message, wparam, lparam);
        return 0;
    }
    return g_game_original_proc != nullptr
            ? CallWindowProcW(g_game_original_proc, window, message, wparam, lparam)
            : DefWindowProcW(window, message, wparam, lparam);
}

static void uninstall_game_input_forwarder();

static void install_game_input_forwarder() {
    HWND game = resolve_game_window();
    if (game == nullptr) return;
    if (g_game_original_proc != nullptr && g_game_input_window == game
            && IsWindow(game)) return;
    if (g_game_original_proc != nullptr || g_game_input_window != nullptr)
        uninstall_game_input_forwarder();
    LONG_PTR previous = SetWindowLongPtrW(game, GWLP_WNDPROC,
            reinterpret_cast<LONG_PTR>(game_input_window_proc));
    if (previous != 0) {
        g_game_input_window = game;
        g_game_original_proc = reinterpret_cast<WNDPROC>(previous);
    }
}

static void uninstall_game_input_forwarder() {
    if (g_game_input_window != nullptr && g_game_original_proc != nullptr
            && IsWindow(g_game_input_window)) {
        WNDPROC current = reinterpret_cast<WNDPROC>(GetWindowLongPtrW(
                g_game_input_window, GWLP_WNDPROC));
        if (current == game_input_window_proc)
            SetWindowLongPtrW(g_game_input_window, GWLP_WNDPROC,
                    reinterpret_cast<LONG_PTR>(g_game_original_proc));
    }
    g_game_input_window = nullptr;
    g_game_original_proc = nullptr;
}

static void make_game_borderless() {
    if (g_game_window_restore.active) return;
    HWND game = resolve_game_window();
    if (game == nullptr || !GetWindowRect(game, &g_game_window_restore.bounds))
        return;
    g_game_window_restore.window = game;
    g_game_window_restore.style = GetWindowLongPtrW(game, GWL_STYLE);
    g_game_window_restore.ex_style = GetWindowLongPtrW(game, GWL_EXSTYLE);

    HMONITOR monitor = MonitorFromWindow(game, MONITOR_DEFAULTTONEAREST);
    MONITORINFO info{};
    info.cbSize = sizeof(info);
    if (!GetMonitorInfoW(monitor, &info)) return;

    const LONG_PTR borderless_style =
            (g_game_window_restore.style & ~(WS_CAPTION | WS_THICKFRAME
                    | WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_SYSMENU))
            | WS_POPUP | WS_VISIBLE;
    const LONG_PTR borderless_ex_style = g_game_window_restore.ex_style
            & ~(WS_EX_DLGMODALFRAME | WS_EX_WINDOWEDGE | WS_EX_CLIENTEDGE
                    | WS_EX_STATICEDGE);
    SetWindowLongPtrW(game, GWL_STYLE, borderless_style);
    SetWindowLongPtrW(game, GWL_EXSTYLE, borderless_ex_style);
    const RECT monitor_bounds = info.rcMonitor;
    SetWindowPos(game, HWND_NOTOPMOST, monitor_bounds.left,
            monitor_bounds.top, monitor_bounds.right - monitor_bounds.left,
            monitor_bounds.bottom - monitor_bounds.top,
            SWP_FRAMECHANGED | SWP_NOACTIVATE | SWP_SHOWWINDOW);
    g_game_window_restore.active = true;
}

static void restore_game_window() {
    if (!g_game_window_restore.active) return;
    HWND game = g_game_window_restore.window;
    if (game != nullptr && IsWindow(game)) {
        SetWindowLongPtrW(game, GWL_STYLE, g_game_window_restore.style);
        SetWindowLongPtrW(game, GWL_EXSTYLE, g_game_window_restore.ex_style);
        const RECT bounds = g_game_window_restore.bounds;
        SetWindowPos(game, nullptr, bounds.left, bounds.top,
                bounds.right - bounds.left, bounds.bottom - bounds.top,
                SWP_FRAMECHANGED | SWP_NOACTIVATE | SWP_NOZORDER
                        | SWP_SHOWWINDOW);
    }
    g_game_window_restore = GameWindowRestoreState();
}

// Covers the complete game client area so the HUD editor can draw handles at
// the exact spot the in-game HUD pass renders its previews.
static void position_hud_overlay() {
    // In OpenGL mode this HWND belongs to Minecraft, not a desktop overlay.
    if (g_embedded_mode || g_imgui_window == nullptr) return;
    HWND game = resolve_game_window();
    if (game == nullptr || IsIconic(game) || g_imgui_window == game) return;
    RECT client{};
    if (!GetClientRect(game, &client)) return;
    POINT top_left{client.left, client.top};
    if (!ClientToScreen(game, &top_left)) return;
    SetWindowPos(g_imgui_window, HWND_TOPMOST, top_left.x, top_left.y,
            client.right - client.left, client.bottom - client.top,
            SWP_NOACTIVATE | SWP_SHOWWINDOW);
}

static bool game_has_foreground(HWND game) {
    if (game == nullptr || IsIconic(game)) return false;
    HWND foreground = GetForegroundWindow();
    if (foreground == nullptr) return false;
    return foreground == game || IsChild(game, foreground)
            || GetAncestor(foreground, GA_ROOT) == game;
}

static void stage_hud_position(const HudElement &element, bool dragging) {
    std::lock_guard<std::mutex> lock(g_hud_mutex);
    HudPositionDraft &draft = g_hud_position_drafts[element.id];
    draft.x = element.x;
    draft.y = element.y;
    draft.dragging = dragging;
    for (HudElement &current : g_hud_elements) {
        if (current.id == element.id) {
            current.x = element.x;
            current.y = element.y;
            break;
        }
    }
}

static void queue_hud_move(const HudElement &element, float nx, float ny,
        bool commit = false) {
    MenuAction action;
    action.type = ActionType::HudMove;
    action.value_id = element.id;
    action.commit = commit;
    action.hud_x = nx;
    action.hud_y = ny;
    queue_action(std::move(action));
}

static void finish_hud_drag() {
    if (g_hud_drag_id.empty()) return;
    HudElement element;
    element.id = g_hud_drag_id;
    g_hud_drag_id.clear();
    {
        std::lock_guard<std::mutex> lock(g_hud_mutex);
        auto draft = g_hud_position_drafts.find(element.id);
        if (draft == g_hud_position_drafts.end()) return;
        draft->second.dragging = false;
        element.x = draft->second.x;
        element.y = draft->second.y;
    }
    queue_hud_move(element, element.x, element.y, true);
}

static void start_hud_edit() {
    g_hud_drag_id.clear();
    g_hud_edit = true;
    MenuAction action;
    action.type = ActionType::HudEdit;
    action.enabled = true;
    queue_action(std::move(action));
    position_hud_overlay();
}

static void stop_hud_edit(bool restore_menu_layout) {
    if (!g_hud_edit.exchange(false)) return;
    finish_hud_drag();
    MenuAction action;
    action.type = ActionType::HudEdit;
    action.enabled = false;
    queue_action(std::move(action));
    if (restore_menu_layout) position_overlay();
}

// Full-screen transparent editor: draws a labelled frame on each HUD element
// and moves it with the mouse. The in-game pass renders the actual preview.
static void render_hud_editor() {
    const ImVec2 display_size = ImGui::GetIO().DisplaySize;
    const float width = std::max(1.f, display_size.x);
    const float height = std::max(1.f, display_size.y);
    ImGui::SetNextWindowPos(ImVec2(0.f, 0.f));
    ImGui::SetNextWindowSize(ImVec2(width, height));
    ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(0.f, 0.f));
    ImGui::PushStyleColor(ImGuiCol_WindowBg, IM_COL32(0, 0, 0, 40));
    ImGui::Begin(SV_STR(SV_A9CEB4A0D02FD78B, "##hud_editor"), nullptr,
            ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove
            | ImGuiWindowFlags_NoScrollbar | ImGuiWindowFlags_NoScrollWithMouse
            | ImGuiWindowFlags_NoBringToFrontOnFocus);
    ImGui::PushFont(g_font_regular);
    ImDrawList *draw = ImGui::GetWindowDrawList();
    std::vector<HudElement> elements;
    {
        std::lock_guard<std::mutex> lock(g_hud_mutex);
        elements = g_hud_elements;
    }

    const bool released = ImGui::IsMouseReleased(ImGuiMouseButton_Left);

    for (int index = 0; index < static_cast<int>(elements.size()); ++index) {
        HudElement &element = elements[index];
        const ImVec2 min(element.x * width, element.y * height);
        const ImVec2 box_size(element.w * width, element.h * height);
        ImGui::SetCursorScreenPos(min);
        ImGui::PushID(element.id.c_str());
        ImGui::InvisibleButton(SV_STR(SV_00EC70BE719DFDA5, "##hud_handle"), box_size);
        const bool hovered = ImGui::IsItemHovered() || g_hud_drag_id == element.id;
        if (ImGui::IsItemActivated()) {
            g_hud_drag_id = element.id;
            const ImVec2 mouse = ImGui::GetIO().MousePos;
            g_hud_drag_offset = ImVec2(mouse.x - min.x, mouse.y - min.y);
            stage_hud_position(element, true);
        }
        const bool final_release = released && g_hud_drag_id == element.id;
        if ((ImGui::IsItemActive() && g_hud_drag_id == element.id)
                || final_release) {
            const ImVec2 mouse = ImGui::GetIO().MousePos;
            if (ImGui::IsMousePosValid(&mouse)) {
                element.x = ImClamp((mouse.x - g_hud_drag_offset.x) / width,
                        0.f, std::max(0.f, 1.f - element.w));
                element.y = ImClamp((mouse.y - g_hud_drag_offset.y) / height,
                        0.f, std::max(0.f, 1.f - element.h));
            }
            stage_hud_position(element, !released);
            // The Java HUD reads this draft every frame through JNI. Only the
            // final release needs the HTTP/profile-save path.
            if (released) queue_hud_move(element, element.x, element.y, true);
            if (released) g_hud_drag_id.clear();
        }
        ImGui::PopID();
        const ImVec2 drawn_min(element.x * width, element.y * height);
        const ImVec2 drawn_max(drawn_min.x + element.w * width,
                drawn_min.y + element.h * height);
        const ImU32 fill = hovered ? IM_COL32(255, 72, 78, 36)
                : IM_COL32(255, 255, 255, 18);
        const ImU32 border = hovered ? IM_COL32(255, 72, 78, 220)
                : IM_COL32(255, 255, 255, 110);
        draw->AddRectFilled(drawn_min, drawn_max, fill, 6.f);
        draw->AddRect(drawn_min, drawn_max, border, 6.f, 0, 1.5f);
        ImFont *font = g_font_micro != nullptr ? g_font_micro : g_font_regular;
        const ImVec2 label_size = font->CalcTextSizeA(11.f, FLT_MAX, 0.f,
                element.label.c_str());
        draw->AddRectFilled(ImVec2(drawn_min.x, drawn_min.y - 16.f),
                ImVec2(drawn_min.x + label_size.x + 10.f, drawn_min.y),
                IM_COL32(8, 8, 8, 220), 4.f);
        draw->AddText(font, 11.f, ImVec2(drawn_min.x + 5.f,
                drawn_min.y - 14.f), IM_COL32(242, 242, 244, 255),
                element.label.c_str());
    }
    const char *hint = SV_STR(SV_304F89C80C98794B, "Drag modules to move them  |  ESC or Done to finish");
    ImFont *font = g_font_regular != nullptr ? g_font_regular : ImGui::GetFont();
    const ImVec2 hint_size = font->CalcTextSizeA(13.f, FLT_MAX, 0.f, hint);
    const float hint_x = (width - hint_size.x) * .5f;
    draw->AddRectFilled(ImVec2(hint_x - 12.f, height - 46.f),
            ImVec2(hint_x + hint_size.x + 12.f, height - 14.f),
            IM_COL32(8, 8, 8, 230), 8.f);
    draw->AddText(font, 13.f, ImVec2(hint_x, height - 42.f),
            IM_COL32(242, 242, 244, 255), hint);
    ImGui::SetCursorScreenPos(ImVec2(width - 96.f, height - 48.f));
    if (ImGui::Button(SV_STR(SV_E784FED226887EE9, "Done"), ImVec2(80.f, 30.f)))
        stop_hud_edit(true);
    ImGui::PopFont();
    ImGui::End();
    ImGui::PopStyleColor();
    ImGui::PopStyleVar();
}

static void position_overlay() {
    if (g_embedded_mode || g_imgui_window == nullptr) return;
    if (g_hud_edit.load()) {
        position_hud_overlay();
        return;
    }
    RECT bounds{};
    HWND game = resolve_game_window();
    if (g_imgui_window == game) return;
    bool usable_game_bounds = game != nullptr && !IsIconic(game)
            && GetWindowRect(game, &bounds) != FALSE;
    HMONITOR monitor = MonitorFromWindow(
            usable_game_bounds ? game : GetForegroundWindow(),
            MONITOR_DEFAULTTOPRIMARY);
    MONITORINFO info{};
    info.cbSize = sizeof(info);
    GetMonitorInfoW(monitor, &info);
    RECT work = info.rcWork;
    if (!usable_game_bounds) bounds = work;
    const LONG available_width = usable_game_bounds
            ? std::min<LONG>(work.right - work.left, bounds.right - bounds.left)
            : work.right - work.left;
    const LONG available_height = usable_game_bounds
            ? std::min<LONG>(work.bottom - work.top, bounds.bottom - bounds.top)
            : work.bottom - work.top;
    // Scale against the actual Minecraft window, not only the desktop. This
    // keeps the same proportions in windowed, borderless and fullscreen use.
    g_menu_scale = std::min(1.f, std::min(
            available_width * 0.84f / kMenuWidth,
            available_height * 0.84f / kMenuHeight));
    const int width = std::max(1, static_cast<int>(kMenuWidth * g_menu_scale));
    const int height = std::max(1, static_cast<int>(kMenuHeight * g_menu_scale));
    LONG x = bounds.left + ((bounds.right - bounds.left) - width) / 2;
    LONG y = bounds.top + ((bounds.bottom - bounds.top) - height) / 2;
    x = std::max<LONG>(work.left,
            std::min<LONG>(x, work.right - width));
    y = std::max<LONG>(work.top,
            std::min<LONG>(y, work.bottom - height));
    SetWindowPos(g_imgui_window, HWND_TOPMOST, x, y, width, height,
            SWP_NOACTIVATE);
}

static LRESULT WINAPI overlay_window_proc(HWND window, UINT message,
        WPARAM wparam, LPARAM lparam) {
    if (g_imgui_context != nullptr) {
        ImGui::SetCurrentContext(g_imgui_context);
        if (g_capture_kind.load() != 0) {
            int key = message_bind_code(message, wparam, lparam);
            if (key != 0 && handle_bind_capture_key(key))
                return 0;
        }
        if ((message == WM_KEYDOWN || message == WM_SYSKEYDOWN)
                && static_cast<int>(wparam) == g_suppressed_bind_key.load()
                && GetTickCount64() < g_suppressed_bind_key_until.load())
            return 0;
        if (ImGui_ImplWin32_WndProcHandler(window, message, wparam, lparam))
            return 0;
    }
    switch (message) {
        case WM_DISPLAYCHANGE:
            position_overlay();
            return 0;
        case WM_KEYDOWN:
        case WM_SYSKEYDOWN:
            if (wparam == VK_ESCAPE) {
                if (g_hud_edit.load()) {
                    stop_hud_edit(true);
                    return 0;
                }
                hide_menu();
                return 0;
            }
            break;
        case WM_SIZE:
            if (g_device != nullptr && wparam != SIZE_MINIMIZED) {
                cleanup_render_target();
                g_swap_chain->ResizeBuffers(0, LOWORD(lparam), HIWORD(lparam),
                        DXGI_FORMAT_UNKNOWN, 0);
                create_render_target();
            }
            return 0;
        case WM_SYSCOMMAND:
            if ((wparam & 0xfff0) == SC_KEYMENU) return 0;
            break;
        case WM_CLOSE:
            hide_menu();
            return 0;
        case WM_DESTROY:
            g_ui_running = false;
            PostQuitMessage(0);
            return 0;
        default:
            break;
    }
    return DefWindowProcW(window, message, wparam, lparam);
}

static void overlay_thread_main() {
    HINSTANCE instance = GetModuleHandleW(nullptr);
    const wchar_t *class_name = SV_WSTR(SV_4FF0AFB1B9714EC5, L"CrimsonClientOverlay");
    WNDCLASSEXW window_class{};
    window_class.cbSize = sizeof(window_class);
    window_class.style = CS_CLASSDC;
    window_class.lpfnWndProc = overlay_window_proc;
    window_class.hInstance = instance;
    window_class.hCursor = LoadCursorW(nullptr, MAKEINTRESOURCEW(32512));
    window_class.lpszClassName = class_name;
    if (!RegisterClassExW(&window_class)
            && GetLastError() != ERROR_CLASS_ALREADY_EXISTS) {
        g_ui_running = false;
        g_ui_started = false;
        return;
    }
    g_imgui_window = CreateWindowExW(WS_EX_TOPMOST | WS_EX_TOOLWINDOW
                    | WS_EX_NOACTIVATE,
            class_name, SV_WSTR(SV_859042FE7328DFEB, L"Crimson"), WS_POPUP, 0, 0, kMenuWidth, kMenuHeight,
            nullptr, nullptr, instance, nullptr);
    if (g_imgui_window == nullptr || !create_d3d_device(g_imgui_window)) {
        if (g_imgui_window != nullptr) DestroyWindow(g_imgui_window);
        g_imgui_window = nullptr;
        cleanup_d3d_device();
        UnregisterClassW(class_name, instance);
        g_ui_running = false;
        g_ui_started = false;
        return;
    }

    MARGINS margins{-1};
    DwmExtendFrameIntoClientArea(g_imgui_window, &margins);
    g_stream_mode = SetWindowDisplayAffinity(g_imgui_window, 0x00000011) != FALSE;
    position_overlay();
    ShowWindow(g_imgui_window, SW_HIDE);

    g_imgui_context = ImGui::CreateContext();
    ImGui::SetCurrentContext(g_imgui_context);
    initialize_style();
    ImGui_ImplWin32_Init(g_imgui_window);
    ImGui_ImplDX11_Init(g_device, g_device_context);
    install_game_input_forwarder();
    g_ui_running = true;
    ULONGLONG next_position_check = 0;
    ULONGLONG next_input_hook_check = 0;

    while (!g_stop.load() && g_ui_running.load()) {
        MSG message;
        while (PeekMessageW(&message, nullptr, 0, 0, PM_REMOVE)) {
            if (message.message == WM_QUIT) {
                g_ui_running = false;
                break;
            }
            if (should_translate_message(message)) TranslateMessage(&message);
            DispatchMessageW(&message);
        }
        if (!g_ui_running.load()) break;

        // LWJGL can recreate its window after a fullscreen/context reset.
        // Re-resolve it periodically so keyboard forwarding does not remain
        // attached to an old HWND after the menu has been open for a while.
        const ULONGLONG hook_now = GetTickCount64();
        if (hook_now >= next_input_hook_check) {
            install_game_input_forwarder();
            next_input_hook_check = hook_now + 500;
        }

        if (g_visible.load() && g_capture_kind.load() != 0)
            poll_bind_capture();

        if (g_endpoint_ready.load()) {
            int key = g_gui_bind.load();
            bool down = is_key_down(key);
            ULONGLONG now = GetTickCount64();
            bool text_input_active = false;
            if (g_imgui_context != nullptr) {
                ImGui::SetCurrentContext(g_imgui_context);
                text_input_active = ImGui::GetIO().WantTextInput;
            }
            if (down && !g_hotkey_was_down && g_capture_kind.load() == 0
                    && !text_input_active
                    && now >= g_hotkey_suppressed_until.load()) {
                if (!g_visible.load()) position_overlay();
                toggle_menu();
            }
            g_hotkey_was_down = down;
        }

        if (!g_visible.load()) {
            Sleep(10);
            continue;
        }
        HWND game_window = resolve_game_window();
        if (!game_has_foreground(game_window)) {
            finish_hud_drag();
            if (!g_overlay_hidden_by_focus && g_imgui_window != nullptr)
                ShowWindow(g_imgui_window, SW_HIDE);
            g_overlay_hidden_by_focus = true;
            Sleep(10);
            continue;
        }
        if (g_overlay_hidden_by_focus) {
            position_overlay();
            if (g_imgui_window != nullptr)
                ShowWindow(g_imgui_window, SW_SHOWNOACTIVATE);
            g_overlay_hidden_by_focus = false;
            next_position_check = 0;
        }
        const ULONGLONG position_now = GetTickCount64();
        if (position_now >= next_position_check) {
            position_overlay();
            next_position_check = position_now + 500;
        }
        ImGui::SetCurrentContext(g_imgui_context);
        ImGui_ImplDX11_NewFrame();
        ImGui_ImplWin32_NewFrame();
        ImGuiIO &io = ImGui::GetIO();
        io.AddFocusEvent(true);
        const ImVec2 pixel_size = io.DisplaySize;
        if (pixel_size.x <= 0.f || pixel_size.y <= 0.f) {
            Sleep(10);
            continue;
        }
        const bool full_screen_editor = g_hud_edit.load();
        const ImVec2 scale = full_screen_editor ? ImVec2(1.f, 1.f)
                : ImVec2(pixel_size.x / kMenuWidth,
                        pixel_size.y / kMenuHeight);
        io.DisplaySize = full_screen_editor ? pixel_size
                : ImVec2(static_cast<float>(kMenuWidth),
                        static_cast<float>(kMenuHeight));
        if (!full_screen_editor) {
            for (ImGuiInputEvent &event : g_imgui_context->InputEventsQueue) {
                if (event.Type == ImGuiInputEventType_MousePos) {
                    event.MousePos.PosX /= scale.x;
                    event.MousePos.PosY /= scale.y;
                }
            }
        }
        ImGui::NewFrame();
        // Deferred input events stay in physical coordinates until the next frame.
        if (!full_screen_editor) {
            for (ImGuiInputEvent &event : g_imgui_context->InputEventsQueue) {
                if (event.Type == ImGuiInputEventType_MousePos) {
                    event.MousePos.PosX *= scale.x;
                    event.MousePos.PosY *= scale.y;
                }
            }
        }
        render_menu_frame(g_imgui_window);
        ImGui::Render();
        ImDrawData *draw_data = ImGui::GetDrawData();
        if (!full_screen_editor) {
            for (int index = 0; index < draw_data->CmdListsCount; ++index) {
                for (ImDrawVert &vertex : draw_data->CmdLists[index]->VtxBuffer) {
                    vertex.pos.x *= scale.x;
                    vertex.pos.y *= scale.y;
                }
            }
            draw_data->ScaleClipRects(scale);
        }
        draw_data->DisplaySize = pixel_size;
        const float clear_color[4] = {0.f, 0.f, 0.f, 0.f};
        g_device_context->OMSetRenderTargets(1, &g_render_target, nullptr);
        g_device_context->ClearRenderTargetView(g_render_target, clear_color);
        ImGui_ImplDX11_RenderDrawData(ImGui::GetDrawData());
        g_swap_chain->Present(1, 0);
    }

    hide_menu();
    uninstall_game_input_forwarder();
    ImGui::SetCurrentContext(g_imgui_context);
    ImGui_ImplDX11_Shutdown();
    ImGui_ImplWin32_Shutdown();
    ImGui::DestroyContext(g_imgui_context);
    g_imgui_context = nullptr;
    cleanup_d3d_device();
    if (g_imgui_window != nullptr) DestroyWindow(g_imgui_window);
    g_imgui_window = nullptr;
    UnregisterClassW(class_name, instance);
    g_ui_running = false;
    g_ui_started = false;
}

static bool initialize_embedded_opengl(HWND window) {
    HGLRC current_context = wglGetCurrentContext();
    if (current_context == nullptr || window == nullptr) return false;

    if (g_imgui_context == nullptr) {
        g_imgui_context = ImGui::CreateContext();
        if (g_imgui_context == nullptr) return false;
        ImGui::SetCurrentContext(g_imgui_context);
        initialize_style();
    } else {
        ImGui::SetCurrentContext(g_imgui_context);
    }

    if (g_imgui_opengl_initialized
            && (g_imgui_gl_context != current_context
                    || g_imgui_window != window)) {
        ImGui_ImplOpenGL3_Shutdown();
        ImGui_ImplWin32_Shutdown();
        g_imgui_opengl_initialized = false;
    }
    if (!g_imgui_opengl_initialized) {
        if (!ImGui_ImplWin32_InitForOpenGL(window)
                || !ImGui_ImplOpenGL3_Init(
                        SV_STR(SV_0BB968A6101D71FB, "#version 130"))) {
            if (ImGui::GetIO().BackendPlatformUserData != nullptr)
                ImGui_ImplWin32_Shutdown();
            return false;
        }
        g_imgui_gl_context = current_context;
        g_imgui_window = window;
        g_embedded_mode = true;
        g_imgui_opengl_initialized = true;
    }
    return true;
}

static bool render_embedded_opengl(HWND requested_window) {
    HDC device_context = wglGetCurrentDC();
    HWND window = requested_window;
    if (window == nullptr && device_context != nullptr)
        window = WindowFromDC(device_context);
    if (window == nullptr) window = resolve_game_window();

    std::lock_guard<std::mutex> render_lock(g_imgui_mutex);
    if (!initialize_embedded_opengl(window)) return false;
    if (!g_visible.load()) return true;

    ImGui::SetCurrentContext(g_imgui_context);
    ImGui_ImplOpenGL3_NewFrame();
    ImGui_ImplWin32_NewFrame();
    ImGuiIO &io = ImGui::GetIO();
    const bool focused = game_has_foreground(window);
    io.AddFocusEvent(focused);
    if (!focused) {
        finish_hud_drag();
        return true;
    }
    io.MouseDrawCursor = true;

    const ImVec2 client_size = io.DisplaySize;
    if (client_size.x <= 0.f || client_size.y <= 0.f) return false;
    GLint viewport[4]{0, 0, 0, 0};
    glGetIntegerv(GL_VIEWPORT, viewport);
    const ImVec2 pixel_size(
            viewport[2] > 0 ? static_cast<float>(viewport[2]) : client_size.x,
            viewport[3] > 0 ? static_cast<float>(viewport[3]) : client_size.y);
    const float client_to_pixel_x = pixel_size.x / client_size.x;
    const float client_to_pixel_y = pixel_size.y / client_size.y;
    const bool full_screen_editor = g_hud_edit.load();
    if (full_screen_editor) {
        io.DisplaySize = pixel_size;
        for (ImGuiInputEvent &event : g_imgui_context->InputEventsQueue) {
            if (event.Type == ImGuiInputEventType_MousePos) {
                event.MousePos.PosX *= client_to_pixel_x;
                event.MousePos.PosY *= client_to_pixel_y;
            }
        }
        ImGui::NewFrame();
        for (ImGuiInputEvent &event : g_imgui_context->InputEventsQueue) {
            if (event.Type == ImGuiInputEventType_MousePos) {
                event.MousePos.PosX /= client_to_pixel_x;
                event.MousePos.PosY /= client_to_pixel_y;
            }
        }
        render_menu_frame(window);
        ImGui::Render();
        ImDrawData *draw_data = ImGui::GetDrawData();
        draw_data->DisplaySize = pixel_size;
        draw_data->FramebufferScale = ImVec2(1.f, 1.f);
        ImGui_ImplOpenGL3_RenderDrawData(draw_data);
        return true;
    }
    const float margin_x = 20.f * client_to_pixel_x;
    const float margin_y = 20.f * client_to_pixel_y;
    const float scale = std::max(0.05f, std::min(1.f, std::min(
            (pixel_size.x - margin_x) / static_cast<float>(kMenuWidth),
            (pixel_size.y - margin_y) / static_cast<float>(kMenuHeight))));
    const ImVec2 offset(
            (pixel_size.x - static_cast<float>(kMenuWidth) * scale) * .5f,
            (pixel_size.y - static_cast<float>(kMenuHeight) * scale) * .5f);
    io.DisplaySize = ImVec2(static_cast<float>(kMenuWidth),
            static_cast<float>(kMenuHeight));
    for (ImGuiInputEvent &event : g_imgui_context->InputEventsQueue) {
        if (event.Type == ImGuiInputEventType_MousePos) {
            event.MousePos.PosX = (event.MousePos.PosX * client_to_pixel_x
                    - offset.x) / scale;
            event.MousePos.PosY = (event.MousePos.PosY * client_to_pixel_y
                    - offset.y) / scale;
        }
    }

    ImGui::NewFrame();
    // ImGui may defer a mouse event to preserve click/down/up ordering. Put
    // unconsumed events back into Win32 client coordinates so the next frame
    // cannot apply the logical-menu transform to the same event twice.
    for (ImGuiInputEvent &event : g_imgui_context->InputEventsQueue) {
        if (event.Type == ImGuiInputEventType_MousePos) {
            event.MousePos.PosX = (event.MousePos.PosX * scale + offset.x)
                    / client_to_pixel_x;
            event.MousePos.PosY = (event.MousePos.PosY * scale + offset.y)
                    / client_to_pixel_y;
        }
    }
    render_menu_frame(window);
    ImGui::Render();

    ImDrawData *draw_data = ImGui::GetDrawData();
    for (int index = 0; index < draw_data->CmdListsCount; ++index) {
        for (ImDrawVert &vertex : draw_data->CmdLists[index]->VtxBuffer) {
            vertex.pos.x = vertex.pos.x * scale + offset.x;
            vertex.pos.y = vertex.pos.y * scale + offset.y;
        }
        for (ImDrawCmd &command : draw_data->CmdLists[index]->CmdBuffer) {
            command.ClipRect.x = command.ClipRect.x * scale + offset.x;
            command.ClipRect.y = command.ClipRect.y * scale + offset.y;
            command.ClipRect.z = command.ClipRect.z * scale + offset.x;
            command.ClipRect.w = command.ClipRect.w * scale + offset.y;
        }
    }
    draw_data->DisplaySize = pixel_size;
    draw_data->FramebufferScale = ImVec2(1.f, 1.f);
    ImGui_ImplOpenGL3_RenderDrawData(draw_data);
    return true;
}

} // namespace

void sonyvegas_module_menu_set_visibility_callback(
        sonyvegas_module_menu_visibility_callback callback) {
    g_visibility_callback = callback;
}

BOOL sonyvegas_module_menu_is_visible(void) {
    return g_visible.load() ? TRUE : FALSE;
}

BOOL sonyvegas_module_menu_set_visible(BOOL visible) {
    const bool next = visible != FALSE;
    if (!next) {
        // Keep the Java bridge and the standalone renderer in sync.  Merely
        // clearing g_visible left a standalone ImGui window on screen and
        // skipped restoration of the game window.
        hide_menu();
        return TRUE;
    }
    const bool previous = g_visible.exchange(true);
    if (previous) return TRUE;
    sonyvegas_module_menu_visibility_callback callback =
            g_visibility_callback.load();
    if (callback != nullptr) callback(TRUE);
    return TRUE;
}

BOOL sonyvegas_module_menu_render_opengl(HWND window) {
    return render_embedded_opengl(window) ? TRUE : FALSE;
}

unsigned sonyvegas_module_menu_hud_positions(float positions[6]) {
    if (positions == nullptr) return 0;
    const char *ids[] = {SV_STR(SV_890073FDF1BFBFD7, "h0"), SV_STR(SV_0C5BB63E540D878B, "h1"), SV_STR(SV_7ADFF5874F6552B9, "h2")};
    std::lock_guard<std::mutex> lock(g_hud_mutex);
    unsigned flags = g_hud_edit.load() ? 8u : 0u;
    for (unsigned index = 0; index < 3u; ++index) {
        auto draft = g_hud_position_drafts.find(ids[index]);
        if (draft == g_hud_position_drafts.end()) continue;
        positions[index * 2u] = draft->second.x;
        positions[index * 2u + 1u] = draft->second.y;
        flags |= 1u << index;
    }
    return flags;
}

BOOL sonyvegas_module_menu_handle_message(HWND window, UINT message,
        WPARAM wparam, LPARAM lparam) {
    if (!g_visible.load() || g_imgui_context == nullptr
            || !g_imgui_opengl_initialized) return FALSE;
    // Bind capture owns the next physical key. Handle it before touching the
    // render mutex so Escape always cancels the capture and never reaches the
    // Java menu-close path, even if a frame is being submitted concurrently.
    if (g_capture_kind.load() != 0) {
        const int key = message_bind_code(message, wparam, lparam);
        if (key != 0 && handle_bind_capture_key(key)) return TRUE;
    }
    std::unique_lock<std::mutex> render_lock(g_imgui_mutex, std::try_to_lock);
    if (!render_lock.owns_lock()) return FALSE;
    ImGui::SetCurrentContext(g_imgui_context);
    if (message == WM_KEYDOWN || message == WM_SYSKEYDOWN) {
        const int key = message_bind_code(message, wparam, lparam);
        // The menu key and Escape belong to ClientSettings unless a bind was
        // captured above. This also distinguishes RSHIFT from LSHIFT by scan
        // code after an input field has captured keyboard focus.
        if (key == g_gui_bind.load() || key == VK_ESCAPE) return FALSE;
    }
    const bool handled = ImGui_ImplWin32_WndProcHandler(
            window, message, wparam, lparam) != 0;
    const ImGuiIO &io = ImGui::GetIO();
    const bool mouse_message = is_game_mouse_message(message);
    const bool keyboard_message = is_forwarded_keyboard_message(message);
    return (handled || mouse_message && io.WantCaptureMouse
            || keyboard_message && io.WantCaptureKeyboard) ? TRUE : FALSE;
}

void sonyvegas_module_menu_accept_bootstrap(const unsigned char *data,
        size_t length) {
    accept_bootstrap(data, length);
    std::lock_guard<std::mutex> lock(g_lifecycle_mutex);
    if (g_stop.load()) return;
    if (!g_worker_started.exchange(true)) {
        g_worker_running = true;
        try {
            std::thread(worker_loop).detach();
        } catch (...) {
            g_worker_running = false;
            g_worker_started = false;
        }
    }
    if (!g_head_worker_started.exchange(true)) {
        g_head_worker_running = true;
        try {
            std::thread(player_head_worker_loop).detach();
        } catch (...) {
            g_head_worker_running = false;
            g_head_worker_started = false;
        }
    }
    // Rendering now stays on Minecraft's Java/OpenGL render thread. The
    // transport workers remain available, but no desktop overlay is created.
}

void sonyvegas_module_menu_shutdown(void) {
    // Close synchronously while the Java callback is still registered. This
    // restores a fullscreen session before bridge references are released.
    hide_menu();
    {
        std::lock_guard<std::mutex> lock(g_lifecycle_mutex);
        g_stop = true;
    }
    for (int attempt = 0; attempt < 300
            && (g_worker_running.load() || g_head_worker_running.load()
                    || g_ui_running.load()); ++attempt) {
        Sleep(10);
    }
}
